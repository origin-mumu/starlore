"""统计 & 管理相关 Schema。"""

from datetime import datetime

from pydantic import BaseModel

from app.schemas.article import ArticleSummary


class CategoryInfo(BaseModel):
    id: int
    name: str
    article_count: int


class BlogStatsData(BaseModel):
    totalArticles: int
    totalCategories: int
    totalViews: int
    popularArticles: list[ArticleSummary]
    popularCategories: list[CategoryInfo]


class BlogStatsResponse(BaseModel):
    data: BlogStatsData


class DailyItem(BaseModel):
    date: str
    count: int


class DailyStatsResponse(BaseModel):
    data: list[DailyItem]


from app.schemas.auth import UserInfo

class AdminUserUpdateResponse(BaseModel):
    message: str
    data: UserInfo

