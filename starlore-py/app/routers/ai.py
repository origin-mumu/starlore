"""AI 路由：对话、会话管理、模型列表、角色卡、配额。"""

import json
import logging

from fastapi import APIRouter, Depends, Query, Request
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.schemas.ai import (
    AICharacterCardsResponse,
    AIMessageListResponse,
    AIModelsResponse,
    AISessionListResponse,
    AISessionResponse,
    AppendPairRequest,
    CreateSessionRequest,
    UpdateSessionRequest,
)
from app.schemas.common import SimpleResponse
from app.services import ai_service, ai_stream_service, ai_quota_service

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/ai", tags=["ai"])


# ---------- 模型 & 角色卡 ----------

@router.get("/models", response_model=AIModelsResponse)
async def get_models(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    models = await ai_service.get_models(db)
    return AIModelsResponse(success=True, models=models)


@router.get("/character-cards", response_model=AICharacterCardsResponse)
async def get_character_cards(user: User = Depends(get_current_user)):
    cards = ai_service.get_character_cards()
    return AICharacterCardsResponse(success=True, cards=cards)


# ---------- SSE 流式对话（统一入口） ----------

def _sse_response(db: AsyncSession, model: str, messages: list[dict]) -> StreamingResponse:
    """创建 SSE 流式响应的辅助函数。"""
    return StreamingResponse(
        ai_stream_service.stream_chat(db, model, messages),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


@router.get("/sse")
async def sse_get(
    model: str = Query("deepseek-chat"),
    messages: str = Query("[]"),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    try:
        msg_list = json.loads(messages)
    except Exception:
        msg_list = []
    return _sse_response(db, model, msg_list)


@router.post("/sse")
async def sse_post(
    request: Request,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    body = await request.json()
    model = body.get("model", "deepseek-chat")
    messages = body.get("messages", [])
    return _sse_response(db, model, messages)


@router.post("/thinking-sse")
async def thinking_sse(
    request: Request,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    body = await request.json()
    model = body.get("model", "deepseek-reasoner")
    messages = body.get("messages", [])
    return _sse_response(db, model, messages)


@router.post("/agent-sse")
async def agent_sse(
    request: Request,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    body = await request.json()
    model = body.get("model", "deepseek-chat")
    messages = body.get("messages", [])
    return _sse_response(db, model, messages)


# ---------- 图片分析 ----------

@router.post("/analyze-image")
async def analyze_image(
    request: Request,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    body = await request.json()
    model = body.get("model", "mimo")
    messages = body.get("messages", [])
    # 非流式调用
    content = await ai_stream_service.invoke_model(db, model, messages)
    return {"content": content}


@router.post("/analyze-image/stream")
async def analyze_image_stream(
    request: Request,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    body = await request.json()
    model = body.get("model", "mimo")
    messages = body.get("messages", [])
    return _sse_response(db, model, messages)


# ---------- TTS ----------

@router.post("/tts")
async def text_to_speech(
    request: Request,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    from fastapi.responses import Response
    from app.services import tts_service

    body = await request.json()
    text = body.get("text", "")
    if not text:
        return {"error": "text 不能为空"}

    try:
        audio_bytes = await tts_service.synthesize(db, text)
        return Response(content=audio_bytes, media_type="audio/wav")
    except ValueError as e:
        return {"error": str(e)}
    except Exception as e:
        logger.error("TTS error: %s", e)
        return {"error": f"TTS 调用失败: {e}"}


# ---------- 发散思维 ----------

@router.post("/diverge")
async def diverge(
    request: Request,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    body = await request.json()
    keyword = body.get("keyword", "")
    model = body.get("model", "deepseek-chat")

    prompt = f"""请围绕关键词「{keyword}」进行发散联想，生成 8 个关联词。
每个词包含中文和英文两个版本，分别从以下维度思考：
1. 工具 2. 场景 3. 上下游 4. 风格 5. 品牌 6. 痛点 7. 趋势 8. 创新
返回 JSON 数组格式：[{{"zh": "中文词", "en": "English word", "dimension": "维度"}}]"""

    messages = [{"role": "user", "content": prompt}]
    content = await ai_stream_service.invoke_model(db, model, messages)
    return {"content": content}


# ---------- RAG 重索引 ----------

@router.post("/reindex", response_model=SimpleResponse)
async def reindex(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    from app.services import article_embedding_service
    count = await article_embedding_service.reindex_all(db, user.id)
    return SimpleResponse.ok(f"已重新索引 {count} 篇文章")


# ---------- 会话管理 ----------

@router.get("/sessions", response_model=AISessionListResponse)
async def list_sessions(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    sessions = await ai_service.list_sessions(db, user.id)
    return AISessionListResponse(success=True, sessions=sessions)


@router.post("/sessions", response_model=AISessionResponse)
async def create_session(
    req: CreateSessionRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    session = await ai_service.create_session(db, user.id, req)
    return AISessionResponse(success=True, session=session)


@router.patch("/sessions/{session_id}", response_model=AISessionResponse)
async def update_session(
    session_id: int,
    req: UpdateSessionRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    session = await ai_service.update_session(db, user.id, session_id, req)
    return AISessionResponse(success=True, session=session)


@router.delete("/sessions/{session_id}", response_model=SimpleResponse)
async def delete_session(
    session_id: int,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    await ai_service.delete_session(db, user.id, session_id)
    return SimpleResponse.ok("会话删除成功")


@router.get("/sessions/{session_id}/messages")
async def get_messages(
    session_id: int,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    data = await ai_service.get_messages(db, user.id, session_id)
    return {
        "success": True,
        "session": data["session"],
        "messages": data["messages"],
    }


@router.post("/sessions/{session_id}/append", response_model=SimpleResponse)
async def append_pair(
    session_id: int,
    req: AppendPairRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    await ai_service.append_pair(db, user.id, session_id, req)
    return SimpleResponse.ok("消息已保存")


# ---------- 配额 ----------

@router.get("/quota")
async def get_quota(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    quota = await ai_quota_service.get_quota_info(db, user.id)
    return {"success": True, "quota": quota}
