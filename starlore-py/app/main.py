"""Starlore 后端 — FastAPI 应用入口。"""

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.config import settings
from app.exceptions import register_exception_handlers
from app.middleware import setup_middleware
from app.routers import (
    admin,
    agent,
    ai,
    ai_config,
    articles,
    auth,
    bookmarks,
    categories,
    chunks,
    health,
    multi_agent,
    projects,
    public,
    public_ai,
    resume,
    sync,
    upload,
)

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # 初始化 LangSmith 追踪
    from app.services.langsmith_tracer import init_langsmith
    init_langsmith()

    logger.info("Starlore 后端服务启动，端口 %d", settings.app_port)
    yield


app = FastAPI(
    title="Starlore API",
    description="Starlore 个人知识管理系统后端 API",
    version="1.0.0",
    lifespan=lifespan,
)

# 中间件（CORS + 认证拦截）
setup_middleware(app)

# 全局异常处理器
register_exception_handlers(app)

# 注册路由（注意：chunks.router 必须在 articles.router 之前注册，防止 /api/articles/chunks 被 /{article_id} 误拦截为 422）
app.include_router(health.router)
app.include_router(auth.router)
app.include_router(chunks.router)
app.include_router(articles.router)
app.include_router(categories.router)
app.include_router(bookmarks.router)
app.include_router(ai.router)
app.include_router(agent.router)
app.include_router(multi_agent.router)
app.include_router(public.router)
app.include_router(public_ai.router)
app.include_router(ai_config.router)
app.include_router(resume.router)
app.include_router(projects.router)
app.include_router(admin.router)
app.include_router(upload.router)
app.include_router(sync.router)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=settings.app_host,
        port=settings.app_port,
        reload=True,
    )
