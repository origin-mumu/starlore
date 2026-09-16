"""Harness 路由：会话管理、动态模型获取、SSE 流式内容生成。"""

import json
import logging
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.schemas.harness import (
    HarnessChatRequest,
    HarnessMessageResponse,
    HarnessModelsResponse,
    HarnessSessionCreate,
    HarnessSessionListResponse,
    HarnessSessionResponse,
    HarnessSessionUpdate,
)
from app.services import ai_quota_service, harness_agent_service, harness_service

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/harness", tags=["harness"])


@router.get("/models", response_model=HarnessModelsResponse)
async def get_models(
    db: AsyncSession = Depends(get_db),
    user: User = Depends(get_current_user),
):
    """动态获取已配置厂商的最新可用对话模型列表。"""
    models = await harness_service.get_dynamic_models(db)
    return HarnessModelsResponse(items=models)


@router.get("/sessions", response_model=HarnessSessionListResponse)
async def list_sessions(
    db: AsyncSession = Depends(get_db),
    user: User = Depends(get_current_user),
):
    """获取当前用户的会话列表。"""
    sessions = await harness_service.list_sessions(db, user.id)
    return HarnessSessionListResponse(items=sessions)


@router.post("/sessions", response_model=HarnessSessionResponse)
async def create_session(
    payload: HarnessSessionCreate,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(get_current_user),
):
    """创建新会话。"""
    session = await harness_service.create_session(
        db=db, user_id=user.id, title=payload.title, model_id=payload.model_id
    )
    return session


@router.get("/sessions/{session_id}", response_model=HarnessSessionResponse)
async def get_session(
    session_id: int,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(get_current_user),
):
    """获取单个会话详情。"""
    session = await harness_service.get_session(db, session_id, user.id)
    if not session:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="会话不存在")
    return session


@router.patch("/sessions/{session_id}", response_model=HarnessSessionResponse)
async def update_session(
    session_id: int,
    payload: HarnessSessionUpdate,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(get_current_user),
):
    """重命名会话、置顶或切换模型。"""
    session = await harness_service.update_session(
        db=db,
        session_id=session_id,
        user_id=user.id,
        title=payload.title,
        pinned=payload.pinned,
        model_id=payload.model_id,
    )
    if not session:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="会话不存在")
    return session


@router.delete("/sessions/{session_id}")
async def delete_session(
    session_id: int,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(get_current_user),
):
    """删除会话及其所有关联消息。"""
    ok = await harness_service.delete_session(db, session_id, user.id)
    if not ok:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="会话不存在")
    return {"success": True}


@router.get("/sessions/{session_id}/messages", response_model=list[HarnessMessageResponse])
async def list_messages(
    session_id: int,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(get_current_user),
):
    """获取会话历史消息。"""
    session = await harness_service.get_session(db, session_id, user.id)
    if not session:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="会话不存在")
    messages = await harness_service.list_messages(db, session_id, user.id)
    results: list[HarnessMessageResponse] = []
    for m in messages:
        resp = HarnessMessageResponse.model_validate(m)
        if m.artifacts and isinstance(m.artifacts, dict) and "images" in m.artifacts:
            resp.images = m.artifacts.get("images")
        results.append(resp)
    return results


@router.post("/sessions/{session_id}/chat")
async def chat_stream(
    session_id: int,
    payload: HarnessChatRequest,
    db: AsyncSession = Depends(get_db),
    user: User = Depends(get_current_user),
):
    """SSE 流式交互端点：执行 ReAct 智能体循环生成内容。"""
    session = await harness_service.get_session(db, session_id, user.id)
    if not session:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="会话不存在")

    # 按用户角色限制每日调用次数：admin 无限，member 99 次，user 10 次
    remaining = await ai_quota_service.get_remaining(db, user.id)
    if remaining == 0:
        async def quota_exhausted():
            yield (
                "event: error\n"
                "data: "
                + json.dumps({"message": "今日 AI 调用次数已用尽，请明天再来或联系管理员提升额度"}, ensure_ascii=False)
                + "\n\n"
            )

        return StreamingResponse(
            quota_exhausted(),
            media_type="text/event-stream",
            headers={"Cache-Control": "no-cache", "Connection": "keep-alive"},
        )
    await ai_quota_service.try_consume(db, user.id)

    stream_gen = harness_agent_service.run_harness_turn(
        db=db,
        session_id=session_id,
        user_id=user.id,
        user_input=payload.message,
        images=payload.images,
        override_model_id=payload.model_id,
    )

    return StreamingResponse(
        stream_gen,
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )
