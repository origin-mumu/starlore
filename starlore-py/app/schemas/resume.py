"""简历相关 Schema。"""

from datetime import datetime

from pydantic import BaseModel


class ResumeItem(BaseModel):
    id: int
    userId: int
    title: str | None = None
    template: str | None = None
    name: str | None = None
    job_title: str | None = None
    phone: str | None = None
    email: str | None = None
    photo_url: str | None = None
    content: str | None = None
    status: str | None = None
    created_at: datetime | None = None
    updated_at: datetime | None = None
