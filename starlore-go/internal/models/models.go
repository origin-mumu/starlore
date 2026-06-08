package models

import (
	"database/sql"
	"time"
)

// User 用户模型
type User struct {
	ID           int            `gorm:"primaryKey;autoIncrement" json:"id"`
	Username     string         `gorm:"size:50" json:"username"`
	Email        sql.NullString `gorm:"size:100" json:"email"`
	Password     string         `gorm:"size:255" json:"-"`
	Nickname     sql.NullString `gorm:"size:50" json:"nickname"`
	Avatar       sql.NullString `gorm:"size:500" json:"avatar"`
	Bio          sql.NullString `gorm:"size:500" json:"bio"`
	Location     sql.NullString `gorm:"size:100" json:"location"`
	Website      sql.NullString `gorm:"size:500" json:"website"`
	Github       sql.NullString `gorm:"size:200" json:"github"`
	Role         string         `gorm:"size:20;default:user" json:"role"`
	AIDailyLimit int            `gorm:"column:ai_daily_limit;default:10" json:"ai_daily_limit"`
	AITodayCount int            `gorm:"column:ai_today_count;default:0" json:"ai_today_count"`
	AIResetDate  sql.NullTime   `gorm:"column:ai_reset_date" json:"ai_reset_date"`
	CreatedAt    *time.Time     `gorm:"column:createdAt" json:"createdAt"`
	UpdatedAt    *time.Time     `gorm:"column:updatedAt" json:"updatedAt"`
}

func (User) TableName() string { return "users" }

// Article 文章模型
type Article struct {
	ID          int        `gorm:"primaryKey;autoIncrement" json:"id"`
	UserID      int        `gorm:"column:user_id" json:"user_id"`
	Title       string     `gorm:"size:200" json:"title"`
	Content     string     `gorm:"type:text" json:"content"`
	Description string     `gorm:"size:500" json:"description"`
	Category    string     `gorm:"size:50" json:"category"`
	Tags        string     `gorm:"type:json" json:"tags"`
	CoverImage  string     `gorm:"column:cover_image;size:500" json:"cover_image"`
	ViewCount   int        `gorm:"column:view_count;default:0" json:"view_count"`
	Status      string     `gorm:"size:20;default:published" json:"status"`
	CreatedAt   *time.Time `gorm:"column:createdAt" json:"createdAt"`
	UpdatedAt   *time.Time `gorm:"column:updatedAt" json:"updatedAt"`
}

func (Article) TableName() string { return "articles" }

// Category 分类模型
type Category struct {
	ID           int        `gorm:"primaryKey;autoIncrement" json:"id"`
	UserID       int        `gorm:"column:user_id" json:"user_id"`
	Name         string     `gorm:"size:50" json:"name"`
	Description  string     `gorm:"size:200" json:"description"`
	Color        string     `gorm:"size:20" json:"color"`
	ArticleCount int        `gorm:"column:article_count;default:0" json:"article_count"`
	CreatedAt    *time.Time `gorm:"column:createdAt" json:"createdAt"`
	UpdatedAt    *time.Time `gorm:"column:updatedAt" json:"updatedAt"`
}

func (Category) TableName() string { return "categories" }

// AIConfig AI 模型配置
type AIConfig struct {
	ID        int        `gorm:"primaryKey;autoIncrement" json:"id"`
	ModelKey  string     `gorm:"column:modelKey;size:50" json:"modelKey"`
	ModelName string     `gorm:"column:modelName;size:100" json:"modelName"`
	APIURL    string     `gorm:"column:apiUrl;size:500" json:"apiUrl"`
	ModelID   string     `gorm:"column:modelId;size:100" json:"modelId"`
	APIKey    string     `gorm:"column:apiKey;size:500" json:"apiKey"`
	Enabled   bool       `gorm:"default:true" json:"enabled"`
	CreatedAt *time.Time `gorm:"column:createdAt" json:"createdAt"`
	UpdatedAt *time.Time `gorm:"column:updatedAt" json:"updatedAt"`
}

func (AIConfig) TableName() string { return "ai_configs" }

// AISession AI 会话模型
type AISession struct {
	ID           int        `gorm:"primaryKey;autoIncrement" json:"id"`
	UserID       int        `gorm:"column:user_id" json:"user_id"`
	Title        string     `gorm:"size:255" json:"title"`
	CharacterKey string     `gorm:"column:characterKey;size:64" json:"characterKey"`
	ModelID      string     `gorm:"column:modelId;size:32" json:"modelId"`
	CreatedAt    *time.Time `gorm:"column:createdAt" json:"createdAt"`
	UpdatedAt    *time.Time `gorm:"column:updatedAt" json:"updatedAt"`
}

func (AISession) TableName() string { return "ai_sessions" }

// AIMessage AI 消息模型
type AIMessage struct {
	ID        int        `gorm:"primaryKey;autoIncrement" json:"id"`
	SessionID int        `gorm:"column:sessionId" json:"sessionId"`
	Role      string     `gorm:"size:20" json:"role"`
	Content   string     `gorm:"type:text" json:"content"`
	CreatedAt *time.Time `gorm:"column:createdAt" json:"createdAt"`
}

func (AIMessage) TableName() string { return "ai_messages" }

// Bookmark 收藏模型
type Bookmark struct {
	ID        int        `gorm:"primaryKey;autoIncrement" json:"id"`
	UserID    int        `gorm:"column:user_id" json:"user_id"`
	ArticleID int        `gorm:"column:article_id" json:"article_id"`
	CreatedAt *time.Time `gorm:"column:created_at" json:"created_at"`
}

func (Bookmark) TableName() string { return "bookmarks" }

// Resume 简历模型
type Resume struct {
	ID        int        `gorm:"primaryKey;autoIncrement" json:"id"`
	UserID    int        `gorm:"column:user_id" json:"user_id"`
	Title     string     `gorm:"size:200" json:"title"`
	Template  string     `gorm:"size:50" json:"template"`
	Name      string     `gorm:"size:100" json:"name"`
	JobTitle  string     `gorm:"column:job_title;size:100" json:"job_title"`
	Phone     string     `gorm:"size:20" json:"phone"`
	Email     string     `gorm:"size:100" json:"email"`
	PhotoURL  string     `gorm:"column:photo_url;size:500" json:"photo_url"`
	Content   string     `gorm:"type:text" json:"content"`
	Status    string     `gorm:"size:20;default:active" json:"status"`
	CreatedAt *time.Time `gorm:"column:created_at" json:"created_at"`
	UpdatedAt *time.Time `gorm:"column:updated_at" json:"updated_at"`
}

func (Resume) TableName() string { return "resumes" }

// Project 项目模型
type Project struct {
	ID          int        `gorm:"primaryKey;autoIncrement" json:"id"`
	UserID      int        `gorm:"column:user_id" json:"user_id"`
	Name        string     `gorm:"size:100" json:"name"`
	Description string     `gorm:"type:text" json:"description"`
	URL         string     `gorm:"size:500" json:"url"`
	Image       string     `gorm:"size:500" json:"image"`
	SortOrder   int        `gorm:"column:sort_order;default:0" json:"sort_order"`
	CreatedAt   *time.Time `gorm:"column:created_at" json:"created_at"`
	UpdatedAt   *time.Time `gorm:"column:updated_at" json:"updated_at"`
}

func (Project) TableName() string { return "projects" }

// LoginLog 登录日志模型
type LoginLog struct {
	ID        int64      `gorm:"primaryKey;autoIncrement" json:"id"`
	UserID    int        `gorm:"column:user_id" json:"user_id"`
	Username  string     `gorm:"size:50" json:"username"`
	IP        string     `gorm:"size:50" json:"ip"`
	Country   string     `gorm:"size:100" json:"country"`
	Province  string     `gorm:"size:100" json:"province"`
	City      string     `gorm:"size:100" json:"city"`
	UserAgent string     `gorm:"column:user_agent;size:500" json:"user_agent"`
	LoginTime *time.Time `gorm:"column:login_time" json:"login_time"`
}

func (LoginLog) TableName() string { return "login_logs" }
