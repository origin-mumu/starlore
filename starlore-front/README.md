# Starlore Web 核心前端 (`starlore-front`)

Starlore 个人知识宇宙的核心 Web 交互应用，基于 **Vue 3.5 + Vite 7 + Tailwind CSS v4** 构建。融合磨砂玻璃拟态美学与深空科幻质感，提供沉浸式 3D 知识星图、Cloud Content Harness 内容生产智能体、知识记忆与全功能多模态 AI 交互。

---

## 🛠️ 技术栈

| 类别 | 技术方案 | 作用说明 |
| :--- | :--- | :--- |
| **基础框架** | Vue 3.5 (`<script setup lang="ts">`) | 响应式 Composition API 核心 |
| **构建工具** | Vite 7.3 + TypeScript 5.9 | 极速冷启动与严格类型校验 |
| **样式引擎** | Tailwind CSS v4 (`@tailwindcss/vite`) | 现代原子化样式与 Design Token 集成 |
| **3D 引擎** | Three.js (0.184) | 3D 宇宙知识星图漫游与空间引力场渲染 |
| **动效库** | GSAP (3.15) | 平滑缓动相机飞行与页面过渡效果 |
| **UI 组件库** | Element Plus 2.13 + Lucide Vue | 精细化 UI 组件与统一现代线性图标 |
| **状态管理** | Pinia 3.0 | 模块化应用状态（用户态、主题、AI 会话等） |
| **Markdown 渲染** | Marked + highlight.js + KaTeX | 规范 Markdown 解析、代码高亮与 LaTeX 公式渲染 |
| **移动容器** | Capacitor 8.3 (Android) | 支持一键生成 Android 移动端容器包 |

---

## 🌌 核心页面与路由模块

* **`/` (首页)**：个人知识大盘，提供快速回到最近活动、进入写作、直达 AI 对话的高效路径。
* **`/vr` (3D 知识星图)**：Three.js 打造的三维空间星图，将文章和分类投影为恒星与星系，支持飞船视角的空间平滑穿梭。
* **`/harness` (云端内容生产智能体)**：
  * Codex 风格纯文字工具动作流（`HarnessToolActionRow`）；
  * 思考链耗时胶囊与折叠展开（`HarnessThoughtBar`）；
  * 专属大圆角 MinIO 产物交付卡片（`HarnessArtifactCard`），支持一键下载生成的专业 PPT 或 Word；
  * 左侧项目树分类的会话历史管理。
* **`/knowledge-memory` (知识记忆)**：灵感便签捕获、AI 概念萃取与知识卡片归档。
* **`/echobot` (AI 助手与伴侣工坊)**：
  * 支持文本、图片与 ASR 语音输入的 SSE 渐进式对话；
  * 内置 **AI 伴侣工坊**：自由定制伴侣形象、提示词人设、发散温度与专属 TTS 音色。
* **`/articles` & `/articles/edit/:id?`**：文章流式瀑布流、分类筛选以及富文本/Markdown 编辑。
* **`/resume` & `/resume/edit/:id?`**：结构化简历数据录入、排版调优与直通 `starlore-pdf` 导出。
* **`/diverge` (创意发散)**：基于关键词发散的树状思维导图。

---

## 🚀 快速开始

### 1. 安装依赖

推荐使用 `pnpm` 包管理器：

```bash
cd starlore-front
pnpm install
```

### 2. 启动开发服务器

```bash
pnpm dev
```

* 默认本地访问地址：`http://localhost:5173`
* 接口代理配置位于 `vite.config.ts`，默认代理至后端的 `http://localhost:5000`。

### 3. 类型检查与生产构建

```bash
# 运行 TypeScript 类型检查并打包
pnpm build

# 本地预览生产构建产物
pnpm preview
```

---

## 🎨 设计系统与明暗主题

* 采用双主题切换：`light`（温暖晴空蓝与日落红强调）与 `dark`（深空暗紫与星海蓝光晕）；
* 样式统一依托 CSS Custom Properties 与 Tailwind CSS v4 驱动；
* 核心卡片使用半透明磨砂玻璃层叠规范（`.glass-card`、`.ink-glass-card`）。
