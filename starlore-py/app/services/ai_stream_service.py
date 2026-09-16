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


async def _available_upstream_models(db: AsyncSession, config) -> list[str]:
    """获取该厂商配置下真实可用的上游模型名列表（复用 harness 的动态模型缓存，5 分钟有效）。"""
    try:
        from app.services import harness_service

        await harness_service.get_dynamic_models(db)
        vendor_map = harness_service._MODEL_CACHE.get("model_vendor_map", {})
        base = (config.apiUrl or "").rstrip("/")
        seen: list[str] = []
        for url, _key, real_mid in vendor_map.values():
            if url.rstrip("/") == base and real_mid not in seen:
                seen.append(real_mid)
        return seen
    except Exception as e:
        logger.warning("Failed to fetch available upstream models: %s", e)
        return []


def _pick_fallback_model(available: list[str], config) -> str | None:
    """从真实可用列表中挑选默认模型：配置默认 → deepseek 系 → 列表第一个。"""
    if not available:
        return None
    if config.modelId and config.modelId in available:
        return config.modelId
    for m in available:
        if "deepseek" in m.lower():
            return m
    return available[0]


async def _resolve_model(db: AsyncSession, model: str, temperature: float = 0.7) -> ChatOpenAI:
    """解析模型配置，返回 LangChain ChatOpenAI 实例。"""
    provider_key = None
    target_model = (model or "").strip()

    if "::" in target_model:
        provider_key, target_model = target_model.split("::", 1)

    all_configs = await ai_config_service.get_all_configs(db)
    active_configs = [c for c in all_configs if c.enabled and c.apiKey]

    db_config = None
    if provider_key:
        db_config = next((c for c in active_configs if c.modelKey == provider_key or c.modelName.lower() == provider_key.lower()), None)

    if not db_config:
        db_config = next((c for c in active_configs if c.modelKey == target_model), None)

    if not db_config and active_configs:
        for cfg in active_configs:
            if cfg.modelKey.lower() in target_model.lower() or cfg.modelName.lower() in target_model.lower():
                db_config = cfg
                break
        if not db_config:
            db_config = active_configs[0]

    if db_config and db_config.enabled and db_config.apiKey:
        base_url = db_config.apiUrl
        if base_url.endswith("/chat/completions"):
            base_url = base_url[:-17]
        base_url = base_url.rstrip("/")

        extra_kwargs = {}
        if "xiaomimimo.com" in base_url:
            extra_kwargs["default_headers"] = {"api-key": db_config.apiKey}

        # 动态使用传入的具体模型名，若传入的是 providerKey 则使用默认 modelId
        actual_model = target_model if target_model and target_model != db_config.modelKey else (db_config.modelId or target_model)

        # 校验模型在该厂商真实存在，不存在的旧模型名（如 deepseek-chat）回退到网关实际可用模型
        available = await _available_upstream_models(db, db_config)
        if available:
            if actual_model not in available:
                fallback = _pick_fallback_model(available, db_config)
                if fallback:
                    logger.info("Model %r unavailable at %s, falling back to %r", actual_model, base_url, fallback)
                    actual_model = fallback
        elif not actual_model:
            actual_model = db_config.modelId or "deepseek-chat"

        return ChatOpenAI(
            base_url=base_url,
            api_key=db_config.apiKey,
            model=actual_model,
            temperature=temperature,
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
