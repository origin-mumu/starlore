"""文章服务：CRUD、分页、搜索、统计。"""

import logging
import math
from datetime import datetime, timedelta

from sqlalchemy import select, func, or_, text
from sqlalchemy.ext.asyncio import AsyncSession

from app.exceptions import BadRequestException, NotFoundException
from app.models.article import Article
from app.models.category import Category
from app.models.user import User
from app.schemas.article import (
    ArticleDetail,
    ArticleListResponse,
    ArticleSummary,
    CreateArticleRequest,
    UpdateArticleRequest,
)
from app.schemas.common import PaginationInfo

logger = logging.getLogger(__name__)


def _to_summary(article: Article, author_name: str | None = None) -> ArticleSummary:
    return ArticleSummary(
        id=article.id,
        userId=article.user_id,
        authorName=author_name,
        title=article.title,
        status=article.status,
        description=article.description,
        category=article.category,
        tags=article.tags,
        cover_image=article.cover_image,
        view_count=article.view_count,
        is_public=article.is_public,
        createdAt=article.createdAt,
    )


def _to_detail(article: Article) -> ArticleDetail:
    return ArticleDetail(
        id=article.id,
        title=article.title,
        content=article.content,
        description=article.description,
        category=article.category,
        tags=article.tags,
        cover_image=article.cover_image,
        view_count=article.view_count,
        status=article.status,
        is_public=article.is_public,
        createdAt=article.createdAt,
        updatedAt=article.updatedAt,
    )


async def get_all_articles(
    db: AsyncSession,
    user_id: int | None = None,
    page: int = 1,
    limit: int = 10,
    category: str | None = None,
    search: str | None = None,
    tag: str | None = None,
    filter_user_id: int | None = None,
    published_only: bool = True,
) -> ArticleListResponse:
    """获取文章列表（分页、搜索、筛选）。"""
    query = select(Article)

    # 用户过滤
    if filter_user_id is not None:
        query = query.where(Article.user_id == filter_user_id)
    elif not published_only:
        pass  # admin 模式不过滤
    elif user_id is not None:
        query = query.where(Article.user_id == user_id, Article.status == "published")
    else:
        query = query.where(Article.status == "published")

    # 分类过滤
    if category and category != "全部":
        query = query.where(Article.category == category)

    # 搜索过滤
    if search:
        query = query.where(
            or_(
                Article.title.ilike(f"%{search}%"),
                Article.description.ilike(f"%{search}%"),
            )
        )

    # 标签过滤（使用参数化查询防止 SQL 注入）
    if tag:
        from sqlalchemy import literal
        query = query.where(text("JSON_CONTAINS(tags, JSON_ARRAY(:tag))").bindparams(tag=tag))

    # 计算总数
    count_query = select(func.count()).select_from(query.subquery())
    total_result = await db.execute(count_query)
    total = total_result.scalar() or 0
    pages = math.ceil(total / limit) if limit > 0 else 0

    # 分页查询
    offset = (page - 1) * limit
    query = query.order_by(Article.createdAt.desc()).offset(offset).limit(limit)
    result = await db.execute(query)
    articles = list(result.scalars().all())

    # 批量获取作者名
    user_ids = list({a.user_id for a in articles})
    author_map: dict[int, str] = {}
    if user_ids:
        users_result = await db.execute(select(User).where(User.id.in_(user_ids)))
        for u in users_result.scalars().all():
            author_map[u.id] = u.nickname or u.username

    summaries = [_to_summary(a, author_map.get(a.user_id)) for a in articles]
    return ArticleListResponse(
        data=summaries,
        pagination=PaginationInfo(current=page, total=total, pages=pages),
    )


async def get_public_articles(
    db: AsyncSession,
    page: int = 1,
    limit: int = 10,
    category: str | None = None,
    search: str | None = None,
    tag: str | None = None,
) -> ArticleListResponse:
    """List only published articles explicitly marked as public."""
    query = select(Article).where(Article.status == "published", Article.is_public.is_(True))
    if category and category != "全部":
        query = query.where(Article.category == category)
    if search:
        query = query.where(or_(Article.title.ilike(f"%{search}%"), Article.description.ilike(f"%{search}%")))
    if tag:
        query = query.where(text("JSON_CONTAINS(tags, JSON_ARRAY(:tag))").bindparams(tag=tag))

    total = (await db.execute(select(func.count()).select_from(query.subquery()))).scalar() or 0
    pages = math.ceil(total / limit) if limit else 0
    result = await db.execute(
        query.order_by(Article.createdAt.desc()).offset((page - 1) * limit).limit(limit)
    )
    articles = list(result.scalars().all())
    user_ids = {article.user_id for article in articles}
    author_map: dict[int, str] = {}
    if user_ids:
        users = await db.execute(select(User).where(User.id.in_(user_ids)))
        author_map = {user.id: user.nickname or user.username for user in users.scalars().all()}
    return ArticleListResponse(
        data=[_to_summary(article, author_map.get(article.user_id)) for article in articles],
        pagination=PaginationInfo(current=page, total=total, pages=pages),
    )


async def get_public_article_by_id(db: AsyncSession, article_id: int) -> ArticleDetail:
    result = await db.execute(
        select(Article).where(
            Article.id == article_id,
            Article.status == "published",
            Article.is_public.is_(True),
        )
    )
    article = result.scalar_one_or_none()
    if article is None:
        raise NotFoundException("文章不存在或非公开")
    article.view_count = (article.view_count or 0) + 1
    await db.flush()
    return _to_detail(article)


async def get_public_stats(db: AsyncSession) -> dict:
    public_filter = (Article.status == "published", Article.is_public.is_(True))
    total_articles = (await db.execute(select(func.count()).where(*public_filter))).scalar() or 0
    total_views = (
        await db.execute(select(func.coalesce(func.sum(Article.view_count), 0)).where(*public_filter))
    ).scalar() or 0
    category_rows = (
        await db.execute(
            select(Article.category, func.count(Article.id))
            .where(*public_filter, Article.category.is_not(None))
            .group_by(Article.category)
            .order_by(func.count(Article.id).desc())
        )
    ).all()
    recent = await db.execute(
        select(Article).where(*public_filter).order_by(Article.createdAt.desc()).limit(4)
    )
    categories = await db.execute(select(Category))
    category_ids = {category.name: category.id for category in categories.scalars().all()}
    popular_categories = [
        {"id": category_ids.get(name, 0), "name": name, "article_count": count}
        for name, count in category_rows[:5]
    ]
    return {
        "totalArticles": total_articles,
        "totalCategories": len(category_rows),
        "totalViews": total_views,
        "popularArticles": [_to_summary(article) for article in recent.scalars().all()],
        "popularCategories": popular_categories,
    }


async def get_article_by_id(
    db: AsyncSession, user_id: int, article_id: int, is_admin: bool = False
) -> ArticleDetail:
    """获取文章详情（同时增加浏览量）。admin 可查看任意用户的文章。"""
    filters = [Article.id == article_id]
    if not is_admin:
        filters.append(Article.user_id == user_id)
    result = await db.execute(select(Article).where(*filters))
    article = result.scalar_one_or_none()
    if article is None:
        raise NotFoundException("文章不存在")

    # 增加浏览量
    article.view_count = (article.view_count or 0) + 1
    await db.flush()
    return _to_detail(article)


async def get_blog_stats(db: AsyncSession, user_id: int, is_admin: bool = False) -> dict:
    """获取知识库统计信息。"""
    # 基础过滤条件
    article_filter = [Article.status == "published"]
    category_filter = []
    
    if not is_admin:
        article_filter.append(Article.user_id == user_id)
        category_filter.append(Category.user_id == user_id)

    # 1. 发布文章总数
    total_articles = (await db.execute(select(func.count()).where(*article_filter))).scalar() or 0

    # 2. 分类总数
    total_categories = (await db.execute(select(func.count()).where(*category_filter))).scalar() or 0

    # 3. 总浏览量
    total_views = (await db.execute(select(func.coalesce(func.sum(Article.view_count), 0)).where(*article_filter))).scalar() or 0

    # 4. 最近 4 篇文章
    recent_query = select(Article).where(*article_filter).order_by(Article.createdAt.desc()).limit(4)
    recent_result = await db.execute(recent_query)
    popular_articles = [_to_summary(a) for a in recent_result.scalars().all()]

    # 5. Top 5 分类
    cats_query = select(Category).where(*category_filter).order_by(Category.article_count.desc()).limit(5)
    cats_result = await db.execute(cats_query)
    popular_categories = [
        {"id": c.id, "name": c.name, "article_count": c.article_count}
        for c in cats_result.scalars().all()
    ]

    return {
        "totalArticles": total_articles,
        "totalCategories": total_categories,
        "totalViews": total_views,
        "popularArticles": popular_articles,
        "popularCategories": popular_categories,
    }


async def get_daily_stats(db: AsyncSession, user_id: int, is_admin: bool = False) -> list[dict]:
    """获取近 7 天每日文章创建统计。"""
    today = datetime.now().date()
    start_date = today - timedelta(days=6)

    query = select(
        func.date(Article.createdAt).label("date"),
        func.count().label("count"),
    ).where(
        func.date(Article.createdAt) >= start_date,
    )
    if not is_admin:
        query = query.where(Article.user_id == user_id)
    query = query.group_by(func.date(Article.createdAt))

    result = await db.execute(query)
    rows = result.all()

    # 构建 7 天数据（填充零）
    date_count: dict[str, int] = {}
    for row in rows:
        d = str(row.date) if hasattr(row, "date") else str(row[0])
        date_count[d] = row.count if hasattr(row, "count") else row[1]

    data = []
    for i in range(7):
        d = start_date + timedelta(days=i)
        ds = d.strftime("%Y-%m-%d")
        data.append({"date": ds, "count": date_count.get(ds, 0)})

    return data


async def create_article(db: AsyncSession, user_id: int, req: CreateArticleRequest) -> Article:
    """创建文章。"""
    now = datetime.now()
    article = Article(
        user_id=user_id,
        title=req.title,
        content=req.content,
        description=req.description,
        category=req.category or "随笔",
        tags=req.tags or [],
        cover_image=req.coverImage,
        view_count=0,
        status=req.status or "published",
        is_public=req.is_public if req.is_public is not None else False,
        createdAt=now,
        updatedAt=now,
    )
    db.add(article)
    await db.flush()

    # 更新分类计数
    await _update_category_count(db, article.category, user_id)

    # 自动建立向量索引
    if article.status == "published":
        try:
            from app.services import article_embedding_service
            await article_embedding_service.index_article(db, article)
        except Exception as e:
            logger.warning("[RAG] 自动索引新文章失败: %s", e)

    return article


async def update_article(
    db: AsyncSession,
    article_id: int,
    req: UpdateArticleRequest,
    user_id: int,
    is_admin: bool = False,
) -> Article:
    """更新文章。非 admin 仅能更新自己的文章。"""
    filters = [Article.id == article_id]
    if not is_admin:
        filters.append(Article.user_id == user_id)
    result = await db.execute(select(Article).where(*filters))
    article = result.scalar_one_or_none()
    if article is None:
        raise NotFoundException("文章不存在")

    old_category = article.category

    if req.title is not None:
        article.title = req.title
    if req.content is not None:
        article.content = req.content
    if req.description is not None:
        article.description = req.description
    if req.category is not None:
        article.category = req.category
    if req.tags is not None:
        article.tags = req.tags
    if req.coverImage is not None:
        article.cover_image = req.coverImage
    if req.status is not None:
        article.status = req.status
    if req.is_public is not None:
        article.is_public = req.is_public

    article.updatedAt = datetime.now()
    await db.flush()

    # 更新分类计数
    if req.category is not None and req.category != old_category:
        await _update_category_count(db, old_category, article.user_id)
        await _update_category_count(db, req.category, article.user_id)

    # 自动刷新向量索引
    from app.services import article_embedding_service
    if article.status == "published":
        try:
            await article_embedding_service.index_article(db, article)
        except Exception as e:
            logger.warning("[RAG] 自动刷新文章索引失败: %s", e)
    else:
        try:
            await article_embedding_service.remove_article(article_id, db)
        except Exception as e:
            logger.warning("[RAG] 自动移除未发布文章向量失败: %s", e)

    return article


async def delete_article(
    db: AsyncSession, article_id: int, user_id: int, is_admin: bool = False
) -> Article:
    """删除文章。非 admin 仅能删除自己的文章。"""
    filters = [Article.id == article_id]
    if not is_admin:
        filters.append(Article.user_id == user_id)
    result = await db.execute(select(Article).where(*filters))
    article = result.scalar_one_or_none()
    if article is None:
        raise NotFoundException("文章不存在")

    category = article.category
    user_id = article.user_id

    # 自动清理向量索引与切片
    try:
        from app.services import article_embedding_service
        await article_embedding_service.remove_article(article_id, db)
    except Exception as e:
        logger.warning("[RAG] 自动删除文章向量失败: %s", e)

    await db.delete(article)
    await db.flush()

    await _update_category_count(db, category, user_id)
    return article


async def _update_category_count(db: AsyncSession, category_name: str | None, user_id: int) -> None:
    """更新分类的文章计数。"""
    if not category_name:
        return

    count_result = await db.execute(
        select(func.count()).where(
            Article.category == category_name,
            Article.user_id == user_id,
            Article.status == "published",
        )
    )
    count = count_result.scalar() or 0

    cat_result = await db.execute(
        select(Category).where(Category.name == category_name, Category.user_id == user_id)
    )
    category = cat_result.scalar_one_or_none()

    if category:
        category.article_count = count
    else:
        now = datetime.now()
        new_cat = Category(
            user_id=user_id,
            name=category_name,
            article_count=count,
            createdAt=now,
            updatedAt=now,
        )
        db.add(new_cat)

    await db.flush()
