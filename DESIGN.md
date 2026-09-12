# Design — Starlore

> Last updated: 2026-07-30


## 设计哲学

Starlore 的设计语言融合了两个世界：**温暖文学气质**（默认主题）与**深空科幻美学**（暗黑/VR 场景）。核心原则是——界面如同漂浮在无限空间中的磨砂玻璃面板，光从半透明层间渗出，以光与影组织信息。

**场景感：** 你正驾驶一艘流线型飞船穿越星云。仪表盘由层叠的全息玻璃构成，每个面板柔和地发着光。星辰从舷窗外掠过。信息悬浮在半空中。

---

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue 3 (Composition API + `<script setup lang="ts">`) | 3.5 |
| 构建 | Vite | 7.3 |
| 样式 | Tailwind CSS v4 (@tailwindcss/vite) | 4.3 |
| 语言 | TypeScript | 5.9 |
| 路由 | vue-router | 4.6 |
| 状态 | Pinia (Composition API style) | 3.0 |
| HTTP | axios | 1.13 |
| 3D | Three.js | 0.184 |
| 动画 | GSAP | 3.15 |
| 图表 | ECharts | 6.0 |
| UI 库 | Element Plus (auto-import) | 2.13 |
| 图标 | Lucide Vue | 1.17 |
| 虚拟滚动 | vue-virtual-scroller | 2.0-beta |
| 富文本 | @wangeditor/editor-for-vue | - |
| 代码高亮 | highlight.js | 11.11 |
| 移动封装 | Capacitor (Android) | 8.3 |

---

## 明暗主题系统

当前实现只支持 `light` 和 `dark` 两套主题。`useThemeStore` 将主题名写入 `<html data-theme="light|dark">`，并通过 `localStorage: ro_blog_theme` 持久化。颜色、表面、边框和阴影均由 CSS Custom Properties 驱动。

每套主题内部包含 emerald、sky、rose、amber 等语义强调色；这些是同一主题内的功能色，不是可独立切换的主题。

| 主题 | 画布 | 主强调色 | 墨色 | 气质 |
|------|------|----------|------|------|
| **light** | `#EDF6FC` 晴空浅蓝 | `#DE4331` 日落红 | `#1A1410` 暖炭 | 温暖、轻盈、清晰 |
| **dark** | `#0A051F` 深空紫黑 | `#2A48F3` 星海蓝 | `#E6E8E8` 银白 | 深邃、沉浸、探索 |

---

## Design Tokens

### 颜色分类

```css
/* 画布 */
--canvas          /* 页面最底层背景 */
--canvas-deep     /* 更深的背景层次 */

/* 背景光晕 */
--orb-1, --orb-2, --orb-3   /* body 上的径向渐变球 */
--orb-glow                  /* 光晕发光色 */

/* 文字层级 */
--ink             /* 主文字 */
--ink-soft        /* 次要文字 */
--ink-muted       /* 辅助/meta 文字 */

/* 强调色 */
--accent          /* 主强调色 */
--accent-hover    /* 悬停态 */
--accent-soft     /* 浅底色 */

/* 暖色辅助 */
--warm, --warm-soft

/* 表面 */
--surface, --surface-hover

/* 边框 */
--border              /* 默认边框 */
--border-interactive  /* 可交互边框 */
--border-focus        /* 聚焦态 */

/* 标签/徽章 */
--tag-bg, --tag-hover
--badge-bg, --badge-border

/* 阴影 */
--shadow-sm, --shadow-card, --shadow-card-hover
--shadow-nav, --shadow-button, --shadow-button-hover

/* 圆角 */
--radius-sm:   6px
--radius-md:   10px
--radius-lg:   24px
--radius-xl:   32px
--radius-full: 9999px   /* 胶囊形 */

/* 过渡 */
--ease-out-quart: cubic-bezier(0.25, 1, 0.5, 1)
--transition:      0.25s
--transition-slow: 0.5s

/* 间距阶梯 */
--space-1: 4px  →  --space-2: 8px  →  --space-3: 12px
→ --space-4: 16px → --space-5: 24px → --space-6: 32px
→ --space-7: 48px → --space-8: 64px → --space-9: 96px
```

---

## 字体

### 字体栈

| 角色 | 字体栈 |
|------|--------|
| 正文（中文优先） | `'LXGW WenKai', 'Source Serif 4', 'Georgia', 'Noto Serif SC', serif` |
| 代码 | `'Fira Code', 'Consolas', monospace` |
| UI 辅助 | `'Inter', 'Noto Sans SC', system-ui, sans-serif` |

- Google Fonts 加载：Inter (400-700)、Noto Sans SC (400-700)、LXGW WenKai (300/400/700)、Source Serif 4 (variable)
- 基础字号 16px，行高 1.8
- 标题使用负字间距 (`-0.02em` ~ `-00.05em`) 营造紧凑现代感

### 排版阶梯

| 级别 | 大小 | 字重 | 字间距 | 用途 |
|------|------|------|--------|------|
| Display | clamp(2rem, 4vw, 2.8rem) | 800 | -0.05em | 页面大标题 |
| H1 | 24px | 700 | -0.03em | 区块标题 |
| H2 | 20px | 700 | -0.02em | 卡片标题 |
| H3 | 16px | 600 | 0 | 子区块 |
| Body | 15px | 400 | 0 | 正文 |
| Body small | 13px | 400 | 0 | 描述/meta |
| Caption | 12px | 400 | 0 | 标签/提示 |

正文最大行宽：65ch

---

## 布局

- 全局内容最大宽度：1080px；页面在小屏断点下折叠为单列
- 水平内边距：`clamp(16px, 4vw, 32px)`
- 卡片：20-24px 圆角，磨砂玻璃 + 彩色阴影光晕
- 导航栏：浮动胶囊条，`border-radius: 999px`，`backdrop-filter: blur(30px)`

---

## 玻璃系统

项目中存在两套玻璃效果方案，根据场景选择：

### 方案一：暖色磨砂（默认/浅色主题）

```css
.ink-glass-card {
  background: var(--surface);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}
```

### 方案二：深空玻璃（暗黑/VR 场景）

| 层级 | 透明度 | 模糊 | 用途 |
|------|--------|------|------|
| Base | 5% | 12px | 微妙背景、分割线 |
| Mid | 8% | 20px | 卡片、列表项 |
| Top | 11% | 28px | 导航、输入框、弹窗 |
| Bright | 16% | 36px | 悬停态、激活元素 |

```css
.glass-card {
  background: var(--glass-mid);           /* oklch(1 0 0 / 0.08) */
  backdrop-filter: blur(20px);
  border: 0.5px solid var(--glass-border); /* oklch(1 0 0 / 0.09) */
  border-radius: 20px;
  box-shadow:
    0 8px 20px rgba(0,0,0,0.23),
    0 20px 40px rgba(0,0,0,0.12);
}
```

---

## 深度模型

```
Layer 0: 宇宙底景（动态星空 + 星云光晕 / 暖色渐变光球）
Layer 1: 内容卡片（glass-mid / surface）
Layer 2: 导航 / 输入框（glass-top）
Layer 3: 按钮 / 标签（glass-bright / gradient）
Layer 4: 发光效果（box-shadow 穿透各层）
```

- 浅色主题：阴影带暖色调，不用纯黑
- 深色主题：阴影带青色/紫色色调，不用纯黑

---

## 组件规范

### 导航栏

- **桌面端：** 顶部浮动胶囊条，`backdrop-filter: blur(30px)`，居中，左右留白 20px
- **移动端：** 底部浮动胶囊导航栏，68px 高，28px 圆角
- 激活标签：彩色强调色 + 微妙光晕背景
- 未激活：ghost 文字色
- 主题切换器：桌面导航栏中的太阳/月亮图标按钮；移动端更多菜单沿用明暗模式切换

### 卡片

```css
.article-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  transition: box-shadow var(--transition), transform var(--transition);
}
.article-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}
```

- 需要强调交互性的内容卡可使用 `.starlore-spotlight`：鼠标位置通过 `--spot-x` / `--spot-y` 驱动径向柔光和 1.25px 局部边框高光
- 追光只在精确指针设备上更新坐标；触控设备保留静态卡片状态

### 按钮

- **Primary：** 胶囊形 (`border-radius: var(--radius-full)`)，强调色背景，悬停时微上浮 `translateY(-1px)`
- **Secondary：** 玻璃背景，图标 + 文字，无渐变
- **Icon 按钮：** 38px 方形，glass-mid，12px 圆角
- **深空风格 Primary：** 渐变 (cyan → purple)，26px 圆角，按下时彩色发光阴影

### 标签 / Chips

- 选中态：带色调背景 + 彩色边框 + 微妙光晕
- 未选中态：玻璃背景，muted 文字

### 云端智能体 (Cloud Content Harness) 组件规范

- **纯文字工具动作流 (`HarnessToolActionRow`)**：
  - 拒绝带边框卡片噪点，统一采用 Codex 样式的纯文本操作记录行；
  - 规格：`text-xs text-gray-500 leading-relaxed`，配合 14px Lucide 单色矢量图标与动宾短语；
  - 详细结果通过灰色缩进树形展开（`ChevronDown` 驱动，无边框包裹）。
- **思考链耗时折叠 (`HarnessThoughtBar`)**：
  - 实时展示“已思考 Xs”，点击展开灰度思维链正文；完成推导后自动收敛为微型时间胶囊。
- **产物交付卡片 (`HarnessArtifactCard`)**：
  - 位于答复末尾，整个长文本中唯一的显性结构化卡片；
  - 采用 `rounded-2xl` 大圆角、极轻微边框与微妙阴影，包含文件类型图标、文件大小、一键【立即下载】（MinIO 签名直链）与【复制链接】。

---

## 动效系统

### CSS 动画


| 动画名 | 效果 | 用途 |
|--------|------|------|
| `fadeInUp` | 淡入 + 上移 16px | 页面区块入场，**最主要的入场动画**，配合交错 `animation-delay` |
| `spin` | 360° 旋转 | 加载指示器 |
| `floatCard` / `floatCardTag` / `floatDot` | 微浮动 | 首页装饰性元素 |
| `breathe-text` | 透明度脉冲 | 交互提示文字 |
| `typing-bounce` | 三点跳动 | AI 打字指示器 |
| `nodeFloat` | 垂直微浮动 | 知识图谱节点 |
| `pulse` | 透明度脉冲 | 加载态 |
| `blink` | 光标闪烁 | 终端光标 |
| `trace-pulse` | 脉冲 | Agent 状态指示 |
| `ring-rotate` | 旋转 | VR 加载环 |
| `bar-slide` | 滑动 | 加载进度条 |
| `orbit-drift` | 星轨线稿缓慢平移、旋转和缩放 | 普通页面全局环境背景 |

### 过渡参数

- 页面入场：fade-in + slide-up (16px)，500ms `ease-out-quart`，交错 40-60ms
- 卡片悬停：`translateY(-2px)`，150ms
- 按钮按下：`scale(0.97)`，150ms
- 标签切换：颜色交叉淡入，200ms
- 背景：慢速漂移星云 (20s 循环)，闪烁星空

### Vue Transition

```html
<Transition name="ui-fade">      <!-- VR 覆层淡入 -->
<Transition name="loading-fade">  <!-- VR 加载屏 -->
<Transition name="modal-fade">    <!-- 弹窗 -->
<Transition name="panel-fade">    <!-- 侧边面板 -->
```

### GSAP

- VR 场景中用于相机平滑过渡：`gsap.to(camera.position, ...)`
- 星球悬停缩放：`gsap.to(mesh.scale, { x: 1.12, ... })`

### 减弱动效

```css
@media (prefers-reduced-motion: reduce) {
  * { animation-duration: 0.01ms !important; transition-duration: 0.01ms !important; }
}
```

---

## Canvas 2D 特效

| 组件 | 技术 | 描述 |
|------|------|------|
| `BlurredBubbles.vue` | Simplex 噪声 + 物理模拟 | 漂浮模糊光球，碰撞避免、速度阻尼、边界力，light/dark 两套调色板，6 FPS 节流 |
| `CosmicBackdrop.vue` | Canvas + SVG | 低密度闪烁星尘、稀疏点状流星和缓慢漂移的轨道线稿；页面隐藏时暂停，移动端降低星点数量 |
| `StellarDotsBand.vue` | Canvas 正弦波 | 首页首屏与内容区之间的三行点阵星河；离开视口时暂停 |
| `AICore.vue` | 2D Canvas + 透视投影 | 1200 粒子 3D 球体，鼠标拖拽旋转，状态响应变色（signal/core/void 三类粒子） |
| `ImmersiveMode.vue` | 全屏 Canvas | 800 粒子 3D 球 + 250 星星 + 多层正弦波浪 + 液态 blob 形态 |
| `StarfieldCanvas.vue` | Canvas + 视差 | 视差星空 + 流星 + 知识节点光晕 + 旋转虚线环 + 卫星点 |

---

## Three.js / WebGL 特效

| 组件 | 粒子数 | 核心技术 |
|------|--------|----------|
| `GeoNexusGlobe.vue` | 12,000 | 自定义 GLSL 着色器（idle/thinking/answering 三态顶点位移），UnrealBloomPass 后处理，加法混合 |
| `ParticleGlobe.vue` | ~10,000 | Fibonacci 球面分布，程序化大陆检测，Fresnel 大气着色器，贝塞尔曲线神经网络连接 + 脉冲点动画 |
| `FloatingParticles.vue` | 2,300 | 球壳粒子云，自定义着色器，鼠标轨道控制，滚动缩放 |
| VR Scene (`useVRScene.ts`) | ~500 | 线框八面体核心 + 发光精灵，轨道线框星球，Canvas 标签精灵，GSAP 相机动画，Raycaster 交互 |

---

## 沉浸模式 (Immersive Mode)

Echobot（AI 助手）拥有专属全屏沉浸模式：

- 全屏 Canvas 背景（粒子球 + 星空 + 波浪可视化）
- 语音输入 + 静音检测
- TTS 语音播放（`useTTS` composable，支持队列、预加载、浏览器降级）
- 侧边聊天面板，`mask-image` 渐变淡出
- `Escape` 键退出
- `mix-blend-mode: screen` 叠加混合

---

## 响应式设计

### 断点

| 断点 | 场景 |
|------|------|
| `600px` | 小屏微调 |
| `768px` | **主移动端断点**（导航栏切换为底部栏） |
| `900px` | 平板（网格列数折叠） |
| `1024px` | 中屏调整 |

### 模式

- **流式字号：** `font-size: clamp(2rem, 4vw, 2.8rem)`
- **流式内边距：** `padding: 0 clamp(16px, 4vw, 32px)`
- **CSS Grid 响应式列数：** `repeat(3, 1fr)` → `repeat(2, 1fr)` → `1fr`
- **多列布局：** 文章卡片 `columns: 2` + `break-inside: avoid`
- **全屏视图：** `max-height: 100vh; overflow: hidden`（Echobot、VR）
- **性能降级：** 移动端减少粒子数（`isMobile ? 100 : 250` 星星），Three.js 像素比上限 1.2（桌面 1.5）
- **普通页面环境层：** `CosmicBackdrop` 桌面 76 个星点、移动端 34 个；登录、Echobot 和 VR 页面不挂载
- **文章阅读反馈：** 文章详情页顶部使用 3px 固定进度线，按正文区域而非整页高度计算

---

## 状态管理

### Pinia Stores (Composition API style)

| Store | 职责 | 持久化 |
|-------|------|--------|
| `useThemeStore` | 主题名 → `data-theme` 属性同步 | `localStorage: ro_blog_theme` |
| `useUserStore` | JWT token、用户信息、登录/注册/登出 | `localStorage: ro_blog_token` |

- 视图级数据在组件内管理（无集中式文章/分类 store）
- 使用 `ref()` / `reactive()` 管理局部状态，`computed()` 派生状态

---

## 路由

### 14 条路由

| 路径 | 名称 | 认证 | 角色 |
|------|------|------|------|
| `/` | home | 访客可访问 | - |
| `/about` | about | 访客可访问 | - |
| `/categories` | categories | 访客可访问 | - |
| `/echobot` | echobot | 访客可访问 | - |
| `/diverge` | diverge | 访客可访问 | - |
| `/vr` | vr | 访客可访问 | - |
| `/login` | login | - | - |
| `/profile` | profile | 需要认证 | - |
| `/articles` | articles | 需要认证 | - |
| `/articles/:id` | articleDetail | 需要认证 | - |
| `/articles/edit/:id?` | articleEdit | 需要认证 | - |
| `/resume` | resume | 需要认证 | member, admin |
| `/resume/edit/:id?` | resumeEdit | 需要认证 | member, admin |
| `/:pathMatch(.*)*` | notFound | - | - |

### 导航守卫

- `guestAllowed: true` 的页面无需认证即可访问（含 demo 数据降级）
- 未认证访问受保护路由 → 重定向 `/login`
- 已认证访问 `/login` → 重定向 `/`
- 所有路由组件懒加载 `() => import(...)`

---

## API 集成

### Axios 实例 (`utils/request.ts`)

- 基础 URL：`/api`（Vite 代理到后端）
- 请求拦截器：附加 JWT `Authorization: Bearer <token>`
- 响应拦截器：解包 `result.data`，401 清除 token 并重定向，403 toast 通知并重定向首页
- 代理超时：120 秒

### API 服务模式

- 按领域拆分文件：`src/api/article.ts`、`auth.ts`、`ai.ts`、`diverge.ts`、`project.ts`、`resume.ts`
- 返回类型化 Promise：`request.get<never, { data: UserInfo }>('/auth/me')`
- TypeScript 接口与服务函数就近定义

### SSE 流式

- AI 聊天使用 `fetch()` + `ReadableStream` 实现 SSE 流式响应
- 手动解析：按 `\n` 分割，提取 `data:` 行，JSON 解析
- 应用于：多 Agent 聊天、图片分析、语音转录
- `AbortController` 取消支持

---

## CSS 方法论

### 原则

- **Scoped styles：** 每个 `.vue` 文件使用 `<style scoped>`
- **CSS Custom Properties：** 主题驱动的核心机制
- **语义化类名：** 无 BEM、无 Tailwind，自定义语义命名（`.article-card`、`.card-cover`、`.card-body`）
- **`:deep()`** 用于 `v-html` 渲染内容（Markdown 排版、聊天气泡）
- **`:global()`** 用于路由级 body/html 覆盖（如 Echobot 全屏模式）

### 全局样式文件

| 文件 | 职责 |
|------|------|
| `base.css` | Reset + 全部 Design Tokens |
| `main.css` | 工具类（`.page-container`、`.btn-primary`、`.fade-in-up` 等） |

### CSS 视觉技巧

| 技巧 | 用途 |
|------|------|
| `backdrop-filter: blur()` | 导航栏、弹窗、VR 覆层的磨砂玻璃 |
| `radial-gradient` | body 背景光球、发光效果 |
| `linear-gradient` | 文字渐变填充 (`-webkit-background-clip: text`)、波浪填充 |
| `box-shadow` | 温暖的多层阴影营造深度 |
| `mask-image` | 聊天面板边缘渐变淡出 |
| `mix-blend-mode: screen` | 波浪可视化的加法混合 |
| `oklch()` | 现代色彩空间操作 |

---

## 无障碍

### 已实现

- `aria-label` 用于交互元素（明暗主题按钮、菜单切换）
- `aria-hidden="true"` 用于装饰元素（背景 Canvas、分隔线）
- `rel="noopener noreferrer"` 用于外部链接
- `autocomplete` 用于登录/注册表单
- `loading="lazy"` 用于文章封面图
- `::selection` 自定义文字选择样式
- `@media (prefers-reduced-motion: reduce)` 禁用所有动画
- 键盘支持：`Escape` 关闭沉浸模式、`Ctrl+Z` 撤销、`Enter` 发送
- 语义化 HTML：`<nav>`、`<main>`、`<aside>`、`<footer>`、`<section>`、`<header>`

### 待改进

- 弹窗缺少 `role="dialog"`
- 折叠元素缺少 `aria-expanded`
- 无 skip-to-content 链接
- Canvas 元素缺少文本替代（已标记 `aria-hidden="true"`）

---

## 反模式

- 不把主题内的 emerald、sky、rose、amber 功能色误写成独立主题；当前只支持 light/dark
- 不使用衬线字体作为 UI 字体（正文用衬线营造文学感，UI 元素用无衬线）
- 不使用纯白背景（light 主题使用带色调的浅蓝画布）
- 不使用纯黑 `#000` 或纯白 `#fff`（所有中性色带宇宙/主题色调）
- 不使用无色阴影（所有阴影带色调）
- 不使用静态背景（每页都有动态光球或星空）
- 不使用无玻璃效果的纯色卡片
