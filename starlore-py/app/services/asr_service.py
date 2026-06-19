"""AI ASR 语音转文字服务 — 调用 MiMo ASR API。"""

import logging
import json
import httpx
from typing import AsyncGenerator
from sqlalchemy.ext.asyncio import AsyncSession
from app.services import ai_config_service

logger = logging.getLogger(__name__)

MIMO_ASR_MODEL_KEY = "mimo"
ASR_MODEL_NAME = "mimo-v2.5-asr"


async def transcribe(db: AsyncSession, audio_uri: str) -> str:
    """非流式语音转录，一次性返回全文。"""
    config = await ai_config_service.get_config_by_key(db, MIMO_ASR_MODEL_KEY)
    if not config or not config.enabled or not config.apiKey:
        raise ValueError("MiMo ASR 未配置，请在 ai_configs 表中启用 modelKey='mimo'")

    api_url = config.apiUrl.rstrip("/") + "/chat/completions"
    headers = {
        "Content-Type": "application/json",
        "api-key": config.apiKey,
    }

    payload = {
        "model": ASR_MODEL_NAME,
        "messages": [
            {
                "role": "user",
                "content": [
                    {
                        "type": "input_audio",
                        "input_audio": {"data": audio_uri}
                    }
                ]
            }
        ],
        "asr_options": {"language": "auto"},
        "stream": False
    }

    async with httpx.AsyncClient(timeout=60.0) as client:
        resp = await client.post(api_url, json=payload, headers=headers)
        resp.raise_for_status()
        data = resp.json()
        content = data.get("choices", [{}])[0].get("message", {}).get("content", "").strip()
        return content


async def transcribe_stream(db: AsyncSession, audio_uri: str) -> AsyncGenerator[str, None]:
    """流式语音转录，yield SSE 格式事件字符串。"""
    config = await ai_config_service.get_config_by_key(db, MIMO_ASR_MODEL_KEY)
    if not config or not config.enabled or not config.apiKey:
        yield f"data: {json.dumps({'error': 'MiMo ASR 未配置，请在 ai_configs 表中启用 modelKey=mimo'}, ensure_ascii=False)}\n\n"
        return

    api_url = config.apiUrl.rstrip("/") + "/chat/completions"
    headers = {
        "Content-Type": "application/json",
        "api-key": config.apiKey,
    }

    payload = {
        "model": ASR_MODEL_NAME,
        "messages": [
            {
                "role": "user",
                "content": [
                    {
                        "type": "input_audio",
                        "input_audio": {"data": audio_uri}
                    }
                ]
            }
        ],
        "asr_options": {"language": "auto"},
        "stream": True
    }

    full_text = ""
    try:
        async with httpx.AsyncClient(timeout=60.0) as client:
            async with client.stream("POST", api_url, json=payload, headers=headers) as resp:
                if resp.status_code != 200:
                    err_msg = f"MiMo ASR API error: {resp.status_code}"
                    yield f"data: {json.dumps({'error': err_msg}, ensure_ascii=False)}\n\n"
                    return

                async for line in resp.iter_lines():
                    trimmed = line.strip()
                    if not trimmed:
                        continue
                    if trimmed == "data: [DONE]":
                        break
                    if not trimmed.startswith("data: "):
                        continue

                    data_str = trimmed[6:]
                    try:
                        chunk = json.loads(data_str)
                        choices = chunk.get("choices", [{}])
                        if not choices:
                            continue
                        delta = choices[0].get("delta", {})
                        content = delta.get("content", "")
                        finish_reason = choices[0].get("finish_reason")

                        if content:
                            full_text += content
                            yield f"data: {json.dumps({'partial': content, 'text': full_text}, ensure_ascii=False)}\n\n"

                        if finish_reason == "stop":
                            break
                    except Exception:
                        pass
    except Exception as e:
        logger.error("ASR stream error: %s", e)
        yield f"data: {json.dumps({'error': f'转录失败: {e}'}, ensure_ascii=False)}\n\n"
        return

    yield f"data: {json.dumps({'text': full_text, 'done': True}, ensure_ascii=False)}\n\n"
