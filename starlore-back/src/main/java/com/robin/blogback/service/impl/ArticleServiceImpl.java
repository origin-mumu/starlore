package com.robin.blogback.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robin.blogback.dto.*;
import com.robin.blogback.entity.Article;
import com.robin.blogback.entity.Category;
import com.robin.blogback.entity.User;
import com.robin.blogback.exception.NotFoundException;
import com.robin.blogback.mapper.ArticleMapper;
import com.robin.blogback.mapper.CategoryMapper;
import com.robin.blogback.mapper.UserMapper;
import com.robin.blogback.service.ArticleService;
import com.robin.blogback.service.ArticleEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ArticleServiceImpl implements ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleServiceImpl.class);

    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired(required = false)
    private ArticleEmbeddingService articleEmbeddingService;


    @Override
    public ArticleListResponse getAllArticles(Integer userId, int page, int limit, String category, String search, String tag) {
        return getAllArticles(userId, page, limit, category, search, tag, null, true);
    }

    /** 扩展方法：支持查看全部（管理员）和按用户筛选 */
    public ArticleListResponse getAllArticles(Integer currentUserId, int page, int limit,
                                              String category, String search, String tag,
                                              Integer filterUserId, boolean publishedOnly) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();

        // 管理员查看全部 / 按用户筛选
        if (filterUserId != null) {
            wrapper.eq(Article::getUserId, filterUserId);
        } else if (!publishedOnly) {
            // 管理员查看全部，不过滤用户
        } else {
            wrapper.eq(Article::getUserId, currentUserId);
            wrapper.eq(Article::getStatus, "published");
        }

        if (publishedOnly) {
            wrapper.eq(Article::getStatus, "published");
        }

        if (StringUtils.hasText(category) && !"全部".equals(category)) {
            wrapper.eq(Article::getCategory, category);
        }
        if (StringUtils.hasText(search)) {
            wrapper.and(w -> w.like(Article::getTitle, search)
                    .or().like(Article::getDescription, search));
        }
        if (StringUtils.hasText(tag)) {
            wrapper.apply("JSON_CONTAINS(tags, JSON_ARRAY({0}))", tag);
        }

        wrapper.orderByDesc(Article::getCreatedAt);
        wrapper.select(Article::getId, Article::getUserId, Article::getTitle, Article::getStatus,
                Article::getDescription, Article::getCategory, Article::getTags,
                Article::getCoverImage, Article::getViewCount, Article::getCreatedAt);

        Page<Article> pageObj = new Page<>(page, limit);
        articleMapper.selectPage(pageObj, wrapper);

        // 查询作者名
        Set<Integer> userIds = pageObj.getRecords().stream()
                .map(Article::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Integer, String> userNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> userNameMap.put(u.getId(), u.getNickname()));
        }

        List<ArticleSummary> summaries = pageObj.getRecords().stream()
                .map(a -> toArticleSummaryWithAuthor(a, userNameMap.getOrDefault(a.getUserId(), "")))
                .toList();

        PaginationInfo pagination = new PaginationInfo(
                (int) pageObj.getCurrent(),
                pageObj.getTotal(),
                (int) pageObj.getPages()
        );

        return new ArticleListResponse(summaries, pagination);
    }

    @Override
    public ArticleDetail getArticleById(Integer userId, Integer id) {
        Article article = articleMapper.selectOne(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getId, id)
                        .eq(Article::getUserId, userId));
        if (article == null) {
            throw new NotFoundException("文章不存在");
        }

        // Increment view_count
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, id)
                .setSql("view_count = view_count + 1"));

        article.setViewCount(article.getViewCount() + 1);
        return toArticleDetail(article);
    }

    public BlogStatsResponse getBlogStats(Integer userId) {
        return getBlogStats(userId, false);
    }

    /** 获取博客统计，isAdmin=true 时查看全站数据 */
    public BlogStatsResponse getBlogStats(Integer userId, boolean isAdmin) {
        LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, "published");
        LambdaQueryWrapper<Category> categoryWrapper = new LambdaQueryWrapper<>();
        if (!isAdmin) {
            articleWrapper.eq(Article::getUserId, userId);
            categoryWrapper.eq(Category::getUserId, userId);
        }

        long totalArticles = articleMapper.selectCount(articleWrapper);
        long totalCategories = categoryMapper.selectCount(categoryWrapper);

        // Calculate total views
        LambdaQueryWrapper<Article> viewWrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, "published").select(Article::getViewCount);
        if (!isAdmin) viewWrapper.eq(Article::getUserId, userId);
        long totalViews = articleMapper.selectList(viewWrapper).stream()
                .mapToLong(a -> a.getViewCount() != null ? a.getViewCount() : 0).sum();

        // Popular articles (latest 4)
        LambdaQueryWrapper<Article> popularWrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, "published")
                .orderByDesc(Article::getCreatedAt).last("LIMIT 4")
                .select(Article::getId, Article::getUserId, Article::getTitle, Article::getDescription,
                        Article::getCreatedAt, Article::getCategory, Article::getCoverImage, Article::getTags);
        if (!isAdmin) popularWrapper.eq(Article::getUserId, userId);
        List<ArticleSummary> popularArticles = articleMapper.selectList(popularWrapper).stream()
                .map(this::toArticleSummary).toList();

        // Popular categories (top 5)
        LambdaQueryWrapper<Category> catWrapper = new LambdaQueryWrapper<Category>()
                .orderByDesc(Category::getArticleCount).last("LIMIT 5");
        if (!isAdmin) catWrapper.eq(Category::getUserId, userId);
        List<BlogStatsResponse.CategoryInfo> popularCategories = categoryMapper.selectList(catWrapper).stream()
                .map(c -> new BlogStatsResponse.CategoryInfo(c.getId(), c.getName(), c.getArticleCount()))
                .toList();

        BlogStatsResponse.BlogStatsData data = new BlogStatsResponse.BlogStatsData(
                totalArticles, totalCategories, totalViews, popularArticles, popularCategories);
        return new BlogStatsResponse(data);
    }

    public DailyStatsResponse getDailyStats(Integer userId) {
        return getDailyStats(userId, false);
    }

    /** 获取每日统计，isAdmin=true 时查看全站数据 */
    public DailyStatsResponse getDailyStats(Integer userId, boolean isAdmin) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, "published")
                .ge(Article::getCreatedAt, startDate.atStartOfDay())
                .select(Article::getCreatedAt);
        if (!isAdmin) wrapper.eq(Article::getUserId, userId);

        List<Article> articlesList = articleMapper.selectList(wrapper);

        Map<LocalDate, Integer> countMap = new LinkedHashMap<>();
        for (LocalDate d = startDate; !d.isAfter(today); d = d.plusDays(1)) {
            countMap.put(d, 0);
        }
        for (Article a : articlesList) {
            LocalDate date = a.getCreatedAt().toLocalDate();
            countMap.merge(date, 1, Integer::sum);
        }

        List<DailyStatsResponse.DailyItem> items = countMap.entrySet().stream()
                .map(e -> new DailyStatsResponse.DailyItem(e.getKey().toString(), e.getValue()))
                .toList();

        return new DailyStatsResponse(items);
    }

    @Override
    @Transactional
    public ArticleDetail createArticle(Integer userId, CreateArticleRequest request) {
        Article article = new Article();
        article.setUserId(userId);
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setDescription(request.getDescription());
        article.setCategory(request.getCategory() != null ? request.getCategory() : "随笔");
        article.setTags(request.getTags() != null ? request.getTags() : new ArrayList<>());
        article.setCoverImage(request.getCoverImage());
        article.setStatus(request.getStatus() != null ? request.getStatus() : "published");
        article.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : false);
        article.setViewCount(0);
        LocalDateTime now = LocalDateTime.now();
        article.setCreatedAt(now);
        article.setUpdatedAt(now);
        articleMapper.insert(article);

        // 自动索引用于语义搜索
        if (articleEmbeddingService != null && "published".equals(article.getStatus())) {
            try { articleEmbeddingService.indexArticle(article); } catch (Exception e) { log.warn("[RAG] 索引文章失败: {}", e.getMessage()); }
        }
        updateCategoryCount(article.getCategory(), userId);
        return toArticleDetail(article);
    }

    @Override
    @Transactional
    public ArticleDetail updateArticle(Integer id, UpdateArticleRequest request) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new NotFoundException("文章不存在");
        }

        String oldCategory = article.getCategory();

        if (request.getTitle() != null) article.setTitle(request.getTitle());
        if (request.getContent() != null) article.setContent(request.getContent());
        if (request.getDescription() != null) article.setDescription(request.getDescription());
        if (request.getCategory() != null) article.setCategory(request.getCategory());
        if (request.getTags() != null) article.setTags(request.getTags());
        if (request.getCoverImage() != null) article.setCoverImage(request.getCoverImage());
        if (request.getStatus() != null) article.setStatus(request.getStatus());
        if (request.getIsPublic() != null) article.setIsPublic(request.getIsPublic());
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);

        // 重新索引用于语义搜索
        if (articleEmbeddingService != null) {
            try { articleEmbeddingService.indexArticle(article); } catch (Exception e) { log.warn("[RAG] 重新索引文章失败: {}", e.getMessage()); }
        }
        if (!oldCategory.equals(article.getCategory())) {
            updateCategoryCount(oldCategory, null);
            updateCategoryCount(article.getCategory(), null);
        }

        return toArticleDetail(article);
    }

    @Override
    @Transactional
    public Map<String, Object> deleteArticle(Integer id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new NotFoundException("文章不存在");
        }
        String category = article.getCategory();
        articleMapper.deleteById(id);

        // 删除向量嵌入
        if (articleEmbeddingService != null) {
            try { articleEmbeddingService.removeArticle(id); } catch (Exception e) { log.warn("[RAG] 删除文章向量失败: {}", e.getMessage()); }
        }
        updateCategoryCount(category, null);
        return Map.of("message", "文章删除成功");
    }

    private void updateCategoryCount(String categoryName, Integer userId) {
        if (!StringUtils.hasText(categoryName)) return;
        long count = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getCategory, categoryName)
                        .eq(Article::getStatus, "published"));

        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .eq(Category::getName, categoryName);
        if (userId != null) {
            wrapper.eq(Category::getUserId, userId);
        }
        Category category = categoryMapper.selectOne(wrapper);
        if (category != null) {
            category.setArticleCount((int) count);
            categoryMapper.updateById(category);
        } else {
            category = new Category();
            category.setUserId(userId);
            category.setName(categoryName);
            category.setArticleCount((int) count);
            categoryMapper.insert(category);
        }
    }

    private ArticleSummary toArticleSummary(Article a) {
        return toArticleSummaryWithAuthor(a, null);
    }

    private ArticleSummary toArticleSummaryWithAuthor(Article a, String authorName) {
        ArticleSummary s = new ArticleSummary();
        s.setId(a.getId());
        s.setUserId(a.getUserId());
        s.setAuthorName(authorName);
        s.setTitle(a.getTitle());
        s.setStatus(a.getStatus());
        s.setIsPublic(a.getIsPublic());
        s.setDescription(a.getDescription());
        s.setCategory(a.getCategory());
        s.setTags(a.getTags());
        s.setCoverImage(a.getCoverImage());
        s.setViewCount(a.getViewCount());
        s.setCreatedAt(a.getCreatedAt());
        return s;
    }

    private ArticleDetail toArticleDetail(Article a) {
        ArticleDetail d = new ArticleDetail();
        d.setId(a.getId());
        d.setTitle(a.getTitle());
        d.setContent(a.getContent());
        d.setDescription(a.getDescription());
        d.setCategory(a.getCategory());
        d.setTags(a.getTags());
        d.setCoverImage(a.getCoverImage());
        d.setViewCount(a.getViewCount());
        d.setStatus(a.getStatus());
        d.setIsPublic(a.getIsPublic());
        d.setCreatedAt(a.getCreatedAt());
        d.setUpdatedAt(a.getUpdatedAt());
        return d;
    }

    @Override
    public ArticleListResponse getPublicArticles(int page, int limit, String category, String search, String tag) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, "published");
        wrapper.eq(Article::getIsPublic, true);

        if (StringUtils.hasText(category) && !"全部".equals(category)) {
            wrapper.eq(Article::getCategory, category);
        }
        if (StringUtils.hasText(search)) {
            wrapper.and(w -> w.like(Article::getTitle, search)
                    .or().like(Article::getDescription, search));
        }
        if (StringUtils.hasText(tag)) {
            wrapper.apply("JSON_CONTAINS(tags, JSON_ARRAY({0}))", tag);
        }

        wrapper.orderByDesc(Article::getCreatedAt);
        wrapper.select(Article::getId, Article::getUserId, Article::getTitle, Article::getStatus, Article::getIsPublic,
                Article::getDescription, Article::getCategory, Article::getTags,
                Article::getCoverImage, Article::getViewCount, Article::getCreatedAt);

        Page<Article> pageObj = new Page<>(page, limit);
        articleMapper.selectPage(pageObj, wrapper);

        Set<Integer> userIds = pageObj.getRecords().stream()
                .map(Article::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Integer, String> userNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> userNameMap.put(u.getId(), u.getNickname()));
        }

        List<ArticleSummary> summaries = pageObj.getRecords().stream()
                .map(a -> toArticleSummaryWithAuthor(a, userNameMap.getOrDefault(a.getUserId(), "")))
                .toList();

        PaginationInfo pagination = new PaginationInfo(
                (int) pageObj.getCurrent(),
                pageObj.getTotal(),
                (int) pageObj.getPages()
        );

        return new ArticleListResponse(summaries, pagination);
    }

    @Override
    public ArticleDetail getPublicArticleById(Integer id) {
        Article article = articleMapper.selectOne(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getId, id)
                        .eq(Article::getStatus, "published")
                        .eq(Article::getIsPublic, true));
        if (article == null) {
            throw new NotFoundException("文章不存在或非公开");
        }

        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, id)
                .setSql("view_count = view_count + 1"));

        article.setViewCount(article.getViewCount() + 1);
        return toArticleDetail(article);
    }

    @Override
    public BlogStatsResponse getPublicStats() {
        LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, "published")
                .eq(Article::getIsPublic, true);

        long totalArticles = articleMapper.selectCount(articleWrapper);

        List<Category> allCategories = categoryMapper.selectList(null);
        List<BlogStatsResponse.CategoryInfo> popularCategories = allCategories.stream()
                .map(c -> {
                    long count = articleMapper.selectCount(
                            new LambdaQueryWrapper<Article>()
                                    .eq(Article::getCategory, c.getName())
                                    .eq(Article::getStatus, "published")
                                    .eq(Article::getIsPublic, true));
                    return new BlogStatsResponse.CategoryInfo(c.getId(), c.getName(), (int) count);
                })
                .filter(c -> c.getArticleCount() > 0)
                .sorted((a, b) -> Integer.compare(b.getArticleCount(), a.getArticleCount()))
                .limit(5)
                .toList();

        long totalCategories = allCategories.stream()
                .filter(c -> articleMapper.selectCount(
                        new LambdaQueryWrapper<Article>()
                                .eq(Article::getCategory, c.getName())
                                .eq(Article::getStatus, "published")
                                .eq(Article::getIsPublic, true)) > 0)
                .count();

        long totalViews = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "published")
                        .eq(Article::getIsPublic, true)
                        .select(Article::getViewCount)
        ).stream().mapToLong(a -> a.getViewCount() != null ? a.getViewCount() : 0).sum();

        LambdaQueryWrapper<Article> popularWrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, "published")
                .eq(Article::getIsPublic, true)
                .orderByDesc(Article::getCreatedAt).last("LIMIT 4")
                .select(Article::getId, Article::getUserId, Article::getTitle, Article::getDescription,
                        Article::getCreatedAt, Article::getCategory, Article::getCoverImage, Article::getTags);
        List<ArticleSummary> popularArticles = articleMapper.selectList(popularWrapper).stream()
                .map(this::toArticleSummary).toList();

        BlogStatsResponse.BlogStatsData data = new BlogStatsResponse.BlogStatsData(
                totalArticles, totalCategories, totalViews, popularArticles, popularCategories);
        return new BlogStatsResponse(data);
    }
}
