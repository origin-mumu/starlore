"""文章路由：CRUD、分页、搜索、统计。"""

from fastapi import APIRouter, Depends, Query, Request
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.schemas.article import (
    ArticleDetail,
    ArticleDetailResponse,
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
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    is_admin = user.role == "admin"
    # 管理后台：admin 可见全部用户的文章（含草稿）；普通用户仅见自己的已发布文章
    return await article_service.get_all_articles(
        db,
        user_id=None if is_admin else user.id,
        page=page,
        limit=limit,
        category=category,
        search=search,
        tag=tag,
        published_only=not is_admin,
    )


@router.get("/stats/summary")
async def blog_stats(
    request: Request,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    is_admin = user.role == "admin"
    stats = await article_service.get_blog_stats(db, user.id, is_admin=is_admin)
    return {"data": stats}


@router.get("/stats/daily")
async def daily_stats(
    request: Request,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    is_admin = user.role == "admin"
    data = await article_service.get_daily_stats(db, user.id, is_admin=is_admin)
    return {"data": data}


@router.get("/{article_id}", response_model=ArticleDetailResponse)
async def get_article(
    article_id: int,
    request: Request,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    is_admin = user.role == "admin"
    detail = await article_service.get_article_by_id(db, user.id, article_id, is_admin=is_admin)
    return {"data": detail}


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
    is_admin = user.role == "admin"
    await article_service.update_article(db, article_id, req, user_id=user.id, is_admin=is_admin)
    return SimpleResponse.ok("文章更新成功")


@router.delete("/{article_id}", response_model=SimpleResponse)
async def delete_article(
    article_id: int,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    is_admin = user.role == "admin"
    await article_service.delete_article(db, article_id, user_id=user.id, is_admin=is_admin)
    return SimpleResponse.ok("文章删除成功")
