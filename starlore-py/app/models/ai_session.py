"""AI 会话模型。"""

from datetime import datetime

from sqlalchemy import Integer, String, DateTime
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class AiSession(Base):
    __tablename__ = "ai_sessions"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer)
    title: Mapped[str] = mapped_column(String(255))
    characterKey: Mapped[str] = mapped_column(String(64))  # noqa: N815
    modelId: Mapped[str | None] = mapped_column(String(32))  # noqa: N815
    createdAt: Mapped[datetime | None] = mapped_column(DateTime)  # noqa: N815
    updatedAt: Mapped[datetime | None] = mapped_column(DateTime)  # noqa: N815
