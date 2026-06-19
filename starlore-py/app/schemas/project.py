"""项目相关 Schema。"""

from datetime import datetime

from pydantic import BaseModel


class ProjectRequest(BaseModel):
    name: str | None = None
    description: str | None = None
    url: str | None = None
    image: str | None = None
    sortOrder: int | None = None


class ProjectInfo(BaseModel):
    id: int
    name: str
    description: str | None = None
    url: str | None = None
    image: str | None = None
    sortOrder: int | None = None
    createdAt: datetime | None = None
    updatedAt: datetime | None = None


class ProjectResponse(BaseModel):
    data: ProjectInfo
    message: str

