"""Agent 路由 — LangGraph 多 Agent 工作流 + RAGAS 评估。"""

import json
import logging

from fastapi import APIRouter, Depends, Request
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import async_session_factory, get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.schemas.common import SimpleResponse
from app.services import ai_stream_service, article_embedding_service
from app.services.langgraph_agent import run_agent, run_agent_simple
from app.services.ragas_evaluator import evaluate_rag

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/agent", tags=["agent"])


@router.post("/chat")
async def agent_chat(
    request: Request,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """LangGraph Agent 流式对话。"""
    body = await request.json()
    model = body.get("model", "deepseek-chat")
    messages = body.get("messages", [])
    character_key = body.get("characterKey", "default")

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
        async with async_session_factory() as workflow_db:
            try:
                async for event in run_agent(
                    workflow_db, user.id, llm, messages, character_prompt
                ):
                    yield f"data: {json.dumps(event, ensure_ascii=False)}\n\n"
                await workflow_db.commit()
            except Exception:
                await workflow_db.rollback()
                raise
        yield "data: [DONE]\n\n"

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "Connection": "keep-alive"},
    )


@router.post("/evaluate")
async def evaluate_rag_quality(
    request: Request,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """评估 RAG 质量（RAGAS）。"""
    body = await request.json()
    question = body.get("question", "")
    answer = body.get("answer", "")

    if not question or not answer:
        return SimpleResponse.fail("question 和 answer 不能为空")

    # 检索相关文档
    similar_articles = await article_embedding_service.search_similar(db, question, user.id, top_k=5)
    contexts = [(a.content or "")[:2000] for a in similar_articles]

    if not contexts:
        return SimpleResponse.fail("未找到相关文档，无法评估")

    # 运行 RAGAS 评估
    evaluation = await evaluate_rag(question, answer, contexts)

    return {
        "success": True,
        "evaluation": {
            "faithfulness": round(evaluation.faithfulness, 4),
            "answer_relevancy": round(evaluation.answer_relevancy, 4),
            "context_precision": round(evaluation.context_precision, 4),
            "context_recall": round(evaluation.context_recall, 4),
            "overall_score": round(evaluation.overall_score, 4),
        },
    }
