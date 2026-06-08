"""AI 模型配置 CRUD 服务。"""

from datetime import datetime

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.exceptions import BadRequestException, ConflictException, NotFoundException
from app.models.ai_config import AiConfig
from app.schemas.ai import AIConfigRequest


async def get_all_configs(db: AsyncSession) -> list[AiConfig]:
    result = await db.execute(select(AiConfig).order_by(AiConfig.modelKey))
    return list(result.scalars().all())


async def get_config_by_id(db: AsyncSession, config_id: int) -> AiConfig:
    result = await db.execute(select(AiConfig).where(AiConfig.id == config_id))
    config = result.scalar_one_or_none()
    if config is None:
        raise NotFoundException("配置不存在")
    return config


async def get_config_by_key(db: AsyncSession, model_key: str) -> AiConfig | None:
    result = await db.execute(select(AiConfig).where(AiConfig.modelKey == model_key))
    return result.scalar_one_or_none()


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
