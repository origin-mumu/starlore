"""AI 消息模型。"""

from datetime import datetime

from sqlalchemy import Integer, String, Text, DateTime
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class AiMessage(Base):
    __tablename__ = "ai_messages"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    sessionId: Mapped[int] = mapped_column(Integer)  # noqa: N815
    role: Mapped[str] = mapped_column(String(20))
    content: Mapped[str | None] = mapped_column(Text)
    createdAt: Mapped[datetime | None] = mapped_column(DateTime)  # noqa: N815
