"""AI Agent 工具集 — 基于 LangChain @tool。"""

import json
import logging
from datetime import datetime

from langchain_core.tools import tool
from sqlalchemy import select, func, or_
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.article import Article
from app.models.category import Category
from app.services import article_embedding_service

logger = logging.getLogger(__name__)


async def search_articles_impl(
    db: AsyncSession,
    user_id: int,
    keyword: str | None = None,
    category: str | None = None,
    tag: str | None = None,
) -> str:
    """搜索文章，优先语义搜索，回退智能分词关键词搜索。未命中时不假冒命中。"""
    articles = []
    clean_kw = (keyword or "").strip()

    # 1. 尝试语义向量搜索（支持 category 和 tag 筛选）
    if clean_kw:
        try:
            similar = await article_embedding_service.search_similar(
                db, clean_kw, user_id, top_k=10, category=category, tag=tag
            )
            if similar:
                articles = similar
        except Exception as e:
            logger.warning("[RAG] 语义检索异常，自动降级为关键词检索: %s", e)

    # 2. 语义搜索未命中（或过滤后为空），回退到整句模糊匹配
    if not articles and clean_kw:
        try:
            conditions = [
                Article.user_id == user_id,
                Article.status == "published",
                or_(
                    Article.title.ilike(f"%{clean_kw}%"),
                    Article.description.ilike(f"%{clean_kw}%"),
                ),
            ]
            if category:
                conditions.append(Article.category == category)
            result = await db.execute(
                select(Article).where(*conditions).order_by(Article.createdAt.desc()).limit(10)
            )
            articles = list(result.scalars().all())
        except Exception as e:
            logger.warning("[RAG] 整句关键词检索异常: %s", e)

    # 3. 若整句未命中（用户长句提问），提取核心关键词元进行联合模糊召回
    if not articles and clean_kw:
        try:
            import re
            # 过滤标点符号与提问虚词
            raw_tokens = re.split(r"[\s,:：、，。！？_—\-\(\)\[\]]+", clean_kw)
            stop_words = {"文章", "讲的", "内容", "什么", "关于", "这个", "知识库", "一篇", "看看", "请问", "帮我", "系统", "介绍"}
            tokens = [t.strip() for t in raw_tokens if len(t.strip()) >= 2 and t.strip().lower() not in stop_words]

            if tokens:
                token_conditions = []
                for t in tokens[:6]:
                    token_conditions.append(Article.title.ilike(f"%{t}%"))
                    token_conditions.append(Article.description.ilike(f"%{t}%"))

                query = select(Article).where(
                    Article.user_id == user_id,
                    Article.status == "published",
                    or_(*token_conditions),
                )
                if category:
                    query = query.where(Article.category == category)
                query = query.order_by(Article.createdAt.desc()).limit(10)
                result = await db.execute(query)
                articles = list(result.scalars().all())
                if articles:
                    logger.info("[RAG] 通过关键词元分词召回 %d 篇文章: %s", len(articles), tokens[:4])
        except Exception as e:
            logger.warning("[RAG] 分词模糊检索异常: %s", e)

    # ⚠️ 彻底消除旧版“兜底检索最新文章”的假命中逻辑：
    # 检索未找到就明确返回空，杜绝拿无关文章欺骗大模型产生幻觉。

    items = []
    for a in articles:
        content_preview = (a.content or "")[:1500]
        items.append({
            "id": a.id,
            "title": a.title,
            "category": a.category,
            "tags": a.tags or [],
            "description": a.description,
            "content_preview": content_preview,
            "view_count": a.view_count,
        })
    return json.dumps(items, ensure_ascii=False)


async def get_article_detail_impl(db: AsyncSession, user_id: int, article_id: int) -> str:
    """获取文章详情（不增加浏览量）。"""
    result = await db.execute(
        select(Article).where(Article.id == article_id, Article.user_id == user_id)
    )
    article = result.scalar_one_or_none()
    if article is None:
        return json.dumps({"error": "文章不存在"}, ensure_ascii=False)

    content = (article.content or "")[:3000]
    return json.dumps({
        "id": article.id,
        "title": article.title,
        "content": content,
        "category": article.category,
        "tags": article.tags or [],
        "description": article.description,
        "status": article.status,
    }, ensure_ascii=False)


async def get_categories_impl(db: AsyncSession, user_id: int) -> str:
    """获取所有分类。"""
    result = await db.execute(
        select(Category).where(Category.user_id == user_id).order_by(Category.article_count.desc())
    )
    categories = [
        {"id": c.id, "name": c.name, "description": c.description, "article_count": c.article_count}
        for c in result.scalars().all()
    ]
    return json.dumps(categories, ensure_ascii=False)


async def get_blog_stats_impl(db: AsyncSession, user_id: int) -> str:
    """获取知识库统计。"""
    from app.services import article_service
    stats = await article_service.get_blog_stats(db, user_id)

    popular = []
    for a in stats.get("popularArticles", []):
        if hasattr(a, "model_dump"):
            popular.append(a.model_dump())
        else:
            popular.append(a)

    return json.dumps({
        "totalArticles": stats["totalArticles"],
        "totalCategories": stats["totalCategories"],
        "totalViews": stats["totalViews"],
        "popularArticles": popular,
        "popularCategories": stats["popularCategories"],
    }, ensure_ascii=False, default=str)


async def get_recent_articles_impl(db: AsyncSession, user_id: int, limit: int = 5) -> str:
    """获取最近文章。"""
    limit = min(limit, 20)
    result = await db.execute(
        select(Article).where(
            Article.user_id == user_id, Article.status == "published"
        ).order_by(Article.createdAt.desc()).limit(limit)
    )
    articles = [
        {"id": a.id, "title": a.title, "category": a.category, "description": a.description}
        for a in result.scalars().all()
    ]
    return json.dumps(articles, ensure_ascii=False)


async def get_articles_missing_metadata_impl(
    db: AsyncSession, user_id: int, limit: int = 100
) -> str:
    """Return articles whose description or tags need to be completed."""
    limit = max(1, min(limit, 100))
    result = await db.execute(
        select(Article).where(
            Article.user_id == user_id,
            or_(
                Article.description.is_(None),
                func.trim(Article.description) == "",
                Article.tags.is_(None),
                func.json_length(Article.tags) == 0,
            ),
        ).order_by(Article.createdAt.desc()).limit(limit)
    )
    return json.dumps([
        {
            "id": article.id,
            "title": article.title,
            "category": article.category,
            "description": article.description,
            "tags": article.tags or [],
            "content_preview": (article.content or "")[:3000],
            "status": article.status,
        }
        for article in result.scalars().all()
    ], ensure_ascii=False)


async def write_article_impl(
    db: AsyncSession,
    user_id: int,
    title: str,
    content: str,
    category: str = "随笔",
    tags: str = "[]",
    description: str = "",
    status: str = "published",
) -> str:
    """创建新文章。"""
    try:
        tag_list = json.loads(tags) if isinstance(tags, str) else tags
    except Exception:
        tag_list = []

    now = datetime.now()
    article = Article(
        user_id=user_id,
        title=title,
        content=content,
        description=description,
        category=category,
        tags=tag_list,
        view_count=0,
        status=status,
        createdAt=now,
        updatedAt=now,
    )
    db.add(article)
    await db.flush()

    if status == "published":
        try:
            await article_embedding_service.index_article(db, article)
        except Exception as e:
            logger.warning("RAG index failed: %s", e)

    return json.dumps({"success": True, "id": article.id, "message": "文章创建成功"}, ensure_ascii=False)


async def update_article_impl(
    db: AsyncSession,
    user_id: int,
    article_id: int,
    title: str | None = None,
    content: str | None = None,
    category: str | None = None,
    tags: str | None = None,
    description: str | None = None,
    status: str | None = None,
) -> str:
    """更新文章。"""
    result = await db.execute(
        select(Article).where(Article.id == article_id, Article.user_id == user_id)
    )
    article = result.scalar_one_or_none()
    if article is None:
        return json.dumps({"error": "文章不存在"}, ensure_ascii=False)

    if title is not None:
        article.title = title
    if content is not None:
        article.content = content
    if category is not None:
        article.category = category
    if tags is not None:
        try:
            article.tags = json.loads(tags) if isinstance(tags, str) else tags
        except Exception:
            pass
    if description is not None:
        article.description = description
    if status is not None:
        article.status = status

    article.updatedAt = datetime.now()
    await db.flush()
    await db.refresh(article)

    try:
        await article_embedding_service.index_article(db, article)
    except Exception as e:
        logger.warning("RAG index failed: %s", e)

    return json.dumps({
        "success": True,
        "id": article.id,
        "description": article.description,
        "tags": article.tags or [],
        "message": "文章更新成功",
    }, ensure_ascii=False)


async def delete_article_impl(db: AsyncSession, user_id: int, article_id: int) -> str:
    """删除文章。"""
    result = await db.execute(
        select(Article).where(Article.id == article_id, Article.user_id == user_id)
    )
    article = result.scalar_one_or_none()
    if article is None:
        return json.dumps({"error": "文章不存在"}, ensure_ascii=False)

    await db.delete(article)
    await db.flush()

    await article_embedding_service.remove_article(article_id, db)
    return json.dumps({"success": True, "message": "文章删除成功"}, ensure_ascii=False)


async def get_all_tags_impl(db: AsyncSession, user_id: int) -> str:
    """获取所有去重标签。"""
    result = await db.execute(
        select(Article.tags).where(Article.user_id == user_id, Article.status == "published")
    )
    tag_set: set[str] = set()
    for row in result.all():
        if row[0]:
            tag_set.update(row[0])
    return json.dumps(sorted(tag_set), ensure_ascii=False)


async def get_articles_by_category_impl(db: AsyncSession, user_id: int, category: str) -> str:
    """获取分类下的文章。"""
    result = await db.execute(
        select(Article).where(
            Article.user_id == user_id,
            Article.category == category,
            Article.status == "published",
        ).order_by(Article.createdAt.desc())
    )
    articles = [
        {"id": a.id, "title": a.title, "description": a.description, "view_count": a.view_count}
        for a in result.scalars().all()
    ]
    return json.dumps(articles, ensure_ascii=False)


async def create_category_impl(db: AsyncSession, user_id: int, name: str, description: str = "", color: str = "") -> str:
    """创建分类。"""
    now = datetime.now()
    category = Category(
        user_id=user_id,
        name=name,
        description=description,
        color=color,
        article_count=0,
        createdAt=now,
        updatedAt=now,
    )
    db.add(category)
    await db.flush()
    return json.dumps({"success": True, "id": category.id, "message": "分类创建成功"}, ensure_ascii=False)


async def get_user_resumes_impl(db: AsyncSession, user_id: int) -> str:
    """获取当前用户的简历概要列表。"""
    from app.services import resume_service
    resumes = await resume_service.list_by_user(db, user_id)
    summary_list = []
    for r in resumes:
        summary_list.append({
            "id": r.id,
            "title": r.title or "个人简历",
            "name": r.name,
            "job_title": r.job_title,
            "updated_at": r.updated_at.strftime("%Y-%m-%d %H:%M") if r.updated_at else None,
        })
    return json.dumps(summary_list, ensure_ascii=False)


async def get_resume_detail_impl(db: AsyncSession, user_id: int, resume_id: int | None = None) -> str:
    """获取具体简历的完整结构化详情（若不传 resume_id 则返回最近一份简历）。"""
    from app.services import resume_service
    target_resume = None
    if resume_id:
        try:
            target_resume = await resume_service.get_by_id(db, resume_id, user_id)
        except Exception:
            target_resume = None
    if not target_resume:
        resumes = await resume_service.list_by_user(db, user_id)
        if resumes:
            target_resume = resumes[0]

    if not target_resume:
        return json.dumps({"error": "未找到任何简历信息"}, ensure_ascii=False)

    detail = {
        "id": target_resume.id,
        "title": target_resume.title,
        "name": target_resume.name,
        "job_title": target_resume.job_title,
        "phone": target_resume.phone,
        "email": target_resume.email,
        "content": target_resume.content,
        "updated_at": target_resume.updated_at.strftime("%Y-%m-%d %H:%M") if target_resume.updated_at else None,
    }
    return json.dumps(detail, ensure_ascii=False)

