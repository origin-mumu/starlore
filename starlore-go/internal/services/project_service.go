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

func ListProjects(c *gin.Context) {
	uid := utils.GetUserID(c)
	var projects []models.Project
	database.DB.Where("user_id = ?", uid).Order("sort_order ASC, created_at DESC").Find(&projects)

	items := make([]schemas.ProjectInfo, len(projects))
	for i, p := range projects {
		items[i] = schemas.ProjectInfo{
			ID:          p.ID,
			Name:        p.Name,
			Description: p.Description,
			URL:         p.URL,
			Image:       p.Image,
			SortOrder:   p.SortOrder,
			CreatedAt:   p.CreatedAt,
			UpdatedAt:   p.UpdatedAt,
		}
	}
	c.JSON(http.StatusOK, gin.H{"data": items})
}

func CreateProject(c *gin.Context, req schemas.ProjectRequest) {
	uid := utils.GetUserID(c)
	now := time.Now()
	project := models.Project{
		UserID:    uid,
		CreatedAt: &now,
		UpdatedAt: &now,
	}
	if req.Name != nil {
		project.Name = *req.Name
	}
	if req.Description != nil {
		project.Description = *req.Description
	}
	if req.URL != nil {
		project.URL = *req.URL
	}
	if req.Image != nil {
		project.Image = *req.Image
	}
	if req.SortOrder != nil {
		project.SortOrder = *req.SortOrder
	}
	database.DB.Create(&project)
	utils.Ok(c, "项目创建成功", gin.H{"id": project.ID})
}

func UpdateProject(c *gin.Context, req schemas.ProjectRequest) {
	uid := utils.GetUserID(c)
	projectID := utils.ParseInt(c.Param("id"), 0)

	var project models.Project
	if err := database.DB.First(&project, projectID).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "项目不存在")
		return
	}
	if project.UserID != uid {
		utils.Fail(c, http.StatusUnauthorized, "无权操作该项目")
		return
	}

	updates := map[string]interface{}{}
	if req.Name != nil {
		updates["name"] = *req.Name
	}
	if req.Description != nil {
		updates["description"] = *req.Description
	}
	if req.URL != nil {
		updates["url"] = *req.URL
	}
	if req.Image != nil {
		updates["image"] = *req.Image
	}
	if req.SortOrder != nil {
		updates["sort_order"] = *req.SortOrder
	}
	now := time.Now()
	updates["updated_at"] = &now
	database.DB.Model(&project).Updates(updates)
	utils.Ok(c, "项目更新成功", nil)
}

func DeleteProject(c *gin.Context) {
	uid := utils.GetUserID(c)
	projectID := utils.ParseInt(c.Param("id"), 0)

	var project models.Project
	if err := database.DB.First(&project, projectID).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "项目不存在")
		return
	}
	if project.UserID != uid {
		utils.Fail(c, http.StatusUnauthorized, "无权操作该项目")
		return
	}
	database.DB.Delete(&project)
	utils.Ok(c, "项目删除成功", nil)
}
