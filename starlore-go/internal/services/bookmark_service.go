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

// ListBookmarks 收藏列表
func ListBookmarks(c *gin.Context) {
	uid := utils.GetUserID(c)

	var bookmarks []models.Bookmark
	database.DB.Where("user_id = ?", uid).Order("created_at DESC").Find(&bookmarks)

	if len(bookmarks) == 0 {
		c.JSON(http.StatusOK, gin.H{"data": []schemas.ArticleSummary{}})
		return
	}

	// 批量查询文章（避免 N+1）
	articleIDs := make([]int, len(bookmarks))
	for i, bm := range bookmarks {
		articleIDs[i] = bm.ArticleID
	}

	var articles []models.Article
	database.DB.Where("id IN ?", articleIDs).Find(&articles)

	articleMap := make(map[int]models.Article)
	for _, a := range articles {
		articleMap[a.ID] = a
	}

	items := make([]schemas.ArticleSummary, 0, len(bookmarks))
	for _, bm := range bookmarks {
		if article, ok := articleMap[bm.ArticleID]; ok {
			items = append(items, toArticleSummary(article, ""))
		}
	}

	c.JSON(http.StatusOK, gin.H{"data": items})
}

// CheckBookmark 检查是否已收藏
func CheckBookmark(c *gin.Context) {
	uid := utils.GetUserID(c)
	articleID := utils.ParseInt(c.Param("articleId"), 0)

	var count int64
	database.DB.Model(&models.Bookmark{}).Where("user_id = ? AND article_id = ?", uid, articleID).Count(&count)
	c.JSON(http.StatusOK, gin.H{"bookmarked": count > 0})
}

// AddBookmark 添加收藏
func AddBookmark(c *gin.Context) {
	uid := utils.GetUserID(c)
	var req struct {
		ArticleID int `json:"articleId" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		utils.Fail(c, http.StatusBadRequest, "请求参数错误")
		return
	}

	var article models.Article
	if database.DB.First(&article, req.ArticleID).Error != nil {
		utils.Fail(c, http.StatusNotFound, "文章不存在")
		return
	}

	var count int64
	database.DB.Model(&models.Bookmark{}).Where("user_id = ? AND article_id = ?", uid, req.ArticleID).Count(&count)
	if count > 0 {
		utils.Ok(c, "已收藏", nil)
		return
	}

	now := time.Now()
	database.DB.Create(&models.Bookmark{UserID: uid, ArticleID: req.ArticleID, CreatedAt: &now})
	utils.Ok(c, "收藏成功", nil)
}

// RemoveBookmark 取消收藏
func RemoveBookmark(c *gin.Context) {
	uid := utils.GetUserID(c)
	articleID := utils.ParseInt(c.Param("articleId"), 0)

	result := database.DB.Where("user_id = ? AND article_id = ?", uid, articleID).Delete(&models.Bookmark{})
	if result.RowsAffected == 0 {
		utils.Fail(c, http.StatusNotFound, "收藏不存在")
		return
	}
	utils.Ok(c, "取消收藏成功", nil)
}
