package services

import (
	"encoding/json"
	"math"
	"net/http"
	"time"

	"starlore-go/internal/database"
	"starlore-go/internal/models"
	"starlore-go/internal/schemas"
	"starlore-go/internal/utils"

	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

// ListArticles 文章列表
func ListArticles(c *gin.Context) {
	page := utils.ParseInt(c.Query("page"), 1)
	limit := utils.ParseInt(c.Query("limit"), 10)
	category := c.Query("category")
	search := c.Query("search")
	tag := c.Query("tag")
	uid, _ := c.Get("userId")

	query := database.DB.Model(&models.Article{})

	if uid != nil {
		query = query.Where("user_id = ? AND status = ?", uid, "published")
	}

	if category != "" && category != "全部" {
		query = query.Where("category = ?", category)
	}
	if search != "" {
		query = query.Where("title LIKE ? OR description LIKE ?", "%"+search+"%", "%"+search+"%")
	}
	if tag != "" {
		query = query.Where("JSON_CONTAINS(tags, JSON_ARRAY(?))", tag)
	}

	var total int64
	query.Count(&total)
	pages := int(math.Ceil(float64(total) / float64(limit)))

	var articles []models.Article
	query.Order("createdAt DESC").Offset((page - 1) * limit).Limit(limit).Find(&articles)

	// 批量获取作者名（避免 N+1）
	userIDs := make(map[int]bool)
	for _, a := range articles {
		userIDs[a.UserID] = true
	}
	authorMap := make(map[int]string)
	if len(userIDs) > 0 {
		ids := make([]int, 0, len(userIDs))
		for id := range userIDs {
			ids = append(ids, id)
		}
		var users []models.User
		database.DB.Where("id IN ?", ids).Find(&users)
		for _, u := range users {
			name := u.Nickname.String
			if name == "" {
				name = u.Username
			}
			authorMap[u.ID] = name
		}
	}

	summaries := make([]schemas.ArticleSummary, len(articles))
	for i, a := range articles {
		summaries[i] = toArticleSummary(a, authorMap[a.UserID])
	}

	c.JSON(http.StatusOK, schemas.ArticleListResponse{
		Data: summaries,
		Pagination: schemas.PaginationInfo{
			Current: page,
			Total:   total,
			Pages:   pages,
		},
	})
}

// GetArticle 获取文章详情
func GetArticle(c *gin.Context) {
	id := utils.ParseInt(c.Param("id"), 0)
	uid, _ := c.Get("userId")

	var article models.Article
	query := database.DB.Where("id = ?", id)
	if uid != nil {
		query = query.Where("user_id = ?", uid)
	}
	if err := query.First(&article).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "文章不存在")
		return
	}

	// 使用 SQL 表达式避免竞态条件
	database.DB.Model(&article).UpdateColumn("view_count", gorm.Expr("view_count + 1"))
	article.ViewCount++

	c.JSON(http.StatusOK, toArticleDetail(article))
}

// CreateArticle 创建文章
func CreateArticle(c *gin.Context, req schemas.CreateArticleRequest) {
	uid := utils.GetUserID(c)
	if uid == 0 {
		utils.Fail(c, http.StatusUnauthorized, "未登录")
		return
	}

	now := time.Now()
	tagsJSON := "[]"
	if req.Tags != nil {
		b, err := json.Marshal(req.Tags)
		if err == nil {
			tagsJSON = string(b)
		}
	}

	article := models.Article{
		UserID:      uid,
		Title:       req.Title,
		Content:     req.Content,
		Description: req.Description,
		Category:    req.Category,
		Tags:        tagsJSON,
		CoverImage:  req.CoverImage,
		ViewCount:   0,
		Status:      "published",
		CreatedAt:   &now,
		UpdatedAt:   &now,
	}
	if article.Category == "" {
		article.Category = "随笔"
	}
	if req.Status != "" {
		article.Status = req.Status
	}

	database.DB.Create(&article)
	updateCategoryCount(article.Category, uid)

	utils.Ok(c, "文章创建成功", gin.H{"id": article.ID})
}

// UpdateArticle 更新文章
func UpdateArticle(c *gin.Context, req schemas.UpdateArticleRequest) {
	uid := utils.GetUserID(c)
	id := utils.ParseInt(c.Param("id"), 0)

	var article models.Article
	if err := database.DB.Where("id = ? AND user_id = ?", id, uid).First(&article).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "文章不存在")
		return
	}

	oldCategory := article.Category
	updates := map[string]interface{}{}
	if req.Title != nil {
		updates["title"] = *req.Title
	}
	if req.Content != nil {
		updates["content"] = *req.Content
	}
	if req.Description != nil {
		updates["description"] = *req.Description
	}
	if req.Category != nil {
		updates["category"] = *req.Category
	}
	if req.Tags != nil {
		tagsJSON, _ := json.Marshal(req.Tags)
		updates["tags"] = string(tagsJSON)
	}
	if req.CoverImage != nil {
		updates["cover_image"] = *req.CoverImage
	}
	if req.Status != nil {
		updates["status"] = *req.Status
	}
	now := time.Now()
	updates["updatedAt"] = &now

	database.DB.Model(&article).Updates(updates)

	if req.Category != nil && *req.Category != oldCategory {
		updateCategoryCount(oldCategory, article.UserID)
		updateCategoryCount(*req.Category, article.UserID)
	}

	utils.Ok(c, "文章更新成功", nil)
}

// DeleteArticle 删除文章
func DeleteArticle(c *gin.Context) {
	uid := utils.GetUserID(c)
	id := utils.ParseInt(c.Param("id"), 0)

	var article models.Article
	if err := database.DB.Where("id = ? AND user_id = ?", id, uid).First(&article).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "文章不存在")
		return
	}

	database.DB.Delete(&article)
	updateCategoryCount(article.Category, article.UserID)

	utils.Ok(c, "文章删除成功", nil)
}

// GetBlogStats 获取博客统计
func GetBlogStats(c *gin.Context) {
	uid := utils.GetUserID(c)

	var totalArticles int64
	database.DB.Model(&models.Article{}).Where("user_id = ? AND status = ?", uid, "published").Count(&totalArticles)

	var totalCategories int64
	database.DB.Model(&models.Category{}).Where("user_id = ?", uid).Count(&totalCategories)

	var totalViews int64
	database.DB.Model(&models.Article{}).Where("user_id = ? AND status = ?", uid, "published").
		Select("COALESCE(SUM(view_count), 0)").Scan(&totalViews)

	var popularArticles []models.Article
	database.DB.Where("user_id = ? AND status = ?", uid, "published").
		Order("createdAt DESC").Limit(4).Find(&popularArticles)

	articleSummaries := make([]schemas.ArticleSummary, len(popularArticles))
	for i, a := range popularArticles {
		articleSummaries[i] = toArticleSummary(a, "")
	}

	var popularCategories []schemas.CategoryInfo
	database.DB.Model(&models.Category{}).Where("user_id = ?", uid).
		Order("article_count DESC").Limit(5).
		Select("id, name, article_count").Scan(&popularCategories)

	c.JSON(http.StatusOK, gin.H{
		"data": schemas.BlogStatsData{
			TotalArticles:     totalArticles,
			TotalCategories:   totalCategories,
			TotalViews:        totalViews,
			PopularArticles:   articleSummaries,
			PopularCategories: popularCategories,
		},
	})
}

// GetDailyStats 获取每日统计
func GetDailyStats(c *gin.Context) {
	uid := utils.GetUserID(c)

	type dailyRow struct {
		Date  string
		Count int
	}
	var rows []dailyRow

	startDate := time.Now().AddDate(0, 0, -6).Format("2006-01-02")
	database.DB.Model(&models.Article{}).
		Where("user_id = ? AND DATE(createdAt) >= ?", uid, startDate).
		Select("DATE(createdAt) as date, COUNT(*) as count").
		Group("DATE(createdAt)").Scan(&rows)

	dateMap := make(map[string]int)
	for _, r := range rows {
		dateMap[r.Date] = r.Count
	}

	data := make([]schemas.DailyItem, 7)
	for i := 0; i < 7; i++ {
		d := time.Now().AddDate(0, 0, -6+i)
		ds := d.Format("2006-01-02")
		data[i] = schemas.DailyItem{Date: ds, Count: dateMap[ds]}
	}

	c.JSON(http.StatusOK, gin.H{"data": data})
}

func updateCategoryCount(categoryName string, userID int) {
	if categoryName == "" {
		return
	}
	var count int64
	database.DB.Model(&models.Article{}).
		Where("category = ? AND user_id = ? AND status = ?", categoryName, userID, "published").
		Count(&count)

	var category models.Category
	if database.DB.Where("name = ? AND user_id = ?", categoryName, userID).First(&category).Error == nil {
		database.DB.Model(&category).Update("article_count", count)
	} else {
		now := time.Now()
		database.DB.Create(&models.Category{
			UserID:       userID,
			Name:         categoryName,
			ArticleCount: int(count),
			CreatedAt:    &now,
			UpdatedAt:    &now,
		})
	}
}

func toArticleSummary(a models.Article, authorName string) schemas.ArticleSummary {
	return schemas.ArticleSummary{
		ID:          a.ID,
		UserID:      a.UserID,
		AuthorName:  authorName,
		Title:       a.Title,
		Status:      a.Status,
		Description: a.Description,
		Category:    a.Category,
		Tags:        utils.ParseTags(a.Tags),
		CoverImage:  a.CoverImage,
		ViewCount:   a.ViewCount,
		CreatedAt:   a.CreatedAt,
	}
}

func toArticleDetail(a models.Article) schemas.ArticleDetail {
	return schemas.ArticleDetail{
		ID:          a.ID,
		Title:       a.Title,
		Content:     a.Content,
		Description: a.Description,
		Category:    a.Category,
		Tags:        utils.ParseTags(a.Tags),
		CoverImage:  a.CoverImage,
		ViewCount:   a.ViewCount,
		Status:      a.Status,
		CreatedAt:   a.CreatedAt,
		UpdatedAt:   a.UpdatedAt,
	}
}
