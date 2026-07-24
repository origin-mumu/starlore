from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from pydantic import BaseModel

from app.database import get_db
from app.dependencies import get_current_user
from app.models.article import Article
from app.models.article_chunk import ArticleChunk
from app.schemas.common import SimpleResponse
from app.services import article_embedding_service

router = APIRouter(prefix="/api/articles", tags=["knowledge-chunks"])

class ChunkUpdateRequest(BaseModel):
    content: str | None = None
    isEnabled: int | None = None

@router.get("/{article_id}/chunks")
async def get_article_chunks(
    article_id: int,
    db: AsyncSession = Depends(get_db),
    user = Depends(get_current_user)
):
    res = await db.execute(select(ArticleChunk).where(ArticleChunk.articleId == article_id).order_by(ArticleChunk.chunkIndex))
    chunks = res.scalars().all()
    
    # 如果暂无切片记录，做虚拟切片填充
    if not chunks:
        art_res = await db.execute(select(Article).where(Article.id == article_id))
        article = art_res.scalar_one_or_none()
        if article and article.content:
            text_content = article.content
            chunk_size = 500
            parts = [text_content[i:i+chunk_size] for i in range(0, len(text_content), chunk_size)]
            created_chunks = []
            for idx, part in enumerate(parts):
                chk = ArticleChunk(
                    articleId=article_id,
                    chunkIndex=idx,
                    content=part,
                    tokenCount=len(part),
                    isEnabled=1
                )
                db.add(chk)
                created_chunks.append(chk)
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
    user = Depends(get_current_user)
):
    res = await db.execute(select(ArticleChunk).where(ArticleChunk.id == chunk_id))
    chunk = res.scalar_one_or_none()
    if not chunk:
        raise HTTPException(status_code=404, detail="Chunk not found")
    
    if req.content is not None:
        chunk.content = req.content
        chunk.tokenCount = len(req.content)
    if req.isEnabled is not None:
        chunk.isEnabled = req.isEnabled
        
    await db.commit()
    return SimpleResponse.ok("切片更新成功")

@router.post("/{article_id}/reindex")
async def reindex_article(
    article_id: int,
    db: AsyncSession = Depends(get_db),
    user = Depends(get_current_user)
):
    await article_embedding_service.reindex_all(db)
    return SimpleResponse.ok("文章索引重建完成")
