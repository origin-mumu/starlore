"""与 Java 后端一致的结构化知识切片服务。"""

import math
import re

from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.article import Article
from app.models.article_chunk import ArticleChunk

TARGET_TOKENS = 600
MAX_TOKENS = 800
OVERLAP_TOKENS = 80

_HEADING = re.compile(r"^#{1,6}\s+.+")
_SENTENCE_BOUNDARY = re.compile(r"(?<=[。！？；.!?;])|(?=\n)|(?<=\n)")


def estimate_tokens(text: str | None) -> int:
    """与 Java 端保持同一估算口径：CJK 约 1 token，其余约 4 字符 1 token。"""
    if not text or not text.strip():
        return 0
    cjk = sum(
        1
        for char in text
        if "\u3400" <= char <= "\u9fff"
        or "\u3040" <= char <= "\u30ff"
        or "\uac00" <= char <= "\ud7af"
    )
    return cjk + max(1, math.ceil((len(text) - cjk) / 4))


def split_text(text: str | None) -> list[str]:
    normalized = (text or "").replace("\r\n", "\n").replace("\r", "\n").strip()
    if not normalized:
        return []

    chunks: list[str] = []
    current = ""
    for unit in _structural_units(normalized):
        for part in _split_oversized(unit):
            candidate = f"{current}\n\n{part}".strip() if current else part.strip()
            if current and estimate_tokens(candidate) > MAX_TOKENS:
                completed = current.strip()
                chunks.append(completed)
                current = _overlap_tail(completed)
            current = f"{current}\n\n{part}".strip() if current else part.strip()
            if estimate_tokens(current) >= TARGET_TOKENS:
                completed = current.strip()
                chunks.append(completed)
                current = _overlap_tail(completed)

    remainder = current.strip()
    overlap_only = (
        bool(chunks)
        and estimate_tokens(remainder) <= OVERLAP_TOKENS
        and chunks[-1].endswith(remainder)
    )
    if remainder and not overlap_only and (not chunks or remainder != chunks[-1]):
        chunks.append(remainder)
    return chunks


async def get_chunks(db: AsyncSession, article_id: int) -> list[ArticleChunk]:
    result = await db.execute(
        select(ArticleChunk)
        .where(ArticleChunk.articleId == article_id)
        .order_by(ArticleChunk.chunkIndex)
    )
    return list(result.scalars().all())


async def ensure_chunks(db: AsyncSession, article: Article) -> list[ArticleChunk]:
    chunks = await get_chunks(db, article.id)
    return chunks or await rebuild_chunks(db, article)


async def rebuild_chunks(db: AsyncSession, article: Article) -> list[ArticleChunk]:
    await db.execute(delete(ArticleChunk).where(ArticleChunk.articleId == article.id))
    chunks: list[ArticleChunk] = []
    for index, content in enumerate(split_text(article.content)):
        chunk = ArticleChunk(
            articleId=article.id,
            chunkIndex=index,
            content=content,
            tokenCount=estimate_tokens(content),
            isEnabled=1,
        )
        db.add(chunk)
        chunks.append(chunk)
    await db.flush()
    return chunks


async def delete_chunks(db: AsyncSession, article_id: int) -> None:
    await db.execute(delete(ArticleChunk).where(ArticleChunk.articleId == article_id))


def _structural_units(text: str) -> list[str]:
    units: list[str] = []
    block: list[str] = []
    in_code = False

    def flush() -> None:
        value = "\n".join(block).strip()
        if value:
            units.append(value)
        block.clear()

    for line in text.split("\n"):
        stripped = line.strip()
        if stripped.startswith("```"):
            if not in_code:
                flush()
            in_code = not in_code
            block.append(line)
            if not in_code:
                flush()
        elif not in_code and (not stripped or _HEADING.match(stripped)):
            flush()
            if stripped:
                units.append(stripped)
        else:
            block.append(line)
    flush()
    return units


def _split_oversized(unit: str) -> list[str]:
    if estimate_tokens(unit) <= MAX_TOKENS:
        return [unit]
    parts: list[str] = []
    current = ""
    for sentence in _SENTENCE_BOUNDARY.split(unit):
        if not sentence.strip():
            continue
        if current and estimate_tokens(current + sentence) > MAX_TOKENS:
            parts.append(current.strip())
            current = _overlap_tail(current)
        current += sentence
    if current.strip():
        parts.append(current.strip())

    bounded: list[str] = []
    for part in parts:
        bounded.extend([part] if estimate_tokens(part) <= MAX_TOKENS else _hard_split(part))
    return bounded


def _hard_split(text: str) -> list[str]:
    parts: list[str] = []
    current = ""
    for char in text:
        current += char
        if estimate_tokens(current) >= MAX_TOKENS:
            completed = current.strip()
            parts.append(completed)
            current = _overlap_tail(completed)
    if current.strip():
        parts.append(current.strip())
    return parts


def _overlap_tail(text: str) -> str:
    start = len(text)
    while start > 0 and estimate_tokens(text[start:]) < OVERLAP_TOKENS:
        start -= 1
    tail = text[start:].strip()
    boundaries = [tail.find(mark) for mark in ("。", "！", "？", "\n")]
    boundary = max(boundaries)
    return tail[boundary + 1 :].strip() if 0 <= boundary + 1 < len(tail) else tail
