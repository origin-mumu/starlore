"""认证服务：注册、登录、用户信息、资料更新。"""

import asyncio
import logging
from datetime import date, datetime

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import async_session_factory
from app.exceptions import BadRequestException, ConflictException, NotFoundException
from app.models.login_log import LoginLog
from app.models.user import User
from app.schemas.auth import AuthData, AuthResponse, LoginRequest, RegisterRequest, UpdateProfileRequest, UserInfo
from app.security import generate_token, hash_password, verify_password
from app.services.ip_location_service import lookup_ip

logger = logging.getLogger(__name__)


def to_user_info(user: User) -> UserInfo:
    """将 User 模型转换为 UserInfo DTO。"""
    return UserInfo(
        id=user.id,
        username=user.username,
        nickname=user.nickname,
        email=user.email,
        avatar=user.avatar,
        bio=user.bio,
        location=user.location,
        website=user.website,
        github=user.github,
        role=user.role,
        aiDailyLimit=user.ai_daily_limit,
        aiTodayCount=user.ai_today_count,
        createdAt=user.createdAt,
        updatedAt=user.updatedAt,
    )


async def register(db: AsyncSession, req: RegisterRequest, ip: str | None = None) -> AuthResponse:
    """用户注册。"""
    if not req.username or not req.password:
        raise BadRequestException("用户名和密码不能为空")
    if len(req.password) < 6:
        raise BadRequestException("密码长度不能少于6位")

    existing = await db.execute(select(User).where(User.username == req.username))
    if existing.scalar_one_or_none() is not None:
        raise ConflictException("用户名已存在")

    normalized_ip = ip
    if normalized_ip == "0:0:0:0:0:0:0:1":
        normalized_ip = "127.0.0.1"

    hashed_pwd = hash_password(req.password)
    now = datetime.now()
    user = User(
        username=req.username,
        password=hashed_pwd,
        email=req.email,
        nickname=req.nickname,
        role="user",
        ai_daily_limit=10,
        ai_today_count=0,
        ai_reset_date=date.today(),
        register_ip=normalized_ip,
        createdAt=now,
        updatedAt=now,
    )
    db.add(user)
    await db.flush()

    # 异步查询注册 IP 地理位置
    if normalized_ip and not normalized_ip.startswith("127.") and normalized_ip != "0:0:0:0:0:0:0:1":
        user_id = user.id
        asyncio.create_task(_update_user_register_location(user_id, normalized_ip))

    token = generate_token(user.id, user.username)
    return AuthResponse(
        message="注册成功",
        data=AuthData(token=token, user=to_user_info(user)),
    )


async def login(db: AsyncSession, req: LoginRequest, ip: str | None = None, user_agent: str | None = None) -> AuthResponse:
    """用户登录。"""
    if not req.username or not req.password:
        raise BadRequestException("用户名和密码不能为空")

    result = await db.execute(select(User).where(User.username == req.username))
    user = result.scalar_one_or_none()
    if user is None or not verify_password(req.password, user.password):
        raise BadRequestException("用户名或密码错误")

    token = generate_token(user.id, user.username)

    # 规范化 IP
    normalized_ip = ip
    if normalized_ip == "0:0:0:0:0:0:0:1":
        normalized_ip = "127.0.0.1"

    # 记录登录日志
    log_entry = LoginLog(
        user_id=user.id,
        username=user.username,
        ip=normalized_ip,
        user_agent=user_agent,
        login_time=datetime.now(),
    )
    db.add(log_entry)
    await db.flush()

    # 异步查询 IP 地理位置（使用独立会话，避免泄漏请求会话）
    log_id = log_entry.id
    asyncio.create_task(_update_login_log_location(log_id, normalized_ip))

    return AuthResponse(
        message="登录成功",
        data=AuthData(token=token, user=to_user_info(user)),
    )


async def _update_login_log_location(log_id: int, ip: str | None) -> None:
    """异步更新登录日志的地理位置信息（使用独立会话）。"""
    try:
        location = await lookup_ip(ip)
        if location:
            async with async_session_factory() as session:
                result = await session.execute(select(LoginLog).where(LoginLog.id == log_id))
                log_entry = result.scalar_one_or_none()
                if log_entry:
                    log_entry.country = location["country"]
                    log_entry.province = location["province"]
                    log_entry.city = location["city"]
                    await session.commit()
    except Exception as e:
        logger.warning("Failed to update login log location: %s", e)


async def _update_user_register_location(user_id: int, ip: str | None) -> None:
    """异步更新用户注册的地理位置信息（使用独立会话）。"""
    try:
        location = await lookup_ip(ip)
        if location:
            async with async_session_factory() as session:
                result = await session.execute(select(User).where(User.id == user_id))
                user = result.scalar_one_or_none()
                if user:
                    user.register_country = location["country"]
                    user.register_province = location["province"]
                    user.register_city = location["city"]
                    await session.commit()
    except Exception as e:
        logger.warning("Failed to update user register location: %s", e)


async def get_current_user(db: AsyncSession, user_id: int) -> UserInfo:
    """获取当前用户信息。"""
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()
    if user is None:
        raise NotFoundException("用户不存在")
    return to_user_info(user)


async def update_profile(db: AsyncSession, user_id: int, req: UpdateProfileRequest) -> UserInfo:
    """更新用户资料。"""
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()
    if user is None:
        raise NotFoundException("用户不存在")

    if req.nickname is not None:
        user.nickname = req.nickname
    if req.avatar is not None:
        user.avatar = req.avatar
    if req.bio is not None:
        user.bio = req.bio
    if req.email is not None:
        user.email = req.email
    if req.location is not None:
        user.location = req.location
    if req.website is not None:
        user.website = req.website
    if req.github is not None:
        user.github = req.github

    if req.username is not None and req.username != user.username:
        existing = await db.execute(select(User).where(User.username == req.username))
        if existing.scalar_one_or_none() is not None:
            raise ConflictException("用户名已存在")
        user.username = req.username

    if req.newPassword is not None:
        if not req.oldPassword:
            raise BadRequestException("请输入旧密码")
        if not verify_password(req.oldPassword, user.password):
            raise BadRequestException("旧密码错误")
        user.password = hash_password(req.newPassword)

    user.updatedAt = datetime.now()
    await db.flush()
    return to_user_info(user)
