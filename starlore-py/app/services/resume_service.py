"""简历服务（软删除）。"""

from datetime import datetime

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.exceptions import NotFoundException
from app.models.resume import Resume


async def list_by_user(db: AsyncSession, user_id: int) -> list[Resume]:
    result = await db.execute(
        select(Resume).where(Resume.user_id == user_id, Resume.status == "active")
        .order_by(Resume.updated_at.desc())
    )
    return list(result.scalars().all())


async def get_by_id(db: AsyncSession, resume_id: int, user_id: int) -> Resume:
    result = await db.execute(
        select(Resume).where(Resume.id == resume_id, Resume.user_id == user_id)
    )
    resume = result.scalar_one_or_none()
    if resume is None:
        raise NotFoundException("简历不存在")
    return resume


async def create(db: AsyncSession, resume: Resume, user_id: int) -> Resume:
    resume.user_id = user_id
    resume.status = "active"
    now = datetime.now()
    resume.created_at = now
    resume.updated_at = now
    db.add(resume)
    await db.flush()
    return resume


async def update(db: AsyncSession, resume: Resume, user_id: int) -> Resume:
    result = await db.execute(
        select(Resume).where(Resume.id == resume.id, Resume.user_id == user_id)
    )
    existing = result.scalar_one_or_none()
    if existing is None:
        raise NotFoundException("简历不存在")

    resume.updated_at = datetime.now()
    # 合并字段
    for field in ("title", "template", "name", "job_title", "phone", "email", "photo_url", "content"):
        val = getattr(resume, field, None)
        if val is not None:
            setattr(existing, field, val)
    existing.updated_at = datetime.now()
    await db.flush()
    await db.refresh(existing)
    return existing


async def delete(db: AsyncSession, resume_id: int, user_id: int) -> None:
    result = await db.execute(
        select(Resume).where(Resume.id == resume_id, Resume.user_id == user_id)
    )
    resume = result.scalar_one_or_none()
    if resume is None:
        raise NotFoundException("简历不存在")

    resume.status = "deleted"
    resume.updated_at = datetime.now()
    await db.flush()
