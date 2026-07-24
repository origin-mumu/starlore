"""管理后台路由：用户管理、登录日志。"""

from datetime import datetime

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel
from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import require_admin
from app.exceptions import NotFoundException
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
