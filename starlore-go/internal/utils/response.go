package utils

import (
	"encoding/json"
	"net/http"
	"strconv"

	"starlore-go/internal/schemas"

	"github.com/gin-gonic/gin"
)

// Ok 成功响应
func Ok(c *gin.Context, message string, data interface{}) {
	c.JSON(http.StatusOK, schemas.SimpleResponse{
		Success: true,
		Message: message,
		Data:    data,
	})
}

// Fail 失败响应
func Fail(c *gin.Context, status int, message string) {
	c.JSON(status, schemas.SimpleResponse{
		Success: false,
		Message: message,
	})
}

// GetUserID 从上下文获取用户 ID
func GetUserID(c *gin.Context) int {
	uid, exists := c.Get("userId")
	if !exists {
		return 0
	}
	id, ok := uid.(int)
	if !ok {
		return 0
	}
	return id
}

// ParseInt 解析字符串为整数，失败返回默认值
func ParseInt(s string, defaultVal int) int {
	if s == "" {
		return defaultVal
	}
	n, err := strconv.Atoi(s)
	if err != nil {
		return defaultVal
	}
	return n
}

// ParseFloat64ToInt 安全地将 float64 转为 int（JWT claims 中的数字）
func ParseFloat64ToInt(v interface{}) (int, bool) {
	switch val := v.(type) {
	case float64:
		return int(val), true
	case int:
		return val, true
	case int64:
		return int(val), true
	default:
		return 0, false
	}
}

// ParseTags 解析 JSON 标签字符串
func ParseTags(s string) []string {
	if s == "" || s == "null" {
		return []string{}
	}
	var tags []string
	if err := json.Unmarshal([]byte(s), &tags); err != nil {
		return []string{}
	}
	return tags
}
