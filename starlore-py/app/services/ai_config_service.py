import logging
import httpx
from datetime import datetime

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.exceptions import BadRequestException, ConflictException, NotFoundException
from app.models.ai_config import AiConfig
from app.schemas.ai import AIConfigRequest

logger = logging.getLogger(__name__)


async def fetch_provider_models(api_url: str, api_key: str | None) -> list[dict]:
    """从厂商官方接口动态拉取最新模型列表。"""
    base_url = (api_url or "").rstrip("/")
    if base_url.endswith("/chat/completions"):
        base_url = base_url[:-17].rstrip("/")

    # 优先请求 /models 与 /v1/models
    urls_to_try = [
        f"{base_url}/models",
        f"{base_url}/v1/models",
    ]
    if base_url.endswith("/v1"):
        urls_to_try = [f"{base_url}/models", f"{base_url[:-3]}/models"]

    headers = {
        "Accept": "application/json",
    }
    if api_key:
        headers["Authorization"] = f"Bearer {api_key.strip()}"
        if "xiaomimimo.com" in base_url:
            headers["api-key"] = api_key.strip()

    async with httpx.AsyncClient(timeout=10.0, trust_env=True, verify=False) as client:
        for url in urls_to_try:
            try:
                resp = await client.get(url, headers=headers)
                logger.info("Fetch models from %s -> status %s", url, resp.status_code)
                if resp.status_code == 200:
                    data = resp.json()
                    items = data.get("data", []) if isinstance(data, dict) else (data if isinstance(data, list) else [])
                    models = []
                    for item in items:
                        m_id = item.get("id") if isinstance(item, dict) else str(item)
                        if m_id and "embedding" not in m_id.lower() and "rerank" not in m_id.lower():
                            models.append({
                                "id": m_id,
                                "name": item.get("name") or m_id,
                                "owned_by": item.get("owned_by", "") if isinstance(item, dict) else "",
                            })
                    if models:
                        return models
                else:
                    logger.warning("Fetch models from %s failed: status=%s, body=%s", url, resp.status_code, resp.text[:200])
            except Exception as e:
                logger.warning("Failed to fetch models from %s: %s", url, e)
                continue
    return []


async def get_all_configs(db: AsyncSession) -> list[AiConfig]:
    result = await db.execute(select(AiConfig).order_by(AiConfig.modelKey))
    return list(result.scalars().all())


async def get_config_by_id(db: AsyncSession, config_id: int) -> AiConfig:
    result = await db.execute(select(AiConfig).where(AiConfig.id == config_id))
    config = result.scalar_one_or_none()
    if config is None:
        raise NotFoundException("配置不存在")
    return config


async def get_config_by_key(db: AsyncSession, model_key: str, fallback: bool = False) -> AiConfig | None:
    result = await db.execute(
        select(AiConfig).where(
            (AiConfig.modelKey == model_key) | (AiConfig.modelName == model_key)
        )
    )
    res = result.scalar_one_or_none()
    if res:
        return res
    all_cfgs = await get_all_configs(db)
    for c in all_cfgs:
        if (c.modelKey and c.modelKey.lower() == model_key.lower()) or (c.modelName and c.modelName.lower() == model_key.lower()):
            return c
    return all_cfgs[0] if (fallback and all_cfgs) else None


async def create_config(db: AsyncSession, req: AIConfigRequest) -> AiConfig:
    if not req.modelKey or not req.modelName or not req.apiUrl or not req.modelId:
        raise BadRequestException("modelKey, modelName, apiUrl, modelId 不能为空")

    # 检查 modelKey 唯一性
    existing = await get_config_by_key(db, req.modelKey)
    if existing is not None:
        raise ConflictException("modelKey 已存在")

    now = datetime.now()
    config = AiConfig(
        modelKey=req.modelKey,
        modelName=req.modelName,
        apiUrl=req.apiUrl,
        modelId=req.modelId,
        apiKey=req.apiKey,
        enabled=req.enabled if req.enabled is not None else True,
        createdAt=now,
        updatedAt=now,
    )
    db.add(config)
    await db.flush()
    return config


async def update_config(db: AsyncSession, config_id: int, req: AIConfigRequest) -> AiConfig:
    config = await get_config_by_id(db, config_id)

    if req.modelKey is not None:
        if req.modelKey != config.modelKey:
            existing = await get_config_by_key(db, req.modelKey)
            if existing is not None:
                raise ConflictException("modelKey 已存在")
        config.modelKey = req.modelKey
    if req.modelName is not None:
        config.modelName = req.modelName
    if req.apiUrl is not None:
        config.apiUrl = req.apiUrl
    if req.modelId is not None:
        config.modelId = req.modelId
    if req.apiKey is not None:
        config.apiKey = req.apiKey
    if req.enabled is not None:
        config.enabled = req.enabled

    config.updatedAt = datetime.now()
    await db.flush()
    return config


async def delete_config(db: AsyncSession, config_id: int) -> None:
    config = await get_config_by_id(db, config_id)
    await db.delete(config)
    await db.flush()

