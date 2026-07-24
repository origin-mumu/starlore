from datetime import datetime
from sqlalchemy import BigInteger, String, Float, Integer, DateTime
from sqlalchemy.orm import Mapped, mapped_column
from app.database import Base

class AgentConfig(Base):
    __tablename__ = "agent_configs"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    userId: Mapped[int] = mapped_column("user_id", BigInteger, nullable=False, unique=True)
    modelName: Mapped[str] = mapped_column("model_name", String(50), default="glm-4-flash")
    similarityThreshold: Mapped[float] = mapped_column("similarity_threshold", Float, default=0.6)
    topK: Mapped[int] = mapped_column("top_k", Integer, default=5)
    temperature: Mapped[float] = mapped_column(Float, default=0.7)
    enableRerank: Mapped[int] = mapped_column("enable_rerank", Integer, default=1)
    updatedAt: Mapped[datetime] = mapped_column("updated_at", DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
