"""分类服务：CRUD、文章计数维护。"""

from datetime import datetime

from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.exceptions import BadRequestException, ConflictException, NotFoundException
from app.models.article import Article
from app.models.category import Category
from app.schemas.article import ArticleSummary
from app.schemas.category import CategoryItem, CreateCategoryRequest, UpdateCategoryRequest


def _to_item(cat: Category) -> CategoryItem:
    return CategoryItem(
        id=cat.id,
        userId=cat.user_id,
        name=cat.name,
        description=cat.description,
        color=cat.color,
        article_count=cat.article_count,
        createdAt=cat.createdAt,
        updatedAt=cat.updatedAt,
    )


async def get_all_categories(db: AsyncSession, user_id: int | None = None) -> list[CategoryItem]:
    query = select(Category)
    if user_id is not None:
        query = query.where(Category.user_id == user_id)
    query = query.order_by(Category.article_count.desc())
    result = await db.execute(query)
    return [_to_item(c) for c in result.scalars().all()]


async def get_category_by_id(db: AsyncSession, user_id: int, category_id: int) -> dict:
    result = await db.execute(
        select(Category).where(Category.id == category_id, Category.user_id == user_id)
    )
    category = result.scalar_one_or_none()
    if category is None:
        raise NotFoundException("分类不存在")

    # 获取该分类下的已发布文章
    articles_result = await db.execute(
        select(Article).where(
            Article.category == category.name,
            Article.user_id == user_id,
            Article.status == "published",
        ).order_by(Article.createdAt.desc())
    )
    articles = [
        ArticleSummary(
            id=a.id,
            title=a.title,
            description=a.description,
            cover_image=a.cover_image,
            view_count=a.view_count,
            createdAt=a.createdAt,
        )
        for a in articles_result.scalars().all()
    ]

    return {
        "id": category.id,
        "name": category.name,
        "description": category.description,
        "color": category.color,
        "article_count": category.article_count,
        "createdAt": category.createdAt,
        "updatedAt": category.updatedAt,
        "articles": articles,
    }


async def create_category(db: AsyncSession, user_id: int, req: CreateCategoryRequest) -> CategoryItem:
    if not req.name or not req.name.strip():
        raise BadRequestException("分类名不能为空")

    # 检查唯一性
    existing = await db.execute(
        select(Category).where(Category.name == req.name, Category.user_id == user_id)
    )
    if existing.scalar_one_or_none() is not None:
        raise ConflictException("分类名已存在")

    now = datetime.now()
    category = Category(
        user_id=user_id,
        name=req.name,
        description=req.description,
        color=req.color,
        article_count=0,
        createdAt=now,
        updatedAt=now,
    )
    db.add(category)
    await db.flush()
    return _to_item(category)


async def update_category(db: AsyncSession, category_id: int, req: UpdateCategoryRequest) -> CategoryItem:
    result = await db.execute(select(Category).where(Category.id == category_id))
    category = result.scalar_one_or_none()
    if category is None:
        raise NotFoundException("分类不存在")

    old_name = category.name

    if req.name is not None and req.name != old_name:
        # 检查新名称唯一性
        existing = await db.execute(
            select(Category).where(Category.name == req.name, Category.user_id == category.user_id)
        )
        if existing.scalar_one_or_none() is not None:
            raise ConflictException("分类名已存在")

        # 更新所有引用旧分类名的文章
        articles_result = await db.execute(
            select(Article).where(Article.category == old_name, Article.user_id == category.user_id)
        )
        for article in articles_result.scalars().all():
            article.category = req.name

        category.name = req.name

    if req.description is not None:
        category.description = req.description
    if req.color is not None:
        category.color = req.color

    category.updatedAt = datetime.now()
    await db.flush()
    return _to_item(category)


async def delete_category(db: AsyncSession, category_id: int) -> None:
    result = await db.execute(select(Category).where(Category.id == category_id))
    category = result.scalar_one_or_none()
    if category is None:
        raise NotFoundException("分类不存在")

    # 检查是否有文章引用
    count_result = await db.execute(
        select(func.count()).where(Article.category == category.name)
    )
    if count_result.scalar() > 0:
        raise BadRequestException("该分类下有文章，无法删除")

    await db.delete(category)
    await db.flush()
