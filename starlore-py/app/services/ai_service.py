"""AI 会话/消息管理服务。"""

import json
import logging
import os
from datetime import datetime
from pathlib import Path

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.exceptions import BadRequestException, NotFoundException, UnauthorizedException
from app.models.ai_message import AiMessage
from app.models.ai_session import AiSession
from app.schemas.ai import (
    AppendPairRequest,
    CharacterCard,
    CreateSessionRequest,
    MessageItem,
    ModelInfo,
    SessionData,
    SessionInfo,
    SessionItem,
    UpdateSessionRequest,
)
from app.services import ai_config_service

logger = logging.getLogger(__name__)


async def get_models(db: AsyncSession | None = None) -> list[ModelInfo]:
    """获取可用 AI 模型列表。"""
    configs = await ai_config_service.get_all_configs(db)

    if configs:
        models = []
        for cfg in configs:
            models.append(ModelInfo(
                id=cfg.modelKey,
                name=cfg.modelName,
                configured=cfg.enabled and bool(cfg.apiKey),
            ))
        return models

    # 回退：检查环境变量
    models = []
    if os.environ.get("AI_KEY_QWEN") or os.environ.get("VITE_API_KEY"):
        models.append(ModelInfo(id="qwen-plus", name="通义千问", configured=True))
    if os.environ.get("AI_KEY_MIMO") or os.environ.get("VITE_MIMO_API_KEY"):
        models.append(ModelInfo(id="mimo", name="MiMo", configured=True))
    return models


def get_character_cards() -> list[CharacterCard]:
    """加载角色卡配置。"""
    cards_path = Path(__file__).parent.parent.parent / "resources" / "ai_character_cards.json"
    if not cards_path.exists():
        cards_path = Path("resources/ai_character_cards.json")
    if not cards_path.exists():
        return []
    try:
        with open(cards_path, encoding="utf-8") as f:
            data = json.load(f)
        return [CharacterCard(**card) for card in data]
    except Exception as e:
        logger.warning("Failed to load character cards: %s", e)
        return []


async def list_sessions(db: AsyncSession, user_id: int) -> list[SessionItem]:
    """列出用户的会话（最多 80 条）。"""
    result = await db.execute(
        select(AiSession)
        .where(AiSession.user_id == user_id)
        .order_by(AiSession.updatedAt.desc())
        .limit(80)
    )
    sessions = result.scalars().all()
    return [
        SessionItem(
            id=s.id,
            title=s.title,
            characterKey=s.characterKey,
            modelId=s.modelId,
            createdAt=s.createdAt,
            updatedAt=s.updatedAt,
        )
        for s in sessions
    ]


async def create_session(db: AsyncSession, user_id: int, req: CreateSessionRequest) -> SessionData:
    """创建新会话。"""
    now = datetime.now()
    session = AiSession(
        user_id=user_id,
        title=req.title or "新会话",
        characterKey=req.characterKey or "default",
        modelId=req.modelId or "deepseek-chat",
        createdAt=now,
        updatedAt=now,
    )
    db.add(session)
    await db.flush()
    return SessionData(
        id=session.id,
        title=session.title,
        characterKey=session.characterKey,
        modelId=session.modelId,
        createdAt=session.createdAt,
        updatedAt=session.updatedAt,
    )


async def _verify_ownership(db: AsyncSession, user_id: int, session_id: int) -> AiSession:
    """验证会话归属。"""
    result = await db.execute(select(AiSession).where(AiSession.id == session_id))
    session = result.scalar_one_or_none()
    if session is None:
        raise NotFoundException("会话不存在")
    if session.user_id != user_id:
        raise UnauthorizedException("无权访问该会话")
    return session


async def update_session(db: AsyncSession, user_id: int, session_id: int, req: UpdateSessionRequest) -> SessionData:
    session = await _verify_ownership(db, user_id, session_id)

    if req.title is not None:
        session.title = req.title[:255]
    if req.characterKey is not None:
        session.characterKey = req.characterKey[:64]
    if req.modelId is not None:
        session.modelId = req.modelId[:32]

    session.updatedAt = datetime.now()
    await db.flush()
    return SessionData(
        id=session.id,
        title=session.title,
        characterKey=session.characterKey,
        modelId=session.modelId,
        createdAt=session.createdAt,
        updatedAt=session.updatedAt,
    )


async def delete_session(db: AsyncSession, user_id: int, session_id: int) -> None:
    session = await _verify_ownership(db, user_id, session_id)

    # 级联删除消息
    result = await db.execute(select(AiMessage).where(AiMessage.sessionId == session_id))
    for msg in result.scalars().all():
        await db.delete(msg)

    await db.delete(session)
    await db.flush()


async def get_messages(db: AsyncSession, user_id: int, session_id: int) -> dict:
    session = await _verify_ownership(db, user_id, session_id)

    result = await db.execute(
        select(AiMessage).where(AiMessage.sessionId == session_id).order_by(AiMessage.id)
    )
    messages = [
        MessageItem(id=m.id, role=m.role, content=m.content, agentTrace=m.agentTrace, createdAt=m.createdAt)
        for m in result.scalars().all()
    ]
    return {
        "session": SessionInfo(id=session.id, title=session.title, characterKey=session.characterKey, modelId=session.modelId),
        "messages": messages,
    }


async def append_pair(db: AsyncSession, user_id: int, session_id: int, req: AppendPairRequest) -> None:
    """追加一对 user + assistant 消息。"""
    session = await _verify_ownership(db, user_id, session_id)

    if not req.userContent or not req.assistantContent:
        raise BadRequestException("userContent 和 assistantContent 不能为空")

    now = datetime.now()

    db.add(AiMessage(sessionId=session_id, role="user", content=req.userContent, createdAt=now))
    db.add(AiMessage(sessionId=session_id, role="assistant", content=req.assistantContent, agentTrace=req.agentTrace, createdAt=now))

    # 自动重命名会话
    if session.title == "新会话":
        title = req.userContent[:28]
        if len(req.userContent) > 28:
            title += "..."
        session.title = title

    session.updatedAt = datetime.now()
    await db.flush()
