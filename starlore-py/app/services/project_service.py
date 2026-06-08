"""项目 CRUD 服务。"""

from datetime import datetime

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.exceptions import NotFoundException, UnauthorizedException
from app.models.project import Project
from app.schemas.project import ProjectInfo, ProjectRequest


def _to_info(project: Project) -> ProjectInfo:
    return ProjectInfo(
        id=project.id,
        name=project.name,
        description=project.description,
        url=project.url,
        image=project.image,
        sortOrder=project.sort_order,
        createdAt=project.created_at,
        updatedAt=project.updated_at,
    )


async def list_projects(db: AsyncSession, user_id: int) -> list[ProjectInfo]:
    result = await db.execute(
        select(Project).where(Project.user_id == user_id)
        .order_by(Project.sort_order.asc(), Project.created_at.desc())
    )
    return [_to_info(p) for p in result.scalars().all()]


async def create_project(db: AsyncSession, user_id: int, req: ProjectRequest) -> ProjectInfo:
    now = datetime.now()
    project = Project(
        user_id=user_id,
        name=req.name or "",
        description=req.description,
        url=req.url,
        image=req.image,
        sort_order=req.sortOrder or 0,
        created_at=now,
        updated_at=now,
    )
    db.add(project)
    await db.flush()
    return _to_info(project)


async def update_project(db: AsyncSession, user_id: int, project_id: int, req: ProjectRequest) -> ProjectInfo:
    result = await db.execute(select(Project).where(Project.id == project_id))
    project = result.scalar_one_or_none()
    if project is None:
        raise NotFoundException("项目不存在")
    if project.user_id != user_id:
        raise UnauthorizedException("无权操作该项目")

    if req.name is not None:
        project.name = req.name
    if req.description is not None:
        project.description = req.description
    if req.url is not None:
        project.url = req.url
    if req.image is not None:
        project.image = req.image
    if req.sortOrder is not None:
        project.sort_order = req.sortOrder

    project.updated_at = datetime.now()
    await db.flush()
    return _to_info(project)


async def delete_project(db: AsyncSession, user_id: int, project_id: int) -> None:
    result = await db.execute(select(Project).where(Project.id == project_id))
    project = result.scalar_one_or_none()
    if project is None:
        raise NotFoundException("项目不存在")
    if project.user_id != user_id:
        raise UnauthorizedException("无权操作该项目")

    await db.delete(project)
    await db.flush()
