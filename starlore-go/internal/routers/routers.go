package routers

import (
	"starlore-go/internal/services"
	"starlore-go/internal/schemas"

	"github.com/gin-gonic/gin"
)

func RegisterAll(r *gin.Engine) {
	// ---------- 健康检查 ----------
	r.GET("/api/health", func(c *gin.Context) {
		c.JSON(200, gin.H{"success": true, "message": "博客后端服务运行正常"})
	})

	// ---------- 认证 ----------
	auth := r.Group("/api/auth")
	{
		auth.POST("/register", func(c *gin.Context) {
			var req schemas.RegisterRequest
			if err := c.ShouldBindJSON(&req); err != nil {
				c.JSON(400, gin.H{"message": "请求参数错误"})
				return
			}
			services.Register(c, req)
		})
		auth.POST("/login", func(c *gin.Context) {
			var req schemas.LoginRequest
			if err := c.ShouldBindJSON(&req); err != nil {
				c.JSON(400, gin.H{"message": "请求参数错误"})
				return
			}
			services.Login(c, req)
		})
		auth.GET("/me", services.GetCurrentUser)
		auth.PUT("/profile", func(c *gin.Context) {
			var req schemas.UpdateProfileRequest
			c.ShouldBindJSON(&req)
			services.UpdateProfile(c, req)
		})
	}

	// ---------- 文章 ----------
	articles := r.Group("/api/articles")
	{
		articles.GET("", services.ListArticles)
		articles.GET("/stats/summary", services.GetBlogStats)
		articles.GET("/stats/daily", services.GetDailyStats)
		articles.GET("/:id", services.GetArticle)
		articles.POST("", func(c *gin.Context) {
			var req schemas.CreateArticleRequest
			if err := c.ShouldBindJSON(&req); err != nil {
				c.JSON(400, gin.H{"message": "请求参数错误"})
				return
			}
			services.CreateArticle(c, req)
		})
		articles.PUT("/:id", func(c *gin.Context) {
			var req schemas.UpdateArticleRequest
			c.ShouldBindJSON(&req)
			services.UpdateArticle(c, req)
		})
		articles.DELETE("/:id", services.DeleteArticle)
	}

	// ---------- 分类 ----------
	categories := r.Group("/api/categories")
	{
		categories.GET("", services.ListCategories)
		categories.GET("/:id", services.GetCategory)
		categories.POST("", func(c *gin.Context) {
			var req schemas.CreateCategoryRequest
			if err := c.ShouldBindJSON(&req); err != nil {
				c.JSON(400, gin.H{"message": "请求参数错误"})
				return
			}
			services.CreateCategory(c, req)
		})
		categories.PUT("/:id", func(c *gin.Context) {
			var req schemas.UpdateCategoryRequest
			c.ShouldBindJSON(&req)
			services.UpdateCategory(c, req)
		})
		categories.DELETE("/:id", services.DeleteCategory)
	}

	// ---------- 收藏 ----------
	bookmarks := r.Group("/api/bookmarks")
	{
		bookmarks.GET("", services.ListBookmarks)
		bookmarks.GET("/check/:articleId", services.CheckBookmark)
		bookmarks.POST("", services.AddBookmark)
		bookmarks.DELETE("/:articleId", services.RemoveBookmark)
	}

	// ---------- AI ----------
	ai := r.Group("/api/ai")
	{
		ai.GET("/models", services.GetModels)
		ai.GET("/character-cards", services.GetCharacterCards)
		ai.GET("/sse", services.StreamChat)
		ai.POST("/sse", services.StreamChat)
		ai.POST("/thinking-sse", services.StreamChat)
		ai.POST("/agent-sse", services.StreamChat)
		ai.POST("/analyze-image", services.StreamChat)
		ai.POST("/analyze-image/stream", services.StreamChat)
		ai.POST("/tts", services.TextToSpeech)
		ai.POST("/diverge", services.Diverge)
		ai.POST("/reindex", services.Reindex)

		// 会话管理
		ai.GET("/sessions", services.ListSessions)
		ai.POST("/sessions", func(c *gin.Context) {
			var req schemas.CreateSessionRequest
			c.ShouldBindJSON(&req)
			services.CreateSession(c, req)
		})
		ai.PATCH("/sessions/:id", func(c *gin.Context) {
			var req schemas.UpdateSessionRequest
			c.ShouldBindJSON(&req)
			services.UpdateSession(c, req)
		})
		ai.DELETE("/sessions/:id", services.DeleteSession)
		ai.GET("/sessions/:id/messages", services.GetMessages)
		ai.POST("/sessions/:id/append", func(c *gin.Context) {
			var req schemas.AppendPairRequest
			c.ShouldBindJSON(&req)
			services.AppendPair(c, req)
		})
		ai.GET("/quota", services.GetQuota)
	}

	// ---------- AI 配置 ----------
	aiConfig := r.Group("/api/ai-config")
	{
		aiConfig.GET("", services.GetAllAiConfigs)
		aiConfig.GET("/:id", services.GetAiConfigByID)
		aiConfig.POST("", func(c *gin.Context) {
			var req schemas.AIConfigRequest
			c.ShouldBindJSON(&req)
			services.CreateAiConfig(c, req)
		})
		aiConfig.PUT("/:id", func(c *gin.Context) {
			var req schemas.AIConfigRequest
			c.ShouldBindJSON(&req)
			services.UpdateAiConfig(c, req)
		})
		aiConfig.DELETE("/:id", services.DeleteAiConfig)
	}

	// ---------- 简历 ----------
	resume := r.Group("/api/resume")
	{
		resume.GET("/list", services.ListResumes)
		resume.GET("/:id", services.GetResume)
		resume.POST("/create", services.CreateResume)
		resume.PUT("/update", services.UpdateResume)
		resume.DELETE("/:id", services.DeleteResume)
		resume.GET("/:id/export-pdf", func(c *gin.Context) {
			c.JSON(200, gin.H{"message": "PDF 导出功能需要配置 starlore-pdf 服务"})
		})
	}

	// ---------- 项目 ----------
	projects := r.Group("/api/projects")
	{
		projects.GET("", services.ListProjects)
		projects.POST("", func(c *gin.Context) {
			var req schemas.ProjectRequest
			c.ShouldBindJSON(&req)
			services.CreateProject(c, req)
		})
		projects.PUT("/:id", func(c *gin.Context) {
			var req schemas.ProjectRequest
			c.ShouldBindJSON(&req)
			services.UpdateProject(c, req)
		})
		projects.DELETE("/:id", services.DeleteProject)
	}

	// ---------- 文件上传 ----------
	upload := r.Group("/api/upload")
	{
		upload.POST("/image", services.UploadImage)
	}

	// ---------- 管理后台 ----------
	admin := r.Group("/api/admin")
	{
		admin.GET("/users", services.AdminListUsers)
		admin.GET("/users/:id", services.AdminGetUser)
		admin.PUT("/users/:id", services.AdminUpdateUser)
		admin.DELETE("/users/:id", services.AdminDeleteUser)
		admin.GET("/login-logs", services.AdminLoginLogs)
	}
}
