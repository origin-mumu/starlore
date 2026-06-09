"""收藏路由。"""

from datetime import datetime

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.exceptions import NotFoundException
from app.models.article import Article
from app.models.bookmark import Bookmark
from app.models.user import User
from app.schemas.article import ArticleSummary
from app.schemas.common import SimpleResponse

router = APIRouter(prefix="/api/bookmarks", tags=["bookmarks"])


@router.get("")
async def list_bookmarks(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Bookmark).where(Bookmark.user_id == user.id).order_by(Bookmark.created_at.desc())
    )
    bookmarks = result.scalars().all()

    if not bookmarks:
        return {"data": []}

    # 批量查询文章（避免 N+1）
    article_ids = [bm.article_id for bm in bookmarks]
    art_result = await db.execute(select(Article).where(Article.id.in_(article_ids)))
    article_map = {a.id: a for a in art_result.scalars().all()}

    items = []
    for bm in bookmarks:
        article = article_map.get(bm.article_id)
        if article:
            items.append(ArticleSummary(
                id=article.id,
                title=article.title,
                description=article.description,
                category=article.category,
                tags=article.tags,
                cover_image=article.cover_image,
                view_count=article.view_count,
                createdAt=article.createdAt,
            ))
    return {"data": items}


@router.get("/check/{article_id}")
async def check_bookmark(
    article_id: int,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Bookmark).where(Bookmark.user_id == user.id, Bookmark.article_id == article_id)
    )
    bookmarked = result.scalar_one_or_none() is not None
    return {"bookmarked": bookmarked}


@router.post("", response_model=SimpleResponse)
async def add_bookmark(
    article_id: int,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    art = await db.execute(select(Article).where(Article.id == article_id))
    if art.scalar_one_or_none() is None:
        raise NotFoundException("文章不存在")

    existing = await db.execute(
        select(Bookmark).where(Bookmark.user_id == user.id, Bookmark.article_id == article_id)
    )
    if existing.scalar_one_or_none() is not None:
        return SimpleResponse.ok("已收藏")

    bookmark = Bookmark(user_id=user.id, article_id=article_id, created_at=datetime.now())
    db.add(bookmark)
    await db.flush()
    return SimpleResponse.ok("收藏成功")


@router.delete("/{article_id}", response_model=SimpleResponse)
async def remove_bookmark(
    article_id: int,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Bookmark).where(Bookmark.user_id == user.id, Bookmark.article_id == article_id)
    )
    bookmark = result.scalar_one_or_none()
    if bookmark is None:
        raise NotFoundException("收藏不存在")

    await db.delete(bookmark)
    await db.flush()
    return SimpleResponse.ok("取消收藏成功")
