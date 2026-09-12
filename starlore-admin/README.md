# Starlore 后台管理系统 (`starlore-admin`)

Starlore 个人知识宇宙的集中化运营控制台与系统管理平台，基于 **Vue 3.5 + Vite 7 + Element Plus + ECharts** 构建。用于系统监控大盘、文章与分类治理、AI 模型调度与 Prompt 配置以及登录安全审计。

---

## 🛠️ 技术栈

| 类别 | 技术方案 | 说明 |
| :--- | :--- | :--- |
| **基础框架** | Vue 3.5 (`<script setup lang="ts">`) | Composition API 现代化架构 |
| **构建工具** | Vite 7.3 + TypeScript 5.9 | 极速冷启动与热更新 |
| **组件库** | Element Plus 2.13 | 完整的企业级后台基础组件库 |
| **数据大屏** | ECharts 6.0 + vue-echarts | 统计图表可视化（访问折线、分类分布饼图等） |
| **富文本** | WangEditor 5.1 | 所见即所得文章内容编辑 |
| **状态管理** | Pinia 3.0 | 统一管理管理员认证 Token 与全站配置 |

---

## 📊 核心功能模块

1. **系统数据大屏 (`views/HomeView.vue`)**：
   - 知识库总文章数、分类数、用户总数以及 AI 对话调用频次统计；
   - 访问趋势动态折线图与文章分类占比饼图。
2. **AI 模型与 Prompt 配置中心 (`views/ai-config/`)**：
   - 动态配置 AI 厂商参数（DeepSeek / Qwen / OpenAI 等的 Base URL 与 API Key）；
   - 集中维护和版本追踪系统各个 Agent 节点的 System Prompt 提示词预设；
   - 零硬编码动态拉取模型列表。
3. **文章管理 (`views/articles/`)**：
   - 全站文章的分页检索、分类筛选、置顶与批量删除；
   - 内置富文本编辑器，支持在线调整文章内容与元数据。
4. **分类管理 (`views/categories/`)**：
   - 维护知识星域分类目录、图标名称与层级排序。
5. **用户治理 (`views/users/`)**：
   - 用户列表查询、状态启禁用与角色赋权（`admin` / `member` / `guest`）。
6. **安全登录审计 (`views/login-logs/`)**：
   - 记录每次登录的 IP 地址、地理位置解析、User-Agent 客户端标识与时间戳。

---

## 🚀 快速开始

### 1. 安装依赖

```bash
cd starlore-admin
pnpm install
```

### 2. 启动开发环境

```bash
pnpm dev
```

* 默认启动端口通常为 `http://localhost:5173`（或 Vite 自动顺延端口）。
* 接口代理配置于 `vite.config.ts`，开发时代理转发至 Python 后端服务（默认 `http://localhost:5000`）。

### 3. 生产打包

```bash
pnpm build
```
产物输出至 `dist/` 目录，可直接配合 Nginx 托管。
