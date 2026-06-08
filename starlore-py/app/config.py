"""应用配置管理，使用 pydantic-settings 从环境变量 / .env 读取。"""

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # ---------- 服务 ----------
    app_port: int = 5000
    app_host: str = "0.0.0.0"

    # ---------- 数据库 ----------
    db_host: str = "127.0.0.1"
    db_port: int = 3306
    db_user: str = "ro-blog"
    db_password: str = ""
    db_name: str = "ro-blog"
    db_pool_size: int = 10
    db_pool_min_idle: int = 2

    # ---------- JWT ----------
    jwt_secret: str = "ro-blog-secret-key-2024"
    jwt_expiration: int = 604800000  # 毫秒，7 天

    # ---------- MinIO ----------
    minio_endpoint: str = "http://47.94.128.65:9000"
    minio_public_url: str = "https://www.robin-blog.cn/minio"
    minio_access_key: str = "minioadmin"
    minio_secret_key: str = "minioadmin123456"
    minio_bucket: str = "my-files"

    # ---------- AI 默认配置 (本地回退) ----------
    ai_api_key: str = ""
    ai_base_url: str = "https://api.deepseek.com"
    ai_model: str = "deepseek-chat"

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}

    @property
    def database_url(self) -> str:
        return (
            f"mysql+aiomysql://{self.db_user}:{self.db_password}"
            f"@{self.db_host}:{self.db_port}/{self.db_name}?charset=utf8mb4"
        )

    @property
    def database_url_sync(self) -> str:
        """同步 URL，仅供 Alembic 等工具使用。"""
        return (
            f"mysql+pymysql://{self.db_user}:{self.db_password}"
            f"@{self.db_host}:{self.db_port}/{self.db_name}?charset=utf8mb4"
        )


settings = Settings()
