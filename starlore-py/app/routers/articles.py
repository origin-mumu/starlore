"""文章路由：CRUD、分页、搜索、统计。"""

from fastapi import APIRouter, Depends, Query, Request
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.schemas.article import (
    ArticleDetail,
    ArticleListResponse,
    ArticleSummary,
    CreateArticleRequest,
    UpdateArticleRequest,
)
from app.schemas.common import SimpleResponse
from app.schemas.admin import BlogStatsResponse, DailyStatsResponse
from app.services import article_service

router = APIRouter(prefix="/api/articles", tags=["articles"])


@router.get("", response_model=ArticleListResponse)
async def list_articles(
    request: Request,
    page: int = Query(1, ge=1),
    limit: int = Query(10, ge=1),
    category: str | None = None,
    search: str | None = None,
    tag: str | None = None,
    db: AsyncSession = Depends(get_db),
):
    user_id = getattr(request.state, "user_id", None)
    return await article_service.get_all_articles(
        db, user_id=user_id, page=page, limit=limit, category=category, search=search, tag=tag,
    )


@router.get("/stats/summary")
async def blog_stats(
    request: Request,
    db: AsyncSession = Depends(get_db),
):
    user_id = getattr(request.state, "user_id", None)
    stats = await article_service.get_blog_stats(db, user_id)
    return {"data": stats}


@router.get("/stats/daily")
async def daily_stats(
    request: Request,
    db: AsyncSession = Depends(get_db),
):
    user_id = getattr(request.state, "user_id", None)
    data = await article_service.get_daily_stats(db, user_id)
    return {"data": data}


@router.get("/{article_id}", response_model=ArticleDetail)
async def get_article(
    article_id: int,
    request: Request,
    db: AsyncSession = Depends(get_db),
):
    user_id = getattr(request.state, "user_id", None)
    return await article_service.get_article_by_id(db, user_id, article_id)


@router.post("", response_model=SimpleResponse)
async def create_article(
    req: CreateArticleRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    article = await article_service.create_article(db, user.id, req)
    return SimpleResponse.ok("文章创建成功", {"id": article.id})


@router.put("/{article_id}", response_model=SimpleResponse)
async def update_article(
    article_id: int,
    req: UpdateArticleRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    await article_service.update_article(db, article_id, req)
    return SimpleResponse.ok("文章更新成功")


@router.delete("/{article_id}", response_model=SimpleResponse)
async def delete_article(
    article_id: int,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    await article_service.delete_article(db, article_id)
    return SimpleResponse.ok("文章删除成功")
