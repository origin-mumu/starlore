# Starlore PDF 渲染微服务 (`starlore-pdf`)

Starlore 个人知识宇宙的简历 PDF 高保真渲染独立微服务。基于 **Node.js + Express + Puppeteer + Mustache** 构建，提供毫米级排版控制与矢量级无损 PDF 导出能力。

---

## 🛠️ 技术栈

| 组件 | 选用技术 | 说明 |
| :--- | :--- | :--- |
| **Web 框架** | Express 4.21 | 轻量 HTTP API 服务 |
| **无头浏览器** | Puppeteer 23.0 | 驱动 Chromium / Edge 渲染并执行打印输出 |
| **模板引擎** | Mustache 4.2 | 简历 HTML 结构与变量插值渲染 |
| **持久化缓存** | 内存级模板文件缓存 | 避免重复磁盘 I/O，极速响应 |

---

## 💡 核心机制

1. **跨平台浏览器自适应**：
   - **Windows 开发环境**：自动优先探测本机 Microsoft Edge 浏览器路径，免除额外下载庞大 Chromium 的开销；
   - **Linux 容器环境**：使用 Puppeteer 自带的无头 Chromium，并通过 `--no-sandbox`、`--disable-dev-shm-usage` 等参数强化容器稳定性。
2. **浏览器实例常驻复用**：
   - 采用单例懒加载与自动断线重连（`browserPromise`），避免每次生成 PDF 频繁启停浏览器进程带来的巨大延迟与内存抖动。
3. **精细化版面间距控制**：
   - 支持动态透传 `moduleGap`（模块间距）、`lineHeight`（行高比例）与 `fontSize`（字号缩放），确保简历恰好排布在单页或指定页数内，杜绝尴尬断行。

---

## 🚀 快速开始

### 1. 安装依赖

推荐使用 `pnpm`：

```bash
cd starlore-pdf
pnpm install
```

### 2. 启动服务

```bash
# 开发模式（监听文件变动自动热重启）
pnpm dev

# 生产运行
pnpm start
```

* 默认服务端口：**`3001`**（可通过环境变量 `PORT` 自定义）。

---

## 📡 API 接口

### 1. 健康检查

- **URL**: `GET /health`
- **响应**: `{"status": "ok"}`

### 2. 生成简历 PDF

- **URL**: `POST /api/pdf/resume`
- **Headers**: `Content-Type: application/json`
- **请求体格式**:

```json
{
  "template": "classic",
  "data": {
    "title": "高级全栈工程师简历",
    "name": "张三",
    "jobTitle": "全栈开发 / 架构师",
    "phone": "13800000000",
    "email": "example@domain.com",
    "content": {
      "spacing": {
        "moduleGap": 20,
        "lineHeight": 6,
        "fontSize": 14
      }
    }
  }
}
```

- **响应格式**: `application/pdf` 二进制文件流（自动带上 `Content-Disposition: inline; filename="resume.pdf"`）。

---

## 🐳 Docker 构建

目录下已包含优化配置的 `Dockerfile`：

```bash
docker build -t starlore-pdf:latest .
docker run -d -p 3001:3001 --name starlore-pdf starlore-pdf:latest
```
