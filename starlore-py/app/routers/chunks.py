import math
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from pydantic import BaseModel

from app.database import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.models.article import Article
from app.models.article_chunk import ArticleChunk
from app.models.knowledge_document import KnowledgeDocumentChunk
from app.schemas.common import SimpleResponse
from app.services import article_embedding_service, knowledge_chunk_service

router = APIRouter(prefix="/api/articles", tags=["knowledge-chunks"])


class ChunkUpdateRequest(BaseModel):
    content: str | None = None
    isEnabled: int | None = None


@router.get("/chunks")
async def get_all_chunks(
    page: int = Query(1, ge=1),
    limit: int = Query(12, ge=1, le=48),
    category: str | None = Query(None),
    article_id: int | None = Query(None),
    articleId: int | None = Query(None),
    search: str | None = Query(None),
    db: AsyncSession = Depends(get_db),
    user: User = Depends(get_current_user),
):
    """获取用户所有文章的结构化切片（带分页、分类、文章筛选与检索）。"""
    target_article_id = article_id or articleId
    stmt = select(Article).where(Article.user_id == user.id).order_by(Article.updatedAt.desc())
    if category and category.strip():
        stmt = stmt.where(Article.category == category.strip())
    if target_article_id:
        stmt = stmt.where(Article.id == target_article_id)

    art_res = await db.execute(stmt)
    articles = list(art_res.scalars().all())

    # 获取所有文章列表供前端下拉框筛选
    all_arts_stmt = select(Article).where(Article.user_id == user.id).order_by(Article.title.asc())
    all_arts_res = await db.execute(all_arts_stmt)
    all_articles_list = [
        {
            "id": a.id,
            "title": a.title,
            "category": a.category or "",
        }
        for a in all_arts_res.scalars().all()
    ]

    cat_stmt = select(Article.category).where(Article.user_id == user.id).distinct()
    cat_res = await db.execute(cat_stmt)
    categories = sorted([c for c in cat_res.scalars().all() if c and c.strip()])

    normalized_search = (search or "").strip().lower()

    all_matches = []
    for article in articles:
        chunks = await knowledge_chunk_service.ensure_chunks(db, article)
        for chunk in chunks:
            if (
                not normalized_search
                or normalized_search in (chunk.content or "").lower()
                or normalized_search in (article.title or "").lower()
            ):
                all_matches.append({
                    "id": chunk.id,
                    "articleId": chunk.articleId,
                    "articleTitle": article.title,
                    "articleCategory": article.category,
                    "chunkIndex": chunk.chunkIndex,
                    "content": chunk.content,
                    "tokenCount": chunk.tokenCount,
                    "isEnabled": chunk.isEnabled,
                })

    total = len(all_matches)
    pages = max(1, math.ceil(total / limit))
    safe_page = min(page, pages)
    from_idx = min((safe_page - 1) * limit, total)
    to_idx = min(from_idx + limit, total)

    items = all_matches[from_idx:to_idx]
    pagination = {
        "current": safe_page,
        "total": total,
        "pages": pages,
        "limit": limit,
    }

    return SimpleResponse.ok(
        "获取切片成功",
        {
            "items": items,
            "pagination": pagination,
            "categories": categories,
            "articles": all_articles_list,
        },
    )


@router.post("/reindex")
async def reindex_all_articles(
    db: AsyncSession = Depends(get_db),
    user: User = Depends(get_current_user),
):
    """重建全站文章向量索引。"""
    count = await article_embedding_service.reindex_all(db, user.id)
    await db.commit()
    return SimpleResponse.ok(f"已重建 {count} 篇文章的向量索引", {"count": count})


@router.get("/{article_id}/chunks")
async def get_article_chunks(
    article_id: int,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(get_current_user)
):
    res = await db.execute(select(ArticleChunk).where(ArticleChunk.articleId == article_id).order_by(ArticleChunk.chunkIndex))
    chunks = res.scalars().all()
    
    # 如果暂无切片记录，按 Markdown/段落/句子结构初始化。
    if not chunks:
        art_res = await db.execute(select(Article).where(Article.id == article_id))
        article = art_res.scalar_one_or_none()
        if article and article.content:
            created_chunks = await knowledge_chunk_service.rebuild_chunks(db, article)
            await db.commit()
            chunks = created_chunks

    data = [
        {
            "id": c.id,
            "articleId": c.articleId,
            "chunkIndex": c.chunkIndex,
            "content": c.content,
            "tokenCount": c.tokenCount,
            "isEnabled": c.isEnabled,
        }
        for c in chunks
    ]
    return SimpleResponse.ok("获取切片成功", data)


@router.put("/chunks/{chunk_id}")
async def update_chunk(
    chunk_id: int,
    req: ChunkUpdateRequest,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(get_current_user)
):
    res = await db.execute(select(ArticleChunk).where(ArticleChunk.id == chunk_id))
    chunk = res.scalar_one_or_none()
    if not chunk:
        doc_res = await db.execute(select(KnowledgeDocumentChunk).where(KnowledgeDocumentChunk.id == chunk_id))
        doc_chunk = doc_res.scalar_one_or_none()
        if doc_chunk:
            if req.content is not None:
                doc_chunk.content = req.content
                doc_chunk.tokenCount = knowledge_chunk_service.estimate_tokens(req.content)
            if req.isEnabled is not None:
                doc_chunk.isEnabled = req.isEnabled
            await db.commit()
            return SimpleResponse.ok("切片更新成功")
        raise HTTPException(status_code=404, detail="Chunk not found")
    
    if req.content is not None:
        chunk.content = req.content
        chunk.tokenCount = knowledge_chunk_service.estimate_tokens(req.content)
    if req.isEnabled is not None:
        chunk.isEnabled = req.isEnabled
        
    await db.commit()
    art_res = await db.execute(select(Article).where(Article.id == chunk.articleId))
    article = art_res.scalar_one_or_none()
    if article:
        await article_embedding_service.refresh_article_vectors(db, article)
    return SimpleResponse.ok("切片更新成功")


@router.post("/{article_id}/reindex")
async def reindex_article(
    article_id: int,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(get_current_user)
):
    art_res = await db.execute(
        select(Article).where(Article.id == article_id, Article.user_id == user.id)
    )
    article = art_res.scalar_one_or_none()
    if not article:
        raise HTTPException(status_code=404, detail="Article not found")
    await article_embedding_service.index_article(db, article)
    await db.commit()
    return SimpleResponse.ok("文章索引重建完成")

