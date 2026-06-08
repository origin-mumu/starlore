package config

import (
	"log"
	"os"
	"strconv"

	"github.com/joho/godotenv"
)

type appConfig struct {
	Port int
}

type dbConfig struct {
	Host     string
	Port     int
	User     string
	Password string
	Name     string
}

type jwtConfig struct {
	Secret     string
	Expiration int64 // 毫秒
}

type minioConfig struct {
	Endpoint   string
	PublicURL  string
	AccessKey  string
	SecretKey  string
	Bucket     string
}

type aiConfig struct {
	APIKey  string
	BaseURL string
	Model   string
}

var (
	App   appConfig
	DB    dbConfig
	JWT   jwtConfig
	MinIO minioConfig
	AI    aiConfig
)

func Load() {
	_ = godotenv.Load()

	App = appConfig{
		Port: getEnvInt("APP_PORT", 5000),
	}

	DB = dbConfig{
		Host:     getEnv("DB_HOST", "127.0.0.1"),
		Port:     getEnvInt("DB_PORT", 3306),
		User:     getEnv("DB_USER", "ro-blog"),
		Password: getEnv("DB_PASSWORD", ""),
		Name:     getEnv("DB_NAME", "ro-blog"),
	}

	JWT = jwtConfig{
		Secret:     getEnv("JWT_SECRET", "ro-blog-secret-key-2024"),
		Expiration: int64(getEnvInt("JWT_EXPIRATION", 604800000)),
	}

	MinIO = minioConfig{
		Endpoint:  getEnv("MINIO_ENDPOINT", "http://47.94.128.65:9000"),
		PublicURL: getEnv("MINIO_PUBLIC_URL", "https://www.robin-blog.cn/minio"),
		AccessKey: getEnv("MINIO_ACCESS_KEY", "minioadmin"),
		SecretKey: getEnv("MINIO_SECRET_KEY", "minioadmin123456"),
		Bucket:    getEnv("MINIO_BUCKET", "my-files"),
	}

	AI = aiConfig{
		APIKey:  getEnv("AI_API_KEY", ""),
		BaseURL: getEnv("AI_BASE_URL", "https://api.deepseek.com"),
		Model:   getEnv("AI_MODEL", "deepseek-chat"),
	}

	log.Println("配置加载完成")
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

func getEnvInt(key string, fallback int) int {
	if v := os.Getenv(key); v != "" {
		if i, err := strconv.Atoi(v); err == nil {
			return i
		}
	}
	return fallback
}
