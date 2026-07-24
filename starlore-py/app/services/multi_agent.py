"""Multi-Agent Planner-Executor-Reviewer 协作图。

基于 LangGraph StateGraph 实现：
  START -> Planner -> Executor -> Reviewer
    ├─ PASS  -> Synthesizer -> END
    ├─ REVISE & canRetry -> Executor (重试)
    └─ FAIL / retryExhausted -> END

支持：
- 依赖感知的子任务拓扑排序与并行执行 (asyncio.gather)
- Bad Case Few-Shot 动态注入
- SSE 流式事件输出
- AgentMetrics 指标记录
"""

import asyncio
import json
import logging
import time
import uuid
from dataclasses import dataclass, field
from typing import Any, Literal

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI

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
)

logger = logging.getLogger(__name__)

MAX_RETRIES = 2
MAX_SUBTASKS = 5


# ---------- 数据结构 ----------

@dataclass
class Subtask:
    id: int
    description: str
    tool_hint: str | None = None
    dependencies: list[int] = field(default_factory=list)


@dataclass
class MultiAgentState:
    """多 Agent 共享状态。"""
    user_query: str
    user_id: int
    db: object  # AsyncSession
    system_prompt: str = ""

    # Planner 输出
    plan_summary: str = ""
    subtasks: list[Subtask] = field(default_factory=list)

    # Executor 输出
    execution_results: dict[int, str] = field(default_factory=dict)
    final_answer: str = ""

    # Reviewer 输出
    review_decision: str = ""  # PASS / REVISE / FAIL
    review_feedback: str = ""
    retry_count: int = 0

    # RAG 上下文 (用于前端卡片及评估)
    rag_articles: list[dict] = field(default_factory=list)

    # 指标
    trace_id: str = ""
    tokens_in: int = 0
    tokens_out: int = 0
    node_timings: dict[str, float] = field(default_factory=dict)
    tool_call_counts: dict[str, int] = field(default_factory=dict)

    # 事件回调（用于 SSE 流式输出）
    event_callback: Any = None


# ---------- Prompt 模板 ----------

PLANNER_PROMPT = """你是 Starlore 博客系统的任务规划专家（Planner）。

## 职责
分析用户的请求，将其拆解为一系列可执行的子任务。每个子任务应该足够具体，
以便执行者（Executor）能够独立完成。

## 可用工具
- searchArticles: 搜索博客文章（语义搜索 + 关键词搜索）
- getArticleDetail: 获取文章详情
- getCategories: 获取所有分类
- getBlogStats: 获取博客统计
- getRecentArticles: 获取最新文章
- writeArticle: 创建文章
- updateArticle: 更新文章
- deleteArticle: 删除文章
- getAllTags: 获取所有标签
- getArticlesByCategory: 按分类获取文章
- createCategory: 创建分类

## 输出格式
请严格按以下 JSON 格式输出，不要输出其他内容：

{
  "summary": "整体规划说明",
  "subtasks": [
    {
      "id": 1,
      "description": "子任务1描述",
      "toolHint": "建议使用的工具名称（可选）",
      "dependencies": []
    },
    {
      "id": 2,
      "description": "子任务2描述",
      "toolHint": "建议使用的工具名称（可选）",
      "dependencies": [1]
    }
  ]
}

## 规则
1. 如果用户请求很简单（如"你好"、"天气怎么样"），只需创建 1 个子任务直接回答。
2. 子任务数量不超过 5 个。
3. 明确标识子任务之间的依赖关系（dependencies）。
4. 尽可能将无关任务设为可并行（空的 dependencies）。"""

EXECUTOR_PROMPT = """你是 Starlore 博客系统的智能执行专家（Executor）。

## 职责
根据 Planner 拆解的子任务，结合上下文数据，执行具体操作并生成结果。

## 输出格式
对于每个子任务，输出结构化的结果：
- 如果成功：直接输出结果内容
- 如果失败：输出 "ERROR: [错误原因]"

## 规则
1. 严格按照子任务描述执行，不要做多余操作
2. 如果子任务需要工具调用，优先使用 toolHint 建议的工具
3. 如果工具调用失败，尝试替代方案
4. 保持输出简洁，不要重复子任务描述"""

REVIEWER_PROMPT = """你是 Starlore 博客系统的审查专家（Reviewer）。

## 职责
审查执行结果的质量、完整性和准确性。给出明确的审查决策。

## 审查标准
1. **完整性**：结果是否完整回答了用户的问题？
2. **准确性**：数据是否准确？是否有明显的错误？
3. **格式**：输出格式是否合理？是否需要调整？
4. **错误处理**：是否有未处理的错误或异常？

## 输出格式
请严格按以下 JSON 格式输出，不要输出其他内容：

{
  "decision": "PASS 或 REVISE 或 FAIL",
  "feedback": "审查意见（如果 decision 是 PASS，可以为空字符串）",
  "suggestions": ["修正建议1", "修正建议2"]
}

## 决策规则
- PASS：结果完整、准确、格式合理
- REVISE：结果基本正确但有小问题（如缺少数据、格式不佳），可修正
- FAIL：结果严重错误或完全无法使用"""


# ---------- 工具执行器 ----------

async def _execute_tool(tool_name: str, db, user_id: int, **kwargs) -> str:
    """执行单个工具调用。"""
    tool_map = {
        "searchArticles": lambda: search_articles_impl(db, user_id, kwargs.get("keyword"), kwargs.get("category"), kwargs.get("tag")),
        "getArticleDetail": lambda: get_article_detail_impl(db, user_id, kwargs.get("article_id", 0)),
        "getCategories": lambda: get_categories_impl(db, user_id),
        "getBlogStats": lambda: get_blog_stats_impl(db, user_id),
        "getRecentArticles": lambda: get_recent_articles_impl(db, user_id, kwargs.get("limit", 5)),
        "writeArticle": lambda: write_article_impl(db, user_id, kwargs.get("title", ""), kwargs.get("content", ""), kwargs.get("category", "随笔"), kwargs.get("tags", "[]"), kwargs.get("description", ""), kwargs.get("status", "published")),
        "updateArticle": lambda: update_article_impl(db, user_id, kwargs.get("article_id", 0), kwargs.get("title"), kwargs.get("content"), kwargs.get("category"), kwargs.get("tags"), kwargs.get("description"), kwargs.get("status")),
        "deleteArticle": lambda: delete_article_impl(db, user_id, kwargs.get("article_id", 0)),
        "getAllTags": lambda: get_all_tags_impl(db, user_id),
        "getArticlesByCategory": lambda: get_articles_by_category_impl(db, user_id, kwargs.get("category", "")),
        "createCategory": lambda: create_category_impl(db, user_id, kwargs.get("name", ""), kwargs.get("description", ""), kwargs.get("color", "")),
    }

    fn = tool_map.get(tool_name)
    if fn:
        return await fn()
    return json.dumps({"error": f"未知工具: {tool_name}"}, ensure_ascii=False)


# ---------- 节点实现 ----------

async def _emit(state: MultiAgentState, event: dict) -> None:
    """发送 SSE 事件。"""
    if state.event_callback:
        await state.event_callback(event)


async def planner_node(state: MultiAgentState, llm: ChatOpenAI) -> MultiAgentState:
    """Planner 节点：分析意图，拆解子任务。"""
    start = time.time()
    await _emit(state, {"type": "plan_start", "query": state.user_query})

    messages = [SystemMessage(content=PLANNER_PROMPT)]
    if state.system_prompt:
        messages.append(SystemMessage(content=state.system_prompt))
    messages.append(HumanMessage(content=state.user_query))

    response = await llm.ainvoke(messages)
    output = response.content or ""
    _track_tokens(state, response)

    # 解析 Plan
    try:
        json_str = output.strip()
        if json_str.startswith("```"):
            json_str = json_str.split("\n", 1)[-1].rsplit("```", 1)[0].strip()

        plan = json.loads(json_str)
        state.plan_summary = plan.get("summary", "")
        subtasks_raw = plan.get("subtasks", [])
        state.subtasks = [
            Subtask(
                id=st.get("id", i + 1),
                description=st.get("description", ""),
                tool_hint=st.get("toolHint"),
                dependencies=st.get("dependencies", []),
            )
            for i, st in enumerate(subtasks_raw[:MAX_SUBTASKS])
        ]
    except Exception as e:
        logger.warning("Planner JSON parse failed, fallback to single task: %s", e)
        state.plan_summary = "直接回答用户问题"
        state.subtasks = [Subtask(id=1, description=state.user_query)]

    state.node_timings["planner"] = (time.time() - start) * 1000

    # 检索相关 RAG 上下文，供前端显示及评估
    try:
        from app.services import article_embedding_service
        similar = await article_embedding_service.search_similar(state.db, state.user_query, state.user_id, top_k=5)
        if similar:
            articles_ctx = [{"articleId": a.id, "title": a.title} for a in similar]
            state.rag_articles = articles_ctx
            await _emit(state, {
                "type": "rag_context",
                "articles": articles_ctx,
                "retrieval_mode": "vector",
            })
    except Exception:
        pass

    await _emit(state, {
        "type": "plan",
        "summary": state.plan_summary,
        "subtasks": [
            {
                "id": st.id,
                "description": st.description,
                "toolHint": st.tool_hint,
                "dependencies": st.dependencies,
            }
            for st in state.subtasks
        ],
    })
    return state


async def executor_node(state: MultiAgentState, llm: ChatOpenAI) -> MultiAgentState:
    """Executor 节点：执行子任务（支持并行）。"""
    start = time.time()

    if not state.subtasks:
        # 无子任务，直接回答
        await _emit(state, {"type": "subtask_start", "node": "direct-answer"})
        messages = [SystemMessage(content=EXECUTOR_PROMPT), HumanMessage(content=state.user_query)]
        response = await llm.ainvoke(messages)
        state.final_answer = response.content or ""
        _track_tokens(state, response)
        state.node_timings["executor"] = (time.time() - start) * 1000
        return state

    # 注入 Few-Shot Bad Case
    few_shot = ""
    try:
        from app.services.bad_case_collector import build_few_shot_prompt
        few_shot = await build_few_shot_prompt(state.db, state.user_query)
    except Exception:
        pass

    # 按依赖关系分批
    batches = _build_execution_batches(state.subtasks)

    for batch in batches:
        if len(batch) == 1:
            await _execute_subtask(batch[0], state, llm, few_shot)
        else:
            # 并行执行
            await _emit(state, {"type": "subtask_start", "node": f"parallel-{len(batch)}"})
            tasks = [_execute_subtask(st, state, llm, few_shot) for st in batch]
            await asyncio.gather(*tasks, return_exceptions=True)

    # 合并结果
    _merge_results(state)
    state.node_timings["executor"] = (time.time() - start) * 1000
    return state


async def _execute_subtask(
    subtask: Subtask,
    state: MultiAgentState,
    llm: ChatOpenAI,
    few_shot: str = "",
) -> None:
    """执行单个子任务。"""
    await _emit(state, {
        "type": "subtask_running",
        "subtask_id": subtask.id,
        "node": f"subtask-{subtask.id}"
    })

    tool_result_str = ""
    # 尝试自动调用建议的工具
    tool_name = subtask.tool_hint
    if not tool_name:
        if "searchArticles" in subtask.description or "搜索" in subtask.description or "查找" in subtask.description:
            tool_name = "searchArticles"
        elif "getCategories" in subtask.description or "分类" in subtask.description:
            tool_name = "getCategories"

    if tool_name:
        try:
            # 提取关键字
            kw = state.user_query.replace("帮我找", "").replace("帮我搜", "").replace("查找", "").replace("文章", "").replace("相关", "").strip()
            if not kw:
                kw = "后端"
            raw_res = await _execute_tool(tool_name, state.db, state.user_id, keyword=kw)
            tool_result_str = f"\n\n## 工具 [{tool_name}] 真实执行结果:\n{raw_res}"

            # 解析文章上下文并推送到前端作为 RAG 依据
            try:
                items = json.loads(raw_res)
                if isinstance(items, list) and items and isinstance(items[0], dict) and "id" in items[0]:
                    articles_event = [{"articleId": item["id"], "title": item.get("title", "")} for item in items]
                    state.rag_articles = articles_event
                    await _emit(state, {
                        "type": "rag_context",
                        "articles": articles_event,
                        "retrieval_mode": "vector",
                    })
            except Exception:
                pass
        except Exception as e:
            logger.warning("Tool execution error: %s", e)

    # 构建上下文
    context = f"## 当前子任务\nID: {subtask.id}\n描述: {subtask.description}"
    if subtask.tool_hint:
        context += f"\n建议工具: {subtask.tool_hint}"
    if tool_result_str:
        context += tool_result_str
    if subtask.dependencies:
        context += "\n\n## 前置任务结果"
        for dep_id in subtask.dependencies:
            dep_result = state.execution_results.get(dep_id)
            if dep_result:
                context += f"\n任务 {dep_id}: {dep_result[:500]}"
    context += f"\n\n## 用户原始请求\n{state.user_query}"

    system_msg = EXECUTOR_PROMPT + few_shot
    messages = [SystemMessage(content=system_msg), HumanMessage(content=context)]

    response = await llm.ainvoke(messages)
    output = response.content or ""
    _track_tokens(state, response)

    state.execution_results[subtask.id] = output
    state.tool_call_counts[f"subtask-{subtask.id}"] = state.tool_call_counts.get(f"subtask-{subtask.id}", 0) + 1

    await _emit(state, {"type": "subtask_result", "subtask_id": subtask.id, "result": output[:200]})


async def reviewer_node(state: MultiAgentState, llm: ChatOpenAI) -> MultiAgentState:
    """Reviewer 节点：审查执行结果。"""
    start = time.time()

    review_input = f"## 用户原始请求\n{state.user_query}\n\n## 执行计划\n{state.plan_summary}\n\n## 执行结果"
    for sid, result in state.execution_results.items():
        review_input += f"\n### 子任务 {sid}\n{result}"
    if state.retry_count > 0:
        review_input += f"\n\n## 之前的审查反馈\n{state.review_feedback}\n（这是第 {state.retry_count} 次重试）"

    messages = [SystemMessage(content=REVIEWER_PROMPT), HumanMessage(content=review_input)]
    response = await llm.ainvoke(messages)
    output = response.content or ""
    _track_tokens(state, response)

    # 解析审查结果
    try:
        json_str = output.strip()
        if json_str.startswith("```"):
            json_str = json_str.split("\n", 1)[-1].rsplit("```", 1)[0].strip()

        review = json.loads(json_str)
        state.review_decision = review.get("decision", "PASS").upper()
        state.review_feedback = review.get("feedback", "")

        if state.review_decision == "REVISE":
            suggestions = review.get("suggestions", [])
            if suggestions:
                state.review_feedback += "\n修正建议："
                for i, s in enumerate(suggestions, 1):
                    state.review_feedback += f"\n{i}. {s}"
    except Exception as e:
        logger.warning("Reviewer JSON parse failed, defaulting to PASS: %s", e)
        state.review_decision = "PASS"
        state.review_feedback = ""

    state.node_timings["reviewer"] = (time.time() - start) * 1000

    # FAIL 或重试耗尽的 REVISE -> 收集 Bad Case
    if state.review_decision == "FAIL" or (state.review_decision == "REVISE" and state.retry_count >= MAX_RETRIES):
        try:
            from app.services.bad_case_collector import collect
            await collect(
                state.db,
                state.user_id,
                state.user_query,
                state.final_answer,
                state.review_feedback,
                "Planner->Executor->Reviewer",
                state.tokens_in + state.tokens_out,
                int(sum(state.node_timings.values())),
            )
        except Exception:
            pass

    await _emit(state, {
        "type": "review",
        "decision": state.review_decision,
        "feedback": state.review_feedback,
        "retry_count": state.retry_count,
    })
    return state


async def synthesizer_node(state: MultiAgentState, llm: ChatOpenAI) -> MultiAgentState:
    """合成最终回答。"""
    if not state.final_answer and state.execution_results:
        _merge_results(state)

    # 流式输出最终回答
    full_content = ""
    messages = [
        SystemMessage(content="你是 Starlore 博客助手。请根据以下执行结果，生成一个完整、友好的最终回答。"),
        HumanMessage(content=state.final_answer or "没有执行结果"),
    ]

    async for chunk in llm.astream(messages):
        if chunk.content:
            full_content += chunk.content
            await _emit(state, {"type": "content", "content": chunk.content})
        _track_tokens(state, chunk)

    if full_content:
        state.final_answer = full_content

    await _emit(state, {
        "type": "metrics",
        "total_tokens_in": state.tokens_in,
        "total_tokens_out": state.tokens_out,
        "total_latency_ms": int(sum(state.node_timings.values())),
    })
    return state


# ---------- 辅助函数 ----------

def _track_tokens(state: MultiAgentState, response) -> None:
    """从 LangChain response 中提取 token 使用量。"""
    if hasattr(response, "usage_metadata") and response.usage_metadata:
        state.tokens_in += getattr(response.usage_metadata, "input_tokens", 0) or 0
        state.tokens_out += getattr(response.usage_metadata, "output_tokens", 0) or 0


def _build_execution_batches(subtasks: list[Subtask]) -> list[list[Subtask]]:
    """按依赖关系拓扑排序，分批执行。"""
    batches: list[list[Subtask]] = []
    completed: set[int] = set()
    remaining = {st.id for st in subtasks}

    while remaining:
        batch = []
        for st in subtasks:
            if st.id in completed:
                continue
            if all(d in completed for d in st.dependencies):
                batch.append(st)

        if not batch:
            # 防死循环：强制放入剩余任务
            batch = [st for st in subtasks if st.id in remaining]
            batches.append(batch)
            break

        for st in batch:
            completed.add(st.id)
            remaining.discard(st.id)
        batches.append(batch)

    return batches


def _merge_results(state: MultiAgentState) -> None:
    """合并所有子任务结果为最终答案。"""
    if not state.execution_results:
        state.final_answer = "执行完成，但没有产生结果。"
        return

    parts = ["## 执行结果\n"]
    for sid, result in sorted(state.execution_results.items()):
        parts.append(f"### 子任务 {sid}\n{result}")
    state.final_answer = "\n\n".join(parts)


# ---------- 条件路由 ----------

def _route_after_review(state: MultiAgentState) -> str:
    """Reviewer 之后的路由决策。"""
    if state.review_decision == "PASS":
        return "synthesizer"
    if state.review_decision == "REVISE" and state.retry_count < MAX_RETRIES:
        state.retry_count += 1
        return "executor"
    return "synthesizer"


# ---------- 公开接口 ----------

async def run_multi_agent(
    db,
    user_id: int,
    llm: ChatOpenAI,
    messages: list[dict],
    character_prompt: str = "",
    event_callback=None,
) -> MultiAgentState:
    """运行 Multi-Agent Planner-Executor-Reviewer 工作流。

    Args:
        db: AsyncSession
        user_id: 用户 ID
        llm: ChatOpenAI 实例
        messages: 对话历史
        character_prompt: 角色卡 prompt
        event_callback: SSE 事件回调 async def callback(event: dict)

    Returns:
        MultiAgentState 最终状态
    """
    state = MultiAgentState(
        user_query=messages[-1].get("content", "") if messages else "",
        user_id=user_id,
        db=db,
        system_prompt=character_prompt,
        trace_id=uuid.uuid4().hex[:16],
        event_callback=event_callback,
    )

    # 路由循环
    current_node = "planner"
    max_steps = 20
    step = 0

    while step < max_steps:
        step += 1

        if current_node == "planner":
            state = await planner_node(state, llm)
            current_node = "executor"

        elif current_node == "executor":
            state = await executor_node(state, llm)
            current_node = "reviewer"

        elif current_node == "reviewer":
            state = await reviewer_node(state, llm)
            route = _route_after_review(state)
            if route == "synthesizer":
                current_node = "synthesizer"
            else:
                current_node = "executor"  # 重试

        elif current_node == "synthesizer":
            state = await synthesizer_node(state, llm)
            break

    await _emit(state, {"type": "done"})

    # 记录指标
    try:
        from app.services.agent_metrics import record_execution
        record_execution(
            tokens_in=state.tokens_in,
            tokens_out=state.tokens_out,
            node_latencies=state.node_timings,
            tool_calls=state.tool_call_counts,
            review_decision=state.review_decision,
        )
    except Exception:
        pass

    return state
