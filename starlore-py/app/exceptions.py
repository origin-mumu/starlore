"""自定义异常与全局异常处理器。"""

import logging

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

logger = logging.getLogger(__name__)


# ---------- 自定义异常 ----------

class NotFoundException(Exception):
    def __init__(self, message: str = "资源不存在"):
        self.message = message


class BadRequestException(Exception):
    def __init__(self, message: str = "请求参数错误"):
        self.message = message


class UnauthorizedException(Exception):
    def __init__(self, message: str = "未登录，请先登录"):
        self.message = message


class ForbiddenException(Exception):
    def __init__(self, message: str = "无权访问"):
        self.message = message


class ConflictException(Exception):
    def __init__(self, message: str = "资源冲突"):
        self.message = message


# ---------- 全局异常处理器 ----------

def register_exception_handlers(app: FastAPI) -> None:
    """注册全局异常处理器。"""

    @app.exception_handler(NotFoundException)
    async def not_found_handler(_req: Request, exc: NotFoundException):
        return JSONResponse(status_code=404, content={"message": exc.message})

    @app.exception_handler(BadRequestException)
    async def bad_request_handler(_req: Request, exc: BadRequestException):
        return JSONResponse(status_code=400, content={"message": exc.message})

    @app.exception_handler(UnauthorizedException)
    async def unauthorized_handler(_req: Request, exc: UnauthorizedException):
        return JSONResponse(status_code=401, content={"message": exc.message})

    @app.exception_handler(ForbiddenException)
    async def forbidden_handler(_req: Request, exc: ForbiddenException):
        return JSONResponse(status_code=403, content={"message": exc.message})

    @app.exception_handler(ConflictException)
    async def conflict_handler(_req: Request, exc: ConflictException):
        return JSONResponse(status_code=409, content={"message": exc.message})

    @app.exception_handler(Exception)
    async def generic_handler(_req: Request, exc: Exception):
        logger.exception("Unexpected error: %s", exc)
        return JSONResponse(status_code=500, content={"message": "服务器内部错误"})
