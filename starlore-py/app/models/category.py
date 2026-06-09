"""分类模型。"""

from datetime import datetime

from sqlalchemy import Integer, String, DateTime
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class Category(Base):
    __tablename__ = "categories"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer)
    name: Mapped[str] = mapped_column(String(50))
    description: Mapped[str | None] = mapped_column(String(200))
    color: Mapped[str | None] = mapped_column(String(20))
    article_count: Mapped[int] = mapped_column(Integer, default=0)
    createdAt: Mapped[datetime | None] = mapped_column(DateTime)  # noqa: N815
    updatedAt: Mapped[datetime | None] = mapped_column(DateTime)  # noqa: N815
