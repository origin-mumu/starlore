"""AI SSE 流式对话服务 — 基于 LangChain ChatOpenAI。"""

import json
import logging
from typing import AsyncGenerator

from langchain_core.messages import HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI

from app.config import settings
from app.services import ai_config_service
from sqlalchemy.ext.asyncio import AsyncSession

logger = logging.getLogger(__name__)


async def _resolve_model(db: AsyncSession, model: str) -> ChatOpenAI:
    """解析模型配置，返回 LangChain ChatOpenAI 实例。"""
    # 优先从数据库读取配置
    db_config = await ai_config_service.get_config_by_key(db, model)
    if (not db_config or not db_config.enabled or not db_config.apiKey) and model in ("deepseek-chat", "default", ""):
        db_config = await ai_config_service.get_config_by_key(db, "deepseek-v4-flash")
    if db_config and db_config.enabled and db_config.apiKey:
        base_url = db_config.apiUrl
        if base_url.endswith("/chat/completions"):
            base_url = base_url[:-17]
        base_url = base_url.rstrip("/")

        extra_kwargs = {}
        if "xiaomimimo.com" in base_url:
            extra_kwargs["default_headers"] = {"api-key": db_config.apiKey}

        return ChatOpenAI(
            base_url=base_url,
            api_key=db_config.apiKey,
            model=db_config.modelId,
            streaming=True,
            **extra_kwargs,
        )

    # 回退本地配置
    if settings.ai_api_key:
        base_url = settings.ai_base_url
        if base_url.endswith("/chat/completions"):
            base_url = base_url[:-17]
        base_url = base_url.rstrip("/")

        return ChatOpenAI(
            base_url=base_url,
            api_key=settings.ai_api_key,
            model=settings.ai_model,
            streaming=True,
        )

    raise ValueError("未配置 API Key，请在 ai_configs 表或 .env 中配置")


def _to_langchain_messages(messages: list[dict]) -> list:
    """将 OpenAI 格式消息列表转换为 LangChain 消息对象。"""
    lc_messages = []
    for msg in messages:
        role = msg.get("role", "user")
        content = msg.get("content", "")
        if role == "system":
            lc_messages.append(SystemMessage(content=content))
        elif role == "assistant":
            from langchain_core.messages import AIMessage
            lc_messages.append(AIMessage(content=content))
        else:
            lc_messages.append(HumanMessage(content=content))
    return lc_messages


async def stream_chat(
    db: AsyncSession,
    model: str,
    messages: list[dict],
) -> AsyncGenerator[str, None]:
    """SSE 流式对话，yield SSE 格式的事件字符串。"""
    try:
        llm = await _resolve_model(db, model)
    except ValueError as e:
        yield f"data: {json.dumps({'error': str(e)}, ensure_ascii=False)}\n\n"
        yield "data: [DONE]\n\n"
        return

    lc_messages = _to_langchain_messages(messages)

    try:
        async for chunk in llm.astream(lc_messages):
            content = chunk.content
            if content:
                yield f"data: {json.dumps({'content': content}, ensure_ascii=False)}\n\n"
    except Exception as e:
        logger.error("SSE stream error: %s", e)
        yield f"data: {json.dumps({'error': str(e)}, ensure_ascii=False)}\n\n"

    yield "data: [DONE]\n\n"


async def invoke_model(
    db: AsyncSession,
    model: str,
    messages: list[dict],
) -> str:
    """非流式调用，返回完整响应文本。"""
    try:
        llm = await _resolve_model(db, model)
    except ValueError as e:
        return f"Error: {e}"

    # 非流式模式
    llm_non_stream = llm.model_copy(update={"streaming": False})
    lc_messages = _to_langchain_messages(messages)

    try:
        response = await llm_non_stream.ainvoke(lc_messages)
        return response.content
    except Exception as e:
        logger.error("Model invoke error: %s", e)
        return f"Error: {e}"
