"""文章模型。"""

from datetime import datetime

from sqlalchemy import Integer, String, Text, DateTime, JSON, Boolean
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class Article(Base):
    __tablename__ = "articles"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer)
    title: Mapped[str] = mapped_column(String(200))
    content: Mapped[str | None] = mapped_column(Text)
    description: Mapped[str | None] = mapped_column(String(500))
    category: Mapped[str | None] = mapped_column(String(50))
    tags: Mapped[list | None] = mapped_column(JSON)
    cover_image: Mapped[str | None] = mapped_column(String(500))
    view_count: Mapped[int] = mapped_column(Integer, default=0)
    status: Mapped[str] = mapped_column(String(20), default="published")
    is_public: Mapped[bool | None] = mapped_column("is_public", Boolean, default=False)
    createdAt: Mapped[datetime | None] = mapped_column(DateTime)  # noqa: N815
    updatedAt: Mapped[datetime | None] = mapped_column(DateTime)  # noqa: N815

