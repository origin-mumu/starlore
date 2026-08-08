FROM python:3.11-slim

WORKDIR /app

# 设置时区与环境变量
ENV TZ=Asia/Shanghai \
    PYTHONUNBUFFERED=1 \
    PYTHONPATH=/app

# 安装系统依赖（编译 faiss/numpy 等库所需）
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 安装 Python 依赖（使用 BuildKit 缓存层，配合锁定版本数秒内完成构建）
COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt

# 拷贝代码
COPY . .

EXPOSE 5000

# 小规格服务器使用单 worker，避免重复加载 AI/RAG 依赖造成资源争抢
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "5000", "--workers", "1"]
