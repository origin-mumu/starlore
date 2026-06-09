"""MCP (Model Context Protocol) Server。

提供标准化的工具协议接口，允许外部 AI 客户端（如 Claude Desktop、Cursor）
通过 MCP 协议调用博客工具。
"""

import json
import logging
from typing import Any

from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import Tool, TextContent

logger = logging.getLogger(__name__)


def create_mcp_server(get_db_session, get_user_id) -> Server:
    """创建 MCP Server 实例。

    Args:
        get_db_session: 获取数据库会话的异步上下文管理器
        get_user_id: 获取当前用户 ID 的函数
    """
    server = Server("starlore-blog")

    @server.list_tools()
    async def list_tools() -> list[Tool]:
        """列出所有可用工具。"""
        return [
            Tool(
                name="search_articles",
                description="搜索博客文章。可以按关键词、分类或标签搜索。",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "keyword": {"type": "string", "description": "搜索关键词"},
                        "category": {"type": "string", "description": "分类名称"},
                        "tag": {"type": "string", "description": "标签名称"},
                    },
                },
            ),
            Tool(
                name="get_article_detail",
                description="获取文章详情，需要提供文章 ID。",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "article_id": {"type": "integer", "description": "文章 ID"},
                    },
                    "required": ["article_id"],
                },
            ),
            Tool(
                name="get_categories",
                description="获取所有博客分类列表。",
                inputSchema={"type": "object", "properties": {}},
            ),
            Tool(
                name="get_blog_stats",
                description="获取博客统计数据，包括文章数、分类数、浏览量等。",
                inputSchema={"type": "object", "properties": {}},
            ),
            Tool(
                name="get_recent_articles",
                description="获取最近的文章列表。",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "limit": {"type": "integer", "description": "返回数量，默认 5"},
                    },
                },
            ),
            Tool(
                name="write_article",
                description="创建新文章。",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "title": {"type": "string", "description": "文章标题"},
                        "content": {"type": "string", "description": "文章内容（Markdown）"},
                        "category": {"type": "string", "description": "分类，默认'随笔'"},
                        "tags": {"type": "string", "description": "标签 JSON 数组"},
                        "description": {"type": "string", "description": "文章摘要"},
                        "status": {"type": "string", "description": "状态：published/draft"},
                    },
                    "required": ["title", "content"],
                },
            ),
            Tool(
                name="get_all_tags",
                description="获取所有文章标签。",
                inputSchema={"type": "object", "properties": {}},
            ),
            Tool(
                name="create_category",
                description="创建新分类。",
                inputSchema={
                    "type": "object",
                    "properties": {
                        "name": {"type": "string", "description": "分类名称"},
                        "description": {"type": "string", "description": "分类描述"},
                        "color": {"type": "string", "description": "分类颜色"},
                    },
                    "required": ["name"],
                },
            ),
        ]

    @server.call_tool()
    async def call_tool(name: str, arguments: dict[str, Any]) -> list[TextContent]:
        """调用指定工具。"""
        from app.services import blog_tools

        user_id = get_user_id()
        async with get_db_session() as db:
            try:
                if name == "search_articles":
                    result = await blog_tools.search_articles_impl(
                        db, user_id,
                        arguments.get("keyword"),
                        arguments.get("category"),
                        arguments.get("tag"),
                    )
                elif name == "get_article_detail":
                    result = await blog_tools.get_article_detail_impl(
                        db, user_id, arguments["article_id"]
                    )
                elif name == "get_categories":
                    result = await blog_tools.get_categories_impl(db, user_id)
                elif name == "get_blog_stats":
                    result = await blog_tools.get_blog_stats_impl(db, user_id)
                elif name == "get_recent_articles":
                    result = await blog_tools.get_recent_articles_impl(
                        db, user_id, arguments.get("limit", 5)
                    )
                elif name == "write_article":
                    result = await blog_tools.write_article_impl(
                        db, user_id,
                        arguments["title"],
                        arguments["content"],
                        arguments.get("category", "随笔"),
                        arguments.get("tags", "[]"),
                        arguments.get("description", ""),
                        arguments.get("status", "published"),
                    )
                elif name == "get_all_tags":
                    result = await blog_tools.get_all_tags_impl(db, user_id)
                elif name == "create_category":
                    result = await blog_tools.create_category_impl(
                        db, user_id,
                        arguments["name"],
                        arguments.get("description", ""),
                        arguments.get("color", ""),
                    )
                else:
                    result = json.dumps({"error": f"未知工具: {name}"})

                return [TextContent(type="text", text=result)]
            except Exception as e:
                logger.error("MCP tool error: %s", e)
                return [TextContent(type="text", text=json.dumps({"error": str(e)}))]

    return server


async def run_mcp_stdio(server: Server):
    """以 stdio 模式运行 MCP Server。"""
    async with stdio_server() as (read_stream, write_stream):
        await server.run(read_stream, write_stream, server.create_initialization_options())
