package services

import (
	"net/http"
	"time"

	"starlore-go/internal/database"
	"starlore-go/internal/models"
	"starlore-go/internal/schemas"
	"starlore-go/internal/utils"

	"github.com/gin-gonic/gin"
)

// ListCategories 分类列表
func ListCategories(c *gin.Context) {
	uid := utils.GetUserID(c)
	var categories []models.Category
	database.DB.Where("user_id = ?", uid).Order("article_count DESC").Find(&categories)

	items := make([]schemas.CategoryItem, len(categories))
	for i, cat := range categories {
		items[i] = toCategoryItem(cat)
	}
	c.JSON(http.StatusOK, schemas.CategoryListResponse{Data: items})
}

// GetCategory 获取分类详情
func GetCategory(c *gin.Context) {
	uid := utils.GetUserID(c)
	id := utils.ParseInt(c.Param("id"), 0)

	var category models.Category
	if err := database.DB.Where("id = ? AND user_id = ?", id, uid).First(&category).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "分类不存在")
		return
	}

	var articles []models.Article
	database.DB.Where("category = ? AND user_id = ? AND status = ?", category.Name, uid, "published").
		Order("createdAt DESC").Find(&articles)

	summaries := make([]schemas.ArticleSummary, len(articles))
	for i, a := range articles {
		summaries[i] = toArticleSummary(a, "")
	}

	c.JSON(http.StatusOK, gin.H{
		"data": gin.H{
			"id":            category.ID,
			"name":          category.Name,
			"description":   category.Description,
			"color":         category.Color,
			"article_count": category.ArticleCount,
			"createdAt":     category.CreatedAt,
			"updatedAt":     category.UpdatedAt,
			"articles":      summaries,
		},
	})
}

// CreateCategory 创建分类
func CreateCategory(c *gin.Context, req schemas.CreateCategoryRequest) {
	uid := utils.GetUserID(c)
	if uid == 0 {
		utils.Fail(c, http.StatusUnauthorized, "未登录")
		return
	}
	if req.Name == "" {
		utils.Fail(c, http.StatusBadRequest, "分类名不能为空")
		return
	}

	var count int64
	database.DB.Model(&models.Category{}).Where("name = ? AND user_id = ?", req.Name, uid).Count(&count)
	if count > 0 {
		utils.Fail(c, http.StatusConflict, "分类名已存在")
		return
	}

	now := time.Now()
	category := models.Category{
		UserID:       uid,
		Name:         req.Name,
		Description:  req.Description,
		Color:        req.Color,
		ArticleCount: 0,
		CreatedAt:    &now,
		UpdatedAt:    &now,
	}
	database.DB.Create(&category)
	utils.Ok(c, "分类创建成功", gin.H{"id": category.ID})
}

// UpdateCategory 更新分类
func UpdateCategory(c *gin.Context, req schemas.UpdateCategoryRequest) {
	uid := utils.GetUserID(c)
	id := utils.ParseInt(c.Param("id"), 0)

	var category models.Category
	if err := database.DB.Where("id = ? AND user_id = ?", id, uid).First(&category).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "分类不存在")
		return
	}

	oldName := category.Name
	if req.Name != nil && *req.Name != oldName {
		var count int64
		database.DB.Model(&models.Category{}).Where("name = ? AND user_id = ?", *req.Name, uid).Count(&count)
		if count > 0 {
			utils.Fail(c, http.StatusConflict, "分类名已存在")
			return
		}
		database.DB.Model(&models.Article{}).Where("category = ? AND user_id = ?", oldName, uid).Update("category", *req.Name)
		category.Name = *req.Name
	}

	if req.Description != nil {
		category.Description = *req.Description
	}
	if req.Color != nil {
		category.Color = *req.Color
	}
	now := time.Now()
	category.UpdatedAt = &now
	database.DB.Save(&category)

	utils.Ok(c, "分类更新成功", nil)
}

// DeleteCategory 删除分类
func DeleteCategory(c *gin.Context) {
	uid := utils.GetUserID(c)
	id := utils.ParseInt(c.Param("id"), 0)

	var category models.Category
	if err := database.DB.Where("id = ? AND user_id = ?", id, uid).First(&category).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "分类不存在")
		return
	}

	var count int64
	database.DB.Model(&models.Article{}).Where("category = ?", category.Name).Count(&count)
	if count > 0 {
		utils.Fail(c, http.StatusBadRequest, "该分类下有文章，无法删除")
		return
	}

	database.DB.Delete(&category)
	utils.Ok(c, "分类删除成功", nil)
}

func toCategoryItem(cat models.Category) schemas.CategoryItem {
	return schemas.CategoryItem{
		ID:           cat.ID,
		UserID:       cat.UserID,
		Name:         cat.Name,
		Description:  cat.Description,
		Color:        cat.Color,
		ArticleCount: cat.ArticleCount,
		CreatedAt:    cat.CreatedAt,
		UpdatedAt:    cat.UpdatedAt,
	}
}
