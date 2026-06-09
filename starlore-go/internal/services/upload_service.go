package services

import (
	"context"
	"fmt"
	"net/http"
	"path/filepath"
	"strings"
	"sync"

	"starlore-go/internal/config"
	"starlore-go/internal/utils"

	"github.com/gin-gonic/gin"
	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
)

var (
	minioOnce   sync.Once
	minioClient *minio.Client
)

func getMinioClient() *minio.Client {
	minioOnce.Do(func() {
		endpoint := strings.TrimPrefix(strings.TrimPrefix(config.MinIO.Endpoint, "https://"), "http://")
		secure := strings.HasPrefix(config.MinIO.Endpoint, "https")
		client, err := minio.New(endpoint, &minio.Options{
			Creds:  credentials.NewStaticV4(config.MinIO.AccessKey, config.MinIO.SecretKey, ""),
			Secure: secure,
		})
		if err != nil {
			fmt.Printf("MinIO 初始化失败: %v\n", err)
			return
		}
		minioClient = client
	})
	return minioClient
}

// UploadImage 上传图片
func UploadImage(c *gin.Context) {
	file, err := c.FormFile("file")
	if err != nil {
		utils.Fail(c, http.StatusBadRequest, "文件上传失败")
		return
	}

	// 校验文件类型
	allowedExts := map[string]bool{".jpg": true, ".jpeg": true, ".png": true, ".gif": true, ".webp": true}
	ext := strings.ToLower(filepath.Ext(file.Filename))
	if !allowedExts[ext] {
		utils.Fail(c, http.StatusBadRequest, "仅支持 jpg/jpeg/png/gif/webp 格式")
		return
	}

	f, err := file.Open()
	if err != nil {
		utils.Fail(c, http.StatusInternalServerError, "文件打开失败")
		return
	}
	defer f.Close()

	objectName := fmt.Sprintf("%s%s", utils.GenerateUUID(), ext)

	client := getMinioClient()
	if client == nil {
		utils.Fail(c, http.StatusInternalServerError, "MinIO 未配置")
		return
	}

	_, err = client.PutObject(
		context.Background(),
		config.MinIO.Bucket,
		objectName,
		f,
		file.Size,
		minio.PutObjectOptions{ContentType: file.Header.Get("Content-Type")},
	)
	if err != nil {
		utils.Fail(c, http.StatusInternalServerError, "文件上传失败")
		return
	}

	url := fmt.Sprintf("%s/%s", config.MinIO.PublicURL, objectName)
	utils.Ok(c, "上传成功", gin.H{"url": url})
}
