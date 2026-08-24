# Starlore Python 后端

Starlore 个人知识管理系统的 Python 后端实现，基于 FastAPI 框架，与原 Spring Boot 版本 API 完全兼容。

## 技术栈

| 组件 | 技术 |
|------|------|
| Web 框架 | FastAPI |
| ORM | SQLAlchemy 2.0 (async) |
| 数据库 | MySQL (aiomysql 驱动) |
| 数据校验 | Pydantic v2 |
| JWT 认证 | python-jose |
| 密码哈希 | bcrypt |
| 文件存储 | MinIO |
| AI 调用 | httpx (OpenAI 兼容 API) |
| SSE 流式 | sse-starlette |
| ASGI 服务器 | uvicorn |

## 快速开始

### 1. 安装依赖

```bash
cd starlore-py
pip install -r requirements.txt
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 填入数据库密码等配置
```

### 3. 启动服务

```bash
python -m app.main
```

或使用 uvicorn：

```bash
uvicorn app.main:app --host 0.0.0.0 --port 5000 --reload
```

服务启动后访问 http://localhost:5000/docs 查看 API 文档。

## 目录结构

```
starlore-py/
├── app/
│   ├── main.py          # 应用入口
│   ├── config.py         # 配置管理
│   ├── database.py       # 数据库连接
│   ├── dependencies.py   # 依赖注入
│   ├── exceptions.py     # 异常处理
│   ├── middleware.py      # 中间件
│   ├── security.py       # JWT & 密码
│   ├── models/           # ORM 模型 (10 个)
│   ├── schemas/          # Pydantic Schema (29 个)
│   ├── routers/          # API 路由 (11 个)
│   └── services/         # 业务逻辑 (13 个)
├── resources/            # 静态资源
├── data/                 # 运行时数据
├── .env.example          # 环境变量模板
├── requirements.txt      # Python 依赖
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


