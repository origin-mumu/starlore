"""LangSmith 可观测性集成。

提供 LLM 调用链路追踪、调试、评估能力。
配置方式（.env）：
  LANGCHAIN_TRACING_V2=true
  LANGCHAIN_API_KEY=your_key
  LANGCHAIN_PROJECT=starlore
"""

import logging
import os

from app.config import settings

logger = logging.getLogger(__name__)

_tracing_enabled = False


def init_langsmith() -> bool:
    """初始化 LangSmith 追踪。

    Returns:
        True if tracing is enabled, False otherwise.
    """
    global _tracing_enabled

    api_key = settings.langchain_api_key or os.environ.get("LANGCHAIN_API_KEY", "")
    if not api_key:
        logger.info("LangSmith: 未配置 API Key，追踪已禁用")
        _tracing_enabled = False
        return False

    # 启用追踪
    os.environ["LANGCHAIN_TRACING_V2"] = "true"
    os.environ["LANGCHAIN_API_KEY"] = api_key
    os.environ["LANGCHAIN_PROJECT"] = settings.langchain_project

    _tracing_enabled = True
    logger.info("LangSmith: 追踪已启用，项目: %s", settings.langchain_project)
    return True


def is_tracing_enabled() -> bool:
    """检查追踪是否启用。"""
    return _tracing_enabled


def get_trace_url(run_id: str) -> str:
    """获取 LangSmith 追踪 URL。"""
    project = settings.langchain_project
    return f"https://smith.langchain.com/public/{project}/r/{run_id}"
