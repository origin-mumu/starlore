"""AI 相关 Schema。"""

from datetime import datetime

from pydantic import BaseModel


class CreateSessionRequest(BaseModel):
    title: str | None = None
    characterKey: str | None = None
    modelId: str | None = None


class UpdateSessionRequest(BaseModel):
    title: str | None = None
    characterKey: str | None = None
    modelId: str | None = None


class AppendPairRequest(BaseModel):
    userContent: str | None = None
    assistantContent: str | None = None


class AIConfigRequest(BaseModel):
    modelKey: str | None = None
    modelName: str | None = None
    apiUrl: str | None = None
    modelId: str | None = None
    apiKey: str | None = None
    enabled: bool | None = None


class ModelInfo(BaseModel):
    id: str
    name: str
    configured: bool


class AIModelsResponse(BaseModel):
    success: bool
    models: list[ModelInfo]


class SessionData(BaseModel):
    id: int
    title: str
    characterKey: str
    modelId: str | None = None
    createdAt: datetime | None = None
    updatedAt: datetime | None = None


class AISessionResponse(BaseModel):
    success: bool
    session: SessionData


class SessionItem(BaseModel):
    id: int
    title: str
    characterKey: str
    modelId: str | None = None
    createdAt: datetime | None = None
    updatedAt: datetime | None = None


class AISessionListResponse(BaseModel):
    success: bool
    sessions: list[SessionItem]


class MessageItem(BaseModel):
    id: int
    role: str
    content: str | None = None
    createdAt: datetime | None = None


class SessionInfo(BaseModel):
    id: int
    title: str
    characterKey: str
    modelId: str | None = None


class AIMessageListResponse(BaseModel):
    success: bool
    session: SessionInfo
    messages: list[MessageItem]


class CharacterCard(BaseModel):
    key: str
    name: str
    description: str
    systemPrompt: str


class AICharacterCardsResponse(BaseModel):
    success: bool
    cards: list[CharacterCard]


class QuotaInfo(BaseModel):
    dailyLimit: int
    used: int
    remaining: int
    isAdmin: bool


class AIQuotaResponse(BaseModel):
    success: bool
    quota: QuotaInfo
