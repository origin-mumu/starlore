# Starlore Python 核心后端 (`starlore-py`)

Starlore 个人知识宇宙的统一核心 AI 后端服务，基于 **FastAPI** 异步架构开发。集成了 LangGraph 多智能体协同、Cloud Content Harness 内容生产、RAG 向量检索与自动化评测、MCP 工具开放协议及生产级全链路可观测性。

---

## 🛠️ 技术栈

| 层次 / 组件 | 选用技术 | 说明 |
| :--- | :--- | :--- |
| **Web 核心** | FastAPI (ASGI) + Uvicorn | 高性能异步 REST & SSE 流式通信 |
| **持久化 ORM** | SQLAlchemy 2.0 (AsyncIO) + aiomysql | 异步非阻塞 MySQL 驱动 |
| **数据契约** | Pydantic v2 | 严格请求/响应校验与 Schema 生成 |
| **多智能体编排** | LangChain + LangGraph | Planner-Executor-Reviewer 自适应协作图状态机 |
| **云端内容生产** | Harness ReAct 引擎 + python-pptx + python-docx | 深度生产 PPT、结构化 Word 并直连 MinIO 交付 |
| **RAG 向量切片** | FAISS + 智谱 Embedding / OpenAI Embedding | 文本多分块解析与高维向量快速召回 |
| **质量评测** | RAGAS 对齐指标 (Faithfulness, Relevance 等) | LLM-as-a-judge 检索回答多维质量评测 |
| **工具开放协议**| MCP (Model Context Protocol) 客户端 | 动态挂载 stdio Server 与智谱官方联网搜索 |
| **对象存储** | MinIO Python SDK | 云端文件管理与 Harness 产物直链分发 |
| **多模态与语音**| OpenAI 兼容 Vision 接口 + ASR/TTS 引擎 | 多模态图片理解与语音双向交互 |
| **认证与安全** | python-jose (JWT) + Passlib (Bcrypt) | 角色权限校验与无状态 Token 签发 |

---

## 🚀 快速开始

### 1. 创建虚拟环境并安装依赖

```bash
cd starlore-py

# 推荐 Python 3.11+
python -m venv venv

# Windows
venv\Scripts\activate
# Linux/macOS
source venv/bin/activate

# 安装依赖
pip install -r requirements.txt
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 文件，填写数据库连接、MinIO 访问密钥、AI 模型 Base URL 与 API Key
```

### 3. 启动开发服务器

```bash
uvicorn app.main:app --host 0.0.0.0 --port 5000 --reload
```

* 默认服务端口：`5000`
* 交互式 API 文档 (Swagger UI)：`http://localhost:5000/docs`
* 备选 Redoc 格式文档：`http://localhost:5000/redoc`

---

## 📁 目录结构与组件架构

```
starlore-py/
├── app/
│   ├── main.py                  # 应用初始化、中间件注册、路由集中挂载
│   ├── config.py                # Pydantic 驱动的统一环境变量读取
│   ├── database.py              # 异步 Engine 与 SessionLocal 工厂
│   ├── dependencies.py          # FastAPI 鉴权依赖注入与上下文管理
│   ├── exceptions.py            # 全局异常类与处理器
│   ├── middleware.py             # 耗时统计、CORS 与请求日志中间件
│   ├── security.py              # JWT 编码解码与密码 Hash 加密
│   ├── models/                  # SQLAlchemy ORM 实体层 (共 19 个模型)
│   │   ├── user.py              # 用户与角色模型
│   │   ├── article.py           # 文章与元数据
│   │   ├── article_chunk.py     # 知识切片与向量映射
│   │   ├── harness_session.py   # Harness 会话状态
│   │   ├── harness_message.py   # Harness 事件流与产物记录
│   │   ├── knowledge_card.py    # 知识记忆卡片
│   │   └── ...
│   ├── schemas/                 # Pydantic 数据契约 (共 11 个模块)
│   │   ├── common.py, auth.py, article.py, harness.py, knowledge_memory.py ...
│   ├── routers/                 # API 端点路由层 (共 19 个路由模块)
│   │   ├── auth.py              # 用户登录、注册、个人资料
│   │   ├── articles.py          # 文章 CRUD 与分类筛选
│   │   ├── ai.py                # 基础与多模态流式对话
│   │   ├── agent.py             # 单智能体 Tool Calling 对话
│   │   ├── multi_agent.py       # LangGraph 多智能体编排
│   │   ├── harness.py           # 云端生产智能体全生命周期交互
│   │   ├── knowledge_memory.py  # 知识记忆卡片管理
│   │   ├── chunks.py            # 向量切片管理
│   │   ├── resume.py            # 简历结构化数据存储
│   │   ├── admin.py             # 后台系统统计与用户治理
│   │   └── ...
│   └── services/                # 业务调度核心 (共 28 个服务模块)
│       ├── langgraph_agent.py   # LangGraph 状态机构建
│       ├── harness_service.py   # Harness 会话与事件生命周期
│       ├── harness_agent_service.py # Harness ReAct 循环推导
│       ├── harness_content_tools.py # PPTX 与 DOCX 原生渲染生成
│       ├── mcp_client.py        # MCP 协议工具动态发现与调用
│       ├── ragas_evaluator.py   # RAG 端到端四维质量评测
│       ├── article_embedding_service.py # 向量切片生成与 FAISS 索引
│       └── ...
├── data/                        # 本地运行时数据（FAISS 向量索引持久化目录）
├── resources/                   # 静态模板与素材
├── requirements.txt             # 锁定依赖项
└── .env.example                 # 环境变量模板
```

---

## ⚡ 核心功能系统

### 1. Cloud Content Harness 智能体
- 端到端 ReAct 调度循环，实时吐出 SSE 结构化事件（`thought`、`tool_call`、`tool_result`、`artifact`、`message`）；
- 具备安全步数熔断与超时重试机制；
- 产物生成后自动压入 MinIO 存储桶，向前端交付具备带时效签名下载链接的卡片元数据。

### 2. Multi-Agent 编排与 LangSmith 观测
- 实现 Planner（规划拆解） $\rightarrow$ Executor（并行工具执行） $\rightarrow$ Reviewer（质检自纠）全流程闭环；
- 支持将调用链路一键打上 LangSmith 追踪标签，对 Token 消耗与节点耗时进行深度画像。

### 3. RAG 知识库与切片评估
- 支持通过 `/api/knowledge/documents/upload` 接收各类文档格式，自动调用 `file_parse_service` 提取纯文本并进行滑动窗口分块切片；
- 通过 `/api/ai/rag/evaluate` 提供忠实度（Faithfulness）与相关度（Answer Relevance）量化指标。
