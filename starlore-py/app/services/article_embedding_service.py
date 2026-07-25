"""RAG 文章向量索引服务 — 基于 LangChain FAISS。"""

import logging
import re

from langchain_community.vectorstores import FAISS
from langchain_core.documents import Document
from langchain_core.embeddings import Embeddings
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.article import Article
from app.services import ai_config_service, knowledge_chunk_service


logger = logging.getLogger(__name__)

_vector_store: FAISS | None = None
_embeddings: Embeddings | None = None


class _ZhipuEmbeddings(Embeddings):
    """智谱 Embedding 封装，兼容 LangChain Embeddings 接口。"""

    def __init__(self, base_url: str, api_key: str, model: str):
        url = base_url.rstrip("/")
        if not url.endswith("/embeddings"):
            url += "/embeddings"
        self._url = url
        self._api_key = api_key
        self._model = model or "embedding-2"

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        import httpx
        import time
        if not texts:
            return []

        batch_size = 8
        all_embeddings: list[list[float]] = []

        with httpx.Client(timeout=60.0) as client:
            for i in range(0, len(texts), batch_size):
                batch = [t if t and t.strip() else " " for t in texts[i : i + batch_size]]
                last_err = None
                for attempt in range(3):
                    try:
                        resp = client.post(
                            self._url,
                            json={"model": self._model, "input": batch},
                            headers={
                                "Authorization": f"Bearer {self._api_key}",
                                "Content-Type": "application/json",
                            },
                        )
                        if resp.status_code >= 400:
                            logger.error(
                                "[RAG Embedding 异常] 智谱 API 返回 HTTP %d: %s (URL: %s, Model: %s)",
                                resp.status_code,
                                resp.text,
                                self._url,
                                self._model,
                            )
                            resp.raise_for_status()
                        data = resp.json()
                        embeddings = [item["embedding"] for item in data.get("data", [])]
                        all_embeddings.extend(embeddings)
                        last_err = None
                        break
                    except Exception as e:
                        last_err = e
                        logger.warning("[RAG Embedding 重试 %d/3] %s", attempt + 1, e)
                        time.sleep(1.0)

                if last_err is not None:
                    raise last_err

        return all_embeddings

    def embed_query(self, text: str) -> list[float]:
        results = self.embed_documents([text])
        return results[0] if results else []


async def _get_embeddings(db: AsyncSession) -> Embeddings | None:
    """获取 Embedding 实例（从数据库配置）。"""
    global _embeddings
    config = await ai_config_service.get_config_by_key(db, "zhipu-embedding")
    if not config or not config.enabled or not config.apiKey:
        logger.warning("[RAG] 未找到可用的 Embedding 配置（zhipu-embedding），语义搜索已禁用")
        return None

    base_url = config.apiUrl or "https://open.bigmodel.cn/api/paas/v4"
    model_id = config.modelId or "embedding-2"

    _embeddings = _ZhipuEmbeddings(base_url, config.apiKey, model_id)
    logger.info("[RAG] 使用智谱 Embedding: %s, model: %s", base_url, model_id)
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
    """按数据库中的结构化切片为文章建立向量索引。"""
    store = await _ensure_vector_store(db)
    if store is None:
        return

    _delete_article_vectors(store, article.id)
    chunks = await knowledge_chunk_service.rebuild_chunks(db, article)
    await _add_chunk_documents(store, article, chunks)
    _save_vector_store()
    logger.info("[RAG] 文章 %d 已按 %d 个结构化切片索引", article.id, len(chunks))


async def refresh_article_vectors(db: AsyncSession, article: Article) -> None:
    """切片人工编辑或启停后，仅刷新向量，不重新切片。"""
    store = await _ensure_vector_store(db)
    if store is None:
        return
    chunks = await knowledge_chunk_service.ensure_chunks(db, article)
    _delete_article_vectors(store, article.id)
    await _add_chunk_documents(store, article, chunks)
    _save_vector_store()


async def remove_article(article_id: int, db: AsyncSession | None = None) -> None:
    """删除文章的向量索引。"""
    if db is not None:
        await knowledge_chunk_service.delete_chunks(db, article_id)
    store = _get_vector_store()
    if store is None:
        return
    try:
        _delete_article_vectors(store, article_id)
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


async def _add_chunk_documents(store: FAISS, article: Article, chunks) -> None:
    enabled = [chunk for chunk in chunks if chunk.isEnabled == 1]
    if not enabled:
        return
    docs = [
        Document(
            page_content=chunk.content,
            metadata={
                "articleId": article.id,
                "userId": article.user_id,
                "articleTitle": article.title,
                "category": article.category or "",
                "tags": ",".join(article.tags) if article.tags else "",
                "chunkId": chunk.id,
                "chunkIndex": chunk.chunkIndex,
            },
        )
        for chunk in enabled
    ]
    ids = [f"article_{article.id}_chunk_{chunk.chunkIndex}" for chunk in enabled]
    store.add_documents(docs, ids=ids)


def _delete_article_vectors(store: FAISS, article_id: int) -> None:
    ids: list[str] = []
    for vector_id in list(store.index_to_docstore_id.values()):
        doc = store.docstore.search(vector_id)
        if isinstance(doc, Document) and doc.metadata.get("articleId") == article_id:
            ids.append(vector_id)
    existing = list(dict.fromkeys(ids))
    if not existing:
        return
    try:
        store.delete(existing)
    except Exception as exc:
        logger.debug("[RAG] 部分旧向量不存在，忽略清理异常: %s", exc)


async def search_similar(
    db: AsyncSession,
    query: str,
    user_id: int,
    top_k: int = 5,
    similarity_threshold: float = 0.5,
    enable_rerank: int = 1,
    category: str | None = None,
    tag: str | None = None,
) -> list[Article]:
    """根据 Agent 调优参数（Top-K、相似度阈值、BM25 Rerank）真正执行检索。"""
    store = await _ensure_vector_store(db)
    if store is None:
        return []

    try:
        results_with_scores = store.similarity_search_with_score(query, k=top_k * 4)
    except Exception:
        results = store.similarity_search(query, k=top_k * 2)
        results_with_scores = [(doc, 0.0) for doc in results]

    article_ids = []
    seen = set()
    for doc, distance in results_with_scores:
        meta = doc.metadata
        similarity = 1.0 / (1.0 + float(distance))
        if meta.get("userId") == user_id and not meta.get("placeholder"):
            if category and meta.get("category") != category:
                continue
            if tag:
                doc_tags = (meta.get("tags") or "").split(",")
                if tag not in doc_tags:
                    continue
            art_id = meta.get("articleId")
            if art_id and art_id not in seen:
                if similarity >= (similarity_threshold - 0.2):
                    seen.add(art_id)
                    article_ids.append(art_id)
                    if len(article_ids) >= top_k:
                        break

    if not article_ids:
        return []

    result = await db.execute(select(Article).where(Article.id.in_(article_ids)))
    articles = list(result.scalars().all())
    return articles[:top_k]
