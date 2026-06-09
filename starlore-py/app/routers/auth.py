"""认证路由：注册、登录、用户信息、资料更新。"""

from fastapi import APIRouter, Depends, Request
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.schemas.auth import AuthResponse, LoginRequest, RegisterRequest, UpdateProfileRequest, UserInfo
from app.schemas.common import SimpleResponse
from app.services import auth_service

router = APIRouter(prefix="/api/auth", tags=["auth"])


@router.post("/register", response_model=AuthResponse)
async def register(req: RegisterRequest, db: AsyncSession = Depends(get_db)):
    return await auth_service.register(db, req)


@router.post("/login", response_model=AuthResponse)
async def login(req: LoginRequest, request: Request, db: AsyncSession = Depends(get_db)):
    ip = request.client.host if request.client else None
    user_agent = request.headers.get("User-Agent")
    return await auth_service.login(db, req, ip=ip, user_agent=user_agent)


@router.get("/me", response_model=UserInfo)
async def get_me(user: User = Depends(get_current_user)):
    # 直接转换已获取的用户，避免重复查询
    return auth_service.to_user_info(user)


@router.put("/profile", response_model=UserInfo)
async def update_profile(
    req: UpdateProfileRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await auth_service.update_profile(db, user.id, req)
