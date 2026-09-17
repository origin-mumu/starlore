"""AI 调用明细记录服务。"""

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.ai_usage_log import AiUsageLog
from app.models.user import User


async def record_consumption(
    db: AsyncSession,
    user: User,
    scene: str,
    model: str | None = None,
    consumed: bool = True,
) -> int:
    """在配额消耗点写入一条调用明细（status 区分是否耗尽），返回日志 id。"""
    row = AiUsageLog(
        user_id=user.id,
        username=user.username,
        role=user.role,
        scene=scene,
        model=model,
        status="success" if consumed else "quota_exhausted",
    )
    db.add(row)
    await db.flush()
    return row.id


async def finish_log(
    db: AsyncSession,
    log_id: int,
    duration_ms: int = 0,
    tokens_prompt: int = 0,
    tokens_completion: int = 0,
    status: str = "success",
) -> None:
    """回合结束后补全耗时与 token 统计。"""
    result = await db.execute(select(AiUsageLog).where(AiUsageLog.id == log_id))
    row = result.scalar_one_or_none()
    if row is None:
        return
    row.duration_ms = duration_ms
    row.tokens_prompt = tokens_prompt
    row.tokens_completion = tokens_completion
    row.status = status
    await db.flush()
