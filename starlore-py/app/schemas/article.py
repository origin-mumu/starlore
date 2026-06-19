"""文章相关 Schema。"""

from datetime import datetime

from pydantic import BaseModel, Field

from app.schemas.common import PaginationInfo


class CreateArticleRequest(BaseModel):
    title: str = Field(..., max_length=200)
    content: str | None = None
    description: str | None = Field(None, max_length=500)
    category: str | None = None
    tags: list[str] | None = None
    coverImage: str | None = None
    status: str | None = None


class UpdateArticleRequest(BaseModel):
    title: str | None = None
    content: str | None = None
    description: str | None = None
    category: str | None = None
    tags: list[str] | None = None
    coverImage: str | None = None
    status: str | None = None


class ArticleSummary(BaseModel):
    id: int
    userId: int | None = None
    authorName: str | None = None
    title: str
    status: str | None = None
    description: str | None = None
    category: str | None = None
    tags: list[str] | None = None
    cover_image: str | None = None
    view_count: int | None = None
    createdAt: datetime | None = None


class ArticleDetail(BaseModel):
    id: int
    title: str
    content: str | None = None
    description: str | None = None
    category: str | None = None
    tags: list[str] | None = None
    cover_image: str | None = None
    view_count: int | None = None
    status: str | None = None
    createdAt: datetime | None = None
    updatedAt: datetime | None = None


class ArticleListResponse(BaseModel):
    data: list[ArticleSummary]
    pagination: PaginationInfo


class ArticleDetailResponse(BaseModel):
    data: ArticleDetail

