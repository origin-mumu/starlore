package services

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"starlore-go/internal/database"
	"starlore-go/internal/models"
	"starlore-go/internal/utils"

	"github.com/gin-gonic/gin"
)

// ---------- TTS ----------

func TextToSpeech(c *gin.Context) {
	var body struct {
		Model  string                   `json:"model"`
		Input  string                   `json:"input"`
		Voice  string                   `json:"voice"`
	}
	c.ShouldBindJSON(&body)

	// 调用 MiMo TTS API
	apiURL, modelName, apiKey := resolveModelConfig(body.Model)
	if apiKey == "" {
		// 尝试使用 mimo 配置
		apiURL, modelName, apiKey = resolveModelConfig("mimo")
	}
	if apiKey == "" {
		utils.Fail(c, http.StatusBadRequest, "未配置 TTS API Key")
		return
	}

	// 构建 TTS 请求
	ttsBody := map[string]interface{}{
		"model": modelName,
		"input": body.Input,
		"voice": body.Voice,
	}
	bodyJSON, _ := json.Marshal(ttsBody)

	// 替换 URL 为 TTS 端点
	ttsURL := strings.Replace(apiURL, "/chat/completions", "/audio/speech", 1)

	httpReq, _ := http.NewRequest("POST", ttsURL, bytes.NewReader(bodyJSON))
	httpReq.Header.Set("Content-Type", "application/json")
	if strings.Contains(ttsURL, "xiaomimimo.com") {
		httpReq.Header.Set("api-key", apiKey)
	} else {
		httpReq.Header.Set("Authorization", "Bearer "+apiKey)
	}

	client := &http.Client{Timeout: 60 * time.Second}
	resp, err := client.Do(httpReq)
	if err != nil {
		utils.Fail(c, http.StatusInternalServerError, "TTS 调用失败")
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		errBody, _ := io.ReadAll(resp.Body)
		utils.Fail(c, resp.StatusCode, string(errBody))
		return
	}

	audioData, _ := io.ReadAll(resp.Body)
	c.Header("Content-Type", "audio/wav")
	c.Data(http.StatusOK, "audio/wav", audioData)
}

// ---------- 发散思维 ----------

func Diverge(c *gin.Context) {
	var body struct {
		Keyword string `json:"keyword"`
		Model   string `json:"model"`
	}
	c.ShouldBindJSON(&body)

	model := body.Model
	if model == "" {
		model = "deepseek-chat"
	}

	prompt := "请围绕关键词「" + body.Keyword + "」进行发散联想，生成 8 个关联词。\n" +
		"每个词包含中文和英文两个版本，分别从以下维度思考：\n" +
		"1. 工具 2. 场景 3. 上下游 4. 风格 5. 品牌 6. 痛点 7. 趋势 8. 创新\n" +
		"返回 JSON 数组格式：[{\"zh\": \"中文词\", \"en\": \"English word\", \"dimension\": \"维度\"}]"

	messages := []map[string]interface{}{
		{"role": "user", "content": prompt},
	}

	apiURL, modelName, apiKey := resolveModelConfig(model)
	if apiKey == "" {
		utils.Fail(c, http.StatusBadRequest, "未配置 AI API Key")
		return
	}

	bodyMap := map[string]interface{}{
		"model":    modelName,
		"messages": messages,
	}
	bodyJSON, _ := json.Marshal(bodyMap)

	httpReq, _ := http.NewRequest("POST", apiURL, bytes.NewReader(bodyJSON))
	httpReq.Header.Set("Content-Type", "application/json")
	if strings.Contains(apiURL, "xiaomimimo.com") {
		httpReq.Header.Set("api-key", apiKey)
	} else {
		httpReq.Header.Set("Authorization", "Bearer "+apiKey)
	}

	client := &http.Client{Timeout: 60 * time.Second}
	resp, err := client.Do(httpReq)
	if err != nil {
		utils.Fail(c, http.StatusInternalServerError, "AI 调用失败")
		return
	}
	defer resp.Body.Close()

	respBody, _ := io.ReadAll(resp.Body)
	var result map[string]interface{}
	json.Unmarshal(respBody, &result)

	c.JSON(http.StatusOK, gin.H{"content": result})
}

// ---------- 重索引 ----------

func Reindex(c *gin.Context) {
	uid := utils.GetUserID(c)

	var articles []models.Article
	database.DB.Where("user_id = ? AND status = ?", uid, "published").Find(&articles)

	count := len(articles)

	utils.Ok(c, "已重新索引 "+fmt.Sprintf("%d", count)+" 篇文章", nil)
}
