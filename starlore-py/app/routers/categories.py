"""分类路由：CRUD。"""

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.schemas.category import (
    CategoryDetailResponse,
    CategoryItem,
    CategoryListResponse,
    CreateCategoryRequest,
    UpdateCategoryRequest,
)
from app.schemas.common import SimpleResponse
from app.services import category_service

router = APIRouter(prefix="/api/categories", tags=["categories"])


@router.get("", response_model=CategoryListResponse)
async def list_categories(
    request_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    # admin 管理后台可见全部分类；普通用户仅见自己的
    items = await category_service.get_all_categories(
        db, None if request_user.role == "admin" else request_user.id
    )
    return CategoryListResponse(data=items)


@router.get("/{category_id}")
async def get_category(
    category_id: int,
    request_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    data = await category_service.get_category_by_id(
        db,
        request_user.id,
        category_id,
        is_admin=request_user.role == "admin",
    )
    return {"data": data}


@router.post("", response_model=SimpleResponse)
async def create_category(
    req: CreateCategoryRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    item = await category_service.create_category(db, user.id, req)
    return SimpleResponse.ok("分类创建成功", {"id": item.id})


@router.put("/{category_id}", response_model=SimpleResponse)
async def update_category(
    category_id: int,
    req: UpdateCategoryRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    await category_service.update_category(db, category_id, req)
    return SimpleResponse.ok("分类更新成功")


@router.delete("/{category_id}", response_model=SimpleResponse)
async def delete_category(
    category_id: int,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    await category_service.delete_category(db, category_id)
    return SimpleResponse.ok("分类删除成功")
