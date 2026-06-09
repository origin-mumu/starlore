package schemas

import "time"

// ---------- 通用 ----------

// SimpleResponse 统一响应格式
type SimpleResponse struct {
	Success bool        `json:"success"`
	Message string      `json:"message"`
	Data    interface{} `json:"data,omitempty"`
}

// PaginationInfo 分页信息
type PaginationInfo struct {
	Current int   `json:"current"`
	Total   int64 `json:"total"`
	Pages   int   `json:"pages"`
}

// ---------- 认证 ----------

// LoginRequest 登录请求
type LoginRequest struct {
	Username string `json:"username" binding:"required"`
	Password string `json:"password" binding:"required"`
}

// RegisterRequest 注册请求
type RegisterRequest struct {
	Username string `json:"username" binding:"required,min=2,max=20"`
	Password string `json:"password" binding:"required,min=6,max=50"`
	Email    string `json:"email"`
	Nickname string `json:"nickname"`
}

// UserInfo 用户信息
type UserInfo struct {
	ID           int        `json:"id"`
	Username     string     `json:"username"`
	Nickname     string     `json:"nickname,omitempty"`
	Email        string     `json:"email,omitempty"`
	Avatar       string     `json:"avatar,omitempty"`
	Bio          string     `json:"bio,omitempty"`
	Location     string     `json:"location,omitempty"`
	Website      string     `json:"website,omitempty"`
	Github       string     `json:"github,omitempty"`
	Role         string     `json:"role"`
	AIDailyLimit int        `json:"aiDailyLimit"`
	AITodayCount int        `json:"aiTodayCount"`
	CreatedAt    *time.Time `json:"createdAt"`
	UpdatedAt    *time.Time `json:"updatedAt"`
}

// AuthData 认证数据
type AuthData struct {
	Token string   `json:"token"`
	User  UserInfo `json:"user"`
}

// AuthResponse 认证响应
type AuthResponse struct {
	Message string   `json:"message"`
	Data    AuthData `json:"data"`
}

// UpdateProfileRequest 更新资料请求
type UpdateProfileRequest struct {
	Nickname    string `json:"nickname"`
	Avatar      string `json:"avatar"`
	Bio         string `json:"bio"`
	Email       string `json:"email"`
	Location    string `json:"location"`
	Website     string `json:"website"`
	Github      string `json:"github"`
	Username    string `json:"username"`
	OldPassword string `json:"oldPassword"`
	NewPassword string `json:"newPassword"`
}

// ---------- 文章 ----------

// CreateArticleRequest 创建文章请求
type CreateArticleRequest struct {
	Title       string   `json:"title" binding:"required,max=200"`
	Content     string   `json:"content"`
	Description string   `json:"description"`
	Category    string   `json:"category"`
	Tags        []string `json:"tags"`
	CoverImage  string   `json:"coverImage"`
	Status      string   `json:"status"`
}

// UpdateArticleRequest 更新文章请求
type UpdateArticleRequest struct {
	Title       *string  `json:"title"`
	Content     *string  `json:"content"`
	Description *string  `json:"description"`
	Category    *string  `json:"category"`
	Tags        []string `json:"tags"`
	CoverImage  *string  `json:"coverImage"`
	Status      *string  `json:"status"`
}

// ArticleSummary 文章摘要
type ArticleSummary struct {
	ID          int        `json:"id"`
	UserID      int        `json:"userId"`
	AuthorName  string     `json:"authorName,omitempty"`
	Title       string     `json:"title"`
	Status      string     `json:"status,omitempty"`
	Description string     `json:"description,omitempty"`
	Category    string     `json:"category,omitempty"`
	Tags        []string   `json:"tags"`
	CoverImage  string     `json:"cover_image,omitempty"`
	ViewCount   int        `json:"view_count,omitempty"`
	CreatedAt   *time.Time `json:"createdAt"`
}

// ArticleDetail 文章详情
type ArticleDetail struct {
	ID          int        `json:"id"`
	Title       string     `json:"title"`
	Content     string     `json:"content,omitempty"`
	Description string     `json:"description,omitempty"`
	Category    string     `json:"category,omitempty"`
	Tags        []string   `json:"tags"`
	CoverImage  string     `json:"cover_image,omitempty"`
	ViewCount   int        `json:"view_count,omitempty"`
	Status      string     `json:"status,omitempty"`
	CreatedAt   *time.Time `json:"createdAt"`
	UpdatedAt   *time.Time `json:"updatedAt"`
}

// ArticleListResponse 文章列表响应
type ArticleListResponse struct {
	Data       []ArticleSummary `json:"data"`
	Pagination PaginationInfo   `json:"pagination"`
}

// ---------- 分类 ----------

// CreateCategoryRequest 创建分类请求
type CreateCategoryRequest struct {
	Name        string `json:"name" binding:"required,max=50"`
	Description string `json:"description"`
	Color       string `json:"color"`
}

// UpdateCategoryRequest 更新分类请求
type UpdateCategoryRequest struct {
	Name        *string `json:"name"`
	Description *string `json:"description"`
	Color       *string `json:"color"`
}

// CategoryItem 分类项
type CategoryItem struct {
	ID           int        `json:"id"`
	UserID       int        `json:"userId"`
	Name         string     `json:"name"`
	Description  string     `json:"description,omitempty"`
	Color        string     `json:"color,omitempty"`
	ArticleCount int        `json:"article_count"`
	CreatedAt    *time.Time `json:"createdAt"`
	UpdatedAt    *time.Time `json:"updatedAt"`
}

// CategoryListResponse 分类列表响应
type CategoryListResponse struct {
	Data []CategoryItem `json:"data"`
}

// ---------- AI ----------

// CreateSessionRequest 创建会话请求
type CreateSessionRequest struct {
	Title        string `json:"title"`
	CharacterKey string `json:"characterKey"`
	ModelID      string `json:"modelId"`
}

// UpdateSessionRequest 更新会话请求
type UpdateSessionRequest struct {
	Title        *string `json:"title"`
	CharacterKey *string `json:"characterKey"`
	ModelID      *string `json:"modelId"`
}

// AppendPairRequest 追加消息对请求
type AppendPairRequest struct {
	UserContent      string `json:"userContent"`
	AssistantContent string `json:"assistantContent"`
}

// AIConfigRequest AI 配置请求
type AIConfigRequest struct {
	ModelKey  *string `json:"modelKey"`
	ModelName *string `json:"modelName"`
	APIURL    *string `json:"apiUrl"`
	ModelID   *string `json:"modelId"`
	APIKey    *string `json:"apiKey"`
	Enabled   *bool   `json:"enabled"`
}

// ModelInfo 模型信息
type ModelInfo struct {
	ID         string `json:"id"`
	Name       string `json:"name"`
	Configured bool   `json:"configured"`
}

// AIModelsResponse AI 模型列表响应
type AIModelsResponse struct {
	Success bool        `json:"success"`
	Models  []ModelInfo `json:"models"`
}

// SessionData 会话数据
type SessionData struct {
	ID           int        `json:"id"`
	Title        string     `json:"title"`
	CharacterKey string     `json:"characterKey"`
	ModelID      string     `json:"modelId,omitempty"`
	CreatedAt    *time.Time `json:"createdAt"`
	UpdatedAt    *time.Time `json:"updatedAt"`
}

// AISessionResponse AI 会话响应
type AISessionResponse struct {
	Success bool        `json:"success"`
	Session SessionData `json:"session"`
}

// SessionItem 会话列表项
type SessionItem struct {
	ID           int        `json:"id"`
	Title        string     `json:"title"`
	CharacterKey string     `json:"characterKey"`
	ModelID      string     `json:"modelId,omitempty"`
	CreatedAt    *time.Time `json:"createdAt"`
	UpdatedAt    *time.Time `json:"updatedAt"`
}

// AISessionListResponse AI 会话列表响应
type AISessionListResponse struct {
	Success  bool          `json:"success"`
	Sessions []SessionItem `json:"sessions"`
}

// MessageItem 消息项
type MessageItem struct {
	ID        int        `json:"id"`
	Role      string     `json:"role"`
	Content   string     `json:"content,omitempty"`
	CreatedAt *time.Time `json:"createdAt"`
}

// SessionInfo 会话信息
type SessionInfo struct {
	ID           int    `json:"id"`
	Title        string `json:"title"`
	CharacterKey string `json:"characterKey"`
	ModelID      string `json:"modelId,omitempty"`
}

// AIMessageListResponse AI 消息列表响应
type AIMessageListResponse struct {
	Success  bool          `json:"success"`
	Session  SessionInfo   `json:"session"`
	Messages []MessageItem `json:"messages"`
}

// CharacterCard 角色卡
type CharacterCard struct {
	Key          string `json:"key"`
	Name         string `json:"name"`
	Description  string `json:"description"`
	SystemPrompt string `json:"systemPrompt"`
}

// AICharacterCardsResponse AI 角色卡响应
type AICharacterCardsResponse struct {
	Success bool            `json:"success"`
	Cards   []CharacterCard `json:"cards"`
}

// ---------- 统计 ----------

// CategoryInfo 分类统计信息
type CategoryInfo struct {
	ID           int    `json:"id"`
	Name         string `json:"name"`
	ArticleCount int    `json:"article_count"`
}

// BlogStatsData 博客统计数据
type BlogStatsData struct {
	TotalArticles     int64            `json:"totalArticles"`
	TotalCategories   int64            `json:"totalCategories"`
	TotalViews        int64            `json:"totalViews"`
	PopularArticles   []ArticleSummary `json:"popularArticles"`
	PopularCategories []CategoryInfo   `json:"popularCategories"`
}

// BlogStatsResponse 博客统计响应
type BlogStatsResponse struct {
	Data BlogStatsData `json:"data"`
}

// DailyItem 每日统计项
type DailyItem struct {
	Date  string `json:"date"`
	Count int    `json:"count"`
}

// DailyStatsResponse 每日统计响应
type DailyStatsResponse struct {
	Data []DailyItem `json:"data"`
}

// ---------- 项目 ----------

// ProjectRequest 项目请求
type ProjectRequest struct {
	Name        *string `json:"name"`
	Description *string `json:"description"`
	URL         *string `json:"url"`
	Image       *string `json:"image"`
	SortOrder   *int    `json:"sortOrder"`
}

// ProjectInfo 项目信息
type ProjectInfo struct {
	ID          int        `json:"id"`
	Name        string     `json:"name"`
	Description string     `json:"description,omitempty"`
	URL         string     `json:"url,omitempty"`
	Image       string     `json:"image,omitempty"`
	SortOrder   int        `json:"sortOrder"`
	CreatedAt   *time.Time `json:"createdAt"`
	UpdatedAt   *time.Time `json:"updatedAt"`
}
