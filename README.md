# Starlore

> AI-Powered Personal Knowledge Universe — 融合 Multi-Agent 协作、云端内容智能体 (Harness)、RAG 知识检索、3D 空间星图与全链路可观测性的现代智能知识管理系统。

## 🌐 在线体验

- **Web 官方地址**: [https://www.starlore.cn](https://www.starlore.cn)
- **演示环境**: 支持访客免登录浏览文章、探索 3D 知识星图与体验基础 AI 交互。

---

## ✨ 核心特性

### 🤖 Multi-Agent 多智能体协作架构
基于 **LangGraph** 构建结构化编排引擎，实现 **Planner → Executor → Reviewer** 三角色自洽协作：
- **Planner（规划者）**：深度理解复杂意图，自动拆解子任务与依赖关系；
- **Executor（执行者）**：无依赖子任务异步并行执行，按需调度各类知识库检索与计算工具；
- **Reviewer（审查者）**：评估执行产物的完整性与准确性，提供自我纠错（PASS / REVISE / FAIL）与重试机制（最多 2 次）；
- **Few-Shot 反馈机制**：从沉淀的 Bad Case 中动态检索相似历史案例并注入 Prompt，实现智能体自适应进化。

### ⚡ 云端内容生产智能体 (Cloud Content Harness)
参考 Codex 与 DeepSeek-Harness 工业级设计，专为高价值内容生产打造的云端生产力中枢：
- **Turn & Step 状态机**：支持复杂任务的多轮长程推导与超时/循环熔断保护；
- **交付物一级公民 (Artifacts)**：深度打通原生排版引擎，一键从知识库生成专业商务 PPT（`python-pptx`）与结构化排版 Word（`python-docx`），直连 MinIO 生成签名下载；
- **极简透明可观测**：Codex 风格纯文字工具动作流（无边框卡片噪点）、思考链耗时折叠展开、实时 Token 遥测。

### 🌌 VR 3D 知识星域
- 基于 **Three.js** 构建沉浸式宇宙空间；
- 将离散的知识文章映射为星海中的星辰与星座节点，根据关联度形成知识星云引力场；
- 支持星际航行视角的平滑漫游探索与节点快速定位。

### 🧠 知识记忆与灵感沉淀 (`/knowledge-memory`)
- 碎片化想法快速捕获与沉淀；
- AI 智能萃取、自动提炼核心概念并生成结构化知识卡片（Knowledge Cards）；
- 支持卡片多维标签归档与知识库动态融合。

### 🔍 深度 RAG 检索与自动化质量评估
- **多格式解析切片**：支持从 PDF、Word (docx)、Markdown、纯文本自动解析并生成语义向量切片；
- **混合检索策略**：基于 FAISS / 向量库的高维语义检索 + 关键词精确回退召回；
- **对齐 RAGAS 的四维评测**：内置针对检索生成的端到端指标计算（Faithfulness 忠实度、Answer Relevance 回答相关度、Context Precision 召回精度、Context Recall 上下文召回率）。

### 🔌 MCP (Model Context Protocol) 开放工具生态
- 支持通过标准 MCP 协议动态接入本地受信任的 stdio 工具 Server；
- 内置集成**智谱官方实时联网搜索 MCP Server**，赋予智能体实时互联网检索能力；
- 统一的 MCP 工具注册、动态 Schema 发现与鉴权桥接中心。

### 🎙️ 多模态与双向语音交互
- **输入支持**：Markdown 文本、本地图片上传多模态理解，以及 **ASR 语音实时录音识别**；
- **输出支持**：SSE 流式渐进吐字 + **TTS 高拟真语音合成**；
- 实时工具调用状态折叠、代码高亮与公式渲染（KaTeX）。

### 🎭 AI 伴侣工坊 (Companion Studio)
- 提供沉浸式伴侣角色配置面板；
- 自由定制系统人设、提示词预设、模型发散温度（Temperature）与专属 TTS 音色；
- 会话上下文自动压缩与持久化记忆留存。

### 📄 简历工坊与 PDF 生成流水线
- 支持结构化简历信息编辑与实时富文本排版；
- 依托独立无头浏览器渲染微服务（`starlore-pdf`），实现精确到毫米级的分页断行控制与高质量 PDF 导出。

### 📊 全链路可观测性与 Bad Case 治理
- **Tracing 全链路记录**：精准追踪单次交互中所有模型的耗时、Token 消耗及工具路由；
- **Prompt 演进与版本追踪**：沉淀节点提示词演进历史；
- **Bad Case 收集与评测集构建**：一键归档异常交互，反哺 Few-Shot 样本池。

---

## 🏗️ 核心架构与技术栈

主仓库采用前后端解耦与微服务协作架构：

```mermaid
graph TD
    User([用户终端 / 浏览器]) --> Front[Web 核心端<br/>starlore-front (Vue 3.5 + Tailwind v4 + Three.js)]
    AdminUser([管理员]) --> Admin[管理后台<br/>starlore-admin (Vue 3 + Element Plus + ECharts)]

    Front & Admin -->|REST / SSE 网关| PyBack[Python 核心后端<br/>starlore-py (FastAPI + LangGraph + Harness)]
    
    PyBack -->|HTTP 内部调用| PdfService[PDF 渲染服务<br/>starlore-pdf (Express + Puppeteer)]
    PyBack -->|向量检索| FaissStore[(FAISS 向量索引)]
    PyBack -->|数据持久化| MySQL[(MySQL 关系数据库)]
    PyBack -->|对象存储 / 产物分发| MinIO[(MinIO OSS 集群)]
    PyBack -->|大模型调用| LLM[DeepSeek / Qwen / OpenAI 兼容接口]
    PyBack -->|Stdio 协议| MCPServers[外部 MCP Server / 智谱联网搜索]
```

### 组件技术栈详情

| 子工程 | 核心技术 | 职责说明 |
| :--- | :--- | :--- |
| **`starlore-front/`** | Vue 3.5 + Vite 7 + Tailwind CSS v4 + Element Plus + Three.js + Pinia + GSAP | 主 Web 前端（知识库阅读、3D VR 星图、Cloud Harness 交互界面、知识记忆、AI 伴侣工坊） |
| **`starlore-py/`** | FastAPI + SQLAlchemy 2.0 (async) + LangGraph + FAISS + RAGAS + MCP + python-pptx / docx | 统一 AI 后端（ReAct 多智能体状态机、Harness 内容生产、知识库向量切片、模型调度、用户业务） |
| **`starlore-admin/`** | Vue 3 + Vite + Element Plus + ECharts + WangEditor + Pinia | 后台管理系统（系统数据看板、文章与分类运维、AI 模型/Prompt 配置中心、登录安全审计） |
| **`starlore-pdf/`** | Node.js Express + Puppeteer + Mustache | 简历 PDF 生成独立微服务（跨平台无头浏览器渲染与毫米级排版生成） |

---

## 📁 项目目录结构

```
starlore/
├── starlore-front/          # Web 核心前端
│   └── src/
│       ├── api/             # REST 与 SSE API 接口封装
│       ├── components/      # 公共 UI 组件（玻璃拟态、导航栏等）
│       ├── router/          # 路由配置（包含 VR、Harness、记忆、简历等）
│       ├── stores/          # Pinia 状态管理
│       └── views/           # 页面视图（Home/Articles/VR/Harness/Echobot 等）
│
├── starlore-py/             # Python 核心后端
│   └── app/
│       ├── main.py          # FastAPI 应用入口
│       ├── config.py        # 环境变量与应用配置
│       ├── database.py      # 异步数据库引擎
│       ├── models/          # SQLAlchemy 实体模型（用户/文章/Harness会话/知识卡片等）
│       ├── schemas/         # Pydantic 请求与响应结构定义
│       ├── routers/         # API 路由层（harness/ai/articles/knowledge 等）
│       └── services/        # 核心业务服务（LangGraph/Harness/FAISS/MCP/RAGAS等）
│
├── starlore-admin/          # 后台管理前端
│   └── src/
│       ├── views/           # 仪表盘、文章分类管理、AI配置、用户列表、日志审计
│       └── router/          # 管理端路由
│
├── starlore-pdf/            # 简历 PDF 导出独立微服务
│   ├── index.js             # Express 服务入口与 Puppeteer 浏览器实例池
│   └── templates/           # Mustache 简历排版模板
│
├── docs/                    # 核心设计规范文档
│   └── starlore-cloud-harness-development-doc.md  # 云端智能体完整设计规范
│
├── docker-compose.yml       # 生产与开发环境全栈容器编排
└── .env.example             # 环境变量配置模板
```



---

## 🚀 快速开始

### 依赖环境准备

- **Node.js**: `>= 20.19.0`（推荐使用 `pnpm`）
- **Python**: `>= 3.11`
- **MySQL**: `>= 5.7` 或 `8.0`
- **MinIO**: 对象存储服务（用于文件管理与 Harness 产物交付）

---

### 1. 启动 Python 核心后端 (`starlore-py`)

```bash
cd starlore-py

# 创建虚拟环境并激活
python -m venv venv
# Windows:
venv\Scripts\activate
# Linux/macOS:
source venv/bin/activate

# 安装依赖
pip install -r requirements.txt

# 配置环境变量（根据 .env.example 配置数据库、MinIO 与 AI 模型密钥）
cp .env.example .env

# 启动服务（默认端口 5000）
uvicorn app.main:app --host 0.0.0.0 --port 5000 --reload
```

启动后访问 `http://localhost:5000/docs` 查看交互式 Swagger API 文档。

---

### 2. 启动 Web 核心前端 (`starlore-front`)

```bash
cd starlore-front

# 安装依赖
pnpm install

# 启动开发服务器（默认端口 5173）
pnpm dev
```

浏览器打开 `http://localhost:5173` 即可进入 Starlore 前端系统。

---

### 3. 启动后台管理系统 (`starlore-admin`)

```bash
cd starlore-admin

# 安装依赖
pnpm install

# 启动开发服务器
pnpm dev
```

---

### 4. 启动 PDF 导出服务 (`starlore-pdf`)

```bash
cd starlore-pdf

# 安装依赖
pnpm install

# 启动服务（默认监听端口 3001）
pnpm dev
```

---

### 5. 一键 Docker Compose 部署

项目根目录下提供了完整的 `docker-compose.yml` 配置：

```bash
# 复制环境变量并按实际部署填入密钥
cp .env.example .env

# 启动全部微服务与依赖组件（Python后端、前端、MinIO等）
docker compose up -d
```

---

## 📡 核心 API 端点概览

| 模块 | 方法 | 路径 | 描述 |
| :--- | :--- | :--- | :--- |
| **用户认证** | POST | `/api/auth/register` | 用户注册 |
| | POST | `/api/auth/login` | 用户登录并换取 JWT Token |
| | GET | `/api/auth/current` | 获取当前登录用户信息 |
| **云端智能体** | POST | `/api/harness/chat` | **Harness SSE 长程会话流**（自动规划、执行、生成 PPT/Word） |
| | GET | `/api/harness/sessions` | 获取用户的 Harness 历史会话列表 |
| | GET | `/api/harness/sessions/{id}/messages` | 获取指定会话的全量时间线消息与产物 |
| **AI 对话** | POST | `/api/ai/sse` | 标准基础单轮/多轮流式对话 |
| | POST | `/api/ai/agent-sse` | 单智能体 Tool Calling 对话流 |
| | POST | `/api/ai/multi-agent-sse` | **LangGraph Multi-Agent 编排对话流** (Planner-Executor-Reviewer) |
| | POST | `/api/ai/asr` | 语音录音上传与 ASR 文本转换 |
| | POST | `/api/ai/tts` | 文本转高质量语音流 |
| **知识切片与文档**| POST | `/api/knowledge/documents/upload` | 上传本地 PDF/Word/MD 解析并切片入库 |
| | GET | `/api/knowledge/chunks` | 查询向量知识库中的分块列表与元数据 |
| **知识记忆** | GET / POST | `/api/knowledge-memory/cards` | 知识卡片检索与创建 |
| **MCP 工具生态** | GET | `/api/ai/mcp/tools` | 动态发现当前已挂载的所有 MCP 外部工具 |
| | POST | `/api/ai/mcp/call` | 桥接执行指定的 MCP 工具 |
| **RAG 质量评测** | POST | `/api/ai/rag/evaluate` | 对齐 RAGAS 的 Faithfulness / Relevance 等四维评测 |
| **内容与文章** | GET / POST | `/api/articles` | 文章分页查询与新建 |
| | GET / PUT | `/api/articles/{id}` | 文章详情与更新 |
| **简历微服务** | POST | `/api/pdf/resume` | （通过 starlore-pdf 微服务）渲染导出简历 PDF |

---

## 📄 许可证

本项目采用 [MIT](LICENSE) 许可证开源。
