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


async def upload_file(filename: str, data: bytes, content_type: str) -> str:
    """上传文件到 MinIO，返回公开访问 URL。使用线程池避免阻塞事件循环。"""
    ext = Path(filename).suffix or ".bin"
    object_name = f"{uuid.uuid4().hex}{ext}"

    def _upload() -> str:
        client = _get_client()
        client.put_object(
            settings.minio_bucket,
            object_name,
            io.BytesIO(data),
            length=len(data),
            content_type=content_type,
        )
        return f"{settings.minio_public_url}/{object_name}"

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
