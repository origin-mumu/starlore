package services

import (
	"net/http"
	"time"

	"starlore-go/internal/database"
	"starlore-go/internal/models"
	"starlore-go/internal/utils"

	"github.com/gin-gonic/gin"
)

// AdminListUsers 管理员获取用户列表
func AdminListUsers(c *gin.Context) {
	var users []models.User
	database.DB.Order("createdAt DESC").Find(&users)

	data := make([]gin.H, len(users))
	for i, u := range users {
		data[i] = gin.H{
			"id":           u.ID,
			"username":     u.Username,
			"nickname":     u.Nickname.String,
			"email":        u.Email.String,
			"avatar":       u.Avatar.String,
			"bio":          u.Bio.String,
			"location":     u.Location.String,
			"website":      u.Website.String,
			"github":       u.Github.String,
			"role":         u.Role,
			"aiDailyLimit": u.AIDailyLimit,
			"aiTodayCount": u.AITodayCount,
			"createdAt":    u.CreatedAt,
			"updatedAt":    u.UpdatedAt,
		}
	}
	c.JSON(http.StatusOK, gin.H{"data": data})
}

// AdminGetUser 管理员获取单个用户
func AdminGetUser(c *gin.Context) {
	id := utils.ParseInt(c.Param("id"), 0)
	var user models.User
	if err := database.DB.First(&user, id).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "用户不存在")
		return
	}
	c.JSON(http.StatusOK, gin.H{"data": toUserInfo(user)})
}

// AdminUpdateUser 管理员更新用户
func AdminUpdateUser(c *gin.Context) {
	id := utils.ParseInt(c.Param("id"), 0)
	var user models.User
	if err := database.DB.First(&user, id).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "用户不存在")
		return
	}

	var body map[string]interface{}
	if err := c.ShouldBindJSON(&body); err != nil {
		utils.Fail(c, http.StatusBadRequest, "请求参数错误")
		return
	}

	updates := map[string]interface{}{}
	if v, ok := body["nickname"]; ok {
		updates["nickname"] = v
	}
	if v, ok := body["email"]; ok {
		updates["email"] = v
	}
	if v, ok := body["avatar"]; ok {
		updates["avatar"] = v
	}
	if v, ok := body["bio"]; ok {
		updates["bio"] = v
	}
	if v, ok := body["role"]; ok {
		updates["role"] = v
	}
	if v, ok := body["ai_daily_limit"]; ok {
		updates["ai_daily_limit"] = v
	}

	now := time.Now()
	updates["updatedAt"] = &now
	database.DB.Model(&user).Updates(updates)
	utils.Ok(c, "用户更新成功", nil)
}

// AdminDeleteUser 管理员删除用户
func AdminDeleteUser(c *gin.Context) {
	id := utils.ParseInt(c.Param("id"), 0)
	var user models.User
	if err := database.DB.First(&user, id).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "用户不存在")
		return
	}
	database.DB.Delete(&user)
	utils.Ok(c, "用户删除成功", nil)
}

// AdminLoginLogs 管理员获取登录日志
func AdminLoginLogs(c *gin.Context) {
	page := utils.ParseInt(c.Query("page"), 1)
	limit := utils.ParseInt(c.Query("limit"), 20)
	username := c.Query("username")

	query := database.DB.Model(&models.LoginLog{})
	if username != "" {
		query = query.Where("username LIKE ?", "%"+username+"%")
	}

	var total int64
	query.Count(&total)
	pages := (total + int64(limit) - 1) / int64(limit)

	var logs []models.LoginLog
	query.Order("login_time DESC").Offset((page - 1) * limit).Limit(limit).Find(&logs)

	data := make([]gin.H, len(logs))
	for i, logEntry := range logs {
		data[i] = gin.H{
			"id":        logEntry.ID,
			"userId":    logEntry.UserID,
			"username":  logEntry.Username,
			"ip":        logEntry.IP,
			"country":   logEntry.Country,
			"province":  logEntry.Province,
			"city":      logEntry.City,
			"userAgent": logEntry.UserAgent,
			"loginTime": logEntry.LoginTime,
		}
	}

	c.JSON(http.StatusOK, gin.H{
		"data": data,
		"pagination": gin.H{
			"current": page,
			"total":   total,
			"pages":   pages,
		},
	})
}
