package services

import (
	"bufio"
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"strings"
	"time"

	"starlore-go/internal/config"
	"starlore-go/internal/database"
	"starlore-go/internal/models"
	"starlore-go/internal/schemas"
	"starlore-go/internal/utils"

	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

// GetModels 获取可用 AI 模型列表
func GetModels(c *gin.Context) {
	var configs []models.AIConfig
	database.DB.Order("modelKey").Find(&configs)

	if len(configs) > 0 {
		modelList := make([]schemas.ModelInfo, len(configs))
		for i, cfg := range configs {
			modelList[i] = schemas.ModelInfo{
				ID:         cfg.ModelKey,
				Name:       cfg.ModelName,
				Configured: cfg.Enabled && cfg.APIKey != "",
			}
		}
		c.JSON(http.StatusOK, schemas.AIModelsResponse{Success: true, Models: modelList})
		return
	}

	// 回退：检查环境变量
	var modelList []schemas.ModelInfo
	if os.Getenv("AI_KEY_QWEN") != "" || os.Getenv("VITE_API_KEY") != "" {
		modelList = append(modelList, schemas.ModelInfo{ID: "qwen-plus", Name: "通义千问", Configured: true})
	}
	if os.Getenv("AI_KEY_MIMO") != "" || os.Getenv("VITE_MIMO_API_KEY") != "" {
		modelList = append(modelList, schemas.ModelInfo{ID: "mimo", Name: "MiMo", Configured: true})
	}
	c.JSON(http.StatusOK, schemas.AIModelsResponse{Success: true, Models: modelList})
}

// GetCharacterCards 获取角色卡配置
func GetCharacterCards(c *gin.Context) {
	data, err := os.ReadFile("resources/ai_character_cards.json")
	if err != nil {
		c.JSON(http.StatusOK, schemas.AICharacterCardsResponse{Success: true, Cards: []schemas.CharacterCard{}})
		return
	}
	var cards []schemas.CharacterCard
	if err := json.Unmarshal(data, &cards); err != nil {
		c.JSON(http.StatusOK, schemas.AICharacterCardsResponse{Success: true, Cards: []schemas.CharacterCard{}})
		return
	}
	c.JSON(http.StatusOK, schemas.AICharacterCardsResponse{Success: true, Cards: cards})
}

// StreamChat SSE 流式对话
func StreamChat(c *gin.Context) {
	var req struct {
		Model    string                   `json:"model"`
		Messages []map[string]interface{} `json:"messages"`
	}
	if c.Request.Method == "GET" {
		req.Model = c.Query("model")
		if err := json.Unmarshal([]byte(c.Query("messages")), &req.Messages); err != nil {
			req.Messages = []map[string]interface{}{}
		}
	} else {
		if err := c.ShouldBindJSON(&req); err != nil {
			utils.Fail(c, http.StatusBadRequest, "请求参数错误")
			return
		}
	}
	if req.Model == "" {
		req.Model = "deepseek-chat"
	}

	apiURL, modelName, apiKey := resolveModelConfig(req.Model)
	if apiKey == "" {
		c.SSEvent("message", gin.H{"error": "未配置 API Key"})
		return
	}

	body := map[string]interface{}{
		"model":    modelName,
		"messages": req.Messages,
		"stream":   true,
	}
	bodyJSON, err := json.Marshal(body)
	if err != nil {
		utils.Fail(c, http.StatusInternalServerError, "请求构建失败")
		return
	}

	httpReq, err := http.NewRequest("POST", apiURL, bytes.NewReader(bodyJSON))
	if err != nil {
		utils.Fail(c, http.StatusInternalServerError, "请求创建失败")
		return
	}
	httpReq.Header.Set("Content-Type", "application/json")
	if strings.Contains(apiURL, "xiaomimimo.com") {
		httpReq.Header.Set("api-key", apiKey)
	} else {
		httpReq.Header.Set("Authorization", "Bearer "+apiKey)
	}

	c.Header("Content-Type", "text/event-stream")
	c.Header("Cache-Control", "no-cache")
	c.Header("Connection", "keep-alive")
	c.Header("X-Accel-Buffering", "no")

	client := &http.Client{Timeout: 120 * time.Second}
	resp, err := client.Do(httpReq)
	if err != nil {
		writeSSEError(c, err.Error())
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		bodyBytes, _ := io.ReadAll(resp.Body)
		writeSSEError(c, string(bodyBytes))
		return
	}

	scanner := bufio.NewScanner(resp.Body)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" {
			continue
		}
		if strings.HasPrefix(line, "data: ") {
			data := line[6:]
			if data == "[DONE]" {
				fmt.Fprintf(c.Writer, "data: {\"done\":true}\n\n")
				c.Writer.Flush()
				break
			}
			var parsed map[string]interface{}
			if json.Unmarshal([]byte(data), &parsed) == nil {
				if choices, ok := parsed["choices"].([]interface{}); ok && len(choices) > 0 {
					if choice, ok := choices[0].(map[string]interface{}); ok {
						if delta, ok := choice["delta"].(map[string]interface{}); ok {
							if content, ok := delta["content"].(string); ok && content != "" {
								chunk, _ := json.Marshal(gin.H{"content": content})
								fmt.Fprintf(c.Writer, "data: %s\n\n", chunk)
								c.Writer.Flush()
							}
						}
					}
				}
			}
		}
	}
	if err := scanner.Err(); err != nil {
		writeSSEError(c, err.Error())
	}
	fmt.Fprintf(c.Writer, "data: [DONE]\n\n")
	c.Writer.Flush()
}

// writeSSEError 写入 SSE 错误事件
func writeSSEError(c *gin.Context, errMsg string) {
	errJSON, _ := json.Marshal(gin.H{"error": errMsg})
	fmt.Fprintf(c.Writer, "data: %s\n\n", errJSON)
	c.Writer.Flush()
}

func resolveModelConfig(model string) (apiURL, modelName, apiKey string) {
	var cfg models.AIConfig
	if database.DB.Where("modelKey = ? AND enabled = true", model).First(&cfg).Error == nil && cfg.APIKey != "" {
		apiURL = cfg.APIURL
		if !strings.HasSuffix(apiURL, "/chat/completions") {
			apiURL = strings.TrimRight(apiURL, "/") + "/chat/completions"
		}
		return apiURL, cfg.ModelID, cfg.APIKey
	}

	if config.AI.APIKey != "" {
		return strings.TrimRight(config.AI.BaseURL, "/") + "/chat/completions", config.AI.Model, config.AI.APIKey
	}
	return "", "", ""
}

// ListSessions 获取会话列表
func ListSessions(c *gin.Context) {
	uid := utils.GetUserID(c)
	var sessions []models.AISession
	database.DB.Where("user_id = ?", uid).Order("updatedAt DESC").Limit(80).Find(&sessions)

	items := make([]schemas.SessionItem, len(sessions))
	for i, s := range sessions {
		items[i] = schemas.SessionItem{
			ID:           s.ID,
			Title:        s.Title,
			CharacterKey: s.CharacterKey,
			ModelID:      s.ModelID,
			CreatedAt:    s.CreatedAt,
			UpdatedAt:    s.UpdatedAt,
		}
	}
	c.JSON(http.StatusOK, schemas.AISessionListResponse{Success: true, Sessions: items})
}

// CreateSession 创建会话
func CreateSession(c *gin.Context, req schemas.CreateSessionRequest) {
	uid := utils.GetUserID(c)
	now := time.Now()
	session := models.AISession{
		UserID:       uid,
		Title:        "新会话",
		CharacterKey: "default",
		ModelID:      "deepseek-chat",
		CreatedAt:    &now,
		UpdatedAt:    &now,
	}
	if req.Title != "" {
		session.Title = req.Title
	}
	if req.CharacterKey != "" {
		session.CharacterKey = req.CharacterKey
	}
	if req.ModelID != "" {
		session.ModelID = req.ModelID
	}
	database.DB.Create(&session)

	c.JSON(http.StatusOK, schemas.AISessionResponse{
		Success: true,
		Session: schemas.SessionData{
			ID:           session.ID,
			Title:        session.Title,
			CharacterKey: session.CharacterKey,
			ModelID:      session.ModelID,
			CreatedAt:    session.CreatedAt,
			UpdatedAt:    session.UpdatedAt,
		},
	})
}

// UpdateSession 更新会话
func UpdateSession(c *gin.Context, req schemas.UpdateSessionRequest) {
	uid := utils.GetUserID(c)
	sessionID := utils.ParseInt(c.Param("id"), 0)

	var session models.AISession
	if err := database.DB.First(&session, sessionID).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "会话不存在")
		return
	}
	if session.UserID != uid {
		utils.Fail(c, http.StatusUnauthorized, "无权访问该会话")
		return
	}

	if req.Title != nil {
		session.Title = (*req.Title)[:min(len(*req.Title), 255)]
	}
	if req.CharacterKey != nil {
		session.CharacterKey = (*req.CharacterKey)[:min(len(*req.CharacterKey), 64)]
	}
	if req.ModelID != nil {
		session.ModelID = (*req.ModelID)[:min(len(*req.ModelID), 32)]
	}
	now := time.Now()
	session.UpdatedAt = &now
	database.DB.Save(&session)

	c.JSON(http.StatusOK, schemas.AISessionResponse{
		Success: true,
		Session: schemas.SessionData{
			ID:           session.ID,
			Title:        session.Title,
			CharacterKey: session.CharacterKey,
			ModelID:      session.ModelID,
			CreatedAt:    session.CreatedAt,
			UpdatedAt:    session.UpdatedAt,
		},
	})
}

// DeleteSession 删除会话
func DeleteSession(c *gin.Context) {
	uid := utils.GetUserID(c)
	sessionID := utils.ParseInt(c.Param("id"), 0)

	var session models.AISession
	if err := database.DB.First(&session, sessionID).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "会话不存在")
		return
	}
	if session.UserID != uid {
		utils.Fail(c, http.StatusUnauthorized, "无权访问该会话")
		return
	}

	// 使用事务确保一致性
	database.DB.Transaction(func(tx *gorm.DB) error {
		tx.Where("sessionId = ?", sessionID).Delete(&models.AIMessage{})
		tx.Delete(&session)
		return nil
	})
	utils.Ok(c, "会话删除成功", nil)
}

// GetMessages 获取会话消息
func GetMessages(c *gin.Context) {
	uid := utils.GetUserID(c)
	sessionID := utils.ParseInt(c.Param("id"), 0)

	var session models.AISession
	if err := database.DB.First(&session, sessionID).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "会话不存在")
		return
	}
	if session.UserID != uid {
		utils.Fail(c, http.StatusUnauthorized, "无权访问该会话")
		return
	}

	var messages []models.AIMessage
	database.DB.Where("sessionId = ?", sessionID).Order("id").Find(&messages)

	items := make([]schemas.MessageItem, len(messages))
	for i, m := range messages {
		items[i] = schemas.MessageItem{ID: m.ID, Role: m.Role, Content: m.Content, CreatedAt: m.CreatedAt}
	}

	c.JSON(http.StatusOK, schemas.AIMessageListResponse{
		Success:  true,
		Session:  schemas.SessionInfo{ID: session.ID, Title: session.Title, CharacterKey: session.CharacterKey, ModelID: session.ModelID},
		Messages: items,
	})
}

// AppendPair 追加消息对
func AppendPair(c *gin.Context, req schemas.AppendPairRequest) {
	uid := utils.GetUserID(c)
	sessionID := utils.ParseInt(c.Param("id"), 0)

	var session models.AISession
	if err := database.DB.First(&session, sessionID).Error; err != nil {
		utils.Fail(c, http.StatusNotFound, "会话不存在")
		return
	}
	if session.UserID != uid {
		utils.Fail(c, http.StatusUnauthorized, "无权访问该会话")
		return
	}

	if req.UserContent == "" || req.AssistantContent == "" {
		utils.Fail(c, http.StatusBadRequest, "userContent 和 assistantContent 不能为空")
		return
	}

	now := time.Now()
	database.DB.Create(&models.AIMessage{SessionID: sessionID, Role: "user", Content: req.UserContent, CreatedAt: &now})
	database.DB.Create(&models.AIMessage{SessionID: sessionID, Role: "assistant", Content: req.AssistantContent, CreatedAt: &now})

	if session.Title == "新会话" {
		title := req.UserContent
		if len([]rune(title)) > 28 {
			title = string([]rune(title)[:28]) + "..."
		}
		session.Title = title
	}
	session.UpdatedAt = &now
	database.DB.Save(&session)

	utils.Ok(c, "消息已保存", nil)
}

// GetQuota 获取 AI 配额
func GetQuota(c *gin.Context) {
	uid := utils.GetUserID(c)
	var user models.User
	database.DB.First(&user, uid)

	isAdmin := user.Role == "admin"
	if user.AIResetDate.Valid && user.AIResetDate.Time.Format("2006-01-02") != time.Now().Format("2006-01-02") {
		database.DB.Model(&user).Updates(map[string]interface{}{"ai_today_count": 0, "ai_reset_date": time.Now()})
		user.AITodayCount = 0
	}

	limit := user.AIDailyLimit
	if limit == 0 {
		limit = 10
	}
	remaining := limit - user.AITodayCount
	if isAdmin {
		remaining = -1
	}

	c.JSON(http.StatusOK, gin.H{
		"success": true,
		"quota": gin.H{
			"dailyLimit": limit,
			"used":       user.AITodayCount,
			"remaining":  remaining,
			"isAdmin":    isAdmin,
		},
	})
}
