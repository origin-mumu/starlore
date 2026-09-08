"""Harness 消息模型。"""

from datetime import datetime
from typing import Any

from sqlalchemy import BigInteger, DateTime, Integer, String, Text, JSON
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class HarnessMessage(Base):
    __tablename__ = "harness_messages"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    session_id: Mapped[int] = mapped_column(BigInteger, index=True)
    user_id: Mapped[int] = mapped_column(Integer, default=1)
    role: Mapped[str] = mapped_column(String(20))  # user / assistant / system
    content: Mapped[str | None] = mapped_column(Text)
    reasoning_content: Mapped[str | None] = mapped_column(Text)
    tool_calls: Mapped[Any | None] = mapped_column(JSON)
    artifacts: Mapped[Any | None] = mapped_column(JSON)
    step_details: Mapped[Any | None] = mapped_column(JSON)
    tokens_prompt: Mapped[int] = mapped_column(Integer, default=0)
    tokens_completion: Mapped[int] = mapped_column(Integer, default=0)
    duration_ms: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime | None] = mapped_column(DateTime, default=datetime.utcnow)
