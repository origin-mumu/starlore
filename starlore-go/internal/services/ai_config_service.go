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

// GetAllAiConfigs 获取所有 AI 配置
func GetAllAiConfigs(c *gin.Context) {
	var configs []models.AIConfig
	database.DB.Order("modelKey").Find(&configs)

	data := make([]gin.H, len(configs))
	for i, cfg := range configs {
		data[i] = gin.H{
			"id":        cfg.ID,
			"modelKey":  cfg.ModelKey,
			"modelName": cfg.ModelName,
			"apiUrl":    cfg.APIURL,
			"modelId":   cfg.ModelID,
			"enabled":   cfg.Enabled,
			"createdAt": cfg.CreatedAt,
			"updatedAt": cfg.UpdatedAt,
		}
	}
	c.JSON(http.StatusOK, gin.H{"data": data})
}

// GetAiConfigByID 获取单个 AI 配置
func GetAiConfigByID(c *gin.Context) {
	id := utils.ParseInt(c.Param("id"), 0)
	var config models.AIConfig
	if err := database.DB.First(&config, id).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "配置不存在")
		return
	}
	c.JSON(http.StatusOK, gin.H{
		"data": gin.H{
			"id":        config.ID,
			"modelKey":  config.ModelKey,
			"modelName": config.ModelName,
			"apiUrl":    config.APIURL,
			"modelId":   config.ModelID,
			"enabled":   config.Enabled,
		},
	})
}

// CreateAiConfig 创建 AI 配置
func CreateAiConfig(c *gin.Context, req schemas.AIConfigRequest) {
	if req.ModelKey == nil || req.ModelName == nil || req.APIURL == nil || req.ModelID == nil {
		utils.Fail(c, http.StatusBadRequest, "modelKey, modelName, apiUrl, modelId 不能为空")
		return
	}

	var count int64
	database.DB.Model(&models.AIConfig{}).Where("modelKey = ?", *req.ModelKey).Count(&count)
	if count > 0 {
		utils.Fail(c, http.StatusConflict, "modelKey 已存在")
		return
	}

	now := time.Now()
	enabled := true
	if req.Enabled != nil {
		enabled = *req.Enabled
	}
	apiKey := ""
	if req.APIKey != nil {
		apiKey = *req.APIKey
	}

	config := models.AIConfig{
		ModelKey:  *req.ModelKey,
		ModelName: *req.ModelName,
		APIURL:    *req.APIURL,
		ModelID:   *req.ModelID,
		APIKey:    apiKey,
		Enabled:   enabled,
		CreatedAt: &now,
		UpdatedAt: &now,
	}
	database.DB.Create(&config)
	utils.Ok(c, "配置创建成功", gin.H{"id": config.ID})
}

// UpdateAiConfig 更新 AI 配置
func UpdateAiConfig(c *gin.Context, req schemas.AIConfigRequest) {
	id := utils.ParseInt(c.Param("id"), 0)

	var config models.AIConfig
	if err := database.DB.First(&config, id).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "配置不存在")
		return
	}

	if req.ModelKey != nil && *req.ModelKey != config.ModelKey {
		var count int64
		database.DB.Model(&models.AIConfig{}).Where("modelKey = ?", *req.ModelKey).Count(&count)
		if count > 0 {
			utils.Fail(c, http.StatusConflict, "modelKey 已存在")
			return
		}
		config.ModelKey = *req.ModelKey
	}
	if req.ModelName != nil {
		config.ModelName = *req.ModelName
	}
	if req.APIURL != nil {
		config.APIURL = *req.APIURL
	}
	if req.ModelID != nil {
		config.ModelID = *req.ModelID
	}
	if req.APIKey != nil {
		config.APIKey = *req.APIKey
	}
	if req.Enabled != nil {
		config.Enabled = *req.Enabled
	}

	now := time.Now()
	config.UpdatedAt = &now
	database.DB.Save(&config)
	utils.Ok(c, "配置更新成功", nil)
}

// DeleteAiConfig 删除 AI 配置
func DeleteAiConfig(c *gin.Context) {
	id := utils.ParseInt(c.Param("id"), 0)

	var config models.AIConfig
	if err := database.DB.First(&config, id).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "配置不存在")
		return
	}
	database.DB.Delete(&config)
	utils.Ok(c, "配置删除成功", nil)
}
