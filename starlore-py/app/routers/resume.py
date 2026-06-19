"""简历路由。"""

from fastapi import APIRouter, Depends
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
    import os
    import httpx
    from fastapi.responses import Response

    r = await resume_service.get_by_id(db, resume_id, user.id)

    pdf_service_url = os.getenv("PDF_SERVICE_URL", "http://localhost:3001")

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            resp = await client.post(
                f"{pdf_service_url}/generate",
                json={
                    "template": r.template or "classic",
                    "data": {
                        "name": r.name or "",
                        "job_title": r.job_title or "",
                        "phone": r.phone or "",
                        "email": r.email or "",
                        "photo_url": r.photo_url or "",
                        "content": r.content or "",
                    },
                },
            )
            resp.raise_for_status()
            return Response(
                content=resp.content,
                media_type="application/pdf",
                headers={"Content-Disposition": f'attachment; filename="resume_{resume_id}.pdf"'},
            )
    except httpx.ConnectError:
        return {"error": f"PDF 服务不可用 ({pdf_service_url})，请确保 starlore-pdf 已启动"}
    except Exception as e:
        return {"error": f"PDF 生成失败: {e}"}
