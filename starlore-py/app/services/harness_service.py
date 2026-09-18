"""Harness 会话管理与多厂商动态模型发现服务。"""

import asyncio
import logging
import time
from typing import Any

import httpx
from sqlalchemy import delete, desc, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.ai_config import AiConfig
from app.models.harness_message import HarnessMessage
from app.models.harness_session import HarnessSession
from app.schemas.harness import HarnessModelItem

logger = logging.getLogger(__name__)

# 内存模型缓存: {"models": [...], "expires_at": float, "vendors": {model_id: (api_url, api_key)}}
_MODEL_CACHE: dict[str, Any] = {
    "models": [],
    "expires_at": 0.0,
    "model_vendor_map": {},
}
_CACHE_TTL = 300  # 5 分钟缓存


async def list_sessions(db: AsyncSession, user_id: int) -> list[HarnessSession]:
    """获取用户的会话列表，置顶优先，最新修改在最前。"""
    stmt = (
        select(HarnessSession)
        .where(HarnessSession.user_id == user_id)
        .order_by(desc(HarnessSession.pinned), desc(HarnessSession.updated_at))
    )
    result = await db.execute(stmt)
    return list(result.scalars().all())


async def create_session(
    db: AsyncSession, user_id: int, title: str | None = None, model_id: str | None = None
) -> HarnessSession:
    """创建新会话。"""
    session = HarnessSession(
        user_id=user_id,
        title=title or "新会话",
        model_id=model_id or "deepseek-chat",
        status="idle",
    )
    db.add(session)
    await db.commit()
    await db.refresh(session)
    return session


async def get_session(db: AsyncSession, session_id: int, user_id: int) -> HarnessSession | None:
    """获取单个会话。"""
    stmt = select(HarnessSession).where(
        HarnessSession.id == session_id, HarnessSession.user_id == user_id
    )
    result = await db.execute(stmt)
    return result.scalar_one_or_none()


async def update_session(
    db: AsyncSession,
    session_id: int,
    user_id: int,
    title: str | None = None,
    pinned: bool | None = None,
    model_id: str | None = None,
) -> HarnessSession | None:
    """更新会话。"""
    session = await get_session(db, session_id, user_id)
    if not session:
        return None

    if title is not None:
        session.title = title
    if pinned is not None:
        session.pinned = pinned
    if model_id is not None:
        session.model_id = model_id

    await db.commit()
    await db.refresh(session)
    return session


async def delete_session(db: AsyncSession, session_id: int, user_id: int) -> bool:
    """删除会话及其所有关联消息。"""
    session = await get_session(db, session_id, user_id)
    if not session:
        return False

    # 删除该会话下的所有消息
    await db.execute(
        delete(HarnessMessage).where(HarnessMessage.session_id == session_id)
    )
    # 删除会话本身
    await db.delete(session)
    await db.commit()
    return True


async def list_messages(
    db: AsyncSession, session_id: int, user_id: int
) -> list[HarnessMessage]:
    """获取指定会话的历史消息列表。"""
    stmt = (
        select(HarnessMessage)
        .where(HarnessMessage.session_id == session_id, HarnessMessage.user_id == user_id)
        .order_by(HarnessMessage.created_at.asc())
    )
    result = await db.execute(stmt)
    return list(result.scalars().all())


async def _fetch_vendor_models(
    client: httpx.AsyncClient, config: AiConfig
) -> list[tuple[HarnessModelItem, str, str, str]]:
    """向厂商的官方 /models 接口动态拉取可用模型列表。
    返回: [(model_item, api_url, api_key, upstream_model_id)]
    """
    results: list[tuple[HarnessModelItem, str, str, str]] = []
    api_url = config.apiUrl.strip().rstrip("/")
    api_key = config.apiKey.strip() if config.apiKey else ""
    vendor_name = config.modelName or config.modelKey or "AI"
    vendor_key = (config.modelKey or config.modelName or "vendor").strip().lower()

    if not api_url or not api_key:
        return results

    models_url = f"{api_url}/models"
    headers = {"Authorization": f"Bearer {api_key}"}

    try:
        resp = await client.get(models_url, headers=headers)
        if resp.status_code == 200:
            data = resp.json().get("data", [])
            for item in data:
                m_id = item.get("id", "")
                if not m_id:
                    continue
                # 过滤掉非对话类模型（如 embedding, rerank, moderation 等）
                lower_id = m_id.lower()
                if any(
                    x in lower_id
                    for x in ["embedding", "rerank", "moderation", "tts", "whisper", "dall-e"]
                ):
                    continue

                display_name = item.get("name") or m_id
                scoped_id = f"{vendor_key}::{m_id}"
                model_item = HarnessModelItem(id=scoped_id, name=display_name, vendor=vendor_name)
                results.append((model_item, api_url, api_key, m_id))
        else:
            logger.warning(
                "Vendor %s /models returned status %s: %s",
                vendor_name,
                resp.status_code,
                resp.text[:200],
            )
    except Exception as e:
        logger.warning("Failed to fetch models from %s (%s): %s", vendor_name, models_url, e)

    # 兜底：如果 /models 请求失败但配置了 modelId，则将配置的 modelId 加入
    if not results and config.modelId:
        scoped_id = f"{vendor_key}::{config.modelId}"
        results.append(
            (
                HarnessModelItem(id=scoped_id, name=config.modelId, vendor=vendor_name),
                api_url,
                api_key,
                config.modelId,
            )
        )

    return results


async def get_dynamic_models(db: AsyncSession) -> list[HarnessModelItem]:
    """从数据库读取所有启用的厂商配置，动态向官方端点获取最新的模型列表（带 5 分钟缓存）。"""
    global _MODEL_CACHE
    now = time.time()
    if _MODEL_CACHE["models"] and now < _MODEL_CACHE["expires_at"]:
        return _MODEL_CACHE["models"]

    stmt = select(AiConfig).where(AiConfig.enabled.is_(True))
    result = await db.execute(stmt)
    configs = list(result.scalars().all())

    model_items: list[HarnessModelItem] = []
    vendor_map: dict[str, tuple[str, str, str]] = {}

    async with httpx.AsyncClient(timeout=10.0) as client:
        tasks = [_fetch_vendor_models(client, cfg) for cfg in configs]
        fetched_lists = await asyncio.gather(*tasks, return_exceptions=True)

        for res in fetched_lists:
            if isinstance(res, list):
                for item, url, key, upstream_mid in res:
                    model_items.append(item)
                    # 同时支持作用域 ID 和原始 ID 查找
                    vendor_map[item.id] = (url, key, upstream_mid)
                    # 若该原始 ID 尚未在 vendor_map 中，或当前厂商是 DeepSeek 官方且模型是 deepseek，则优先记录
                    if (
                        upstream_mid not in vendor_map
                        or ("deepseek" in upstream_mid.lower() and "deepseek.com" in url)
                    ):
                        vendor_map[upstream_mid] = (url, key, upstream_mid)

    # 无真实模型时保持空列表：没有配置可用厂商就如实返回空，绝不伪造模型
    _MODEL_CACHE["models"] = model_items
    _MODEL_CACHE["expires_at"] = now + _CACHE_TTL
    _MODEL_CACHE["model_vendor_map"] = vendor_map

    return model_items


async def resolve_model_credentials(
    db: AsyncSession, model_id: str
) -> tuple[str, str, str]:
    """根据选择的 model_id 解析其对应的 (apiUrl, apiKey, upstream_model_id)。"""
    await get_dynamic_models(db)
    vendor_map = _MODEL_CACHE.get("model_vendor_map", {})

    if model_id in vendor_map:
        url, key, real_mid = vendor_map[model_id]
        return url, key, real_mid

    if "::" in model_id:
        vendor_key, real_mid = model_id.split("::", 1)
        stmt = select(AiConfig).where(AiConfig.enabled.is_(True))
        result = await db.execute(stmt)
        configs = list(result.scalars().all())
        for cfg in configs:
            cfg_vk = (cfg.modelKey or cfg.modelName or "").lower()
            if vendor_key in cfg_vk or cfg_vk in vendor_key:
                return cfg.apiUrl, cfg.apiKey or "", real_mid

    # 如果传的是无前缀名称（如历史旧消息），优先匹配最符合的厂商配置
    stmt = select(AiConfig).where(AiConfig.enabled.is_(True))
    result = await db.execute(stmt)
    configs = list(result.scalars().all())
    lower_mid = model_id.lower()
    for cfg in configs:
        cfg_name = (cfg.modelName or cfg.modelKey or "").lower()
        if "deepseek" in lower_mid and "deepseek" in cfg_name:
            return cfg.apiUrl, cfg.apiKey or "", model_id
        if "qwen" in lower_mid and "qwen" in cfg_name:
            return cfg.apiUrl, cfg.apiKey or "", model_id

    if configs:
        first = configs[0]
        return first.apiUrl, first.apiKey or "", model_id

    raise ValueError("未找到可用的 AI 厂商配置，请先在后台 AI 配置中启用厂商并填写密钥")


# ---------- 任务清单（会话级整表快照，last-write-wins） ----------

async def save_harness_todos(
    db: AsyncSession, session_id: int, user_id: int, todos: list[dict]
) -> None:
    """保存会话任务清单快照（每会话一行，整体覆盖）。"""
    from datetime import datetime

    from app.models.harness_todo import HarnessTodo

    result = await db.execute(select(HarnessTodo).where(HarnessTodo.session_id == session_id))
    row = result.scalars().first()
    if row:
        row.todos = todos
        row.user_id = user_id
        row.updated_at = datetime.utcnow()
    else:
        db.add(HarnessTodo(session_id=session_id, user_id=user_id, todos=todos))
    await db.flush()


async def get_harness_todos(db: AsyncSession, session_id: int, user_id: int) -> list[dict]:
    """读取会话当前任务清单快照。"""
    from app.models.harness_todo import HarnessTodo

    result = await db.execute(
        select(HarnessTodo).where(
            HarnessTodo.session_id == session_id, HarnessTodo.user_id == user_id
        )
    )
    row = result.scalars().first()
    return list(row.todos or []) if row and row.todos else []
