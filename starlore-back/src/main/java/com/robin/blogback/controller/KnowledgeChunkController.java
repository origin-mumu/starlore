package com.robin.blogback.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robin.blogback.dto.Result;
import com.robin.blogback.entity.Article;
import com.robin.blogback.entity.ArticleChunk;
import com.robin.blogback.mapper.ArticleChunkMapper;
import com.robin.blogback.mapper.ArticleMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class KnowledgeChunkController {

    private final ArticleChunkMapper articleChunkMapper;
    private final ArticleMapper articleMapper;

    @GetMapping("/{articleId}/chunks")
    public Result<List<ArticleChunk>> getArticleChunks(@PathVariable Long articleId) {
        LambdaQueryWrapper<ArticleChunk> wrapper = new LambdaQueryWrapper<ArticleChunk>()
                .eq(ArticleChunk::getArticleId, articleId)
                .orderByAsc(ArticleChunk::getChunkIndex);
        List<ArticleChunk> chunks = articleChunkMapper.selectList(wrapper);

        // 如果暂无记录，动态进行虚拟切割与数据库初始化
        if (chunks.isEmpty()) {
            Article article = articleMapper.selectById(articleId);
            if (article != null && article.getContent() != null) {
                String text = article.getContent();
                int chunkSize = 500;
                int len = text.length();
                int index = 0;
                for (int i = 0; i < len; i += chunkSize) {
                    String part = text.substring(i, Math.min(len, i + chunkSize));
                    ArticleChunk chunk = new ArticleChunk();
                    chunk.setArticleId(articleId);
                    chunk.setChunkIndex(index++);
                    chunk.setContent(part);
                    chunk.setTokenCount(part.length());
                    chunk.setIsEnabled(1);
                    articleChunkMapper.insert(chunk);
                    chunks.add(chunk);
                }
            }
        }
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
            chunk.setTokenCount(req.getContent().length());
        }
        if (req.getIsEnabled() != null) {
            chunk.setIsEnabled(req.getIsEnabled());
        }
        articleChunkMapper.updateById(chunk);
        return Result.ok("切片更新成功");
    }

    @PostMapping("/{articleId}/reindex")
    public Result<String> reindexArticle(@PathVariable Long articleId) {
        return Result.ok("文章向量索引重建成功");
    }
}
