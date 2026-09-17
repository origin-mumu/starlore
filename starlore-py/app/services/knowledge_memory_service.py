"""知识记忆：从完整 Markdown 生成题卡并管理复习进度。"""

from __future__ import annotations

import asyncio
import hashlib
import json
import logging
from datetime import datetime, timedelta

from sqlalchemy import case, delete, func, or_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.exceptions import BadRequestException, NotFoundException
from app.models.article import Article
from app.models.knowledge_card import KnowledgeCard, KnowledgeReview
from app.models.user import User
from app.schemas.knowledge_memory import (
    GenerateKnowledgeCardsRequest,
    KnowledgeArticleItem,
    KnowledgeCardItem,
    KnowledgeGroupItem,
    ReviewKnowledgeCardRequest,
    UpdateKnowledgeCardRequest,
)
from app.services import ai_quota_service, ai_stream_service, ai_usage_service

logger = logging.getLogger(__name__)

_MAX_ARTICLE_CHARACTERS = 120_000


def _source_hash(content: str) -> str:
    return hashlib.sha256(content.encode("utf-8")).hexdigest()


def _card_item(card: KnowledgeCard, article_title: str) -> KnowledgeCardItem:
    return KnowledgeCardItem(
        id=card.id,
        articleId=card.article_id,
        articleTitle=article_title,
        question=card.question,
        answerMarkdown=card.answer_markdown,
        sourceLineStart=card.source_line_start,
        sourceLineEnd=card.source_line_end,
        sourceHash=card.source_hash,
        difficulty=card.difficulty,
        sortOrder=card.sort_order,
        status=card.status,
        reviewStage=card.review_stage,
        intervalDays=card.interval_days,
        reviewCount=card.review_count,
        lapseCount=card.lapse_count,
        nextReviewAt=card.next_review_at,
        lastReviewedAt=card.last_reviewed_at,
        createdAt=card.created_at,
        updatedAt=card.updated_at,
    )


async def _owned_article(db: AsyncSession, user_id: int, article_id: int) -> Article:
    result = await db.execute(
        select(Article).where(Article.id == article_id, Article.user_id == user_id)
    )
    article = result.scalar_one_or_none()
    if article is None:
        raise NotFoundException("文章不存在或不属于当前用户")
    return article


async def list_articles(db: AsyncSession, user_id: int) -> list[KnowledgeArticleItem]:
    card_counts = (
        select(
            KnowledgeCard.article_id.label("article_id"),
            func.count(KnowledgeCard.id).label("card_count"),
        )
        .where(
            KnowledgeCard.user_id == user_id,
            KnowledgeCard.status == "active",
        )
        .group_by(KnowledgeCard.article_id)
        .subquery()
    )
    result = await db.execute(
        select(
            Article.id,
            Article.title,
            Article.description,
            Article.category,
            Article.updatedAt,
            func.coalesce(card_counts.c.card_count, 0),
        )
        .outerjoin(card_counts, card_counts.c.article_id == Article.id)
        .where(Article.user_id == user_id, Article.content.is_not(None))
        .order_by(Article.updatedAt.desc(), Article.id.desc())
    )
    return [
        KnowledgeArticleItem(
            articleId=row[0],
            title=row[1],
            description=row[2],
            category=row[3],
            updatedAt=row[4],
            cardCount=int(row[5] or 0),
            generated=int(row[5] or 0) > 0,
        )
        for row in result.all()
    ]


async def list_groups(db: AsyncSession, user_id: int) -> list[KnowledgeGroupItem]:
    now = datetime.now()
    stats = (
        select(
            KnowledgeCard.article_id.label("article_id"),
            func.count(KnowledgeCard.id).label("card_count"),
            func.sum(
                case(
                    (
                        or_(
                            KnowledgeCard.next_review_at.is_(None),
                            KnowledgeCard.next_review_at <= now,
                        ),
                        1,
                    ),
                    else_=0,
                )
            ).label("due_count"),
            func.sum(
                case((KnowledgeCard.review_stage == "mastered", 1), else_=0)
            ).label("mastered_count"),
        )
        .where(
            KnowledgeCard.user_id == user_id,
            KnowledgeCard.status == "active",
        )
        .group_by(KnowledgeCard.article_id)
        .subquery()
    )
    result = await db.execute(
        select(
            Article.id,
            Article.title,
            Article.description,
            Article.category,
            Article.updatedAt,
            stats.c.card_count,
            stats.c.due_count,
            stats.c.mastered_count,
        )
        .join(stats, stats.c.article_id == Article.id)
        .where(Article.user_id == user_id)
        .order_by(Article.updatedAt.desc(), Article.id.desc())
    )
    return [
        KnowledgeGroupItem(
            articleId=row[0],
            title=row[1],
            description=row[2],
            category=row[3],
            updatedAt=row[4],
            cardCount=int(row[5] or 0),
            generated=True,
            dueCount=int(row[6] or 0),
            masteredCount=int(row[7] or 0),
        )
        for row in result.all()
    ]


def _decode_model_json(source: str) -> list[dict]:
    cleaned = source.strip()
    if cleaned.startswith("Error:"):
        raise BadRequestException(cleaned.removeprefix("Error:").strip())

    decoder = json.JSONDecoder()
    for index, character in enumerate(cleaned):
        if character not in "[{":
            continue
        try:
            value, _ = decoder.raw_decode(cleaned[index:])
        except json.JSONDecodeError:
            continue
        if isinstance(value, dict) and isinstance(value.get("cards"), list):
            return [item for item in value["cards"] if isinstance(item, dict)]
        if isinstance(value, list):
            return [item for item in value if isinstance(item, dict)]
    raise BadRequestException("AI 未返回可解析的知识卡片数据")


async def _generate_article_cards(
    db: AsyncSession,
    article: Article,
    model: str,
    max_cards: int,
) -> list[dict]:
    content = article.content or ""
    if not content.strip():
        raise BadRequestException("文章正文为空，无法生成知识卡片")
    if len(content) > _MAX_ARTICLE_CHARACTERS:
        raise BadRequestException("文章过长，请精简至 12 万字符以内后再生成")

    original_lines = content.splitlines()
    numbered_markdown = "\n".join(
        f"{index:06d}|{line}" for index, line in enumerate(original_lines, start=1)
    )
    messages = [
        {
            "role": "system",
            "content": (
                "你是求职知识记忆题卡整理器。你会收到一篇带行号的完整 Markdown 文章。"
                "请识别适合背诵和面试回答的知识点，只生成问题，不改写答案。"
                "答案由服务端根据你返回的原文行号截取。不要输出表情符号，不要输出 Markdown 代码围栏。"
                "严格返回 JSON：{\"cards\":[{\"question\":\"问题\","
                "\"startLine\":1,\"endLine\":4,\"difficulty\":\"core\"}]}。"
                "difficulty 只能是 basic、core、advanced。行号范围必须连续、完整、足以独立回答问题。"
            ),
        },
        {
            "role": "user",
            "content": (
                f"文章标题：{article.title}\n"
                f"最多生成 {max_cards} 张卡片。优先覆盖高频面试知识、原理、差异、流程和边界条件；"
                "不要生成重复问题，也不要把目录或纯标题当作答案。\n\n"
                f"完整 Markdown：\n{numbered_markdown}"
            ),
        },
    ]
    try:
        response = await asyncio.wait_for(
            ai_stream_service.invoke_model(db, model, messages),
            timeout=180,
        )
    except TimeoutError as exc:
        raise BadRequestException("AI 生成超时，请稍后重试这篇文章") from exc
    raw_cards = _decode_model_json(str(response))

    cards: list[dict] = []
    questions: set[str] = set()
    total_lines = len(original_lines)
    for raw in raw_cards:
        question = str(raw.get("question") or "").strip()
        try:
            start_line = int(raw.get("startLine"))
            end_line = int(raw.get("endLine"))
        except (TypeError, ValueError):
            continue
        if not question or question in questions:
            continue
        if start_line < 1 or end_line < start_line or end_line > total_lines:
            continue
        answer_markdown = "\n".join(original_lines[start_line - 1 : end_line])
        if not answer_markdown.strip():
            continue
        difficulty = str(raw.get("difficulty") or "core")
        if difficulty not in {"basic", "core", "advanced"}:
            difficulty = "core"
        questions.add(question)
        cards.append(
            {
                "question": question[:500],
                "answer_markdown": answer_markdown,
                "source_line_start": start_line,
                "source_line_end": end_line,
                "difficulty": difficulty,
            }
        )
        if len(cards) >= max_cards:
            break
    if not cards:
        raise BadRequestException("AI 没有生成有效题卡，请检查文章内容后重试")
    return cards


async def generate_cards(
    db: AsyncSession,
    user_id: int,
    req: GenerateKnowledgeCardsRequest,
) -> dict:
    generated: list[dict] = []
    errors: list[dict] = []
    article_ids = list(dict.fromkeys(req.articleIds))

    user_result = await db.execute(select(User).where(User.id == user_id))
    usage_user = user_result.scalar_one_or_none()

    for article_id in article_ids:
        article: Article | None = None
        try:
            article = await _owned_article(db, user_id, article_id)
            consumed = await ai_quota_service.try_consume(db, user_id)
            if usage_user is not None:
                await ai_usage_service.record_consumption(
                    db, usage_user, scene="knowledge", model=req.model or None, consumed=consumed
                )
            if not consumed:
                errors.append({"articleId": article_id, "title": article.title, "message": "今日 AI 额度已用尽"})
                continue
            generated_cards = await _generate_article_cards(
                db,
                article,
                req.model,
                req.maxCardsPerArticle,
            )
            old_card_ids = select(KnowledgeCard.id).where(
                KnowledgeCard.user_id == user_id,
                KnowledgeCard.article_id == article_id,
            )
            await db.execute(
                delete(KnowledgeReview).where(KnowledgeReview.card_id.in_(old_card_ids))
            )
            await db.execute(
                delete(KnowledgeCard).where(
                    KnowledgeCard.user_id == user_id,
                    KnowledgeCard.article_id == article_id,
                )
            )
            now = datetime.now()
            fingerprint = _source_hash(article.content or "")
            for index, card_data in enumerate(generated_cards):
                db.add(
                    KnowledgeCard(
                        user_id=user_id,
                        article_id=article_id,
                        source_hash=fingerprint,
                        sort_order=index,
                        status="active",
                        review_stage="new",
                        interval_days=0,
                        review_count=0,
                        lapse_count=0,
                        next_review_at=None,
                        created_at=now,
                        updated_at=now,
                        **card_data,
                    )
                )
            await db.flush()
            generated.append(
                {
                    "articleId": article.id,
                    "title": article.title,
                    "cardCount": len(generated_cards),
                }
            )
        except (BadRequestException, NotFoundException) as exc:
            errors.append(
                {
                    "articleId": article_id,
                    "title": article.title if article is not None else None,
                    "message": exc.message,
                }
            )
        except Exception as exc:
            logger.exception("生成文章 %s 的知识卡片失败: %s", article_id, exc)
            errors.append(
                {"articleId": article_id, "title": None, "message": "生成失败，请稍后重试"}
            )
    return {"generated": generated, "errors": errors}


async def list_cards(
    db: AsyncSession,
    user_id: int,
    article_id: int,
    due_only: bool = False,
) -> list[KnowledgeCardItem]:
    article = await _owned_article(db, user_id, article_id)
    query = select(KnowledgeCard).where(
        KnowledgeCard.user_id == user_id,
        KnowledgeCard.article_id == article_id,
        KnowledgeCard.status == "active",
    )
    if due_only:
        now = datetime.now()
        query = query.where(
            or_(KnowledgeCard.next_review_at.is_(None), KnowledgeCard.next_review_at <= now)
        )
    result = await db.execute(query.order_by(KnowledgeCard.sort_order, KnowledgeCard.id))
    return [_card_item(card, article.title) for card in result.scalars().all()]


async def update_card(
    db: AsyncSession,
    user_id: int,
    card_id: int,
    req: UpdateKnowledgeCardRequest,
) -> KnowledgeCardItem:
    result = await db.execute(
        select(KnowledgeCard, Article.title)
        .join(Article, Article.id == KnowledgeCard.article_id)
        .where(KnowledgeCard.id == card_id, KnowledgeCard.user_id == user_id)
    )
    row = result.first()
    if row is None:
        raise NotFoundException("知识卡片不存在")
    card, article_title = row
    if req.question is not None:
        card.question = req.question.strip()
    if req.answerMarkdown is not None:
        card.answer_markdown = req.answerMarkdown
    if req.difficulty is not None:
        card.difficulty = req.difficulty
    if req.status is not None:
        card.status = req.status
    card.updated_at = datetime.now()
    await db.flush()
    return _card_item(card, article_title)


async def delete_card(db: AsyncSession, user_id: int, card_id: int) -> None:
    result = await db.execute(
        select(KnowledgeCard).where(
            KnowledgeCard.id == card_id,
            KnowledgeCard.user_id == user_id,
        )
    )
    card = result.scalar_one_or_none()
    if card is None:
        raise NotFoundException("知识卡片不存在")
    await db.execute(delete(KnowledgeReview).where(KnowledgeReview.card_id == card_id))
    await db.delete(card)
    await db.flush()


async def delete_article_cards(db: AsyncSession, user_id: int, article_id: int) -> None:
    await _owned_article(db, user_id, article_id)
    card_ids = select(KnowledgeCard.id).where(
        KnowledgeCard.user_id == user_id,
        KnowledgeCard.article_id == article_id,
    )
    await db.execute(delete(KnowledgeReview).where(KnowledgeReview.card_id.in_(card_ids)))
    await db.execute(
        delete(KnowledgeCard).where(
            KnowledgeCard.user_id == user_id,
            KnowledgeCard.article_id == article_id,
        )
    )
    await db.flush()


async def review_card(
    db: AsyncSession,
    user_id: int,
    card_id: int,
    req: ReviewKnowledgeCardRequest,
) -> KnowledgeCardItem:
    result = await db.execute(
        select(KnowledgeCard, Article.title)
        .join(Article, Article.id == KnowledgeCard.article_id)
        .where(KnowledgeCard.id == card_id, KnowledgeCard.user_id == user_id)
    )
    row = result.first()
    if row is None:
        raise NotFoundException("知识卡片不存在")
    card, article_title = row
    now = datetime.now()
    previous_interval = card.interval_days or 0
    next_interval = previous_interval
    if req.rating == "again":
        next_interval = 0
        card.next_review_at = now + timedelta(minutes=10)
        card.review_stage = "learning"
        card.lapse_count = (card.lapse_count or 0) + 1
    elif req.rating == "hard":
        next_interval = max(1, round(max(previous_interval, 1) * 1.25))
        card.next_review_at = now + timedelta(days=next_interval)
        card.review_stage = "reviewing"
    else:
        next_interval = 1 if previous_interval == 0 else min(365, max(previous_interval + 1, previous_interval * 2))
        card.next_review_at = now + timedelta(days=next_interval)
        card.review_stage = "mastered" if (card.review_count or 0) + 1 >= 4 else "reviewing"

    card.interval_days = next_interval
    card.review_count = (card.review_count or 0) + 1
    card.last_reviewed_at = now
    card.updated_at = now
    db.add(
        KnowledgeReview(
            user_id=user_id,
            card_id=card.id,
            rating=req.rating,
            previous_interval_days=previous_interval,
            next_interval_days=next_interval,
            duration_ms=req.durationMs,
            reviewed_at=now,
        )
    )
    await db.flush()
    return _card_item(card, article_title)
