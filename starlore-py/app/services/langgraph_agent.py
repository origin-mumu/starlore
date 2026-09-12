"""LangGraph 多 Agent 工作流。

提供基于 LangGraph 的 Agent 编排，支持：
- 多步推理（ReAct 模式）
- 工具自动调用
- 流式输出中间步骤
"""

import json
import logging
from typing import Annotated, TypedDict

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI
from langgraph.graph import END, StateGraph
from langgraph.graph.message import add_messages
from langgraph.prebuilt import ToolNode

from app.services.blog_tools import (
    search_articles_impl,
    get_article_detail_impl,
    get_categories_impl,
    get_blog_stats_impl,
    get_recent_articles_impl,
    write_article_impl,
    update_article_impl,
    delete_article_impl,
    get_all_tags_impl,
    get_articles_by_category_impl,
    create_category_impl,
    get_user_resumes_impl,
    get_resume_detail_impl,
)

logger = logging.getLogger(__name__)


# ---------- Agent State ----------

class AgentState(TypedDict):
    """Agent 工作流状态。"""
    messages: Annotated[list, add_messages]
    user_id: int
    db_session: object  # AsyncSession


# ---------- 工具定义 ----------

def _create_tools(db, user_id):
    """创建绑定到当前用户和数据库会话的工具列表。"""
    from langchain_core.tools import tool

    @tool
    async def search_articles(keyword: str = "", category: str = "", tag: str = "") -> str:
        """搜索知识库文章。可以按关键词、分类或标签搜索。"""
        return await search_articles_impl(db, user_id, keyword or None, category or None, tag or None)

    @tool
    async def get_article_detail(article_id: int) -> str:
        """获取文章详情，需要提供文章 ID。"""
        return await get_article_detail_impl(db, user_id, article_id)

    @tool
    async def get_categories() -> str:
        """获取所有知识库分类列表。"""
        return await get_categories_impl(db, user_id)

    @tool
    async def get_blog_stats() -> str:
        """获取知识库统计数据，包括文章数、分类数、浏览量等。"""
        return await get_blog_stats_impl(db, user_id)

    @tool
    async def get_recent_articles(limit: int = 5) -> str:
        """获取最近的文章列表，默认 5 篇。"""
        return await get_recent_articles_impl(db, user_id, limit)

    @tool
    async def write_article(
        title: str,
        content: str,
        category: str = "随笔",
        tags: str = "[]",
        description: str = "",
        status: str = "published",
    ) -> str:
        """创建新文章。tags 是 JSON 数组字符串，如 '["tag1","tag2"]'。"""
        return await write_article_impl(db, user_id, title, content, category, tags, description, status)

    @tool
    async def get_all_tags() -> str:
        """获取所有文章标签。"""
        return await get_all_tags_impl(db, user_id)

    @tool
    async def get_articles_by_category(category: str) -> str:
        """获取指定分类下的所有文章。"""
        return await get_articles_by_category_impl(db, user_id, category)

    @tool
    async def update_article(
        article_id: int,
        title: str | None = None,
        content: str | None = None,
        category: str | None = None,
        tags: str | None = None,
        description: str | None = None,
        status: str | None = None,
    ) -> str:
        """更新已有文章。只需提供要修改的字段，其余传 None 保持不变。"""
        return await update_article_impl(db, user_id, article_id, title, content, category, tags, description, status)

    @tool
    async def delete_article(article_id: int) -> str:
        """删除指定文章，需要提供文章 ID。"""
        return await delete_article_impl(db, user_id, article_id)

    @tool
    async def create_category(name: str, description: str = "", color: str = "") -> str:
        """创建新分类。"""
        return await create_category_impl(db, user_id, name, description, color)

    @tool
    async def get_user_resumes() -> str:
        """获取当前用户的简历列表概要。"""
        return await get_user_resumes_impl(db, user_id)

    @tool
    async def get_resume_detail(resume_id: int = 0) -> str:
        """获取指定简历的完整详情。如果不传或传0则默认获取最近一份。"""
        return await get_resume_detail_impl(db, user_id, resume_id or None)

    return [
        search_articles,
        get_article_detail,
        get_categories,
        get_blog_stats,
        get_recent_articles,
        write_article,
        update_article,
        delete_article,
        get_all_tags,
        get_articles_by_category,
        create_category,
        get_user_resumes,
        get_resume_detail,
    ]


# ---------- Agent 图 ----------

def _build_graph(llm, tools):
    """构建 LangGraph ReAct Agent 图。"""
    tool_node = ToolNode(tools)

    # 绑定工具到 LLM
    llm_with_tools = llm.bind_tools(tools)

    async def agent_node(state: AgentState):
        """Agent 节点：调用 LLM 决定下一步。"""
        messages = state["messages"]
        response = await llm_with_tools.ainvoke(messages)
        return {"messages": [response]}

    def should_continue(state: AgentState) -> str:
        """判断是否继续调用工具。"""
        last_message = state["messages"][-1]
        if isinstance(last_message, AIMessage) and last_message.tool_calls:
            return "tools"
        return END

    # 构建图
    graph = StateGraph(AgentState)
    graph.add_node("agent", agent_node)
    graph.add_node("tools", tool_node)

    graph.set_entry_point("agent")
    graph.add_conditional_edges("agent", should_continue, {"tools": "tools", END: END})
    graph.add_edge("tools", "agent")

    return graph.compile()


# ---------- 公开接口 ----------

async def run_agent(
    db,
    user_id: int,
    llm: ChatOpenAI,
    messages: list[dict],
    character_prompt: str = "",
):
    """运行 Agent，返回流式事件。"""
    tools = _create_tools(db, user_id)

    # 加载已配置的外部 MCP Server 工具（如 Brave Search 等）
    try:
        from app.services.mcp_client import load_mcp_tools
        mcp_tools = await load_mcp_tools()
        if mcp_tools:
            tools = tools + mcp_tools
            logger.info("已合并 %d 个 MCP 外部工具，总工具数 %d", len(mcp_tools), len(tools))
    except Exception as e:
        logger.debug("MCP 工具加载跳过: %s", e)

    graph = _build_graph(llm, tools)

    # 构建消息列表
    lc_messages = []
    if character_prompt:
        lc_messages.append(SystemMessage(content=character_prompt))

    for msg in messages:
        role = msg.get("role", "user")
        content = msg.get("content", "")
        if role == "system":
            lc_messages.append(SystemMessage(content=content))
        elif role == "assistant":
            lc_messages.append(AIMessage(content=content))
        else:
            lc_messages.append(HumanMessage(content=content))

    # 流式执行
    async for event in graph.astream(
        {"messages": lc_messages, "user_id": user_id, "db_session": db},
        stream_mode="updates",
    ):
        for node_name, node_output in event.items():
            if node_name == "agent":
                msg = node_output["messages"][-1]
                if isinstance(msg, AIMessage):
                    if msg.content:
                        yield {"type": "content", "content": msg.content}
                    if msg.tool_calls:
                        yield {"type": "tool_calls", "calls": msg.tool_calls}
            elif node_name == "tools":
                for tool_msg in node_output["messages"]:
                    yield {"type": "tool_result", "content": tool_msg.content}


async def run_agent_simple(
    db,
    user_id: int,
    llm: ChatOpenAI,
    messages: list[dict],
    character_prompt: str = "",
) -> str:
    """运行 Agent，返回最终文本响应（非流式）。"""
    full_content = ""
    async for event in run_agent(db, user_id, llm, messages, character_prompt):
        if event["type"] == "content":
            full_content += event["content"]
    return full_content
