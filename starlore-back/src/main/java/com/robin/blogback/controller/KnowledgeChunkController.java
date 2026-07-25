package com.robin.blogback.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robin.blogback.dto.Result;
import com.robin.blogback.entity.Article;
import com.robin.blogback.entity.ArticleChunk;
import com.robin.blogback.mapper.ArticleChunkMapper;
import com.robin.blogback.mapper.ArticleMapper;
import com.robin.blogback.service.ArticleEmbeddingService;
import com.robin.blogback.service.KnowledgeChunkService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class KnowledgeChunkController {

    private final ArticleChunkMapper articleChunkMapper;
    private final ArticleMapper articleMapper;
    private final ArticleEmbeddingService articleEmbeddingService;
    private final KnowledgeChunkService knowledgeChunkService;

    @Data
    public static class ChunkView {
        private Long id;
        private Long articleId;
        private String articleTitle;
        private String articleCategory;
        private Integer chunkIndex;
        private String content;
        private Integer tokenCount;
        private Integer isEnabled;
    }

    @GetMapping("/chunks")
    public Result<Map<String, Object>> getAllChunks(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int limit,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }

        int safeLimit = Math.max(1, Math.min(limit, 48));
        int safePage = Math.max(1, page);
        LambdaQueryWrapper<Article> articleQuery = new LambdaQueryWrapper<Article>()
                .eq(Article::getUserId, userId)
                .orderByDesc(Article::getUpdatedAt);
        if (category != null && !category.isBlank()) {
            articleQuery.eq(Article::getCategory, category.trim());
        }
        List<Article> articles = articleMapper.selectList(articleQuery);

        String normalizedSearch = search == null ? "" : search.trim().toLowerCase();
        List<ChunkView> allMatches = articles.stream()
                .flatMap(article -> knowledgeChunkService.ensureChunks(article).stream()
                        .map(chunk -> toView(chunk, article)))
                .filter(chunk -> normalizedSearch.isEmpty()
                        || chunk.getContent().toLowerCase().contains(normalizedSearch)
                        || chunk.getArticleTitle().toLowerCase().contains(normalizedSearch))
                .toList();

        long total = allMatches.size();
        int pages = Math.max(1, (int) Math.ceil((double) total / safeLimit));
        safePage = Math.min(safePage, pages);
        int from = Math.min((safePage - 1) * safeLimit, allMatches.size());
        int to = Math.min(from + safeLimit, allMatches.size());

        List<String> categories = articleMapper.selectList(
                        new LambdaQueryWrapper<Article>()
                                .eq(Article::getUserId, userId)
                                .select(Article::getCategory))
                .stream()
                .map(Article::getCategory)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("current", safePage);
        pagination.put("total", total);
        pagination.put("pages", pages);
        pagination.put("limit", safeLimit);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", allMatches.subList(from, to));
        payload.put("pagination", pagination);
        payload.put("categories", categories);
        return Result.ok("获取切片成功", payload);
    }

    @GetMapping("/{articleId}/chunks")
    public Result<List<ArticleChunk>> getArticleChunks(@PathVariable Long articleId) {
        LambdaQueryWrapper<ArticleChunk> wrapper = new LambdaQueryWrapper<ArticleChunk>()
                .eq(ArticleChunk::getArticleId, articleId)
                .orderByAsc(ArticleChunk::getChunkIndex);
        List<ArticleChunk> chunks = articleChunkMapper.selectList(wrapper);

        if (chunks.isEmpty()) chunks = knowledgeChunkService.ensureChunks(articleMapper.selectById(articleId));
        return Result.ok("获取切片成功", chunks);
    }

    @Data
    public static class ChunkUpdateRequest {
        private String content;
        private Integer isEnabled;
    }

    @PutMapping("/chunks/{chunkId}")
    public Result<String> updateChunk(@PathVariable Long chunkId, @RequestBody ChunkUpdateRequest req) {
        ArticleChunk chunk = articleChunkMapper.selectById(chunkId);
        if (chunk == null) {
            return Result.error("切片不存在");
        }
        if (req.getContent() != null) {
            chunk.setContent(req.getContent());
            chunk.setTokenCount(knowledgeChunkService.estimateTokens(req.getContent()));
        }
        if (req.getIsEnabled() != null) {
            chunk.setIsEnabled(req.getIsEnabled());
        }
        articleChunkMapper.updateById(chunk);
        Article article = articleMapper.selectById(chunk.getArticleId());
        articleEmbeddingService.refreshArticleVectors(article);
        return Result.ok("切片更新成功");
    }

    @PostMapping("/{articleId}/reindex")
    public Result<String> reindexArticle(@PathVariable Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            return Result.error("文章不存在");
        }
        articleEmbeddingService.indexArticle(article);
        return Result.ok("文章向量索引重建成功");
    }

    @PostMapping("/reindex")
    public Result<String> reindexAll(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        int count = articleEmbeddingService.reindexAll(userId);
        return Result.ok("已重建 " + count + " 篇文章的向量索引");
    }

    private ChunkView toView(ArticleChunk chunk, Article article) {
        ChunkView view = new ChunkView();
        view.setId(chunk.getId());
        view.setArticleId(chunk.getArticleId());
        view.setArticleTitle(article.getTitle());
        view.setArticleCategory(article.getCategory());
        view.setChunkIndex(chunk.getChunkIndex());
        view.setContent(chunk.getContent());
        view.setTokenCount(chunk.getTokenCount());
        view.setIsEnabled(chunk.getIsEnabled());
        return view;
    }
}
