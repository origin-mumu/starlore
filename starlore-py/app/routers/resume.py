"""简历路由。"""

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.resume import Resume
from app.models.user import User
from app.schemas.common import MessageResponse, SimpleResponse
from app.schemas.resume import ResumeDetailResponse, ResumeItem, ResumeListResponse, ResumeResponse
from app.services import resume_service

router = APIRouter(prefix="/api/resume", tags=["resume"])


class CreateResumeRequest(BaseModel):
    title: str | None = None
    template: str | None = None
    name: str | None = None
    jobTitle: str | None = None
    phone: str | None = None
    email: str | None = None
    photoUrl: str | None = None
    content: str | None = None


class UpdateResumeRequest(BaseModel):
    id: int
    title: str | None = None
    template: str | None = None
    name: str | None = None
    jobTitle: str | None = None
    phone: str | None = None
    email: str | None = None
    photoUrl: str | None = None
    content: str | None = None


@router.get("/list", response_model=ResumeListResponse)
async def list_resumes(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    resumes = await resume_service.list_by_user(db, user.id)
    return {
        "data": [
            ResumeItem(
                id=r.id,
                userId=r.user_id,
                title=r.title,
                template=r.template,
                name=r.name,
                jobTitle=r.job_title,
                phone=r.phone,
                email=r.email,
                photoUrl=r.photo_url,
                content=r.content,
                status=r.status,
                createdAt=r.created_at,
                updatedAt=r.updated_at,
            )
            for r in resumes
        ]
    }


@router.get("/{resume_id}", response_model=ResumeDetailResponse)
async def get_resume(
    resume_id: int,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    r = await resume_service.get_by_id(db, resume_id, user.id)
    return {
        "data": ResumeItem(
            id=r.id,
            userId=r.user_id,
            title=r.title,
            template=r.template,
            name=r.name,
            jobTitle=r.job_title,
            phone=r.phone,
            email=r.email,
            photoUrl=r.photo_url,
            content=r.content,
            status=r.status,
            createdAt=r.created_at,
            updatedAt=r.updated_at,
        )
    }


@router.post("/create", response_model=ResumeResponse)
async def create_resume(
    req: CreateResumeRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    resume = Resume(
        title=req.title,
        template=req.template,
        name=req.name,
        job_title=req.jobTitle,
        phone=req.phone,
        email=req.email,
        photo_url=req.photoUrl,
        content=req.content,
    )
    await resume_service.create(db, resume, user.id)
    item = ResumeItem(
        id=resume.id,
        userId=resume.user_id,
        title=resume.title,
        template=resume.template,
        name=resume.name,
        jobTitle=resume.job_title,
        phone=resume.phone,
        email=resume.email,
        photoUrl=resume.photo_url,
        content=resume.content,
        status=resume.status,
        createdAt=resume.created_at,
        updatedAt=resume.updated_at,
    )
    return {"data": item, "message": "创建成功"}


@router.put("/update", response_model=ResumeResponse)
async def update_resume(
    req: UpdateResumeRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    resume = Resume(
        id=req.id,
        title=req.title,
        template=req.template,
        name=req.name,
        job_title=req.jobTitle,
        phone=req.phone,
        email=req.email,
        photo_url=req.photoUrl,
        content=req.content,
    )
    updated = await resume_service.update(db, resume, user.id)
    item = ResumeItem(
        id=updated.id,
        userId=updated.user_id,
        title=updated.title,
        template=updated.template,
        name=updated.name,
        jobTitle=updated.job_title,
        phone=updated.phone,
        email=updated.email,
        photoUrl=updated.photo_url,
        content=updated.content,
        status=updated.status,
        createdAt=updated.created_at,
        updatedAt=updated.updated_at,
    )
    return {"data": item, "message": "更新成功"}


@router.delete("/{resume_id}", response_model=MessageResponse)
async def delete_resume(
    resume_id: int,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    await resume_service.delete(db, resume_id, user.id)
    return {"message": "删除成功"}


@router.get("/{resume_id}/export-pdf")
async def export_pdf(
    resume_id: int,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """导出简历为 PDF（调用外部 starlore-pdf 服务）。"""
    import base64
    import json
    import os
    import urllib.parse
    from urllib.parse import urlparse
    import httpx
    from fastapi.responses import Response
    from app.config import settings

    r = await resume_service.get_by_id(db, resume_id, user.id)

    pdf_service_url = os.getenv("PDF_SERVICE_URL", "http://localhost:3001")

    # PDF 服务期望 content 为对象（含 spacing/education 等），而非 JSON 字符串
    try:
        content_obj = json.loads(r.content) if r.content else {}
    except (json.JSONDecodeError, TypeError):
        content_obj = {}

    photo_url = r.photo_url or ""
    if photo_url:
        parsed_photo_url = urlparse(photo_url)
        if parsed_photo_url.hostname in {"127.0.0.1", "localhost", "minio"}:
            object_name = parsed_photo_url.path.rsplit("/", 1)[-1]
            if object_name:
                photo_url = f"{settings.minio_public_url.rstrip('/')}/{object_name}"

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            embedded_photo_url = photo_url
            if photo_url:
                try:
                    photo_response = await client.get(photo_url)
                    photo_response.raise_for_status()
                    content_type = photo_response.headers.get("content-type", "image/jpeg")
                    encoded_photo = base64.b64encode(photo_response.content).decode("ascii")
                    embedded_photo_url = f"data:{content_type};base64,{encoded_photo}"
                except httpx.HTTPError:
                    # 保留原地址作为降级方案，避免头像下载异常阻断整份简历导出。
                    embedded_photo_url = photo_url

            resp = await client.post(
                f"{pdf_service_url}/api/pdf/resume",
                json={
                    "template": r.template or "classic",
                    "data": {
                        "title": r.title or "",
                        "name": r.name or "",
                        "jobTitle": r.job_title or "",
                        "phone": r.phone or "",
                        "email": r.email or "",
                        "photoUrl": embedded_photo_url,
                        "content": content_obj,
                    },
                },
            )
            resp.raise_for_status()
            # 文件名取简历标题，与前端/Java 后端保持一致
            file_name = f"{r.title or '简历'}.pdf"
            encoded = urllib.parse.quote(file_name)
            return Response(
                content=resp.content,
                media_type="application/pdf",
                headers={
                    "Content-Disposition": f"attachment; filename*=UTF-8''{encoded}",
                },
            )
    except httpx.ConnectError:
        raise HTTPException(
            status_code=502,
            detail=f"PDF 服务不可用 ({pdf_service_url})，请确保 starlore-pdf 已启动",
        )
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"PDF 生成失败: {e}")
