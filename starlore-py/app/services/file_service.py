"""文件上传服务（MinIO）。"""

import asyncio
import io
import logging
import uuid
from pathlib import Path

from minio import Minio
from minio.error import S3Error

from app.config import settings

logger = logging.getLogger(__name__)

_client: Minio | None = None


def _get_client() -> Minio:
    global _client
    if _client is None:
        endpoint = settings.minio_endpoint.replace("http://", "").replace("https://", "")
        _client = Minio(
            endpoint,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=settings.minio_endpoint.startswith("https"),
        )
        try:
            if not _client.bucket_exists(settings.minio_bucket):
                _client.make_bucket(settings.minio_bucket)
                logger.info("MinIO bucket '%s' created", settings.minio_bucket)
        except S3Error as e:
            logger.warning("MinIO bucket check failed: %s", e)
    return _client


async def upload_file(
    filename: str,
    data: bytes,
    content_type: str,
    user_id: int | None = None,
    folder: str = "harness",
) -> str:
    """上传文件到 MinIO，返回公开访问 URL。支持按用户 ID 隔离目录。使用线程池避免阻塞事件循环。"""
    import re
    from urllib.parse import quote

    ext = Path(filename).suffix or ".bin"
    stem = Path(filename).stem
    safe_stem = re.sub(r'[\\/*?:"<>| ]', '_', stem)[:30] or "file"
    safe_uuid = uuid.uuid4().hex[:8]
    final_file_name = f"{safe_stem}_{safe_uuid}{ext}"

    if user_id:
        object_name = f"users/{user_id}/{folder}/{final_file_name}"
    else:
        object_name = f"{final_file_name}"

    encoded_name = quote(filename)

    def _upload() -> str:
        client = _get_client()
        client.put_object(
            settings.minio_bucket,
            object_name,
            io.BytesIO(data),
            length=len(data),
            content_type=content_type,
            metadata={
                "Content-Disposition": f"attachment; filename*=UTF-8''{encoded_name}"
            },
        )
        public_base = settings.minio_public_url.rstrip("/")
        # 确保包含 bucket 名称，以兼容 MinIO 路径规范与 Nginx 反向代理
        if not public_base.endswith(settings.minio_bucket):
            return f"{public_base}/{settings.minio_bucket}/{object_name}"
        return f"{public_base}/{object_name}"

    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(None, _upload)


async def delete_file(file_url: str) -> None:
    """从 MinIO 删除文件。使用线程池避免阻塞事件循环。"""
    def _delete() -> None:
        try:
            object_name = file_url.split("/")[-1]
            client = _get_client()
            client.remove_object(settings.minio_bucket, object_name)
        except Exception as e:
            logger.warning("Failed to delete file from MinIO: %s", e)

    loop = asyncio.get_event_loop()
    await loop.run_in_executor(None, _delete)
