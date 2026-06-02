package com.robin.blogback.service;

import com.robin.blogback.dto.*;

import java.util.Map;

public interface ArticleService {
    ArticleListResponse getAllArticles(Integer userId, int page, int limit, String category, String search, String tag);
    ArticleListResponse getAllArticles(Integer currentUserId, int page, int limit, String category, String search, String tag, Integer filterUserId, boolean publishedOnly);
    ArticleDetail getArticleById(Integer userId, Integer id);
    BlogStatsResponse getBlogStats(Integer userId, boolean isAdmin);
    DailyStatsResponse getDailyStats(Integer userId, boolean isAdmin);
    ArticleDetail createArticle(Integer userId, CreateArticleRequest request);
    ArticleDetail updateArticle(Integer id, UpdateArticleRequest request);
    Map<String, Object> deleteArticle(Integer id);
}
