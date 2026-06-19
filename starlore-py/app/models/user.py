"""用户模型。"""

from datetime import date, datetime

from sqlalchemy import Integer, String, Date, DateTime
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    username: Mapped[str] = mapped_column(String(50))
    email: Mapped[str | None] = mapped_column(String(100))
    password: Mapped[str] = mapped_column(String(255))
    nickname: Mapped[str | None] = mapped_column(String(50))
    avatar: Mapped[str | None] = mapped_column(String(500))
    bio: Mapped[str | None] = mapped_column(String(500))
    location: Mapped[str | None] = mapped_column(String(100))
    website: Mapped[str | None] = mapped_column(String(500))
    github: Mapped[str | None] = mapped_column(String(200))
    role: Mapped[str] = mapped_column(String(20), default="user")
    ai_daily_limit: Mapped[int] = mapped_column(Integer, default=10)
    ai_today_count: Mapped[int] = mapped_column(Integer, default=0)
    ai_reset_date: Mapped[date | None] = mapped_column(Date)
    register_ip: Mapped[str | None] = mapped_column(String(50))
    register_country: Mapped[str | None] = mapped_column(String(50))
    register_province: Mapped[str | None] = mapped_column(String(50))
    register_city: Mapped[str | None] = mapped_column(String(50))
    createdAt: Mapped[datetime | None] = mapped_column(DateTime)  # noqa: N815
    updatedAt: Mapped[datetime | None] = mapped_column(DateTime)  # noqa: N815
