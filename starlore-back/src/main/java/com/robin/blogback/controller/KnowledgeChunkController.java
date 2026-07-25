package com.robin.blogback.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robin.blogback.dto.SimpleResponse;
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
    public SimpleResponse getAllChunks(
            HttpServletRequest request,
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) String articleCategory,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int limit) {

        Integer userId = (Integer) request.getAttribute("userId");
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(userId != null, Article::getUserId, userId)
                        .eq(articleCategory != null && !articleCategory.isEmpty(), Article::getCategory, articleCategory)
        );

        if (articles.isEmpty()) {
            Map<String, Object> emptyPayload = new LinkedHashMap<>();
            emptyPayload.put("items", List.of());
            emptyPayload.put("pagination", Map.of("total", 0, "page", page, "limit", limit, "totalPages", 0));
            emptyPayload.put("categories", List.of());
            return SimpleResponse.ok("获取切片成功", emptyPayload);
        }

        Map<Integer, Article> articleMap = new LinkedHashMap<>();
        for (Article article : articles) {
            articleMap.put(article.getId(), article);
        }

        List<ArticleChunk> chunks = articleChunkMapper.selectList(
                new LambdaQueryWrapper<ArticleChunk>()
                        .in(ArticleChunk::getArticleId, articleMap.keySet())
                        .eq(articleId != null, ArticleChunk::getArticleId, articleId)
                        .orderByAsc(ArticleChunk::getArticleId)
                        .orderByAsc(ArticleChunk::getChunkIndex)
        );

        List<ChunkView> allMatches = chunks.stream()
                .filter(chunk -> {
                    if (keyword == null || keyword.trim().isEmpty()) return true;
                    return chunk.getContent() != null && chunk.getContent().toLowerCase().contains(keyword.trim().toLowerCase());
                })
                .map(chunk -> toView(chunk, articleMap.get(chunk.getArticleId() == null ? null : chunk.getArticleId().intValue())))
                .filter(view -> view.getArticleTitle() != null)
                .toList();

        List<String> categories = articleMapper.selectList(
                new LambdaQueryWrapper<Article>().eq(userId != null, Article::getUserId, userId)
        ).stream().map(Article::getCategory).filter(c -> c != null && !c.isEmpty()).distinct().toList();

        int total = allMatches.size();
        int totalPages = (int) Math.ceil((double) total / limit);
        int from = Math.min((page - 1) * limit, total);
        int to = Math.min(from + limit, total);

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("total", total);
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("totalPages", totalPages);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", allMatches.subList(from, to));
        payload.put("pagination", pagination);
        payload.put("categories", categories);
        return SimpleResponse.ok("获取切片成功", payload);
    }

    @GetMapping("/{articleId}/chunks")
    public SimpleResponse getArticleChunks(@PathVariable Long articleId) {
        LambdaQueryWrapper<ArticleChunk> wrapper = new LambdaQueryWrapper<ArticleChunk>()
                .eq(ArticleChunk::getArticleId, articleId)
                .orderByAsc(ArticleChunk::getChunkIndex);
        List<ArticleChunk> chunks = articleChunkMapper.selectList(wrapper);

        if (chunks.isEmpty() && articleId != null) chunks = knowledgeChunkService.ensureChunks(articleMapper.selectById(articleId.intValue()));
        return SimpleResponse.ok("获取切片成功", chunks);
    }

    @Data
    public static class ChunkUpdateRequest {
        private String content;
        private Integer isEnabled;
    }

    @PutMapping("/chunks/{chunkId}")
    public SimpleResponse updateChunk(@PathVariable Long chunkId, @RequestBody ChunkUpdateRequest req) {
        ArticleChunk chunk = articleChunkMapper.selectById(chunkId);
        if (chunk == null) {
            return SimpleResponse.fail("切片不存在");
        }
        if (req.getContent() != null) {
            chunk.setContent(req.getContent());
            chunk.setTokenCount(knowledgeChunkService.estimateTokens(req.getContent()));
        }
        if (req.getIsEnabled() != null) {
            chunk.setIsEnabled(req.getIsEnabled());
        }
        articleChunkMapper.updateById(chunk);
        Article article = chunk.getArticleId() != null ? articleMapper.selectById(chunk.getArticleId().intValue()) : null;
        if (article != null) articleEmbeddingService.refreshArticleVectors(article);
        return SimpleResponse.ok("切片更新成功");
    }

    @PostMapping("/{articleId}/reindex")
    public SimpleResponse reindexArticle(@PathVariable Long articleId) {
        Article article = articleId != null ? articleMapper.selectById(articleId.intValue()) : null;
        if (article == null) {
            return SimpleResponse.fail("文章不存在");
        }
        articleEmbeddingService.indexArticle(article);
        return SimpleResponse.ok("文章向量索引重建成功");
    }

    @PostMapping("/reindex")
    public SimpleResponse reindexAll(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return SimpleResponse.fail("未登录");
        }
        int count = articleEmbeddingService.reindexAll(userId);
        return SimpleResponse.ok("已重建 " + count + " 篇文章的向量索引");
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
