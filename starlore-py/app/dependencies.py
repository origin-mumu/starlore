"""FastAPI 依赖注入。"""

from fastapi import Depends, Request
from jose import JWTError
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.exceptions import UnauthorizedException, ForbiddenException, NotFoundException
from app.models.user import User
from app.security import parse_token


async def get_current_user_id(request: Request) -> int:
    """从 Authorization header 解析当前用户 ID。"""
    auth_header = request.headers.get("Authorization")
    if not auth_header or not auth_header.startswith("Bearer "):
        raise UnauthorizedException()

    token = auth_header[7:]
    try:
        claims = parse_token(token)
    except JWTError:
        raise UnauthorizedException("登录已过期，请重新登录")

    user_id = claims.get("id")
    if user_id is None:
        raise UnauthorizedException()
    return user_id


async def get_current_user(
    request: Request,
    db: AsyncSession = Depends(get_db),
) -> User:
    """获取当前登录用户实体。"""
    user_id = await get_current_user_id(request)
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()
    if user is None:
        raise NotFoundException("用户不存在")
    return user


async def require_admin(
    user: User = Depends(get_current_user),
) -> User:
    """要求当前用户为管理员。"""
    if user.role != "admin":
        raise ForbiddenException("需要管理员权限")
    return user
