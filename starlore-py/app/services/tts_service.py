"""AI TTS 文字转语音服务 — 调用 MiMo TTS API。"""

import logging

import httpx

from app.config import settings
from app.services import ai_config_service
from sqlalchemy.ext.asyncio import AsyncSession

logger = logging.getLogger(__name__)

MIMO_TTS_MODEL = "mimo-tts"


async def synthesize(db: AsyncSession, text: str) -> bytes:
    """调用 MiMo TTS API，返回 WAV 音频二进制。"""
    # 从 DB 获取 MiMo TTS 配置
    config = await ai_config_service.get_config_by_key(db, MIMO_TTS_MODEL)
    if not config or not config.enabled or not config.apiKey:
        raise ValueError("MiMo TTS 未配置，请在 ai_configs 表中添加 mimo-tts 配置")

    api_url = config.apiUrl
    if not api_url.endswith("/chat/completions"):
        api_url = api_url.rstrip("/") + "/chat/completions"

    headers = {
        "Content-Type": "application/json",
        "api-key": config.apiKey,
    }

    payload = {
        "model": config.modelId or "mimo-tts",
        "messages": [{"role": "user", "content": text}],
        "stream": False,
        "response_format": {"type": "audio"},
    }

    async with httpx.AsyncClient(timeout=30.0) as client:
        resp = await client.post(api_url, json=payload, headers=headers)
        resp.raise_for_status()

        # MiMo TTS 返回 JSON，包含 base64 音频
        data = resp.json()
        import base64
        audio_b64 = data.get("choices", [{}])[0].get("message", {}).get("audio", {}).get("data", "")
        if not audio_b64:
            raise ValueError("TTS 返回为空，请检查配置")

        return base64.b64decode(audio_b64)
