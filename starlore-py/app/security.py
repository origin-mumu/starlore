"""JWT 工具与密码哈希。"""

from datetime import datetime, timedelta, timezone

import bcrypt
from jose import JWTError, jwt

from app.config import settings

# ---------- JWT ----------

_ALGORITHM = "HS256"


def generate_token(user_id: int, username: str) -> str:
    """生成 JWT token。"""
    expire = datetime.now(timezone.utc) + timedelta(milliseconds=settings.jwt_expiration)
    payload = {
        "sub": username,
        "id": user_id,
        "username": username,
        "exp": expire,
        "iat": datetime.now(timezone.utc),
    }
    return jwt.encode(payload, settings.jwt_secret, algorithm=_ALGORITHM)


def parse_token(token: str) -> dict:
    """解析 JWT token，返回 payload。无效或过期时抛出 JWTError。"""
    return jwt.decode(token, settings.jwt_secret, algorithms=[_ALGORITHM])


# ---------- 密码 ----------

def hash_password(raw: str) -> str:
    """BCrypt 哈希。"""
    return bcrypt.hashpw(raw.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")


def verify_password(raw: str, hashed: str) -> bool:
    """BCrypt 校验。"""
    return bcrypt.checkpw(raw.encode("utf-8"), hashed.encode("utf-8"))
