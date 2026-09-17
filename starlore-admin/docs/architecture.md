# Starlore Admin 架构说明

基于 `.agents/skills/web-app-framework/SKILL.md` 规范重构的博客后台管理端。

## 技术栈

Vue 3 + TypeScript + Vite + Pinia + Vue Router + ECharts + wangEditor + Element Plus（图标与 ElMessage）。

## 目录结构

```text
src/
├── api/                  # 接口请求层（全部基于泛型 http 助手，返回类型化 VO）
│   ├── article.ts        # 星记 / 星域 / 统计
│   ├── user.ts           # 观星者管理
│   ├── ai-config.ts      # AI 模型配置
│   └── auth.ts           # 登录 / 登录日志
├── assets/               # 静态资源
├── components/
│   ├── common/           # 原子组件：AppModal（纯白弹窗）、StatCard、EmptyState、LoadingState
│   └── business/         # 业务组件：AppSidebar（悬浮胶囊毛玻璃侧边栏）
├── composables/          # 组合式逻辑：useECharts（含 resize 监听与实例销毁托管）、useArticleEditor
├── layouts/
│   └── AppLayout.vue     # 全局唯一外壳：侧边栏 + <router-view>
├── router/
│   ├── routes.ts         # 路由表（业务页面全部嵌套在 AppLayout 下）
│   ├── guards.ts         # 登录态守卫（读取 Pinia user store）
│   └── index.ts
├── stores/
│   └── user.ts           # 会话凭据（token / role）与登出
├── styles/
│   ├── tokens.css        # 设计令牌：品牌色阶、语义色、毛玻璃表面、负 Spread 弥散阴影、圆角/动效阶梯
│   └── main.css          # Reset、布局外壳、Bento 卡片、按钮体系、表格、表单、徽章等公共类
├── types/
│   └── index.ts          # 全局 DTO/VO 契约（严禁 any）
└── views/                # 页面容器（不含侧边栏 DOM，经路由插入 AppLayout）
    ├── login/LoginView.vue
    ├── dashboard/DashboardView.vue
    ├── articles/ArticleListView.vue / ArticleFormView.vue（新增与编辑共用，按路由参数区分）
    ├── categories/CategoryListView.vue
    ├── users/UserListView.vue
    ├── ai-config/AiConfigView.vue
    └── login-logs/LoginLogView.vue
```

## 设计体系要点

- **悬浮胶囊侧边栏**：`sticky` + `calc(100vh - 32px)`，毛玻璃材质，激活态品牌色胶囊 + 左侧 3px 光条。
- **防裁切间距**：`.admin-shell` 与 `.main-stage` 预留 ≥ 20px 四周内边距，侧边栏与主舞台间 24px 呼吸间隙，杜绝软阴影被 overflow 容器切成直角。
- **阴影**：全部采用多层负 Spread 公式（`--shadow-sm/md/lg`），贴合圆角无硬角。
- **弹窗**：`AppModal` 遮罩 `rgba(15,23,42,0.35) + blur(5px)`；本体浅色模式 100% 纯白不透底；弹簧曲线入场；32px 圆角旋转关闭键。
- **图标**：100% SVG 矢量（`@element-plus/icons-vue`），全站零原生 Emoji。

## 后端

API 经 Vite 代理转发至 `http://localhost:5000`（见 `vite.config.ts`），后端不在本目录内。
