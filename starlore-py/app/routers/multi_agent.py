"""Agent 可观测性路由 — 执行追踪、指标快照与 Bad Case 查询。

多智能体（Planner-Executor-Reviewer）独立工作流已并入 Harness 引擎
（delegate_task 委派 + 交卷自查），原 /api/ai/multi-agent-sse 端点随之下线。
"""

import logging

from fastapi import APIRouter, Depends
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.agent_trace_log import AgentTraceLog
from app.models.ai_bad_case import AiBadCase
from app.models.user import User

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/ai", tags=["multi-agent"])


@router.get("/agent-traces")
async def get_agent_traces(
    page: int = 1,
    size: int = 20,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """获取 Agent 执行追踪记录。"""
    result = await db.execute(
        select(AgentTraceLog)
        .where(AgentTraceLog.user_id == user.id)
        .order_by(AgentTraceLog.created_at.desc())
        .offset((max(page, 1) - 1) * min(max(size, 1), 100))
        .limit(min(max(size, 1), 100))
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
