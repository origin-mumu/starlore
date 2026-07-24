from datetime import datetime
from sqlalchemy import BigInteger, String, Text, DateTime
from sqlalchemy.orm import Mapped, mapped_column
from app.database import Base

class AiMessageFeedback(Base):
    __tablename__ = "ai_message_feedbacks"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    messageId: Mapped[int] = mapped_column("message_id", BigInteger, nullable=False, index=True)
    sessionId: Mapped[int] = mapped_column("session_id", BigInteger, nullable=False)
    userId: Mapped[int] = mapped_column("user_id", BigInteger, nullable=False)
    rating: Mapped[str] = mapped_column(String(20), nullable=False)
    feedbackType: Mapped[str | None] = mapped_column("feedback_type", String(50), nullable=True)
    comment: Mapped[str | None] = mapped_column(Text, nullable=True)
    createdAt: Mapped[datetime] = mapped_column("created_at", DateTime, default=datetime.utcnow)
