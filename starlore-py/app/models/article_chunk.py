from datetime import datetime
from sqlalchemy import BigInteger, Integer, String, Text, DateTime
from sqlalchemy.orm import Mapped, mapped_column
from app.database import Base

class ArticleChunk(Base):
    __tablename__ = "article_chunks"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    articleId: Mapped[int] = mapped_column("article_id", BigInteger, nullable=False, index=True)
    chunkIndex: Mapped[int] = mapped_column("chunk_index", Integer, nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    tokenCount: Mapped[int] = mapped_column("token_count", Integer, default=0)
    isEnabled: Mapped[int] = mapped_column("is_enabled", Integer, default=1)
    createdAt: Mapped[datetime] = mapped_column("created_at", DateTime, default=datetime.utcnow)
    updatedAt: Mapped[datetime] = mapped_column("updated_at", DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
