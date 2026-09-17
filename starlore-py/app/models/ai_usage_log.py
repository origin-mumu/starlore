"""AI 调用明细日志模型。"""

from datetime import datetime

from sqlalchemy import BigInteger, DateTime, Index, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class AiUsageLog(Base):
    __tablename__ = "ai_usage_logs"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer, index=True)
    username: Mapped[str | None] = mapped_column(String(50))
    role: Mapped[str | None] = mapped_column(String(20))
    # harness / chat / agent / diverge / knowledge
    scene: Mapped[str] = mapped_column(String(30))
    model: Mapped[str | None] = mapped_column(String(100))
    # success / quota_exhausted / error
    status: Mapped[str] = mapped_column(String(20), default="success")
    duration_ms: Mapped[int] = mapped_column(Integer, default=0)
    tokens_prompt: Mapped[int] = mapped_column(Integer, default=0)
    tokens_completion: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime | None] = mapped_column(DateTime, default=datetime.now)

    __table_args__ = (Index("idx_ai_usage_user_time", "user_id", "created_at"),)
