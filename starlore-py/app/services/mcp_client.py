"""MCP Client 集成 —— 让项目接入外部 MCP Server。

支持通过 stdio 协议连接外部 MCP Server（如 Brave Search、Filesystem 等），
将其工具自动转换为 LangChain Tool，无缝融入现有 Agent 工作流。

配置方式（在 .env 中）：
    MCP_SERVERS='[{"name":"brave-search","command":"npx","args":["-y","@anthropic/mcp-server-brave-search"],"env":{"BRAVE_API_KEY":"xxx"}}]'

用法：
    from app.services.mcp_client import load_mcp_tools
    external_tools = await load_mcp_tools()  # 返回 LangChain Tool 列表
"""

import asyncio
import json
import logging
import os
from typing import Any

from langchain_core.tools import tool as lc_tool

logger = logging.getLogger(__name__)

# MCP 服务端列表，支持空配置或 JSON 配置
# JSON 格式: [{"name":"服务名","command":"启动命令","args":["参数"],"env":{"KEY":"VAL"}}]
MCP_SERVERS_RAW = os.getenv("MCP_SERVERS", "[]")


def _parse_servers() -> list[dict[str, Any]]:
    """解析 MCP_SERVERS 环境变量。"""
    try:
        servers = json.loads(MCP_SERVERS_RAW)
        if not isinstance(servers, list):
            logger.warning("MCP_SERVERS 不是 JSON 数组，已忽略")
            return []
        return servers
    except (json.JSONDecodeError, TypeError):
        logger.warning("MCP_SERVERS 格式无效，已忽略: %s", MCP_SERVERS_RAW)
        return []


async def load_mcp_tools() -> list:
    """加载所有已配置的 MCP Server 工具，返回 LangChain Tool 列表。

    若无配置或全部连接失败，返回空列表（Agent 仅使用本机工具）。
    """
    servers = _parse_servers()
    if not servers:
        return []

    all_tools: list = []

    for cfg in servers:
        name = cfg.get("name", "unknown")
        try:
            tools = await _connect_and_load(cfg)
            all_tools.extend(tools)
            logger.info("MCP [%s] 已连接，加载 %d 个工具", name, len(tools))
        except ImportError:
            logger.warning(
                "MCP [%s] 需要安装 mcp 客户端库: pip install mcp", name
            )
        except Exception as e:
            logger.warning("MCP [%s] 连接失败: %s，跳过", name, e)

    return all_tools


async def _connect_and_load(cfg: dict[str, Any]) -> list:
    """连接单个 MCP Server，加载其工具列表并转为 LangChain Tool。"""
    from mcp import ClientSession, StdioServerParameters
    from mcp.client.stdio import stdio_client

    command = cfg["command"]
    args = cfg.get("args", [])
    env = cfg.get("env", {})
    name = cfg.get("name", command)

    # 合并环境变量（继承当前进程环境，追加配置的环境变量）
    merged_env = os.environ.copy()
    merged_env.update(env)

    server_params = StdioServerParameters(
        command=command,
        args=args,
        env=merged_env,
    )

    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            mcp_tools = await session.list_tools()

            langchain_tools = []
            for mcp_tool in mcp_tools.tools:
                langchain_tools.append(
                    _convert_to_langchain_tool(mcp_tool, session, name)
                )
            return langchain_tools


def _convert_to_langchain_tool(
    mcp_tool: Any,
    session: Any,
    server_name: str,
):
    """将单个 MCP Tool 转换为 LangChain @tool 函数。"""

    tool_name = f"mcp_{server_name}_{mcp_tool.name}"
    tool_desc = mcp_tool.description or f"MCP 工具: {mcp_tool.name}"
    tool_desc += f"（来自 MCP Server: {server_name}）"

    # 解析参数 schema 描述
    param_hints = ""
    if hasattr(mcp_tool, "inputSchema") and mcp_tool.inputSchema:
        props = mcp_tool.inputSchema.get("properties", {})
        required = mcp_tool.inputSchema.get("required", [])
        for pname, pinfo in props.items():
            req_mark = "（必填）" if pname in required else "（可选）"
            param_hints += f"\n  - {pname}: {pinfo.get('description', '')} {req_mark}"

    @lc_tool(name=tool_name, description=tool_desc + param_hints)
    async def call_mcp_tool(**kwargs: Any) -> str:
        """调用 MCP 工具。"""
        result = await session.call_tool(mcp_tool.name, arguments=kwargs)
        # 合并所有 text 内容
        texts = []
        for item in result.content:
            if hasattr(item, "text"):
                texts.append(item.text)
        return "\n".join(texts) if texts else str(result.content)

    return call_mcp_tool
