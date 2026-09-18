"""Harness 任务清单模型（会话级整表快照，last-write-wins）。"""

from datetime import datetime
from typing import Any

from sqlalchemy import BigInteger, DateTime, Integer, JSON
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class HarnessTodo(Base):
    __tablename__ = "harness_todos"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    session_id: Mapped[int] = mapped_column(BigInteger, unique=True, index=True)
    user_id: Mapped[int] = mapped_column(Integer, default=1, index=True)
    todos: Mapped[Any | None] = mapped_column(JSON)
    created_at: Mapped[datetime | None] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime | None] = mapped_column(
        DateTime, default=datetime.utcnow, onupdate=datetime.utcnow
    )
