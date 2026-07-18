package com.robin.blogback.service;

import com.robin.blogback.dto.*;

import java.util.Map;

public interface CategoryService {
    CategoryListResponse getAllCategories(Integer userId);
    CategoryDetailResponse getCategoryById(Integer userId, Integer id);
    CategoryListResponse.CategoryItem createCategory(Integer userId, CreateCategoryRequest request);
    CategoryListResponse.CategoryItem updateCategory(Integer userId, boolean isAdmin, Integer id, UpdateCategoryRequest request);
    Map<String, Object> deleteCategory(Integer userId, boolean isAdmin, Integer id);

    CategoryListResponse getPublicCategories();
    CategoryDetailResponse getPublicCategoryById(Integer id);
}
