# Starlore 一键启动脚本 (Python FastAPI + Vue 3)
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "   Starlore 一键启动前后端服务 (Python + Vue)" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

Write-Host "[1/2] 正在启动 Python 后端 (FastAPI :5000)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'D:\project\starlore\starlore-py'; uvicorn app.main:app --host 0.0.0.0 --port 5000 --reload"

Write-Host "[2/2] 正在启动前端服务 (Vue 3 Vite :5173)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'D:\project\starlore\starlore-front'; npm run dev"

Write-Host "`n前后端服务已在独立终端窗口启动！" -ForegroundColor Yellow
Write-Host "后端接口: http://localhost:5000 (文档: http://localhost:5000/docs)" -ForegroundColor Gray
Write-Host "前端页面: http://localhost:5173" -ForegroundColor Gray
