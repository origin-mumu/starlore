package middleware

import (
	"net/http"
	"strings"

	"starlore-go/internal/utils"

	"github.com/gin-gonic/gin"
)

// AuthMiddleware JWT 认证中间件
func AuthMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		// OPTIONS 预检放行
		if c.Request.Method == "OPTIONS" {
			c.Next()
			return
		}

		path := c.Request.URL.Path

		// 非 /api 路径放行
		if !strings.HasPrefix(path, "/api") {
			c.Next()
			return
		}

		// 公开路径放行
		publicPaths := []string{
			"/api/auth/login",
			"/api/auth/register",
			"/api/health",
			"/api/upload/",
		}
		for _, p := range publicPaths {
			if strings.HasPrefix(path, p) {
				c.Next()
				return
			}
		}

		// 解析 Authorization header
		authHeader := c.GetHeader("Authorization")
		if authHeader == "" || !strings.HasPrefix(authHeader, "Bearer ") {
			c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "未登录，请先登录"})
			c.Abort()
			return
		}

		tokenStr := authHeader[7:]
		claims, err := utils.ParseToken(tokenStr)
		if err != nil {
			c.JSON(http.StatusUnauthorized, gin.H{"success": false, "message": "登录已过期，请重新登录"})
			c.Abort()
			return
		}

		// 安全地提取用户 ID
		if idVal, ok := claims["id"]; ok {
			if id, ok := utils.ParseFloat64ToInt(idVal); ok {
				c.Set("userId", id)
			}
		}
		if username, ok := claims["username"].(string); ok {
			c.Set("username", username)
		}

		c.Next()
	}
}
