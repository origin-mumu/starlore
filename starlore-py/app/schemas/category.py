"""分类相关 Schema。"""

from datetime import datetime

from pydantic import BaseModel, Field

from app.schemas.article import ArticleSummary


class CreateCategoryRequest(BaseModel):
    name: str = Field(..., max_length=50)
    description: str | None = Field(None, max_length=200)
    color: str | None = None


class UpdateCategoryRequest(BaseModel):
    name: str | None = None
    description: str | None = None
    color: str | None = None


class CategoryItem(BaseModel):
    id: int
    userId: int
    name: str
    description: str | None = None
    color: str | None = None
    article_count: int
    createdAt: datetime | None = None
    updatedAt: datetime | None = None


class CategoryListResponse(BaseModel):
    data: list[CategoryItem]


class CategoryDetailData(BaseModel):
    id: int
    name: str
    description: str | None = None
    color: str | None = None
    article_count: int
    createdAt: datetime | None = None
    updatedAt: datetime | None = None
    articles: list[ArticleSummary]


class CategoryDetailResponse(BaseModel):
    data: CategoryDetailData
