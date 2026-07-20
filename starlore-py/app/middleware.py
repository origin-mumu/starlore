"""中间件：CORS、认证拦截。"""

import json
import logging

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from jose import JWTError
from starlette.middleware.base import BaseHTTPMiddleware

from app.security import parse_token
from app.config import settings

logger = logging.getLogger(__name__)

# 不需要认证的路径前缀
_PUBLIC_PATHS = (
    "/api/auth/login",
    "/api/auth/register",
    "/api/health",
    "/api/public/",
    "/api/upload/",
    "/docs",
    "/openapi.json",
    "/redoc",
)


def setup_middleware(app: FastAPI) -> None:
    """注册所有中间件。"""

    # CORS（allow_origins=["*"] 时 allow_credentials 必须为 False）
    origins = [origin.strip() for origin in settings.cors_allowed_origins.split(",") if origin.strip()]
    allow_all = origins == ["*"]
    app.add_middleware(
        CORSMiddleware,
        allow_origins=origins,
        allow_credentials=not allow_all,
        allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
        allow_headers=["*"],
    )

    # 认证拦截
    app.add_middleware(AuthMiddleware)


class AuthMiddleware(BaseHTTPMiddleware):
    """JWT 认证中间件，对应 Java 的 AuthInterceptor。"""

    async def dispatch(self, request: Request, call_next):
        # OPTIONS 预检请求放行
        if request.method == "OPTIONS":
            return await call_next(request)

        path = request.url.path

        # 非 /api 路径放行
        if not path.startswith("/api"):
            return await call_next(request)

        # 公开路径放行
        if any(path.startswith(p) for p in _PUBLIC_PATHS):
            return await call_next(request)

        # 解析 Authorization header
        auth_header = request.headers.get("Authorization")
        if auth_header and auth_header.startswith("Bearer "):
            token = auth_header[7:]
            try:
                claims = parse_token(token)
                # 将用户信息存入 request.state，供下游使用
                request.state.user_id = claims.get("id")
                request.state.username = claims.get("username")
                return await call_next(request)
            except JWTError:
                return JSONResponse(
                    status_code=401,
                    content={"message": "登录已过期，请重新登录"},
                )

        return JSONResponse(
            status_code=401,
            content={"message": "未登录，请先登录"},
        )
