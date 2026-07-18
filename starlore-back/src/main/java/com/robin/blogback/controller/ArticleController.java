package com.robin.blogback.controller;

import com.robin.blogback.dto.*;
import com.robin.blogback.entity.User;
import com.robin.blogback.mapper.UserMapper;
import com.robin.blogback.service.ArticleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private UserMapper userMapper;

    /** 判断当前用户是否为管理员 */
    private boolean isAdmin(Integer userId) {
        if (userId == null) return false;
        User user = userMapper.selectById(userId);
        return user != null && "admin".equals(user.getRole());
    }

    @GetMapping("/stats/summary")
    public ResponseEntity<?> getBlogStats(HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        boolean admin = isAdmin(userId);
        BlogStatsResponse stats = articleService.getBlogStats(userId, admin);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/daily")
    public ResponseEntity<?> getDailyStats(HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        boolean admin = isAdmin(userId);
        DailyStatsResponse stats = articleService.getDailyStats(userId, admin);
        return ResponseEntity.ok(stats);
    }

    @GetMapping
    public ResponseEntity<?> getAllArticles(HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Integer authorUserId) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        boolean admin = isAdmin(userId);

        if (admin) {
            ArticleListResponse response = articleService
                    .getAllArticles(userId, page, limit, category, search, tag, authorUserId, false);
            return ResponseEntity.ok(response);
        }

        ArticleListResponse response = articleService.getAllArticles(userId, page, limit, category, search, tag);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getArticleById(HttpServletRequest httpRequest, @PathVariable Integer id) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        ArticleDetail article = articleService.getArticleById(userId, id);
        return ResponseEntity.ok(Map.of("data", article));
    }

    @PostMapping
    public ResponseEntity<?> createArticle(HttpServletRequest httpRequest, @Valid @RequestBody CreateArticleRequest request) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        ArticleDetail article = articleService.createArticle(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "文章创建成功", "data", article));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateArticle(HttpServletRequest httpRequest, @PathVariable Integer id,
                                           @Valid @RequestBody UpdateArticleRequest request) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        ArticleDetail article = articleService.updateArticle(userId, isAdmin(userId), id, request);
        return ResponseEntity.ok(Map.of("message", "文章更新成功", "data", article));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteArticle(HttpServletRequest httpRequest, @PathVariable Integer id) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        Map<String, Object> result = articleService.deleteArticle(userId, isAdmin(userId), id);
        return ResponseEntity.ok(result);
    }
}
