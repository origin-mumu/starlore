"""知识记忆 API Schema。"""

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field


class GenerateKnowledgeCardsRequest(BaseModel):
    articleIds: list[int] = Field(min_length=1, max_length=10)
    model: str = ""
    maxCardsPerArticle: int = Field(default=20, ge=3, le=40)


class UpdateKnowledgeCardRequest(BaseModel):
    question: str | None = Field(default=None, min_length=1, max_length=500)
    answerMarkdown: str | None = Field(default=None, min_length=1)
    difficulty: Literal["basic", "core", "advanced"] | None = None
    status: Literal["active", "archived"] | None = None


class ReviewKnowledgeCardRequest(BaseModel):
    rating: Literal["again", "hard", "good"]
    durationMs: int | None = Field(default=None, ge=0, le=3600000)


class KnowledgeArticleItem(BaseModel):
    articleId: int
    title: str
    description: str | None = None
    category: str | None = None
    cardCount: int = 0
    generated: bool = False
    updatedAt: datetime | None = None


class KnowledgeGroupItem(KnowledgeArticleItem):
    dueCount: int = 0
    masteredCount: int = 0


class KnowledgeCardItem(BaseModel):
    id: int
    articleId: int
    articleTitle: str
    question: str
    answerMarkdown: str
    sourceLineStart: int
    sourceLineEnd: int
    sourceHash: str
    difficulty: str
    sortOrder: int
    status: str
    reviewStage: str
    intervalDays: int
    reviewCount: int
    lapseCount: int
    nextReviewAt: datetime | None = None
    lastReviewedAt: datetime | None = None
    createdAt: datetime | None = None
    updatedAt: datetime | None = None
