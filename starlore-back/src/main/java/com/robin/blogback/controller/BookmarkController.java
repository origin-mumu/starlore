package com.robin.blogback.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robin.blogback.dto.ArticleSummary;
import com.robin.blogback.entity.Article;
import com.robin.blogback.entity.Bookmark;
import com.robin.blogback.mapper.ArticleMapper;
import com.robin.blogback.mapper.BookmarkMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    @Autowired
    private BookmarkMapper bookmarkMapper;
    @Autowired
    private ArticleMapper articleMapper;

    @GetMapping
    public ResponseEntity<?> listBookmarks(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");

        List<Bookmark> bookmarks = bookmarkMapper.selectList(
                new LambdaQueryWrapper<Bookmark>()
                        .eq(Bookmark::getUserId, userId)
                        .orderByDesc(Bookmark::getCreatedAt));

        if (bookmarks.isEmpty()) {
            return ResponseEntity.ok(Map.of("data", List.of()));
        }

        Set<Integer> articleIds = bookmarks.stream()
                .map(Bookmark::getArticleId)
                .collect(Collectors.toSet());

        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .in(Article::getId, articleIds)
                        .eq(Article::getStatus, "published"));

        Map<Integer, Article> articleMap = articles.stream()
                .collect(Collectors.toMap(Article::getId, a -> a));

        List<ArticleSummary> result = bookmarks.stream()
                .map(b -> articleMap.get(b.getArticleId()))
                .filter(a -> a != null)
                .map(this::toSummary)
                .toList();

        return ResponseEntity.ok(Map.of("data", result));
    }

    @PostMapping
    public ResponseEntity<?> addBookmark(HttpServletRequest request, @RequestBody Map<String, Integer> body) {
        Integer userId = (Integer) request.getAttribute("userId");
        Integer articleId = body.get("articleId");

        Long count = bookmarkMapper.selectCount(
                new LambdaQueryWrapper<Bookmark>()
                        .eq(Bookmark::getUserId, userId)
                        .eq(Bookmark::getArticleId, articleId));
        if (count > 0) {
            return ResponseEntity.ok(Map.of("message", "已收藏"));
        }

        Bookmark bookmark = new Bookmark();
        bookmark.setUserId(userId);
        bookmark.setArticleId(articleId);
        bookmark.setCreatedAt(LocalDateTime.now());
        bookmarkMapper.insert(bookmark);

        return ResponseEntity.ok(Map.of("message", "收藏成功"));
    }

    @DeleteMapping("/{articleId}")
    public ResponseEntity<?> removeBookmark(HttpServletRequest request, @PathVariable Integer articleId) {
        Integer userId = (Integer) request.getAttribute("userId");

        bookmarkMapper.delete(
                new LambdaQueryWrapper<Bookmark>()
                        .eq(Bookmark::getUserId, userId)
                        .eq(Bookmark::getArticleId, articleId));

        return ResponseEntity.ok(Map.of("message", "已取消收藏"));
    }

    @GetMapping("/check/{articleId}")
    public ResponseEntity<?> checkBookmark(HttpServletRequest request, @PathVariable Integer articleId) {
        Integer userId = (Integer) request.getAttribute("userId");

        Long count = bookmarkMapper.selectCount(
                new LambdaQueryWrapper<Bookmark>()
                        .eq(Bookmark::getUserId, userId)
                        .eq(Bookmark::getArticleId, articleId));

        return ResponseEntity.ok(Map.of("bookmarked", count > 0));
    }

    private ArticleSummary toSummary(Article a) {
        ArticleSummary s = new ArticleSummary();
        s.setId(a.getId());
        s.setTitle(a.getTitle());
        s.setStatus(a.getStatus());
        s.setDescription(a.getDescription());
        s.setCategory(a.getCategory());
        s.setTags(a.getTags());
        s.setCoverImage(a.getCoverImage());
        s.setViewCount(a.getViewCount());
        s.setCreatedAt(a.getCreatedAt());
        return s;
    }
}
