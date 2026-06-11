# Starlore

> AI-Powered Personal Knowledge Universe — 融合 Multi-Agent 协作、LangSmith 可观测性、RAG 知识检索、3D 可视化的智能知识管理系统。

## 🌐 在线演示

- **前端地址**: https://www.starlore.cn

## ✨ 核心特性

### 🤖 Multi-Agent 多智能体协作架构

引入 LangGraph 重构单体 Agent 链路，实现 **Planner → Executor → Reviewer** 三角色协作编排：

```
用户提问
  │
  ▼
Planner（规划者）── 分析意图，自动拆解子任务
  │
  ▼
Executor（执行者）── 并行调用 Function 工具，支持依赖调度
  │
  ▼
Reviewer（审查者）── 检查结果完整性，自动纠错重试（最多 2 次）
  │
  ▼
最终回答
```

- 子任务自动拆解与依赖分析
- 无依赖任务并行执行
- 结果自我纠错（PASS / REVISE / FAIL）
- Few-Shot 动态反馈机制

### 📊 LangSmith 全链路可观测性

集成 LangSmith 构建大模型监控体系：

- **全链路 Tracing**：精准记录每次 LLM 调用的 Token 消耗、Function 路由耗时
- **Prompt 演进追踪**：记录每个 Agent 节点的 Prompt 变化
- **Bad Case 沉淀**：自动收集失败案例，建立测试集
- **Few-Shot 反馈**：从历史 Bad Case 中检索相似案例，注入 Executor Prompt

### 🔍 RAG 知识检索

- 向量化知识库（Zhipu Embedding + SimpleVectorStore / FAISS）
- 语义搜索 + 关键词回退
- 文章自动索引与增量更新

### 🌌 VR 知识星图

- Three.js 驱动的 3D 可视化
- 将知识映射为星辰节点
- 沉浸式星域探索

### 💡 创意发散引擎

- AI 驱动的思维导图
- 从关键词发散出无限可能
- 辅助创意和决策

### 🎙️ 多模态 AI 对话

- 文本 / 图片 / 语音多模态输入
- SSE 流式响应
- 工具调用状态实时展示
- Agent 执行追踪嵌入聊天记录

## 🏗️ 技术栈

### 前端 (`starlore-front/`)

| 技术                  | 用途          |
| --------------------- | ------------- |
| Vue 3 + TypeScript    | 框架          |
| Vite                  | 构建工具      |
| Pinia                 | 状态管理      |
| Vue Router            | 路由          |
| Lucide                | 图标库        |
| Three.js              | 3D 可视化     |
| Marked + highlight.js | Markdown 渲染 |

### Java 后端 (`starlore-back/`)

| 技术               | 用途             |
| ------------------ | ---------------- |
| Spring Boot 3.4.5  | 框架             |
| Spring AI 1.0.0-M6 | AI/LLM 集成      |
| MyBatis-Plus       | ORM              |
| MySQL              | 数据库           |
| JWT                | 认证鉴权         |
| MinIO              | 文件存储         |
| LangChain4j        | Agent Graph 引擎 |
| LangSmith          | 可观测性 Tracing |

### Python 后端 (`starlore-py/`)

| 技术                  | 用途                   |
| --------------------- | ---------------------- |
| FastAPI               | 异步 Web 框架          |
| LangChain + LangGraph | Agent 编排             |
| LangSmith             | Tracing                |
| FAISS                 | 向量存储               |
| RAGAS                 | RAG 质量评估           |
| MCP                   | Model Context Protocol |

### 移动端 (`starlore_app/`)

| 技术    | 用途       |
| ------- | ---------- |
| Flutter | 跨平台框架 |
| Dart    | 编程语言   |

## 📁 项目结构

```
starlore/
├── starlore-front/          # Vue 3 前端
│   └── src/
│       ├── api/             # API 接口
│       ├── components/      # 公共组件
│       ├── composables/     # 组合式函数（TTS 等）
│       ├── router/          # 路由配置
│       ├── views/           # 页面组件
│       └── stores/          # Pinia 状态
│
├── starlore-back/           # Spring Boot 后端
│   └── src/main/java/com/robin/blogback/
│       ├── agent/           # Multi-Agent 角色（Planner/Executor/Reviewer）
│       ├── config/          # 配置类（Agent/LangSmith/Security）
│       ├── controller/      # REST 控制器
│       ├── entity/          # 实体类
│       ├── graph/           # Agent Graph 状态机引擎
│       ├── mapper/          # MyBatis-Plus Mapper
│       ├── observability/   # LangSmith Tracing / BadCase / Metrics
│       ├── service/         # 业务逻辑（BlogTools / RAG / AI）
│       └── util/            # 工具类
│
├── starlore-py/             # FastAPI 后端（并行实现）
│   └── app/
│       ├── routers/         # API 路由
│       └── services/        # LangGraph Agent / LangSmith / MCP / RAGAS
│
├── starlore_app/            # Flutter 移动端
│   └── lib/
│
└── starlore-admin/          # 管理后台
```

## 🚀 快速开始

### 环境要求

| 组件    | 版本要求        |
| ------- | --------------- |
| JDK     | >= 17           |
| Node.js | >= 16.0.0       |
| MySQL   | >= 5.7          |
| Python  | >= 3.10（可选） |
| Flutter | >= 3.0（可选）  |

### 后端启动 (starlore-back)

```bash
cd starlore-back

# 配置数据库（修改 application.yaml 或设置环境变量）
# DB_HOST, DB_USER, DB_PASS

# 启动
./mvnw spring-boot:run
```

### 前端启动 (starlore-front)

```bash
cd starlore-front
npm install
npm run dev
```

### Python 后端启动 (starlore-py)

```bash
cd starlore-py
pip install -r requirements.txt

# 配置 .env（LANGCHAIN_API_KEY, DB_URL 等）
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

### 移动端启动 (starlore_app)

```bash
cd starlore_app
flutter pub get
flutter run
```

## 📡 API 接口

### 文章管理

| 方法   | 路径                | 说明                           |
| ------ | ------------------- | ------------------------------ |
| GET    | `/api/articles`     | 获取文章列表（分页/搜索/筛选） |
| GET    | `/api/articles/:id` | 获取文章详情                   |
| POST   | `/api/articles`     | 创建文章                       |
| PUT    | `/api/articles/:id` | 更新文章                       |
| DELETE | `/api/articles/:id` | 删除文章                       |

### 分类管理

| 方法 | 路径              | 说明         |
| ---- | ----------------- | ------------ |
| GET  | `/api/categories` | 获取分类列表 |
| POST | `/api/categories` | 创建分类     |

### AI 对话

| 方法 | 路径                      | 说明                                                  |
| ---- | ------------------------- | ----------------------------------------------------- |
| POST | `/api/ai/sse`             | 基础 SSE 对话                                         |
| POST | `/api/ai/agent-sse`       | 单 Agent SSE 对话（Tool Calling）                     |
| POST | `/api/ai/multi-agent-sse` | **Multi-Agent SSE 对话**（Planner-Executor-Reviewer） |
| POST | `/api/ai/thinking-sse`    | 推理模型 SSE 对话                                     |
| POST | `/api/ai/analyze-image`   | 图片分析                                              |
| POST | `/api/ai/tts`             | 文本转语音                                            |

### Multi-Agent 可观测性

| 方法 | 路径                      | 说明               |
| ---- | ------------------------- | ------------------ |
| GET  | `/api/ai/agent-metrics`   | Agent 执行指标统计 |
| GET  | `/api/ai/agent-traces`    | 执行追踪记录       |
| GET  | `/api/ai/agent-bad-cases` | Bad Case 列表      |

## 🧩 Multi-Agent 工作流

### Graph 拓扑

```java
AgentGraph.builder()
    .node("planner",      plannerAgent)
    .node("executor",     executorAgent)
    .node("reviewer",     reviewerAgent)
    .node("synthesizer",  synthesizerNode)
    .edge(START,          "planner")
    .edge("planner",      "executor")
    .edge("executor",     "reviewer")
    .conditionalEdge("reviewer", state -> {
        if (PASS)    → "synthesizer"
        if (REVISE)  → "executor" (retry)
        if (FAIL)    → END
    })
    .compile("planner")
```

### SSE 事件类型

| 事件             | 说明                      |
| ---------------- | ------------------------- |
| `plan_start`     | Planner 开始分析          |
| `plan`           | 规划完成，返回子任务列表  |
| `subtask_start`  | 子任务开始执行            |
| `subtask_result` | 子任务执行完成            |
| `review`         | Reviewer 审查结果         |
| `metrics`        | Token/耗时/LangSmith 指标 |
| `content`        | 最终回答内容              |
| `done`           | 流程结束                  |

## 📊 可观测性架构

```
用户请求
  │
  ├─→ Multi-Agent Graph
  │     ├─ Planner  ──→ LangSmith Tracer ──→ Token/耗时
  │     ├─ Executor ──→ LangSmith Tracer ──→ 工具调用记录
  │     └─ Reviewer ──→ LangSmith Tracer ──→ 决策记录
  │
  ├─→ BadCase Collector ──→ ai_bad_cases 表
  │                           │
  │                           └─→ Few-Shot 反馈 ──→ Executor Prompt
  │
  └─→ Agent Metrics ──→ 指标看板（节点耗时/通过率/Token 统计）
```

## 🎯 功能清单

### 已完成

- [x] Multi-Agent 协作架构（Planner-Executor-Reviewer）
- [x] Agent Graph 状态机引擎（条件边/并行执行/重试）
- [x] LangSmith 全链路 Tracing
- [x] Bad Case 收集与 Few-Shot 动态反馈
- [x] Agent 执行指标统计
- [x] RAG 语义搜索（Zhipu Embedding + Vector Store）
- [x] 多模态 AI 对话（文本/图片/语音）
- [x] SSE 流式响应 + 工具调用状态展示
- [x] VR 3D 知识星图（Three.js）
- [x] 创意发散引擎
- [x] JWT 认证 + 角色权限
- [x] MinIO 文件存储
- [x] Flutter 移动端
- [x] Python 后端（LangGraph + MCP + RAGAS）

## 🤝 贡献指南

1. Fork 本项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系方式

- 项目地址: https://github.com/origin-mumu/starlore

---

⭐ 如果这个项目对您有帮助，请给个 Star 支持一下！
