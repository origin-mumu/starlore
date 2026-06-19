"""游客 AI 路由：配额、发散、对话。"""

import logging
import json
import httpx
from datetime import date
import threading
from typing import List, Dict, Any

from fastapi import APIRouter, Depends, Request
from fastapi.responses import JSONResponse
from sqlalchemy import select, or_
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models.article import Article
from app.services import ai_config_service, ai_stream_service
from app.services.article_embedding_service import search_similar_public

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/public/ai", tags=["public-ai"])

# In-memory IP based rate limiting
_ip_request_counts = {}
_ip_last_request_dates = {}
_quota_lock = threading.Lock()

CHARACTER_SYSTEM_PROMPTS = {
    "default": "你是一个名叫'星轮助手'的智能助理。你的态度和蔼、专业，对文学、科幻和本站(Starlore)的技术架构及内容有着深入的了解。",
    "philosopher": "你现在扮演哲学家庄子，说话充满道家智慧和哲理，善用寓言，物我两忘，逍遥自在，常以'吾'自称。",
    "explorer": "你现在扮演一名银河探索者，是一个热衷于探索未知星域 of 科幻领航员，说话带有机甲、星域、探索的科幻色彩。"
}

DIVERGE_SYSTEM_PROMPT = "你是一个头脑风暴创意联想助手。能够根据用户输入的词语，向外扩散联想出与之强关联的 8 个最典型、最生动、最具画面感的事物或概念。你必须严格以 JSON 数组形式返回结果，不能包含任何其他 Markdown 语法或额外解释。"


def get_client_ip(request: Request) -> str:
    for header in ["X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP"]:
        ip = request.headers.get(header)
        if ip and ip.lower() != "unknown":
            if "," in ip:
                return ip.split(",")[0].strip()
            return ip.strip()
    return request.client.host if request.client else "127.0.0.1"


def check_and_consume_quota(ip: str, consume: bool = True) -> int:
    today = date.today()
    with _quota_lock:
        last_date = _ip_last_request_dates.get(ip)
        if last_date != today:
            _ip_last_request_dates[ip] = today
            _ip_request_counts[ip] = 0

        current_count = _ip_request_counts[ip]
        if consume:
            if current_count >= 20:
                return -1
            _ip_request_counts[ip] += 1
            return _ip_request_counts[ip]
        else:
            return current_count


@router.get("/guest-quota")
async def get_guest_quota(request: Request):
    ip = get_client_ip(request)
    count = check_and_consume_quota(ip, consume=False)
    remaining = max(0, 20 - count)
    return {"limit": 20, "remaining": remaining}


@router.post("/diverge")
async def guest_diverge(
    request: Request,
    db: AsyncSession = Depends(get_db),
):
    body = await request.json()
    word = body.get("word")
    if not word:
        return JSONResponse(status_code=400, content={"error": "请输入一个词"})

    ip = get_client_ip(request)
    count = check_and_consume_quota(ip, consume=True)
    if count == -1:
        return JSONResponse(
            status_code=429,
            content={"message": "今日访客体验额度（20次）已用尽，登录后即可享受无限次数与专属 Agent 服务哦！"}
        )
    remaining = max(0, 20 - count)

    # 优先从数据库读取配置
    config = await ai_config_service.get_config_by_key(db, "deepseek-chat")
    if config and config.enabled and config.apiKey:
        base_url = config.apiUrl
        api_key = config.apiKey
        model_id = config.modelId
    else:
        from app.config import settings
        base_url = settings.ai_base_url
        api_key = settings.ai_api_key
        model_id = settings.ai_model

    if not api_key:
        return JSONResponse(status_code=503, content={"error": "AI 服务未配置"})

    api_url = base_url.rstrip("/") + "/chat/completions"

    prompt = f"""请输入词为："{word}"。请围绕它向外联想 8 个关联词语。

要求：
1. 每个联想词必须与输入词有强烈的直接关联，逻辑必须合乎常理、生动逼真（例如对于食物或动作，应联想相关器具、食材、流派、场景或直接相关联想词，避免生硬死板地套用无关概念）。
2. 每个联想词包含 zh（中文）和 en（英文翻译），必须使用 JSON 格式表示，例如：[{{"zh":"火锅","en":"hotpot"}}, ...]。
3. 严禁返回任何 Markdown 代码块包裹（如 ```json）或多余的文字说明，仅返回纯粹的 JSON 数组。"""

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {api_key}",
    }

    payload = {
        "model": model_id,
        "messages": [
            {"role": "system", "content": DIVERGE_SYSTEM_PROMPT},
            {"role": "user", "content": prompt}
        ],
        "stream": False
    }

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            resp = await client.post(api_url, json=payload, headers=headers)
            resp.raise_for_status()
            data = resp.json()
            content = data.get("choices", [{}])[0].get("message", {}).get("content", "").strip()

            from app.routers.ai import extract_json_array
            json_str = extract_json_array(content)
            pairs = []
            if json_str:
                try:
                    parsed = json.loads(json_str)
                    if isinstance(parsed, list):
                        for item in parsed:
                            if isinstance(item, dict):
                                pairs.append({
                                    "en": str(item.get("en", "")),
                                    "zh": str(item.get("zh", ""))
                                })
                except Exception:
                    pass

            if not pairs:
                return JSONResponse(status_code=500, content={"error": "AI 返回格式解析失败"})

            return {"pairs": pairs, "remaining": remaining}
    except Exception as e:
        logger.error("Guest diverge error: %s", e)
        return JSONResponse(status_code=500, content={"error": f"Server error: {e}"})


@router.post("/guest-chat")
async def guest_chat(
    request: Request,
    db: AsyncSession = Depends(get_db),
):
    body = await request.json()
    message = body.get("message", "")
    if not message:
        return JSONResponse(status_code=400, content={"error": "消息不能为空"})

    ip = get_client_ip(request)
    count = check_and_consume_quota(ip, consume=True)
    if count == -1:
        return JSONResponse(
            status_code=429,
            content={"message": "今日访客体验额度（20次）已用尽，登录后即可享受无限次数与专属 Agent 服务哦！"}
        )
    remaining = max(0, 20 - count)

    try:
        # 1. 语义搜索公开知识库文章 (RAG)
        matched_articles = []
        try:
            matched_articles = await search_similar_public(db, message, 5)
        except Exception as e:
            logger.warning("Guest RAG semantic search failed, falling back: %s", e)

        if not matched_articles:
            # 回退到数据库关键字搜索
            result = await db.execute(
                select(Article)
                .where(
                    Article.status == "published",
                    Article.is_public == True,
                    or_(
                        Article.title.ilike(f"%{message}%"),
                        Article.description.ilike(f"%{message}%")
                    )
                )
                .order_by(Article.createdAt.desc())
                .limit(5)
            )
            matched_articles = list(result.scalars().all())

        # 2. 格式化上下文
        context_str = ""
        if matched_articles:
            context_str += "\n\n[参考公开知识库内容]\n以下是与用户提问相关的公开文章内容：\n"
            for a in matched_articles:
                context_str += f"--- \n文章标题：《{a.title}》\n分类：{a.category or ''}\n摘要：{a.description or ''}\n"
                if a.content:
                    import re
                    plain = re.sub(r"<[^>]+>", "", a.content)
                    plain = re.sub(r"\s+", " ", plain).strip()
                    if len(plain) > 500:
                        plain = plain[:500] + "..."
                    context_str += f"内容详情: {plain}\n"
            context_str += "\n在回答用户关于本站文章、星野分类或技术栈等问题时，请优先使用上述参考公开文章的内容作为事实根据。"

        # 3. 构建 System Prompt
        character_key = body.get("character", "default")
        char_system = CHARACTER_SYSTEM_PROMPTS.get(character_key, CHARACTER_SYSTEM_PROMPTS["default"])

        final_system_prompt = char_system + context_str + \
            "\n\n注意：你目前正在以'访客体验模式'与用户对话。用户每天有20次真实的AI对话额度。在回答完后，如果合适，请友好地提醒用户：'您可以随时登录，以解锁完整的个人云端空间、多Agent团队协作以及更高级的深度模型流式对话体验！'"

        # 4. 解析多轮历史
        api_messages = [{"role": "system", "content": final_system_prompt}]
        history = body.get("history") or []
        for hist_msg in history:
            role = hist_msg.get("role")
            content = hist_msg.get("content")
            if role and content and content.strip():
                api_messages.append({"role": role, "content": content})
        api_messages.append({"role": "user", "content": message})

        # 5. 优先使用 deepseek-v4-flash 或第一个可用的配置，或者 fallback 默认
        config = await ai_config_service.get_config_by_key(db, "deepseek-v4-flash")
        if not (config and config.enabled and config.apiKey):
            config = await ai_config_service.get_config_by_key(db, "deepseek-chat")

        if config and config.enabled and config.apiKey:
            base_url = config.apiUrl
            api_key = config.apiKey
            model_id = config.modelId
        else:
            from app.config import settings
            base_url = settings.ai_base_url
            api_key = settings.ai_api_key
            model_id = settings.ai_model

        if not api_key:
            return JSONResponse(status_code=503, content={"error": "AI 服务未配置"})

        api_url = base_url.rstrip("/") + "/chat/completions"

        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {api_key}",
        }

        payload = {
            "model": model_id,
            "messages": api_messages,
            "stream": False
        }

        async with httpx.AsyncClient(timeout=45.0) as client:
            resp = await client.post(api_url, json=payload, headers=headers)
            if resp.status_code != 200:
                logger.error("Guest AI Chat API error: status=%d, body=%s", resp.status_code, resp.text)
                return JSONResponse(status_code=502, content={"error": f"AI service error: HTTP {resp.status_code}"})

            resp_data = resp.json()
            message_node = resp_data.get("choices", [{}])[0].get("message", {})
            reply_content = message_node.get("content", "")
            reasoning_content = message_node.get("reasoning_content", "")

            return {
                "content": reply_content,
                "reasoningContent": reasoning_content,
                "remaining": remaining
            }

    except Exception as e:
        logger.error("Guest chat error: %s", e)
        return JSONResponse(status_code=500, content={"error": f"Server error: {e}"})
