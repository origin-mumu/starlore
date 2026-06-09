"""AI 配额管理服务。"""

from datetime import date

from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User


async def get_remaining(db: AsyncSession, user_id: int) -> int:
    """获取用户今日剩余 AI 调用次数。-1 表示无限制。"""
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()
    if user is None:
        return 0
    if user.role == "admin":
        return -1

    # 日期重置
    if user.ai_reset_date != date.today():
        user.ai_today_count = 0
        user.ai_reset_date = date.today()
        await db.flush()

    limit = user.ai_daily_limit or 10
    return max(0, limit - user.ai_today_count)


async def try_consume(db: AsyncSession, user_id: int) -> bool:
    """尝试消耗一次配额。成功返回 True，配额耗尽返回 False。"""
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()
    if user is None:
        return False
    if user.role == "admin":
        return True

    # 日期重置
    if user.ai_reset_date != date.today():
        user.ai_today_count = 0
        user.ai_reset_date = date.today()

    limit = user.ai_daily_limit or 10
    if user.ai_today_count >= limit:
        return False

    user.ai_today_count += 1
    await db.flush()
    return True


async def get_quota_info(db: AsyncSession, user_id: int) -> dict:
    """获取配额详情。"""
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()
    if user is None:
        return {"dailyLimit": 10, "used": 0, "remaining": 0, "isAdmin": False}

    is_admin = user.role == "admin"

    # 日期重置
    if user.ai_reset_date != date.today():
        user.ai_today_count = 0
        user.ai_reset_date = date.today()
        await db.flush()

    limit = user.ai_daily_limit or 10
    used = user.ai_today_count

    return {
        "dailyLimit": limit,
        "used": used,
        "remaining": -1 if is_admin else max(0, limit - used),
        "isAdmin": is_admin,
    }
