#!/bin/bash
set -e

echo "=== 开始自动部署 Starlore Python 后端 ==="

# 检查 .env 配置文件
if [ ! -f .env ]; then
    echo "未找到 .env 配置文件，尝试从 .env.example 复制..."
    cp .env.example .env
    echo "请配置 .env 文件中的数据库和密钥后重新运行！"
    exit 1
fi

# 检查 Docker 是否安装
if command -v docker &> /dev/null; then
    echo "使用 Docker Compose 进行自动构建与部署..."
    docker compose up -d --build
    echo "部署完成！容器状态："
    docker compose ps
else
    echo "未检测到 Docker，尝试基于 Linux Systemd 后台运行..."
    pip install -r requirements.txt
    nohup uvicorn app.main:app --host 0.0.0.0 --port 5000 --workers 4 > uvicorn.log 2>&1 &
    echo "已在后台启动 uvicorn 服务，日志输出在 uvicorn.log"
fi

echo "=== Starlore Python 后端部署成功 (端口 5000) ==="
