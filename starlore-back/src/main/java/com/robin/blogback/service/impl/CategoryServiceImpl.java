package com.robin.blogback.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robin.blogback.dto.*;
import com.robin.blogback.entity.Article;
import com.robin.blogback.entity.Category;
import com.robin.blogback.exception.BadRequestException;
import com.robin.blogback.exception.ConflictException;
import com.robin.blogback.exception.NotFoundException;
import com.robin.blogback.exception.ForbiddenException;
import com.robin.blogback.mapper.ArticleMapper;
import com.robin.blogback.mapper.CategoryMapper;
import com.robin.blogback.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public CategoryListResponse getAllCategories(Integer userId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .orderByDesc(Category::getArticleCount);
        if (userId != null) {
            wrapper.eq(Category::getUserId, userId);
        }
        List<Category> categories = categoryMapper.selectList(wrapper);
        List<CategoryListResponse.CategoryItem> items = categories.stream()
                .map(this::toCategoryItem)
                .toList();
        return new CategoryListResponse(items);
    }

    @Override
    public CategoryDetailResponse getCategoryById(Integer userId, Integer id) {
        Category category = categoryMapper.selectOne(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getId, id)
                        .eq(Category::getUserId, userId));
        if (category == null) {
            throw new NotFoundException("分类不存在");
        }

        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getUserId, userId)
                        .eq(Article::getCategory, category.getName())
                        .eq(Article::getStatus, "published")
                        .orderByDesc(Article::getCreatedAt)
                        .select(Article::getId, Article::getTitle, Article::getDescription,
                                Article::getCoverImage, Article::getViewCount, Article::getCreatedAt));

        List<ArticleSummary> articleSummaries = articles.stream().map(a -> {
            ArticleSummary s = new ArticleSummary();
            s.setId(a.getId());
            s.setTitle(a.getTitle());
            s.setDescription(a.getDescription());
            s.setCoverImage(a.getCoverImage());
            s.setViewCount(a.getViewCount());
            s.setCreatedAt(a.getCreatedAt());
            return s;
        }).toList();

        CategoryDetailResponse.CategoryDetailData data = new CategoryDetailResponse.CategoryDetailData(
                category.getId(), category.getName(), category.getDescription(),
                category.getColor(), category.getArticleCount(),
                category.getCreatedAt(), category.getUpdatedAt(), articleSummaries);
        return new CategoryDetailResponse(data);
    }

    @Override
    @Transactional
    public CategoryListResponse.CategoryItem createCategory(Integer userId, CreateCategoryRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BadRequestException("分类名称不能为空");
        }
        Long count = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getUserId, userId)
                        .eq(Category::getName, request.getName()));
        if (count > 0) {
            throw new ConflictException("分类名称已存在");
        }

        Category category = new Category();
        category.setUserId(userId);
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setColor(request.getColor());
        category.setArticleCount(0);
        LocalDateTime now = LocalDateTime.now();
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        categoryMapper.insert(category);
        return toCategoryItem(category);
    }

    @Override
    @Transactional
    public CategoryListResponse.CategoryItem updateCategory(Integer userId, boolean isAdmin, Integer id, UpdateCategoryRequest request) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new NotFoundException("分类不存在");
        }
        if (!isAdmin && !Objects.equals(category.getUserId(), userId)) {
            throw new ForbiddenException("无权修改其他用户的分类");
        }

        if (StringUtils.hasText(request.getName()) && !request.getName().equals(category.getName())) {
            Long count = categoryMapper.selectCount(
                    new LambdaQueryWrapper<Category>().eq(Category::getName, request.getName()));
            if (count > 0) {
                throw new ConflictException("分类名称已存在");
            }
            // Update article category names
            String oldName = category.getName();
            List<Article> articles = articleMapper.selectList(
                    new LambdaQueryWrapper<Article>().eq(Article::getCategory, oldName));
            for (Article a : articles) {
                a.setCategory(request.getName());
                articleMapper.updateById(a);
            }
            category.setName(request.getName());
        }

        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getColor() != null) category.setColor(request.getColor());
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
        return toCategoryItem(category);
    }

    @Override
    @Transactional
    public Map<String, Object> deleteCategory(Integer userId, boolean isAdmin, Integer id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new NotFoundException("分类不存在");
        }
        if (!isAdmin && !Objects.equals(category.getUserId(), userId)) {
            throw new ForbiddenException("无权删除其他用户的分类");
        }
        Long articleCount = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getCategory, category.getName()));
        if (articleCount > 0) {
            throw new BadRequestException("该分类下有文章，无法删除");
        }
        categoryMapper.deleteById(id);
        return Map.of("message", "分类删除成功");
    }

    private CategoryListResponse.CategoryItem toCategoryItem(Category c) {
        return new CategoryListResponse.CategoryItem(
                c.getId(), c.getUserId(), c.getName(), c.getDescription(),
                c.getColor(), c.getArticleCount(), c.getCreatedAt(), c.getUpdatedAt());
    }

    @Override
    public CategoryListResponse getPublicCategories() {
        List<Category> allCategories = categoryMapper.selectList(null);
        List<CategoryListResponse.CategoryItem> items = allCategories.stream()
                .map(c -> {
                    long publicCount = articleMapper.selectCount(
                            new LambdaQueryWrapper<Article>()
                                    .eq(Article::getCategory, c.getName())
                                    .eq(Article::getStatus, "published")
                                    .eq(Article::getIsPublic, true));
                    CategoryListResponse.CategoryItem item = toCategoryItem(c);
                    item.setArticleCount((int) publicCount);
                    return item;
                })
                .filter(item -> item.getArticleCount() > 0)
                .sorted((a, b) -> Integer.compare(b.getArticleCount(), a.getArticleCount()))
                .toList();
        return new CategoryListResponse(items);
    }

    @Override
    public CategoryDetailResponse getPublicCategoryById(Integer id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new NotFoundException("分类不存在");
        }

        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getCategory, category.getName())
                        .eq(Article::getStatus, "published")
                        .eq(Article::getIsPublic, true)
                        .orderByDesc(Article::getCreatedAt)
                        .select(Article::getId, Article::getTitle, Article::getDescription,
                                Article::getCoverImage, Article::getViewCount, Article::getCreatedAt, Article::getIsPublic));

        List<ArticleSummary> articleSummaries = articles.stream().map(a -> {
            ArticleSummary s = new ArticleSummary();
            s.setId(a.getId());
            s.setTitle(a.getTitle());
            s.setDescription(a.getDescription());
            s.setCoverImage(a.getCoverImage());
            s.setViewCount(a.getViewCount());
            s.setCreatedAt(a.getCreatedAt());
            s.setIsPublic(a.getIsPublic());
            return s;
        }).toList();

        CategoryDetailResponse.CategoryDetailData data = new CategoryDetailResponse.CategoryDetailData(
                category.getId(), category.getName(), category.getDescription(),
                category.getColor(), articleSummaries.size(),
                category.getCreatedAt(), category.getUpdatedAt(), articleSummaries);
        return new CategoryDetailResponse(data);
    }
}
