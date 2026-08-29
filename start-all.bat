@echo off
chcp 65001 >nul
title Starlore Launcher

echo ==============================================
echo        Starlore 一键启动前后端服务 (Python + Vue 3)
echo ==============================================

echo [1/2] 正在启动 Python 后端服务 (FastAPI :5000)...
start "Starlore-Py-Backend" cmd /k "cd /d D:\project\starlore\starlore-py && uvicorn app.main:app --host 0.0.0.0 --port 5000 --reload"

echo [2/2] 正在启动前端服务 (Vue 3 Vite :5173)...
start "Starlore-Frontend" cmd /k "cd /d D:\project\starlore\starlore-front && npm run dev"

echo.
echo 前后端服务已分别在独立控制台窗口启动！
echo 后端接口: http://localhost:5000 (Swagger: http://localhost:5000/docs)
echo 前端页面: http://localhost:5173
echo ==============================================
