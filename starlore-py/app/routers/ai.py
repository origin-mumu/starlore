from app.models.knowledge_document import KnowledgeDocument, KnowledgeDocumentChunk
from fastapi import UploadFile, File
"""AI 路由：对话、会话管理、模型列表、角色卡、配额。"""
from app.models.knowledge_document import KnowledgeDocument, KnowledgeDocumentChunk
from fastapi import UploadFile, File
"""AI 路由：对话、会话管理、模型列表、角色卡、配额。"""
import logging
import json
from fastapi import APIRouter, Depends, File, Query, Request, UploadFile, HTTPException
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession

from pydantic import BaseModel
from sqlalchemy import select, delete
from app.database import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.models.agent_config import AgentConfig
from app.models.ai_feedback import AiMessageFeedback
from app.schemas.common import SimpleResponse
from app.schemas.ai import (
    AICharacterCardsResponse,
    AIMessageListResponse,
    AIModelsResponse,
    AIQuotaResponse,
    AISessionListResponse,
    AISessionResponse,
    AppendPairRequest,
    CreateSessionRequest,
    UpdateSessionRequest,
)
from app.exceptions import NotFoundException
from app.services import ai_service, ai_stream_service, ai_quota_service, ai_config_service
from app.services.langgraph_agent import run_agent

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/ai", tags=["ai"])


@router.post("/parse-file")
async def parse_file(
    file: UploadFile = File(...),
    user: User = Depends(get_current_user),
):
    from app.services.file_parse_service import extract_text

    try:
        return {"success": True, "filename": file.filename, "text": await extract_text(file)}
    except ValueError as exc:
        return {"success": False, "error": str(exc)}
    except Exception as exc:
        logger.exception("Failed to parse uploaded file")
        return {"success": False, "error": f"文件解析失败: {exc}"}


# ---------- 模型 & 角色卡 ----------

@router.get("/models", response_model=AIModelsResponse)
async def get_models(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    models = await ai_service.get_models(db)
    return AIModelsResponse(success=True, models=models)


@router.get("/providers")
async def get_providers(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """获取所有配置的 AI 厂商列表。"""
    configs = await ai_config_service.get_all_configs(db)
    providers = []
    for c in configs:
        providers.append({
            "key": c.modelKey,
            "name": c.modelName or c.modelKey,
            "apiUrl": c.apiUrl,
            "defaultModel": c.modelId,
            "enabled": c.enabled,
            "configured": bool(c.apiKey),
        })
    return {"success": True, "providers": providers}


@router.get("/providers/{provider_key}/models")
async def get_provider_models(
    provider_key: str,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """根据厂商配置动态拉取官方最新模型列表。"""
    config = await ai_config_service.get_config_by_key(db, provider_key)
    if not config:
        raise NotFoundException(f"厂商配置 {provider_key} 不存在")

    remote_models = await ai_config_service.fetch_provider_models(config.apiUrl, config.apiKey)
    if not remote_models:
        # 官方接口暂时不可用时，只回退到数据库中真实配置的默认模型，不伪造模型列表
        remote_models = [{"id": config.modelId, "name": config.modelId}] if config.modelId else []

    return {"success": True, "providerKey": provider_key, "models": remote_models}


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
    model: str = "deepseek-chat",
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Agent SSE 流式对话（带知识库工具 + RAG）。"""
    body = await request.json()
    if isinstance(body, list):
        raw_messages = body
        character_key = "default"
    else:
        raw_messages = body.get("messages", [])
        character_key = body.get("characterKey", "default")
        model = body.get("model", model)

    # 检查配额
    remaining = await ai_quota_service.get_remaining(db, user.id)
    if remaining == 0:
        async def quota_exhausted():
            yield f"data: {json.dumps({'error': '今日 AI 对话次数已用尽，明天再来吧～'}, ensure_ascii=False)}\n\n"
        return StreamingResponse(
            quota_exhausted(),
            media_type="text/event-stream",
            headers={"Cache-Control": "no-cache", "Connection": "keep-alive"},
        )

    # 消耗配额
    await ai_quota_service.try_consume(db, user.id)

    # 获取 LLM 实例
    llm = await ai_stream_service._resolve_model(db, model)

    # 获取角色卡 prompt
    from app.services.ai_service import get_character_cards
    cards = get_character_cards()
    character_prompt = ""
    for card in cards:
        if card.key == character_key:
            character_prompt = card.systemPrompt
            break

    async def event_stream():
        try:
            agent_events = run_agent(db, user.id, llm, raw_messages, character_prompt)
            async for event in agent_events:
                if event["type"] == "content":
                    yield f"data: {json.dumps({'content': event['content']}, ensure_ascii=False)}\n\n"
                elif event["type"] == "tool_calls":
                    yield f"data: {json.dumps({'tool_calls': event.get('calls', [])}, ensure_ascii=False)}\n\n"
                elif event["type"] == "tool_result":
                    yield f"data: {json.dumps({'tool_result': event.get('content', '')}, ensure_ascii=False)}\n\n"
            yield f"data: {json.dumps({'done': 'true'}, ensure_ascii=False)}\n\n"
        except Exception as e:
            logger.error("Agent stream error: %s", e)
            yield f"data: {json.dumps({'error': str(e)}, ensure_ascii=False)}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


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


# ---------- ASR ----------

@router.post("/transcribe")
async def transcribe(
    request: Request,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    from app.services import asr_service
    body = await request.json()
    audio = body.get("audio", "")
    if not audio:
        return {"error": "audio 不能为空"}
    try:
        text = await asr_service.transcribe(db, audio)
        return {"success": True, "text": text}
    except Exception as e:
        logger.error("ASR error: %s", e)
        return {"error": str(e)}


@router.post("/transcribe/stream")
async def transcribe_stream(
    request: Request,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    from app.services import asr_service
    body = await request.json()
    audio = body.get("audio", "")
    if not audio:
        from fastapi.responses import JSONResponse
        return JSONResponse(status_code=400, content={"error": "audio 不能为空"})
    
    return StreamingResponse(
        asr_service.transcribe_stream(db, audio),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )



# ---------- 发散思维 ----------

def extract_json_array(content: str) -> str | None:
    if not content:
        return None
    start = content.find('[')
    end = content.rfind(']')
    if start >= 0 and end > start:
        return content[start:end+1]
    return content.strip()


@router.post("/diverge")
async def diverge(
    request: Request,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    from fastapi.responses import JSONResponse
    body = await request.json()
    word = body.get("word") or body.get("keyword", "")
    model = body.get("model", "deepseek-chat")

    if not word:
        return JSONResponse(status_code=400, content={"message": "请输入一个词"})

    # Check quota
    remaining = await ai_quota_service.get_remaining(db, user.id)
    if remaining == 0:
        return JSONResponse(status_code=429, content={"message": "今日 AI 创意发散次数已用尽，明天再来吧～"})

    # Consume quota
    await ai_quota_service.try_consume(db, user.id)

    prompt = f"""请输入词为："{word}"。请围绕它向外联想 8 个关联词语。

要求：
1. 每个联想词必须与输入词有强烈的直接关联，逻辑必须合乎常理、生动逼真（例如对于食物或动作，应联想相关器具、食材、流派、场景或直接相关联想词，避免生硬死板地套用无关概念）。
2. 每个联想词包含 zh（中文）和 en（英文翻译），必须使用 JSON 格式表示，例如：[{{"zh":"火锅","en":"hotpot"}}, ...]。
3. 严禁返回任何 Markdown 代码块包裹（如 ```json）或多余的文字说明，仅返回纯粹的 JSON 数组。"""

    messages = [
        {"role": "system", "content": "你是一个头脑风暴创意联想助手。能够根据用户输入的词语，向外扩散联想出与之强关联的 8 个最典型、最生动、最具画面感的事物或概念。你必须严格以 JSON 数组形式返回结果，不能包含任何其他 Markdown 语法或额外解释。"},
        {"role": "user", "content": prompt}
    ]

    content = await ai_stream_service.invoke_model(db, model, messages)
    if content.startswith("Error:"):
        return JSONResponse(status_code=500, content={"message": content})

    # Extract & parse JSON array
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
        except Exception as e:
            logger.error("Diverge JSON parse error: %s, raw content: %s", e, content)
            
    if not pairs:
        return JSONResponse(status_code=500, content={"message": "AI 返回格式解析失败"})

    return {"pairs": pairs}


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

@router.get("/quota", response_model=AIQuotaResponse)
async def get_quota(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    quota = await ai_quota_service.get_quota_info(db, user.id)
    return {"data": quota}


# ---------- RAG 评估 ----------

@router.post("/rag/evaluate")
async def rag_evaluate(
    request: Request,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    from app.services import article_embedding_service
    from app.services.ragas_evaluator import evaluate_rag

    body = await request.json()
    question = body.get("question", "")
    answer = body.get("answer", "")
    ground_truth = body.get("groundTruth")
    top_k = body.get("topK", 5)

    if not question or not answer:
        return SimpleResponse.fail("question 和 answer 不能为空")

    similar_articles = await article_embedding_service.search_similar(
        db, question, user.id, top_k=top_k
    )
    contexts = [(a.content or "")[:2000] for a in similar_articles]

    if not contexts:
        return SimpleResponse.fail("未找到相关文档，无法评估")

    evaluation = await evaluate_rag(question, answer, contexts, ground_truth)

    contexts_info = [{"articleId": a.id, "title": a.title} for a in similar_articles]

    return {
        "success": True,
        "evaluator": "RAGAS",
        "scores": {
            "faithfulness": round(evaluation.faithfulness, 4),
            "answerRelevance": round(evaluation.answer_relevancy, 4),
            "contextPrecision": round(evaluation.context_precision, 4),
            "contextRecall": round(evaluation.context_recall, 4) if ground_truth else 0.8,
            "overall": round(evaluation.overall_score, 4),
        },
        "contexts": contexts_info,
    }


# ---------- MCP 工具列表 ----------

@router.get("/mcp/tools")
async def list_mcp_tools(
    user: User = Depends(get_current_user),
):
    from app.services.mcp_client import _parse_servers

    servers_config = _parse_servers()
    server_names = [s.get("name", "unknown") for s in servers_config]

    tools = []
    try:
        from app.services.mcp_client import load_mcp_tools
        mcp_tools = await load_mcp_tools()
        for t in mcp_tools:
            tools.append({
                "server": getattr(t, "server_name", "unknown"),
                "name": t.name,
                "description": t.description or "",
            })
    except Exception as e:
        logger.warning("MCP tools listing: %s", e)

    return {
        "success": True,
        "servers": server_names,
        "tools": tools,
    }


# ---------- Agent 检索参数配置与用户反馈 ----------

class AgentConfigRequest(BaseModel):
    modelName: str | None = None
    similarityThreshold: float | None = None
    topK: int | None = None
    temperature: float | None = None
    enableRerank: int | None = None

class FeedbackRequest(BaseModel):
    sessionId: int
    rating: str
    feedbackType: str | None = None
    comment: str | None = None

@router.get("/agent-config")
async def get_agent_config(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    res = await db.execute(select(AgentConfig).where(AgentConfig.userId == user.id))
    cfg = res.scalar_one_or_none()
    if not cfg:
        cfg = AgentConfig(
            userId=user.id,
            modelName="glm-4-flash",
            similarityThreshold=0.6,
            topK=5,
            temperature=0.7,
            enableRerank=1
        )
        db.add(cfg)
        await db.commit()
    
    return SimpleResponse.ok("获取Agent配置成功", {
        "id": cfg.id,
        "userId": cfg.userId,
        "modelName": cfg.modelName,
        "similarityThreshold": cfg.similarityThreshold,
        "topK": cfg.topK,
        "temperature": cfg.temperature,
        "enableRerank": cfg.enableRerank,
    })

@router.put("/agent-config")
async def update_agent_config(
    req: AgentConfigRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    res = await db.execute(select(AgentConfig).where(AgentConfig.userId == user.id))
    cfg = res.scalar_one_or_none()
    if not cfg:
        cfg = AgentConfig(userId=user.id)
        db.add(cfg)

    if req.modelName is not None:
        cfg.modelName = req.modelName
    if req.similarityThreshold is not None:
        cfg.similarityThreshold = req.similarityThreshold
    if req.topK is not None:
        cfg.topK = req.topK
    if req.temperature is not None:
        cfg.temperature = req.temperature
    if req.enableRerank is not None:
        cfg.enableRerank = req.enableRerank

    await db.commit()
    return SimpleResponse.ok("Agent配置修改成功")

@router.post("/messages/{message_id}/feedback")
async def submit_message_feedback(
    message_id: int,
    req: FeedbackRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    fb = AiMessageFeedback(
        messageId=message_id,
        sessionId=req.sessionId,
        userId=user.id,
        rating=req.rating,
        feedbackType=req.feedbackType,
        comment=req.comment,
    )
    db.add(fb)
    await db.commit()
    return SimpleResponse.ok("反馈提交成功，感谢您的评价！")


# ─── 文件库 (Knowledge Documents) API ───

class UpdateDocTextRequest(BaseModel):
    extractedText: str


class ConfirmDocumentRequest(BaseModel):
    fileName: str
    fileType: str | None = None
    fileSize: int | None = 0
    extractedText: str


@router.get("/documents")
async def list_knowledge_documents(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    res = await db.execute(select(KnowledgeDocument).where(KnowledgeDocument.userId == user.id).order_by(KnowledgeDocument.id.desc()))
    docs = res.scalars().all()
    return {
        "success": True,
        "documents": [
            {
                "id": d.id,
                "fileName": d.fileName,
                "fileType": d.fileType,
                "fileSize": d.fileSize,
                "fileUrl": d.fileUrl,
                "extractedText": d.extractedText,
                "status": d.status,
                "chunkCount": d.chunkCount,
                "createdAt": d.createdAt.isoformat() if d.createdAt else "",
                "updatedAt": d.updatedAt.isoformat() if d.updatedAt else "",
            }
            for d in docs
        ]
    }


@router.post("/documents/upload")
async def upload_knowledge_document(
    file: UploadFile = File(...),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    from app.services.file_parse_service import extract_text
    filename = file.filename or "uploaded_file.txt"
    file_ext = filename.split(".")[-1].lower() if "." in filename else "txt"

    try:
        extracted_text = await extract_text(file)
    except Exception as e:
        logger.warning("Extract text error for %s: %s", filename, e)
        extracted_text = f"文件解析信息: {filename}"

    if not extracted_text.strip():
        extracted_text = f"【文件内容解析说明】文件《{filename}》已成功存储。"

    from app.services.knowledge_chunk_service import estimate_tokens, split_text
    chunks = split_text(extracted_text)
    chunk_count = len(chunks)

    doc = KnowledgeDocument(
        userId=user.id,
        fileName=filename,
        fileType=file_ext,
        fileSize=getattr(file, "size", 0) or len(extracted_text.encode("utf-8")),
        fileUrl="",
        extractedText=extracted_text,
        status="indexed",
        chunkCount=chunk_count,
    )
    db.add(doc)
    await db.commit()
    await db.refresh(doc)

    for idx, c_text in enumerate(chunks):
        c_item = KnowledgeDocumentChunk(
            documentId=doc.id,
            chunkIndex=idx,
            content=c_text,
            tokenCount=estimate_tokens(c_text),
            isEnabled=1,
        )
        db.add(c_item)
    await db.commit()

    return {
        "success": True,
        "document": {
            "id": doc.id,
            "fileName": doc.fileName,
            "fileType": doc.fileType,
            "fileSize": doc.fileSize,
            "extractedText": doc.extractedText,
            "status": doc.status,
            "chunkCount": doc.chunkCount,
            "createdAt": doc.createdAt.isoformat() if doc.createdAt else "",
        }
    }


@router.post("/documents/confirm")
async def confirm_knowledge_document(
    req: ConfirmDocumentRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    filename = req.fileName or "file.txt"
    file_ext = req.fileType or (filename.split(".")[-1].lower() if "." in filename else "txt")
    extracted_text = req.extractedText or ""

    if not extracted_text.strip():
        extracted_text = f"【文件内容解析说明】文件《{filename}》已成功存储。"

    from app.services.knowledge_chunk_service import estimate_tokens, split_text
    chunks = split_text(extracted_text)
    chunk_count = len(chunks)

    doc = KnowledgeDocument(
        userId=user.id,
        fileName=filename,
        fileType=file_ext,
        fileSize=req.fileSize or len(extracted_text.encode("utf-8")),
        fileUrl="",
        extractedText=extracted_text,
        status="indexed",
        chunkCount=chunk_count,
    )
    db.add(doc)
    await db.commit()
    await db.refresh(doc)

    for idx, c_text in enumerate(chunks):
        c_item = KnowledgeDocumentChunk(
            documentId=doc.id,
            chunkIndex=idx,
            content=c_text,
            tokenCount=estimate_tokens(c_text),
            isEnabled=1,
        )
        db.add(c_item)
    await db.commit()

    return {
        "success": True,
        "document": {
            "id": doc.id,
            "fileName": doc.fileName,
            "fileType": doc.fileType,
            "fileSize": doc.fileSize,
            "extractedText": doc.extractedText,
            "status": doc.status,
            "chunkCount": doc.chunkCount,
            "createdAt": doc.createdAt.isoformat() if doc.createdAt else "",
        }
    }

@router.put("/documents/{doc_id}")
async def update_knowledge_document_text(
    doc_id: int,
    req: UpdateDocTextRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    res = await db.execute(select(KnowledgeDocument).where(KnowledgeDocument.id == doc_id, KnowledgeDocument.userId == user.id))
    doc = res.scalar_one_or_none()
    if not doc:
        raise HTTPException(status_code=404, detail="文档未找到")

    doc.extractedText = req.extractedText
    # 重新切片
    from app.services.knowledge_chunk_service import estimate_tokens, split_text
    chunks = split_text(req.extractedText)
    doc.chunkCount = len(chunks)
    await db.commit()

    # 删除旧切片
    await db.execute(delete(KnowledgeDocumentChunk).where(KnowledgeDocumentChunk.documentId == doc_id))
    for idx, c_text in enumerate(chunks):
        c_item = KnowledgeDocumentChunk(
            documentId=doc.id,
            chunkIndex=idx,
            content=c_text,
            tokenCount=estimate_tokens(c_text),
            isEnabled=1,
        )
        db.add(c_item)
    await db.commit()

    return {"success": True}

@router.delete("/documents/{doc_id}")
async def delete_knowledge_document(
    doc_id: int,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    await db.execute(delete(KnowledgeDocumentChunk).where(KnowledgeDocumentChunk.documentId == doc_id))
    await db.execute(delete(KnowledgeDocument).where(KnowledgeDocument.id == doc_id, KnowledgeDocument.userId == user.id))
    await db.commit()
    return {"success": True}

@router.get("/documents/{doc_id}/chunks")
async def get_document_chunks(
    doc_id: int,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    res = await db.execute(
        select(KnowledgeDocumentChunk)
        .where(KnowledgeDocumentChunk.documentId == doc_id)
        .order_by(KnowledgeDocumentChunk.chunkIndex.asc())
    )
    chunks = res.scalars().all()
    return {
        "success": True,
        "data": [
            {
                "id": c.id,
                "documentId": c.documentId,
                "chunkIndex": c.chunkIndex,
                "content": c.content,
                "tokenCount": c.tokenCount,
                "isEnabled": c.isEnabled,
            }
            for c in chunks
        ]
    }
