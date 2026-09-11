"""Harness 相关 Pydantic Schema。"""

from datetime import datetime
from typing import Any
from pydantic import BaseModel, ConfigDict


class HarnessSessionCreate(BaseModel):
    title: str | None = "新会话"
    model_id: str | None = None


class HarnessSessionUpdate(BaseModel):
    title: str | None = None
    pinned: bool | None = None
    model_id: str | None = None


class HarnessSessionResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    title: str
    model_id: str | None = None
    status: str
    pinned: bool
    created_at: datetime | None = None
    updated_at: datetime | None = None


class HarnessSessionListResponse(BaseModel):
    items: list[HarnessSessionResponse]


class HarnessMessageResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    session_id: int
    role: str
    content: str | None = None
    reasoning_content: str | None = None
    tool_calls: Any | None = None
    artifacts: Any | None = None
    images: list[str] | None = None
    step_details: Any | None = None
    tokens_prompt: int = 0
    tokens_completion: int = 0
    duration_ms: int = 0
    created_at: datetime | None = None


class HarnessChatRequest(BaseModel):
    message: str
    images: list[str] | None = None
    model_id: str | None = None


class HarnessModelItem(BaseModel):
    id: str
    name: str
    vendor: str


class HarnessModelsResponse(BaseModel):
    items: list[HarnessModelItem]
