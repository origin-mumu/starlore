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

# 安装 Python 依赖（在 GitHub CI 构建中默认使用官方 PyPI 全球 CDN 节点，15秒内快速完成）
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 拷贝代码
COPY . .

EXPOSE 5000

# 启动 uvicorn 生产服务器（4个 worker 协程进程）
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "5000", "--workers", "4"]
