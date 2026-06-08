"""登录日志模型。"""

from datetime import datetime

from sqlalchemy import BigInteger, Integer, String, DateTime
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


class LoginLog(Base):
    __tablename__ = "login_logs"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer)
    username: Mapped[str | None] = mapped_column(String(50))
    ip: Mapped[str | None] = mapped_column(String(50))
    country: Mapped[str | None] = mapped_column(String(100))
    province: Mapped[str | None] = mapped_column(String(100))
    city: Mapped[str | None] = mapped_column(String(100))
    user_agent: Mapped[str | None] = mapped_column(String(500))
    login_time: Mapped[datetime | None] = mapped_column(DateTime)
