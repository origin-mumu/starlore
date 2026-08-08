"""全量数据同步路由 — 用于 App 一键全量同步与离线本地缓存。"""

from datetime import datetime, timezone
from fastapi import APIRouter, Depends, Request
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models.article import Article
from app.models.category import Category

router = APIRouter(prefix="/api/sync", tags=["sync"])


@router.get("/full")
async def get_full_sync_data(
    request: Request,
    db: AsyncSession = Depends(get_db),
):
    user_id = getattr(request.state, "user_id", None)

    # 1. Categories query
    cat_stmt = select(Category)
    if user_id is not None:
        cat_stmt = cat_stmt.where(Category.user_id == user_id)
    cat_result = await db.execute(cat_stmt)
    categories = cat_result.scalars().all()

    # 2. Articles query (fetch full articles including content)
    art_stmt = select(Article)
    if user_id is not None:
        art_stmt = art_stmt.where(Article.user_id == user_id)
    else:
        art_stmt = art_stmt.where(Article.is_public == True)
    art_stmt = art_stmt.order_by(Article.createdAt.desc())

    art_result = await db.execute(art_stmt)
    articles = art_result.scalars().all()

    categories_data = [
        {
            "id": str(c.id),
            "name": c.name,
            "description": getattr(c, "description", None) or "",
            "icon": getattr(c, "icon", None) or "🪐",
        }
        for c in categories
    ]

    articles_data = [
        {
            "id": str(a.id),
            "title": a.title,
            "content": a.content or "",
            "description": a.description or "",
            "category": a.category or "未分类",
            "tags": a.tags or [],
            "coverImage": a.cover_image or "",
            "isPublic": a.is_public,
            "createdAt": a.createdAt.isoformat() if a.createdAt else "",
        }
        for a in articles
    ]

    return {
        "syncedAt": datetime.now(timezone.utc).isoformat(),
        "userId": user_id,
        "categories": categories_data,
        "articles": articles_data,
    }
