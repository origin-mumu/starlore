# Starlore Go 后端

Starlore 个人知识管理系统的 Go 后端实现，基于 Gin 框架，与原 Spring Boot 版本 API 完全兼容。

## 技术栈

| 组件 | 技术 |
|------|------|
| Web 框架 | Gin |
| ORM | GORM |
| 数据库 | MySQL |
| JWT 认证 | golang-jwt |
| 密码哈希 | bcrypt |
| 文件存储 | MinIO |
| AI 调用 | net/http (OpenAI 兼容 API) |

## 快速开始

### 1. 安装依赖

```bash
cd starlore-go
go mod tidy
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 填入数据库密码等配置
```

### 3. 启动服务

```bash
go run cmd/server/main.go
```

## 目录结构

```
starlore-go/
├── cmd/server/          # 应用入口
├── internal/
│   ├── config/          # 配置管理
│   ├── database/        # 数据库连接
│   ├── models/          # GORM 模型 (10 个)
│   ├── schemas/         # 请求/响应结构体
│   ├── services/        # 业务逻辑 (13 个)
│   ├── routers/         # 路由注册
│   ├── middleware/       # 中间件 (CORS + 认证)
│   └── utils/           # 工具函数 (JWT、密码、响应)
├── resources/           # 静态资源
├── data/                # 运行时数据
├── .env.example         # 环境变量模板
├── go.mod               # Go 模块定义
└── README.md
```

## API 端点

与原 Java 后端完全兼容，前端无需任何改动：

- `GET /api/health` — 健康检查
- `POST /api/auth/register` — 注册
- `POST /api/auth/login` — 登录
- `GET /api/articles` — 文章列表
- `POST /api/ai/sse` — AI 流式对话
- ... 以及所有其他端点

## 与 Java/Python 版的差异

1. **轻量级**: Go 编译为单一二进制文件，无运行时依赖
2. **高并发**: Go 原生协程支持，天然适合高并发场景
3. **SSE 流式**: 使用 Gin 的 SSE 支持 + bufio.Scanner 流式读取
4. **无 ORM 迁移**: 直接连接现有数据库表，无需迁移
