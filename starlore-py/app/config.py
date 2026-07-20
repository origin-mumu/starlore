"""应用配置管理，使用 pydantic-settings 从环境变量 / .env 读取。"""

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # ---------- 服务 ----------
    app_port: int = 5000
    app_host: str = "0.0.0.0"
    cors_allowed_origins: str = "http://localhost:5173,http://127.0.0.1:5173"

    # ---------- 数据库 ----------
    db_host: str = "127.0.0.1"
    db_port: int = 3306
    db_user: str = "ro-blog"
    db_password: str = ""
    db_name: str = "ro-blog"
    db_pool_size: int = 10
    db_pool_min_idle: int = 2

    # ---------- JWT ----------
    jwt_secret: str = "change-me-in-env"
    jwt_expiration: int = 604800000  # 毫秒，7 天

    # ---------- MinIO ----------
    minio_endpoint: str = "http://127.0.0.1:9000"
    minio_public_url: str = "http://127.0.0.1:9000"
    minio_access_key: str = ""
    minio_secret_key: str = ""
    minio_bucket: str = "my-files"

    # ---------- AI 默认配置 (本地回退) ----------
    ai_api_key: str = ""
    ai_base_url: str = "https://api.deepseek.com"
    ai_model: str = "deepseek-chat"

    # ---------- LangChain / LangSmith ----------
    langchain_tracing_v2: bool = False
    langchain_api_key: str = ""
    langchain_project: str = "starlore"
    langchain_endpoint: str = "https://api.smith.langchain.com"

    # ---------- Agent executor ----------
    agent_executor_core_size: int = 4
    agent_executor_max_size: int = 8
    agent_executor_queue_capacity: int = 100

    # ---------- Optional MCP servers (JSON array) ----------
    mcp_servers: str = "[]"

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
