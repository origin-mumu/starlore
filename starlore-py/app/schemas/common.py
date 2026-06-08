"""通用 Schema。"""

from typing import Any

from pydantic import BaseModel


class SimpleResponse(BaseModel):
    success: bool
    message: str
    data: Any = None

    @staticmethod
    def ok(message: str, data: Any = None) -> "SimpleResponse":
        return SimpleResponse(success=True, message=message, data=data)

    @staticmethod
    def fail(message: str, data: Any = None) -> "SimpleResponse":
        return SimpleResponse(success=False, message=message, data=data)


class PaginationInfo(BaseModel):
    current: int
    total: int
    pages: int
