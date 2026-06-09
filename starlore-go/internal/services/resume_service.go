package services

import (
	"net/http"
	"time"

	"starlore-go/internal/database"
	"starlore-go/internal/models"
	"starlore-go/internal/utils"

	"github.com/gin-gonic/gin"
)

// ListResumes 简历列表
func ListResumes(c *gin.Context) {
	uid := utils.GetUserID(c)
	var resumes []models.Resume
	database.DB.Where("user_id = ? AND status = ?", uid, "active").Order("updated_at DESC").Find(&resumes)
	c.JSON(http.StatusOK, gin.H{"data": resumes})
}

// GetResume 获取简历
func GetResume(c *gin.Context) {
	uid := utils.GetUserID(c)
	id := utils.ParseInt(c.Param("id"), 0)

	var resume models.Resume
	if err := database.DB.Where("id = ? AND user_id = ?", id, uid).First(&resume).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "简历不存在")
		return
	}
	c.JSON(http.StatusOK, gin.H{"data": resume})
}

// CreateResume 创建简历
func CreateResume(c *gin.Context) {
	uid := utils.GetUserID(c)
	var resume models.Resume
	if err := c.ShouldBindJSON(&resume); err != nil {
		utils.Fail(c, http.StatusBadRequest, "请求参数错误")
		return
	}

	now := time.Now()
	resume.UserID = uid
	resume.Status = "active"
	resume.CreatedAt = &now
	resume.UpdatedAt = &now
	database.DB.Create(&resume)
	utils.Ok(c, "简历创建成功", gin.H{"id": resume.ID})
}

// UpdateResume 更新简历
func UpdateResume(c *gin.Context) {
	uid := utils.GetUserID(c)
	var resume models.Resume
	if err := c.ShouldBindJSON(&resume); err != nil {
		utils.Fail(c, http.StatusBadRequest, "请求参数错误")
		return
	}

	var existing models.Resume
	if err := database.DB.Where("id = ? AND user_id = ?", resume.ID, uid).First(&existing).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "简历不存在")
		return
	}

	now := time.Now()
	database.DB.Model(&existing).Updates(map[string]interface{}{
		"title":      resume.Title,
		"template":   resume.Template,
		"name":       resume.Name,
		"job_title":  resume.JobTitle,
		"phone":      resume.Phone,
		"email":      resume.Email,
		"photo_url":  resume.PhotoURL,
		"content":    resume.Content,
		"updated_at": &now,
	})
	utils.Ok(c, "简历更新成功", gin.H{"id": existing.ID})
}

// DeleteResume 删除简历（软删除）
func DeleteResume(c *gin.Context) {
	uid := utils.GetUserID(c)
	id := utils.ParseInt(c.Param("id"), 0)

	var resume models.Resume
	if err := database.DB.Where("id = ? AND user_id = ?", id, uid).First(&resume).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "简历不存在")
		return
	}
	now := time.Now()
	database.DB.Model(&resume).Updates(map[string]interface{}{"status": "deleted", "updated_at": &now})
	utils.Ok(c, "简历删除成功", nil)
}
