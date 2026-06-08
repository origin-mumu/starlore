"""收藏模型。"""

from datetime import datetime

from sqlalchemy import Integer, DateTime
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class Bookmark(Base):
    __tablename__ = "bookmarks"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer)
    article_id: Mapped[int] = mapped_column(Integer)
    created_at: Mapped[datetime | None] = mapped_column(DateTime)
