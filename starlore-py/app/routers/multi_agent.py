"""Multi-Agent 路由 — Planner-Executor-Reviewer SSE 流式端点。"""

import json
import logging

from fastapi import APIRouter, Depends, Request
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.services import ai_stream_service
from app.services.multi_agent import run_multi_agent
from app.services.langgraph_agent import run_agent

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/ai", tags=["multi-agent"])


@router.post("/multi-agent-sse")
async def multi_agent_sse(
    request: Request,
    model: str = "deepseek-chat",
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Multi-Agent Planner-Executor-Reviewer SSE 流式端点。"""
    body = await request.json()
    if isinstance(body, list):
        messages = body
        character_key = "default"
    else:
        messages = body.get("messages", [])
        character_key = body.get("characterKey", "default")
        model = body.get("model", model)

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
        """SSE 流式输出。"""
        queue: list[str] = []

        async def callback(event: dict):
            queue.append(f"data: {json.dumps(event, ensure_ascii=False)}\n\n")

        # 运行多 Agent
        state = await run_multi_agent(
            db, user.id, llm, messages, character_prompt, event_callback=callback,
        )

        # 逐个输出事件
        for chunk in queue:
            yield chunk

        yield "data: [DONE]\n\n"

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "Connection": "keep-alive"},
    )


@router.post("/agent-traces")
async def get_agent_traces(
    request: Request,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """获取 Agent 执行追踪记录。"""
    from sqlalchemy import select
    from app.models.agent_trace_log import AgentTraceLog

    result = await db.execute(
        select(AgentTraceLog)
        .where(AgentTraceLog.user_id == user.id)
        .order_by(AgentTraceLog.created_at.desc())
        .limit(50)
    )
    traces = result.scalars().all()
    return {
        "success": True,
        "traces": [
            {
                "id": t.id,
                "traceId": t.trace_id,
                "nodeName": t.node_name,
                "tokensIn": t.tokens_in,
                "tokensOut": t.tokens_out,
                "latencyMs": t.latency_ms,
                "createdAt": str(t.created_at) if t.created_at else None,
            }
            for t in traces
        ],
    }


@router.get("/agent-metrics")
async def get_agent_metrics(
    user: User = Depends(get_current_user),
):
    """获取 Agent 执行指标快照。"""
    from app.services.agent_metrics import get_metrics
    return {"success": True, "metrics": get_metrics()}


@router.get("/agent-bad-cases")
async def get_agent_bad_cases(
    page: int = 1,
    size: int = 20,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """获取 Bad Case 列表（分页）。"""
    from sqlalchemy import select, func
    from app.models.ai_bad_case import AiBadCase

    offset = (page - 1) * size

    # 总数
    count_result = await db.execute(select(func.count()).select_from(AiBadCase))
    total = count_result.scalar() or 0

    # 分页查询
    result = await db.execute(
        select(AiBadCase)
        .order_by(AiBadCase.created_at.desc())
        .offset(offset)
        .limit(size)
    )
    cases = result.scalars().all()

    return {
        "success": True,
        "total": total,
        "page": page,
        "size": size,
        "data": [
            {
                "id": c.id,
                "question": c.question,
                "actualAnswer": c.actual_answer,
                "errorMessage": c.error_message,
                "agentPath": c.agent_path,
                "tokens": c.tokens,
                "latencyMs": c.latency_ms,
                "createdAt": str(c.created_at) if c.created_at else None,
            }
            for c in cases
        ],
    }
