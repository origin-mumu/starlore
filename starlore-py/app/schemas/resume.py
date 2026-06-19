"""简历相关 Schema。"""

from datetime import datetime

from pydantic import BaseModel


class ResumeItem(BaseModel):
    id: int
    userId: int
    title: str | None = None
    template: str | None = None
    name: str | None = None
    jobTitle: str | None = None
    phone: str | None = None
    email: str | None = None
    photoUrl: str | None = None
    content: str | None = None
    status: str | None = None
    createdAt: datetime | None = None
    updatedAt: datetime | None = None


class ResumeResponse(BaseModel):
    data: ResumeItem
    message: str


class ResumeListResponse(BaseModel):
    data: list[ResumeItem]


class ResumeDetailResponse(BaseModel):
    data: ResumeItem


