"""认证相关 Schema。"""

from datetime import datetime

from pydantic import BaseModel, EmailStr, Field


class LoginRequest(BaseModel):
    username: str = Field(..., min_length=1, description="用户名不能为空")
    password: str = Field(..., min_length=1, description="密码不能为空")


class RegisterRequest(BaseModel):
    username: str = Field(..., min_length=2, max_length=20)
    password: str = Field(..., min_length=6, max_length=50)
    email: str | None = None
    nickname: str | None = None


class UserInfo(BaseModel):
    id: int
    username: str
    nickname: str | None = None
    email: str | None = None
    avatar: str | None = None
    bio: str | None = None
    location: str | None = None
    website: str | None = None
    github: str | None = None
    role: str
    aiDailyLimit: int | None = None
    aiTodayCount: int | None = None
    createdAt: datetime | None = None
    updatedAt: datetime | None = None


class AuthData(BaseModel):
    token: str
    user: UserInfo


class AuthResponse(BaseModel):
    message: str
    data: AuthData


class UpdateProfileRequest(BaseModel):
    nickname: str | None = Field(None, min_length=2, max_length=20)
    avatar: str | None = None
    bio: str | None = Field(None, max_length=500)
    email: str | None = None
    location: str | None = None
    website: str | None = None
    github: str | None = None
    username: str | None = Field(None, min_length=2, max_length=20)
    oldPassword: str | None = None
    newPassword: str | None = Field(None, min_length=6, max_length=50)
