# StarLore

一个现代化的知识库管理系统，采用前后端分离架构，支持 Web 端、移动端多平台。

## 🌐 在线演示

- **前端地址**: http://47.94.128.65/

## 📋 项目特性

### 前端特性 (starlore-front)

- ✅ Vue 3 + TypeScript 现代化前端架构
- ✅ Element Plus UI 组件库
- ✅ 响应式设计，支持移动端
- ✅ 知识卡片展示与管理
- ✅ 分类与标签体系
- ✅ 数据可视化统计图表

### 后端特性 (starlore-back)

- ✅ Spring Boot RESTful API
- ✅ MySQL 数据库 + MyBatis-Plus
- ✅ JWT 认证鉴权
- ✅ MinIO 文件存储
- ✅ AI 对话集成（DeepSeek / 通义千问）
- ✅ 完整的 CRUD + 分页、搜索、过滤

### 移动端 (starlore_app)

- ✅ Flutter 跨平台应用
- ✅ 支持 Android / iOS / Web / Desktop

## 🏗️ 技术栈

### 前端 (starlore-front)

- **框架**: Vue 3 + TypeScript
- **构建工具**: Vite
- **UI 组件**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router
- **HTTP 客户端**: Axios
- **图表库**: ECharts

### 后端 (starlore-back)

- **框架**: Spring Boot
- **数据库**: MySQL
- **ORM**: MyBatis-Plus
- **认证**: JWT
- **文件存储**: MinIO
- **AI 集成**: DeepSeek / 通义千问 / Mimo

### 移动端 (starlore_app)

- **框架**: Flutter
- **支持平台**: Android / iOS / Web / Linux / macOS / Windows

## 📁 项目结构

```
starlore/
├── starlore-front/          # Vue 3 前端
│   ├── src/
│   │   ├── api/             # API 接口
│   │   ├── components/      # 公共组件
│   │   ├── router/          # 路由配置
│   │   ├── views/           # 页面组件
│   │   └── utils/           # 工具函数
│   └── package.json
├── starlore-back/           # Spring Boot 后端
│   └── src/main/java/com/robin/blogback/
│       ├── config/          # 配置类
│       ├── controller/      # 控制器
│       ├── entity/          # 实体类
│       ├── mapper/          # 数据访问层
│       ├── service/         # 业务逻辑层
│       └── util/            # 工具类
├── starlore_app/            # Flutter 移动端
│   └── lib/
└── starlore-admin/          # 管理后台
```

## 🚀 快速开始

### 环境要求

- JDK >= 17
- Node.js >= 16.0.0
- MySQL >= 5.7
- Flutter >= 3.0（移动端开发）

### 后端启动 (starlore-back)

```bash
cd starlore-back

# 配置数据库连接（修改 application.yaml 或使用环境变量）
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

### 移动端启动 (starlore_app)

```bash
cd starlore_app
flutter pub get
flutter run
```

## 📊 API 接口

### 知识条目相关

- `GET /api/articles` - 获取知识列表（支持分页、搜索）
- `GET /api/articles/:id` - 获取知识详情
- `POST /api/articles` - 创建新条目
- `PUT /api/articles/:id` - 更新条目
- `DELETE /api/articles/:id` - 删除条目

### 分类相关

- `GET /api/categories` - 获取分类列表
- `POST /api/categories` - 创建新分类
- `PUT /api/categories/:id` - 更新分类
- `DELETE /api/categories/:id` - 删除分类

### AI 对话

- `POST /api/ai/chat` - AI 对话接口

## 🎯 功能模块

### 知识库前台

- [x] 知识条目展示（分页、搜索）
- [x] 详情页（代码高亮、阅读统计）
- [x] 分类与标签筛选
- [x] 响应式设计
- [x] AI 智能对话

### 管理后台

- [x] 知识条目管理（增删改查）
- [x] 分类管理
- [x] 富文本编辑器
- [x] 数据统计图表
- [x] 条目状态管理（发布/草稿）

## 🤝 贡献指南

1. Fork 本项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系方式

- 项目作者: 贾新科
- 项目地址: https://github.com/origin-mumu/starlore

---

⭐ 如果这个项目对您有帮助，请给个 Star 支持一下！
