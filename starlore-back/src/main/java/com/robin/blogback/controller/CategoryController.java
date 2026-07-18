package com.robin.blogback.controller;

import com.robin.blogback.dto.*;
import com.robin.blogback.entity.User;
import com.robin.blogback.mapper.UserMapper;
import com.robin.blogback.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserMapper userMapper;

    private boolean isAdmin(Integer userId) {
        if (userId == null) return false;
        User user = userMapper.selectById(userId);
        return user != null && "admin".equals(user.getRole());
    }

    @GetMapping
    public ResponseEntity<?> getAllCategories(HttpServletRequest httpRequest,
            @RequestParam(required = false) Integer authorUserId) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        boolean admin = isAdmin(userId);

        CategoryListResponse response;
        if (admin && authorUserId == null) {
            response = categoryService.getAllCategories(null);
        } else if (admin && authorUserId != null) {
            response = categoryService.getAllCategories(authorUserId);
        } else {
            response = categoryService.getAllCategories(userId);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(HttpServletRequest httpRequest, @PathVariable Integer id) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        CategoryDetailResponse response = categoryService.getCategoryById(userId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> createCategory(HttpServletRequest httpRequest, @Valid @RequestBody CreateCategoryRequest request) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        CategoryListResponse.CategoryItem item = categoryService.createCategory(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "分类创建成功", "data", item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(HttpServletRequest httpRequest, @PathVariable Integer id,
                                             @Valid @RequestBody UpdateCategoryRequest request) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        CategoryListResponse.CategoryItem item = categoryService.updateCategory(userId, isAdmin(userId), id, request);
        return ResponseEntity.ok(Map.of("message", "分类更新成功", "data", item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(HttpServletRequest httpRequest, @PathVariable Integer id) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        Map<String, Object> result = categoryService.deleteCategory(userId, isAdmin(userId), id);
        return ResponseEntity.ok(result);
    }
}
