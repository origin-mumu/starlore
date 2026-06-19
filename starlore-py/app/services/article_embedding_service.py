"""RAG 文章向量索引服务 — 基于 LangChain FAISS。"""

import logging
import re

from langchain_community.vectorstores import FAISS
from langchain_core.documents import Document
from langchain_core.embeddings import Embeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.article import Article
from app.services import ai_config_service

logger = logging.getLogger(__name__)

_text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=2000,
    chunk_overlap=200,
    separators=["\n\n", "\n", "。", "！", "？", ".", "!", "?", " "],
)

_vector_store: FAISS | None = None
_embeddings: Embeddings | None = None


class _ZhipuEmbeddings(Embeddings):
    """智谱 Embedding 封装，兼容 LangChain Embeddings 接口。"""

    def __init__(self, base_url: str, api_key: str, model: str):
        import httpx
        self._url = base_url.rstrip("/") + "/embeddings"
        self._api_key = api_key
        self._model = model

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        import httpx
        with httpx.Client(timeout=30.0) as client:
            resp = client.post(
                self._url,
                json={"model": self._model, "input": texts},
                headers={"Authorization": f"Bearer {self._api_key}", "Content-Type": "application/json"},
            )
            resp.raise_for_status()
            data = resp.json()
            return [item["embedding"] for item in data["data"]]

    def embed_query(self, text: str) -> list[float]:
        return self.embed_documents([text])[0]


async def _get_embeddings(db: AsyncSession) -> Embeddings | None:
    """获取 Embedding 实例（从数据库配置）。"""
    global _embeddings
    if _embeddings is not None:
        return _embeddings

    config = await ai_config_service.get_config_by_key(db, "zhipu-embedding")
    if not config or not config.enabled or not config.apiKey:
        logger.warning("[RAG] 未找到可用的 Embedding 配置，语义搜索已禁用")
        return None

    base_url = config.apiUrl
    if base_url.endswith("/embeddings"):
        base_url = base_url[: -len("/embeddings")]

    _embeddings = _ZhipuEmbeddings(base_url, config.apiKey, config.modelId)
    logger.info("[RAG] 使用智谱 Embedding: %s, model: %s", base_url, config.modelId)
    return _embeddings


def _strip_html(text: str) -> str:
    """去除 HTML 标签。"""
    return re.sub(r"<[^>]+>", "", text)


def _strip_markdown(text: str) -> str:
    """去除 Markdown 语法标记。"""
    text = re.sub(r"!\[.*?\]\(.*?\)", "", text)
    text = re.sub(r"\[([^\]]+)\]\(.*?\)", r"\1", text)
    text = re.sub(r"^#{1,6}\s+", "", text, flags=re.MULTILINE)
    text = re.sub(r"\*\*(.+?)\*\*", r"\1", text)
    text = re.sub(r"\*(.+?)\*", r"\1", text)
    text = re.sub(r"`{1,3}[^`]*`{1,3}", "", text)
    text = re.sub(r"^[-*+]\s+", "", text, flags=re.MULTILINE)
    text = re.sub(r"^>\s+", "", text, flags=re.MULTILINE)
    text = re.sub(r"\|", " ", text)
    text = re.sub(r"-{3,}", "", text)
    return text


def _build_embedding_text(article: Article) -> str:
    """构建用于 embedding 的文本。"""
    parts = []
    if article.title:
        parts.append(article.title)
    if article.description:
        parts.append(article.description)
    if article.content:
        content = _strip_html(article.content)
        content = _strip_markdown(content)
        parts.append(content[:8000])
    return "\n".join(parts)


def _get_vector_store() -> FAISS | None:
    """获取向量存储实例。"""
    global _vector_store
    return _vector_store


def _save_vector_store() -> None:
    """持久化向量存储到本地。"""
    global _vector_store
    if _vector_store is not None:
        try:
            _vector_store.save_local("./data/faiss_index")
            logger.info("[RAG] 向量存储已保存")
        except Exception as e:
            logger.warning("[RAG] 保存向量存储失败: %s", e)


async def _ensure_vector_store(db: AsyncSession) -> FAISS | None:
    """确保向量存储已初始化。"""
    global _vector_store
    if _vector_store is not None:
        return _vector_store

    embeddings = await _get_embeddings(db)
    if embeddings is None:
        return None

    import os
    if os.path.exists("./data/faiss_index"):
        try:
            _vector_store = FAISS.load_local("./data/faiss_index", embeddings, allow_dangerous_deserialization=True)
            logger.info("[RAG] 已加载向量存储")
        except Exception as e:
            logger.warning("[RAG] 加载向量存储失败，将重新创建: %s", e)
            _vector_store = None

    if _vector_store is None:
        os.makedirs("./data", exist_ok=True)
        # 创建空的 FAISS 索引
        _vector_store = FAISS.from_texts(["placeholder"], embeddings, metadatas=[{"placeholder": True}])
        logger.info("[RAG] 创建新的向量存储")

    return _vector_store


async def index_article(db: AsyncSession, article: Article) -> None:
    """为文章建立向量索引。"""
    store = await _ensure_vector_store(db)
    if store is None:
        return

    text = _build_embedding_text(article)
    if not text.strip():
        return

    # 分割文本
    docs = _text_splitter.create_documents(
        [text],
        metadatas=[{
            "articleId": article.id,
            "userId": article.user_id,
            "category": article.category or "",
            "tags": ",".join(article.tags) if article.tags else "",
        }],
    )

    # 先删除旧索引，再添加新索引
    try:
        store.delete([f"article-{article.id}"])
    except Exception:
        pass

    for i, doc in enumerate(docs):
        doc.metadata["doc_id"] = f"article-{article.id}-{i}"

    store.add_documents(docs)
    _save_vector_store()
    logger.info("[RAG] 文章 %d 已索引", article.id)


async def remove_article(article_id: int) -> None:
    """删除文章的向量索引。"""
    store = _get_vector_store()
    if store is None:
        return
    try:
        store.delete([f"article-{article_id}"])
        _save_vector_store()
    except Exception as e:
        logger.warning("[RAG] 删除索引失败: %s", e)


async def reindex_all(db: AsyncSession, user_id: int) -> int:
    """重新索引用户的所有已发布文章。"""
    result = await db.execute(
        select(Article).where(Article.user_id == user_id, Article.status == "published")
    )
    articles = result.scalars().all()
    count = 0
    for article in articles:
        await index_article(db, article)
        count += 1
    return count


async def search_similar(db: AsyncSession, query: str, user_id: int, top_k: int = 5) -> list[Article]:
    """语义搜索相似文章。"""
    store = await _ensure_vector_store(db)
    if store is None:
        return []

    # FAISS 搜索
    results = store.similarity_search(query, k=top_k * 2)

    # 过滤当前用户的文章
    article_ids = set()
    for doc in results:
        meta = doc.metadata
        if meta.get("userId") == user_id and not meta.get("placeholder"):
            article_ids.add(meta.get("articleId"))

    if not article_ids:
        return []

    result = await db.execute(select(Article).where(Article.id.in_(list(article_ids))))
    return list(result.scalars().all())


async def search_similar_public(db: AsyncSession, query: str, top_k: int = 5) -> list[Article]:
    """语义搜索公开文章（游客模式）。"""
    store = await _ensure_vector_store(db)
    if store is None:
        return []

    # FAISS 搜索
    results = store.similarity_search(query, k=top_k * 2)

    # 提取 articleId 并过滤公开文章
    article_ids = set()
    for doc in results:
        meta = doc.metadata
        if not meta.get("placeholder") and meta.get("articleId"):
            article_ids.add(meta.get("articleId"))

    if not article_ids:
        return []

    result = await db.execute(
        select(Article).where(
            Article.id.in_(list(article_ids)),
            Article.status == "published",
            Article.is_public == True,
        )
    )
    return list(result.scalars().all())

