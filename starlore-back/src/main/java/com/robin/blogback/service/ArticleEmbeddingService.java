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

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ArticleEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(ArticleEmbeddingService.class);
    private static final File STORE_FILE = new File("./data/vector-store.json");

    @Autowired(required = false)
    private VectorStore vectorStore;

    @Autowired
    private ArticleMapper articleMapper;

    /**
     * 索引单篇文章到向量存储
     */
    public void indexArticle(Article article) {
        if (vectorStore == null) return;
        if (article == null || article.getId() == null) return;

        String text = buildEmbeddingText(article);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("articleId", article.getId());
        metadata.put("userId", article.getUserId());
        metadata.put("category", article.getCategory() != null ? article.getCategory() : "");
        metadata.put("tags", article.getTags() != null ? String.join(",", article.getTags()) : "");

        Document doc = new Document("article_" + article.getId(), text, metadata);
        vectorStore.add(List.of(doc));
        saveToFile();
        log.info("[RAG] 已索引文章: id={}, title={}", article.getId(), article.getTitle());
    }

    /**
     * 从向量存储中移除文章
     */
    public void removeArticle(Integer articleId) {
        if (vectorStore == null) return;
        vectorStore.delete(List.of("article_" + articleId));
        saveToFile();
        log.info("[RAG] 已移除文章向量: id={}", articleId);
    }

    /**
     * 重新索引指定用户的所有已发布文章
     */
    public int reindexAll(Integer userId) {
        if (vectorStore == null) {
            log.warn("[RAG] VectorStore 不可用，跳过索引");
            return 0;
        }

        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getUserId, userId)
                        .eq(Article::getStatus, "published"));

        List<Document> documents = new ArrayList<>();
        for (Article article : articles) {
            String text = buildEmbeddingText(article);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("articleId", article.getId());
            metadata.put("userId", article.getUserId());
            metadata.put("category", article.getCategory() != null ? article.getCategory() : "");
            metadata.put("tags", article.getTags() != null ? String.join(",", article.getTags()) : "");
            documents.add(new Document("article_" + article.getId(), text, metadata));
        }

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
            saveToFile();
        }
        log.info("[RAG] 已重新索引 {} 篇文章 (userId={})", documents.size(), userId);
        return documents.size();
    }

    /**
     * 语义搜索相关文章
     */
    public List<Article> searchSimilar(String query, Integer userId, int topK) {
        if (vectorStore == null) return Collections.emptyList();

        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(0.3)
                    .build();

            List<Document> results = vectorStore.similaritySearch(request);
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
     * 访客语义搜索公开相关文章
     */
    public List<Article> searchSimilarPublic(String query, int topK) {
        if (vectorStore == null) return Collections.emptyList();

        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(0.3)
                    .build();

            List<Document> results = vectorStore.similaritySearch(request);
            if (results == null || results.isEmpty()) return Collections.emptyList();

            // 提取 articleId（不需要按 userId 过滤，公开文章即可）
            List<Integer> articleIds = results.stream()
                    .map(doc -> {
                        Object id = doc.getMetadata().get("articleId");
                        return id != null ? Integer.parseInt(id.toString()) : null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (articleIds.isEmpty()) return Collections.emptyList();

            // 批量查询公开文章 (published 且 isPublic = true)
            return articleMapper.selectList(
                    new LambdaQueryWrapper<Article>()
                            .in(Article::getId, articleIds)
                            .eq(Article::getStatus, "published")
                            .eq(Article::getIsPublic, true));
        } catch (Exception e) {
            log.error("[RAG] 访客公开语义搜索失败: {}", e.getMessage());
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

    private void saveToFile() {
        try {
            ((org.springframework.ai.vectorstore.SimpleVectorStore) vectorStore).save(STORE_FILE);
        } catch (Exception e) {
            log.warn("[RAG] 保存向量存储失败: {}", e.getMessage());
        }
    }
}
