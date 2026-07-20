"""Public read-only APIs compatible with the Spring Boot backend."""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.schemas.article import ArticleDetailResponse, ArticleListResponse
from app.schemas.category import CategoryDetailResponse, CategoryListResponse
from app.services import article_service, category_service

router = APIRouter(prefix="/api/public", tags=["public"])


@router.get("/articles", response_model=ArticleListResponse)
async def list_public_articles(
    page: int = Query(1, ge=1),
    limit: int = Query(10, ge=1),
    category: str | None = None,
    search: str | None = None,
    tag: str | None = None,
    db: AsyncSession = Depends(get_db),
):
    return await article_service.get_public_articles(
        db, page=page, limit=limit, category=category, search=search, tag=tag
    )


@router.get("/articles/{article_id}", response_model=ArticleDetailResponse)
async def get_public_article(article_id: int, db: AsyncSession = Depends(get_db)):
    return {"data": await article_service.get_public_article_by_id(db, article_id)}


@router.get("/categories", response_model=CategoryListResponse)
async def list_public_categories(db: AsyncSession = Depends(get_db)):
    return CategoryListResponse(data=await category_service.get_public_categories(db))


@router.get("/categories/{category_id}", response_model=CategoryDetailResponse)
async def get_public_category(category_id: int, db: AsyncSession = Depends(get_db)):
    return {"data": await category_service.get_public_category_by_id(db, category_id)}


@router.get("/stats")
async def get_public_stats(db: AsyncSession = Depends(get_db)):
    return {"data": await article_service.get_public_stats(db)}
