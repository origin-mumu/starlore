"""项目路由。"""

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.schemas.common import MessageResponse, SimpleResponse
from app.schemas.project import ProjectInfo, ProjectRequest, ProjectResponse
from app.services import project_service

router = APIRouter(prefix="/api/projects", tags=["projects"])


@router.get("")
async def list_projects(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    items = await project_service.list_projects(db, user.id)
    return {"data": items}


@router.post("", response_model=ProjectResponse)
async def create_project(
    req: ProjectRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    info = await project_service.create_project(db, user.id, req)
    return {"data": info, "message": "创建成功"}


@router.put("/{project_id}", response_model=ProjectResponse)
async def update_project(
    project_id: int,
    req: ProjectRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    updated = await project_service.update_project(db, user.id, project_id, req)
    return {"data": updated, "message": "更新成功"}


@router.delete("/{project_id}", response_model=MessageResponse)
async def delete_project(
    project_id: int,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    await project_service.delete_project(db, user.id, project_id)
    return {"message": "删除成功"}
