package com.robin.blogback.controller;

import com.robin.blogback.dto.*;
import com.robin.blogback.service.ArticleService;
import com.robin.blogback.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/articles")
    public ResponseEntity<?> getPublicArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tag) {
        ArticleListResponse response = articleService.getPublicArticles(page, limit, category, search, tag);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/articles/{id}")
    public ResponseEntity<?> getPublicArticleById(@PathVariable Integer id) {
        ArticleDetail article = articleService.getPublicArticleById(id);
        return ResponseEntity.ok(Map.of("data", article));
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getPublicCategories() {
        CategoryListResponse response = categoryService.getPublicCategories();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<?> getPublicCategoryById(@PathVariable Integer id) {
        CategoryDetailResponse response = categoryService.getPublicCategoryById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getPublicStats() {
        BlogStatsResponse response = articleService.getPublicStats();
        return ResponseEntity.ok(response);
    }
}
