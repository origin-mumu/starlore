package com.robin.blogback.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robin.blogback.entity.Article;
import com.robin.blogback.mapper.ArticleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ArticleEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(ArticleEmbeddingService.class);
    private static final File STORE_FILE = new File("./data/vector-store.json");
    private final ScheduledExecutorService persistenceExecutor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "vector-store-persistence");
                thread.setDaemon(true);
                return thread;
            });
    private ScheduledFuture<?> pendingSave;

    @Autowired(required = false)
    private VectorStore vectorStore;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private KnowledgeChunkService knowledgeChunkService;

    /**
     * 索引单篇文章到向量存储
     */
    public synchronized void indexArticle(Article article) {
        if (vectorStore == null) return;
        if (article == null || article.getId() == null) return;

        deleteArticleVectors(article.getId(), knowledgeChunkService.getChunks(article.getId()));
        List<com.robin.blogback.entity.ArticleChunk> chunks = knowledgeChunkService.rebuildChunks(article);
        addChunkDocuments(article, chunks);
        scheduleSave();
        log.info("[RAG] 已索引文章切片: id={}, title={}, chunks={}",
                article.getId(), article.getTitle(), chunks.size());
    }

    public synchronized void refreshArticleVectors(Article article) {
        if (vectorStore == null || article == null || article.getId() == null) return;
        List<com.robin.blogback.entity.ArticleChunk> chunks = knowledgeChunkService.ensureChunks(article);
        deleteArticleVectors(article.getId(), chunks);
        addChunkDocuments(article, chunks);
        scheduleSave();
    }

    /**
     * 从向量存储中移除文章
     */
    public synchronized void removeArticle(Integer articleId) {
        if (vectorStore == null) return;
        List<com.robin.blogback.entity.ArticleChunk> chunks = knowledgeChunkService.getChunks(articleId);
        deleteArticleVectors(articleId, chunks);
        knowledgeChunkService.deleteChunks(articleId);
        scheduleSave();
        log.info("[RAG] 已移除文章向量: id={}", articleId);
    }

    /**
     * 重新索引指定用户的所有已发布文章
     */
    public synchronized int reindexAll(Integer userId) {
        if (vectorStore == null) {
            log.warn("[RAG] VectorStore 不可用，跳过索引");
            return 0;
        }

        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getUserId, userId)
                        .eq(Article::getStatus, "published"));

        for (Article article : articles) {
            indexArticle(article);
        }
        log.info("[RAG] 已重新索引 {} 篇文章 (userId={})", articles.size(), userId);
        return articles.size();
    }

    /**
     * 语义搜索相关文章
     */
    public synchronized List<Article> searchSimilar(String query, Integer userId, int topK) {
        if (vectorStore == null) return Collections.emptyList();

        try {
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(Math.max(topK * 3, topK))
                            .similarityThreshold(0.3).build());
            if (results == null || results.isEmpty()) return Collections.emptyList();

            // 过滤当前用户的文档，提取 articleId
            List<Integer> articleIds = results.stream()
                    .filter(doc -> {
                        Object docUserId = doc.getMetadata().get("userId");
                        return docUserId != null && docUserId.toString().equals(userId.toString());
                    })
                    .map(doc -> {
                        Object id = doc.getMetadata().get("articleId");
                        return id != null ? Integer.parseInt(id.toString()) : null;
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(topK)
                    .collect(Collectors.toList());

            if (articleIds.isEmpty()) return Collections.emptyList();

            // 批量查询文章
            return articleMapper.selectList(
                    new LambdaQueryWrapper<Article>()
                            .in(Article::getId, articleIds)
                            .eq(Article::getStatus, "published"));
        } catch (Exception e) {
            log.error("[RAG] 语义搜索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 构建用于 embedding 的文本（标题 + 描述 + 内容摘要）
     */
    private String buildEmbeddingText(Article article) {
        StringBuilder sb = new StringBuilder();
        if (article.getTitle() != null) {
            sb.append(article.getTitle()).append("\n");
        }
        if (article.getDescription() != null) {
            sb.append(article.getDescription()).append("\n");
        }
        if (article.getContent() != null) {
            String plain = article.getContent()
                    .replaceAll("<[^>]+>", "")  // 去 HTML 标签
                    .replaceAll("#+\\s*", "")
                    .replaceAll("\\*\\*?", "")
                    .replaceAll("`{1,3}", "")
                    .replaceAll("\\[([^]]*)\\]\\([^)]*\\)", "$1")
                    .replaceAll("\\n+", " ")
                    .trim();
            if (plain.length() > 8000) {
                plain = plain.substring(0, 8000);
            }
            sb.append(plain);
        }
        return sb.toString();
    }

    private synchronized void scheduleSave() {
        if (pendingSave != null) pendingSave.cancel(false);
        pendingSave = persistenceExecutor.schedule(this::saveToFile, 500, TimeUnit.MILLISECONDS);
    }

    private void addChunkDocuments(Article article, List<com.robin.blogback.entity.ArticleChunk> chunks) {
        List<Document> documents = chunks.stream()
                .filter(chunk -> Integer.valueOf(1).equals(chunk.getIsEnabled()))
                .map(chunk -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("articleId", article.getId());
                    metadata.put("userId", article.getUserId());
                    metadata.put("articleTitle", article.getTitle());
                    metadata.put("category", article.getCategory() != null ? article.getCategory() : "");
                    metadata.put("tags", article.getTags() != null ? String.join(",", article.getTags()) : "");
                    metadata.put("chunkId", chunk.getId());
                    metadata.put("chunkIndex", chunk.getChunkIndex());
                    return new Document(chunkDocumentId(article.getId(), chunk.getChunkIndex()),
                            chunk.getContent(), metadata);
                })
                .toList();
        if (!documents.isEmpty()) vectorStore.add(documents);
    }

    private void deleteArticleVectors(
            Integer articleId, List<com.robin.blogback.entity.ArticleChunk> chunks) {
        List<String> ids = new ArrayList<>();
        ids.add("article_" + articleId);
        chunks.forEach(chunk -> ids.add(chunkDocumentId(articleId, chunk.getChunkIndex())));
        vectorStore.delete(ids);
    }

    private String chunkDocumentId(Integer articleId, Integer chunkIndex) {
        return "article_" + articleId + "_chunk_" + chunkIndex;
    }

    private synchronized void saveToFile() {
        try {
            File parent = STORE_FILE.getAbsoluteFile().getParentFile();
            if (parent != null) Files.createDirectories(parent.toPath());
            File temporaryFile = new File(STORE_FILE.getPath() + ".tmp");
            ((org.springframework.ai.vectorstore.SimpleVectorStore) vectorStore).save(temporaryFile);
            try {
                Files.move(temporaryFile.toPath(), STORE_FILE.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporaryFile.toPath(), STORE_FILE.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            log.warn("[RAG] 保存向量存储失败: {}", e.getMessage());
        }
    }

    @PreDestroy
    public synchronized void shutdownPersistence() {
        if (pendingSave != null && !pendingSave.isDone()) pendingSave.cancel(false);
        if (vectorStore != null) saveToFile();
        persistenceExecutor.shutdown();
    }
}
