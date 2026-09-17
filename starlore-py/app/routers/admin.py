"""管理后台路由：用户管理、登录日志、AI 用量。"""

from datetime import date, datetime, timedelta

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel
from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import require_admin
from app.exceptions import NotFoundException
from app.models.ai_usage_log import AiUsageLog
from app.models.login_log import LoginLog
from app.models.user import User
from app.schemas.common import MessageResponse, SimpleResponse
from app.schemas.admin import AdminUserUpdateResponse
from app.services import auth_service

router = APIRouter(prefix="/api/admin", tags=["admin"])


class UpdateUserRoleRequest(BaseModel):
    nickname: str | None = None
    email: str | None = None
    avatar: str | None = None
    bio: str | None = None
    location: str | None = None
    website: str | None = None
    github: str | None = None
    role: str | None = None
    aiDailyLimit: int | None = None


@router.get("/users")
async def list_users(
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(User).order_by(User.createdAt.desc()))
    users = result.scalars().all()
    return {"data": [auth_service.to_user_info(u) for u in users]}


@router.get("/users/{user_id}")
async def get_user(
    user_id: int,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(User).where(User.id == user_id))
    target = result.scalar_one_or_none()
    if target is None:
        raise NotFoundException("用户不存在")
    return {"data": auth_service.to_user_info(target)}


@router.put("/users/{user_id}", response_model=AdminUserUpdateResponse)
async def update_user(
    user_id: int,
    req: UpdateUserRoleRequest,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(User).where(User.id == user_id))
    target = result.scalar_one_or_none()
    if target is None:
        raise NotFoundException("用户不存在")

    updates = {}
    if req.nickname is not None:
        updates["nickname"] = req.nickname
    if req.email is not None:
        updates["email"] = req.email
    if req.avatar is not None:
        updates["avatar"] = req.avatar
    if req.bio is not None:
        updates["bio"] = req.bio
    if req.location is not None:
        updates["location"] = req.location
    if req.website is not None:
        updates["website"] = req.website
    if req.github is not None:
        updates["github"] = req.github
    if req.role is not None:
        updates["role"] = req.role
    if req.aiDailyLimit is not None:
        updates["ai_daily_limit"] = req.aiDailyLimit

    updates["updatedAt"] = datetime.now()
    for field, value in updates.items():
        setattr(target, field, value)
    await db.flush()
    return {
        "message": "更新成功",
        "data": auth_service.to_user_info(target)
    }


@router.delete("/users/{user_id}", response_model=MessageResponse)
async def delete_user(
    user_id: int,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(User).where(User.id == user_id))
    target = result.scalar_one_or_none()
    if target is None:
        raise NotFoundException("用户不存在")
    await db.delete(target)
    await db.flush()
    return {"message": "用户删除成功"}


@router.get("/login-logs")
async def list_login_logs(
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1),
    username: str | None = None,
    dateFrom: str | None = None,
    dateTo: str | None = None,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    query = select(LoginLog)
    if username:
        query = query.where(LoginLog.username.ilike(f"%{username}%"))
    if dateFrom:
        try:
            dt_from = datetime.fromisoformat(dateFrom + "T00:00:00")
            query = query.where(LoginLog.login_time >= dt_from)
        except ValueError:
            pass
    if dateTo:
        try:
            dt_to = datetime.fromisoformat(dateTo + "T23:59:59")
            query = query.where(LoginLog.login_time <= dt_to)
        except ValueError:
            pass

    count_result = await db.execute(select(func.count()).select_from(query.subquery()))
    total = count_result.scalar() or 0
    pages = (total + limit - 1) // limit

    offset = (page - 1) * limit
    query = query.order_by(LoginLog.login_time.desc()).offset(offset).limit(limit)
    result = await db.execute(query)
    logs = result.scalars().all()

    return {
        "data": [
            {
                "id": log.id,
                "userId": log.user_id,
                "username": log.username,
                "ip": log.ip,
                "country": log.country,
                "province": log.province,
                "city": log.city,
                "userAgent": log.user_agent,
                "loginTime": log.login_time,
            }
            for log in logs
        ],
        "total": total,
        "pages": pages,
        "current": page,
    }


@router.get("/ai-usage")
async def list_ai_usage(
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1),
    userId: int | None = None,
    scene: str | None = None,
    status: str | None = None,
    dateFrom: str | None = None,
    dateTo: str | None = None,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    """AI 调用明细分页列表。"""
    query = select(AiUsageLog)
    if userId:
        query = query.where(AiUsageLog.user_id == userId)
    if scene:
        query = query.where(AiUsageLog.scene == scene)
    if status:
        query = query.where(AiUsageLog.status == status)
    if dateFrom:
        try:
            dt_from = datetime.fromisoformat(dateFrom + "T00:00:00")
            query = query.where(AiUsageLog.created_at >= dt_from)
        except ValueError:
            pass
    if dateTo:
        try:
            dt_to = datetime.fromisoformat(dateTo + "T23:59:59")
            query = query.where(AiUsageLog.created_at <= dt_to)
        except ValueError:
            pass

    count_result = await db.execute(select(func.count()).select_from(query.subquery()))
    total = count_result.scalar() or 0
    pages = (total + limit - 1) // limit

    offset = (page - 1) * limit
    query = query.order_by(AiUsageLog.created_at.desc()).offset(offset).limit(limit)
    result = await db.execute(query)
    logs = result.scalars().all()

    return {
        "data": [
            {
                "id": log.id,
                "userId": log.user_id,
                "username": log.username,
                "role": log.role,
                "scene": log.scene,
                "model": log.model,
                "status": log.status,
                "durationMs": log.duration_ms,
                "tokensPrompt": log.tokens_prompt,
                "tokensCompletion": log.tokens_completion,
                "createdAt": log.created_at,
            }
            for log in logs
        ],
        "total": total,
        "pages": pages,
        "current": page,
    }


@router.get("/ai-usage/summary")
async def ai_usage_summary(
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    """AI 用量汇总：今日概况、近 14 天趋势、场景/模型分布、用户排行。"""
    today = date.today()
    today_start = datetime(today.year, today.month, today.day)
    days14_start = today_start - timedelta(days=13)
    days30_start = today_start - timedelta(days=29)

    async def _count(query) -> int:
        result = await db.execute(query)
        return result.scalar() or 0

    total_today = await _count(
        select(func.count()).select_from(AiUsageLog).where(AiUsageLog.created_at >= today_start)
    )
    exhausted_today = await _count(
        select(func.count())
        .select_from(AiUsageLog)
        .where(AiUsageLog.created_at >= today_start, AiUsageLog.status == "quota_exhausted")
    )
    active_users_today = await _count(
        select(func.count(func.distinct(AiUsageLog.user_id))).where(AiUsageLog.created_at >= today_start)
    )
    tokens_today = await _count(
        select(func.coalesce(func.sum(AiUsageLog.tokens_prompt + AiUsageLog.tokens_completion), 0))
        .where(AiUsageLog.created_at >= today_start)
    )

    # 近 14 天趋势
    trend_result = await db.execute(
        select(func.date(AiUsageLog.created_at), func.count())
        .where(AiUsageLog.created_at >= days14_start)
        .group_by(func.date(AiUsageLog.created_at))
        .order_by(func.date(AiUsageLog.created_at))
    )
    trend_map = {str(row[0]): row[1] for row in trend_result.all()}
    trend = []
    for i in range(14):
        d = today - timedelta(days=13 - i)
        key = d.isoformat()
        trend.append({"date": key, "count": trend_map.get(key, 0)})

    # 场景分布（近 30 天）
    scene_result = await db.execute(
        select(AiUsageLog.scene, func.count())
        .where(AiUsageLog.created_at >= days30_start)
        .group_by(AiUsageLog.scene)
        .order_by(func.count().desc())
    )
    scene_distribution = [{"scene": row[0], "count": row[1]} for row in scene_result.all()]

    # 模型分布（近 30 天）
    model_result = await db.execute(
        select(AiUsageLog.model, func.count())
        .where(AiUsageLog.created_at >= days30_start)
        .group_by(AiUsageLog.model)
        .order_by(func.count().desc())
    )
    model_distribution = [
        {"model": row[0] or "未指定", "count": row[1]} for row in model_result.all()
    ]

    # 用户排行（近 30 天）
    user_result = await db.execute(
        select(
            AiUsageLog.user_id,
            AiUsageLog.username,
            AiUsageLog.role,
            func.count().label("call_count"),
        )
        .where(AiUsageLog.created_at >= days30_start)
        .group_by(AiUsageLog.user_id, AiUsageLog.username, AiUsageLog.role)
        .order_by(func.count().desc())
        .limit(10)
    )
    user_ranking = [
        {
            "userId": row[0],
            "username": row[1] or f"用户 {row[0]}",
            "role": row[2],
            "count": row[3],
        }
        for row in user_result.all()
    ]

    return {
        "data": {
            "today": {
                "total": total_today,
                "exhausted": exhausted_today,
                "activeUsers": active_users_today,
                "tokens": tokens_today,
            },
            "trend": trend,
            "sceneDistribution": scene_distribution,
            "modelDistribution": model_distribution,
            "userRanking": user_ranking,
        }
    }
