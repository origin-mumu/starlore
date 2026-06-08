"""健康检查路由。"""

from datetime import datetime, timezone

from fastapi import APIRouter

router = APIRouter(prefix="/api", tags=["health"])


@router.get("/health")
async def health():
    return {
        "success": True,
        "message": "博客后端服务运行正常",
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }
