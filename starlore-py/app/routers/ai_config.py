"""AI 配置管理路由。"""

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import require_admin
from app.models.user import User
from app.schemas.ai import AIConfigRequest, AiConfigResponse
from app.schemas.common import MessageResponse, SimpleResponse
from app.services import ai_config_service

router = APIRouter(prefix="/api/ai-config", tags=["ai-config"])


def _mask_api_key(key: str | None) -> str:
    """掩码 API 密钥，只显示后 4 位。"""
    if not key or len(key) <= 8:
        return "****"
    return f"****{key[-4:]}"


def _to_masked_config(c) -> dict:
    return {
        "id": c.id,
        "modelKey": c.modelKey,
        "modelName": c.modelName,
        "apiUrl": c.apiUrl,
        "modelId": c.modelId,
        "apiKey": _mask_api_key(c.apiKey),
        "enabled": c.enabled,
        "createdAt": c.createdAt,
        "updatedAt": c.updatedAt,
    }


@router.get("")
async def get_all_configs(
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    configs = await ai_config_service.get_all_configs(db)
    return {
        "data": [
            {
                "id": c.id,
                "modelKey": c.modelKey,
                "modelName": c.modelName,
                "apiUrl": c.apiUrl,
                "modelId": c.modelId,
                "apiKey": _mask_api_key(c.apiKey),
                "enabled": c.enabled,
                "createdAt": c.createdAt,
                "updatedAt": c.updatedAt,
            }
            for c in configs
        ]
    }


@router.get("/{config_id}")
async def get_config_by_id(
    config_id: int,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    config = await ai_config_service.get_config_by_id(db, config_id)
    return {
        "data": {
            "id": config.id,
            "modelKey": config.modelKey,
            "modelName": config.modelName,
            "apiUrl": config.apiUrl,
            "modelId": config.modelId,
            "apiKey": _mask_api_key(config.apiKey),
            "enabled": config.enabled,
        }
    }


@router.post("", response_model=AiConfigResponse)
async def create_config(
    req: AIConfigRequest,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    config = await ai_config_service.create_config(db, req)
    return {"message": "配置创建成功", "data": _to_masked_config(config)}


@router.put("/{config_id}", response_model=AiConfigResponse)
async def update_config(
    config_id: int,
    req: AIConfigRequest,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    config = await ai_config_service.update_config(db, config_id, req)
    return {"message": "配置更新成功", "data": _to_masked_config(config)}


@router.delete("/{config_id}", response_model=MessageResponse)
async def delete_config(
    config_id: int,
    admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    await ai_config_service.delete_config(db, config_id)
    return {"message": "配置删除成功"}
