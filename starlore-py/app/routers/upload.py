"""文件上传路由。"""

from fastapi import APIRouter, File, UploadFile

from app.schemas.common import SimpleResponse
from app.services import file_service

router = APIRouter(prefix="/api/upload", tags=["upload"])


@router.post("/image", response_model=SimpleResponse)
async def upload_image(file: UploadFile = File(...)):
    data = await file.read()
    url = await file_service.upload_file(
        filename=file.filename or "image.bin",
        data=data,
        content_type=file.content_type or "application/octet-stream",
    )
    return SimpleResponse.ok("上传成功", {"url": url})
