package main

import (
	"fmt"
	"log"
	"starlore-go/internal/config"
	"starlore-go/internal/database"
	"starlore-go/internal/routers"
	"starlore-go/internal/middleware"

	"github.com/gin-gonic/gin"
)

func main() {
	config.Load()

	database.Connect()

	r := gin.Default()

	middleware.SetupCORS(r)
	r.Use(middleware.AuthMiddleware())

	routers.RegisterAll(r)

	addr := fmt.Sprintf(":%d", config.App.Port)
	log.Printf("Starlore 后端服务启动，端口 %d", config.App.Port)
	if err := r.Run(addr); err != nil {
		log.Fatalf("启动失败: %v", err)
	}
}
