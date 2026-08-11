"""知识记忆路由。"""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.schemas.common import SimpleResponse
from app.schemas.knowledge_memory import (
    GenerateKnowledgeCardsRequest,
    ReviewKnowledgeCardRequest,
    UpdateKnowledgeCardRequest,
)
from app.services import knowledge_memory_service

router = APIRouter(prefix="/api/knowledge-memory", tags=["knowledge-memory"])


@router.get("/articles")
async def list_available_articles(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    items = await knowledge_memory_service.list_articles(db, user.id)
    return {"data": items}


@router.get("/groups")
async def list_groups(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    items = await knowledge_memory_service.list_groups(db, user.id)
    return {"data": items}


@router.post("/generate", response_model=SimpleResponse)
async def generate_cards(
    req: GenerateKnowledgeCardsRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await knowledge_memory_service.generate_cards(db, user.id, req)
    generated_count = sum(item["cardCount"] for item in result["generated"])
    message = f"已生成 {generated_count} 张知识卡片"
    if result["errors"]:
        message += f"，{len(result['errors'])} 篇文章未完成"
    return SimpleResponse.ok(message, result)


@router.get("/articles/{article_id}/cards")
async def list_cards(
    article_id: int,
    due_only: bool = Query(False, alias="dueOnly"),
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    items = await knowledge_memory_service.list_cards(db, user.id, article_id, due_only)
    return {"data": items}


@router.delete("/articles/{article_id}", response_model=SimpleResponse)
async def delete_article_cards(
    article_id: int,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    await knowledge_memory_service.delete_article_cards(db, user.id, article_id)
    return SimpleResponse.ok("该文章的知识卡片已移除")


@router.put("/cards/{card_id}", response_model=SimpleResponse)
async def update_card(
    card_id: int,
    req: UpdateKnowledgeCardRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    card = await knowledge_memory_service.update_card(db, user.id, card_id, req)
    return SimpleResponse.ok("知识卡片已更新", card)


@router.delete("/cards/{card_id}", response_model=SimpleResponse)
async def delete_card(
    card_id: int,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    await knowledge_memory_service.delete_card(db, user.id, card_id)
    return SimpleResponse.ok("知识卡片已删除")


@router.post("/cards/{card_id}/review", response_model=SimpleResponse)
async def review_card(
    card_id: int,
    req: ReviewKnowledgeCardRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    card = await knowledge_memory_service.review_card(db, user.id, card_id, req)
    return SimpleResponse.ok("复习进度已保存", card)
