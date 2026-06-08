"""AI 模型配置。"""

from datetime import datetime

from sqlalchemy import Integer, String, Boolean, DateTime
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class AiConfig(Base):
    __tablename__ = "ai_configs"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    modelKey: Mapped[str] = mapped_column(String(50))  # noqa: N815
    modelName: Mapped[str] = mapped_column(String(100))  # noqa: N815
    apiUrl: Mapped[str] = mapped_column(String(500))  # noqa: N815
    modelId: Mapped[str] = mapped_column(String(100))  # noqa: N815
    apiKey: Mapped[str | None] = mapped_column(String(500))  # noqa: N815
    enabled: Mapped[bool] = mapped_column(Boolean, default=True)
    createdAt: Mapped[datetime | None] = mapped_column(DateTime)  # noqa: N815
    updatedAt: Mapped[datetime | None] = mapped_column(DateTime)  # noqa: N815
