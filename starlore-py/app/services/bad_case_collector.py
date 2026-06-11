"""Bad Case 沉淀与 Few-Shot 动态反馈。

- collect(): 持久化失败用例到 ai_bad_cases 表
- get_similar_bad_cases(): 关键词 LIKE 查询相似用例
- build_few_shot_prompt(): 构建 Few-Shot 提示注入 Executor
"""

import logging
from datetime import datetime

from sqlalchemy import select, or_
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.ai_bad_case import AiBadCase

logger = logging.getLogger(__name__)


async def collect(
    db: AsyncSession,
    user_id: int,
    question: str,
    actual_answer: str,
    error_message: str,
    agent_path: str = "Planner->Executor->Reviewer",
    tokens: int = 0,
    latency_ms: int = 0,
) -> None:
    """持久化失败用例到数据库。"""
    try:
        bad_case = AiBadCase(
            user_id=user_id,
            question=question[:2000] if question else "",
            actual_answer=actual_answer[:4000] if actual_answer else "",
            error_message=error_message[:2000] if error_message else "",
            agent_path=agent_path,
            tokens=tokens,
            latency_ms=latency_ms,
            created_at=datetime.now(),
        )
        db.add(bad_case)
        await db.flush()
        logger.info("Bad case collected: %s...", question[:50] if question else "")
    except Exception as e:
        logger.warning("Failed to collect bad case: %s", e)


async def get_similar_bad_cases(
    db: AsyncSession,
    keyword: str,
    limit: int = 3,
) -> list[str]:
    """从 Bad Case 库中检索相似案例（关键词 LIKE 查询）。"""
    if not keyword or len(keyword.strip()) < 2:
        return []

    try:
        # 取关键词的前 3 个片段进行 LIKE 匹配
        words = [w.strip() for w in keyword.split() if len(w.strip()) >= 2][:3]
        if not words:
            words = [keyword[:10]]

        conditions = []
        for w in words:
            conditions.append(AiBadCase.question.ilike(f"%{w}%"))

        result = await db.execute(
            select(AiBadCase)
            .where(or_(*conditions))
            .order_by(AiBadCase.created_at.desc())
            .limit(limit)
        )
        cases = result.scalars().all()

        return [
            f"问题: {c.question}\n失败原因: {c.error_message}"
            for c in cases
            if c.question and c.error_message
        ]
    except Exception as e:
        logger.warning("Failed to query similar bad cases: %s", e)
        return []


async def build_few_shot_prompt(db: AsyncSession, keyword: str) -> str:
    """构建 Few-Shot 提示，注入 Executor 的 system prompt。"""
    similar = await get_similar_bad_cases(db, keyword, limit=3)
    if not similar:
        return ""

    lines = ["\n\n## 历史失败案例（请避免类似错误）"]
    for i, case in enumerate(similar, 1):
        lines.append(f"{i}. {case}")
    return "\n".join(lines)
