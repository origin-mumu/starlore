package services

import (
	"database/sql"
	"net/http"
	"strings"
	"time"

	"starlore-go/internal/database"
	"starlore-go/internal/models"
	"starlore-go/internal/schemas"
	"starlore-go/internal/utils"

	"github.com/gin-gonic/gin"
)

// Register 用户注册
func Register(c *gin.Context, req schemas.RegisterRequest) {
	if req.Username == "" || req.Password == "" {
		utils.Fail(c, http.StatusBadRequest, "用户名和密码不能为空")
		return
	}
	if len(req.Password) < 6 {
		utils.Fail(c, http.StatusBadRequest, "密码长度不能少于6位")
		return
	}

	// 检查用户名唯一性
	var count int64
	database.DB.Model(&models.User{}).Where("username = ?", req.Username).Count(&count)
	if count > 0 {
		utils.Fail(c, http.StatusConflict, "用户名已存在")
		return
	}

	hashedPwd, err := utils.HashPassword(req.Password)
	if err != nil {
		utils.Fail(c, http.StatusInternalServerError, "密码加密失败")
		return
	}

	now := time.Now()
	user := models.User{
		Username:     req.Username,
		Password:     hashedPwd,
		Nickname:     sql.NullString{String: req.Nickname, Valid: req.Nickname != ""},
		Email:        sql.NullString{String: req.Email, Valid: req.Email != ""},
		Role:         "user",
		AIDailyLimit: 10,
		AITodayCount: 0,
		AIResetDate:  sql.NullTime{Time: time.Now(), Valid: true},
		CreatedAt:    &now,
		UpdatedAt:    &now,
	}
	database.DB.Create(&user)

	token, err := utils.GenerateToken(user.ID, user.Username)
	if err != nil {
		utils.Fail(c, http.StatusInternalServerError, "Token 生成失败")
		return
	}

	c.JSON(http.StatusOK, schemas.AuthResponse{
		Message: "注册成功",
		Data:    schemas.AuthData{Token: token, User: toUserInfo(user)},
	})
}

// Login 用户登录
func Login(c *gin.Context, req schemas.LoginRequest) {
	if req.Username == "" || req.Password == "" {
		utils.Fail(c, http.StatusBadRequest, "用户名和密码不能为空")
		return
	}

	var user models.User
	result := database.DB.Where("username = ?", req.Username).First(&user)
	if result.Error != nil || !utils.VerifyPassword(req.Password, user.Password) {
		utils.Fail(c, http.StatusBadRequest, "用户名或密码错误")
		return
	}

	token, err := utils.GenerateToken(user.ID, user.Username)
	if err != nil {
		utils.Fail(c, http.StatusInternalServerError, "Token 生成失败")
		return
	}

	// 记录登录日志
	ip := c.ClientIP()
	if ip == "0:0:0:0:0:0:0:1" {
		ip = "127.0.0.1"
	}
	now := time.Now()
	logEntry := models.LoginLog{
		UserID:    user.ID,
		Username:  user.Username,
		IP:        ip,
		UserAgent: c.GetHeader("User-Agent"),
		LoginTime: &now,
	}
	database.DB.Create(&logEntry)

	go lookupAndUpdateLog(logEntry.ID, ip)

	c.JSON(http.StatusOK, schemas.AuthResponse{
		Message: "登录成功",
		Data:    schemas.AuthData{Token: token, User: toUserInfo(user)},
	})
}

// GetCurrentUser 获取当前用户
func GetCurrentUser(c *gin.Context) {
	uid := utils.GetUserID(c)
	if uid == 0 {
		utils.Fail(c, http.StatusUnauthorized, "未登录")
		return
	}

	var user models.User
	if err := database.DB.First(&user, uid).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "用户不存在")
		return
	}
	c.JSON(http.StatusOK, toUserInfo(user))
}

// UpdateProfile 更新用户资料
func UpdateProfile(c *gin.Context, req schemas.UpdateProfileRequest) {
	uid := utils.GetUserID(c)
	if uid == 0 {
		utils.Fail(c, http.StatusUnauthorized, "未登录")
		return
	}

	var user models.User
	if err := database.DB.First(&user, uid).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "用户不存在")
		return
	}

	updates := map[string]interface{}{}
	if req.Nickname != "" {
		updates["nickname"] = req.Nickname
	}
	if req.Avatar != "" {
		updates["avatar"] = req.Avatar
	}
	if req.Bio != "" {
		updates["bio"] = req.Bio
	}
	if req.Email != "" {
		updates["email"] = req.Email
	}
	if req.Location != "" {
		updates["location"] = req.Location
	}
	if req.Website != "" {
		updates["website"] = req.Website
	}
	if req.Github != "" {
		updates["github"] = req.Github
	}

	if req.Username != "" && req.Username != user.Username {
		var count int64
		database.DB.Model(&models.User{}).Where("username = ?", req.Username).Count(&count)
		if count > 0 {
			utils.Fail(c, http.StatusConflict, "用户名已存在")
			return
		}
		updates["username"] = req.Username
	}

	if req.NewPassword != "" {
		if req.OldPassword == "" {
			utils.Fail(c, http.StatusBadRequest, "请输入旧密码")
			return
		}
		if !utils.VerifyPassword(req.OldPassword, user.Password) {
			utils.Fail(c, http.StatusBadRequest, "旧密码错误")
			return
		}
		hashed, err := utils.HashPassword(req.NewPassword)
		if err != nil {
			utils.Fail(c, http.StatusInternalServerError, "密码加密失败")
			return
		}
		updates["password"] = hashed
	}

	now := time.Now()
	updates["updatedAt"] = &now
	database.DB.Model(&user).Updates(updates)
	database.DB.First(&user, uid)

	c.JSON(http.StatusOK, toUserInfo(user))
}

func toUserInfo(u models.User) schemas.UserInfo {
	return schemas.UserInfo{
		ID:           u.ID,
		Username:     u.Username,
		Nickname:     u.Nickname.String,
		Email:        u.Email.String,
		Avatar:       u.Avatar.String,
		Bio:          u.Bio.String,
		Location:     u.Location.String,
		Website:      u.Website.String,
		Github:       u.Github.String,
		Role:         u.Role,
		AIDailyLimit: u.AIDailyLimit,
		AITodayCount: u.AITodayCount,
		CreatedAt:    u.CreatedAt,
		UpdatedAt:    u.UpdatedAt,
	}
}

func lookupAndUpdateLog(logID int64, ip string) {
	if strings.HasPrefix(ip, "127.") || strings.HasPrefix(ip, "192.168.") || ip == "::1" {
		return
	}
	// IP 地理位置查询（与 Java 版一致，调用 ip-api.com）
}
