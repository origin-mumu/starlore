"""云端智能体核心调度引擎 (ReAct Turn & Step 状态机)。"""

import asyncio
import json
import logging
import time
from collections.abc import AsyncGenerator
from typing import Any

import httpx
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.harness_message import HarnessMessage
from app.models.harness_session import HarnessSession
from app.services.ai_usage_service import finish_log
from app.services.harness_content_tools import HARNESS_TOOL_MAP, HARNESS_TOOLS
from app.services.harness_service import (
    get_session,
    resolve_model_credentials,
    save_harness_todos,
)
from app.services.harness_subagent import (
    DELEGATE_TOOL_SCHEMA,
    TODO_WRITE_SCHEMA,
    run_final_review,
    run_sub_agent_owned_session,
    validate_todos,
)

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """你是一个专业的云端全能内容生产与知识库智能体 (Starlore Cloud Content Agent)。
你的核心使命是自主识别用户意图，充分利用可用的生产与管理工具完成高质量的内容交付与知识库治理，绝不做无意义的机械问答。

【语言与排版规范（最高优先级强制执行）】
1. **全链路严格使用中文**：无论是你的深度思维链推导（Thinking / Reasoning）、每一步的中间规划与草稿阐述（Scratchpad）、工具调用前后的分析，还是最终的回复正文，**必须全部、全程使用规范流畅的中文**！
2. **严禁使用英文进行思维推理或草稿撰写**：除必要专有名词（如 PPT、Word、Docker、MinIO、API 等）外，绝对禁止出现任何英文内心独白。所有推导和思考都必须地道自然地用中文展开。
3. **全链路严格禁止使用任何原生表情符号（Emoji）**：
   - 绝对禁止在正文、标题、各级列表项、强调短语、问候、自我介绍或思考过程中输出任何原生表情符号（严禁出现 🤖、🎨、📚、✍️、👤、💡、✨、🚀、🎉、👏、🔥、📌、🎯 等任何 Emoji 表情）。
   - 保持极简、沉稳、严谨专业的技术工作台风格。所有的信息层级与逻辑组织必须完全依靠规范的 Markdown 语法（如各级标题 `#`、`##`、`###`、加粗 `**`、有序数字序号、标准无序列表 `-`）来呈现，绝不依靠表情符号作为前缀或装饰。
4. **专业克制的表达语调**：
   - 自我介绍与引导时，直接阐述产品定位与核心能力，使用专业清晰的技术顾问语调，严禁出现低幼化卖萌或滥用感叹号与情绪装饰。

【核心行为准则与可用工具指南】
1. **内容生产与产物交付**：
   - 生成 PPT、演示文稿：结合需求先分析是否需要检索知识库素材或用户个人简历背景（调用 `search_knowledge` 或 `get_resume_detail`）；随后根据结构化大纲调用 `generate_ppt` 生成文件（善用 cards 卡片栅格与 bullets 要点）；最后输出精炼大纲介绍。
   - 生成 Word 文档、技术方案、演进报告：先调用 `search_knowledge` 或 `get_article_detail` 汲取真实依据；随后调用 `generate_document` 生成排版规范的 Word 文档并交付 MinIO 下载。

2. **知识库深度研读与检索**：
   - 知识库问答或寻找特定资料时：优先调用 `search_knowledge` 进行混合语义检索；
   - 若检索摘要信息不够充分、需要阅读完整技术方案、代码或详细细节时：**主动根据文章 ID 调用 `get_article_detail` 获取全文深入研读**。
   - 查询大盘统计数据（总文章数、分类、浏览量等）：调用 `get_blog_stats`。
   - 查看最新文章或特定分类文章：调用 `get_recent_articles` 或 `get_articles_by_category`。

3. **用户简历与个人档案档案联动**：
   - 当用户询问自身背景、简历内容、求职项目，或要求“根据我的经历做一份汇报/PPT/技术自述”时：
   - 积极调用 `get_user_resumes` 查看用户的简历列表，或调用 `get_resume_detail` 调阅用户的完整教育背景、项目经历、技术栈与岗位职责，产出高度贴合用户真实背景的定制化内容。

4. **知识库主动治理与维护**：
   - 撰写新文章：调用 `write_article`，系统会自动进行切片入库；
   - 修改/完善文章（补充摘要、修正标签、润色内容）：调用 `update_article`；
   - 扫描需要补充摘要或标签的文章：调用 `get_articles_missing_metadata`；
   - 删除文章或创建分类：调用 `delete_article` 或 `create_category`。

5. **互联网实时与前沿检索 (web_search) —— 最高优先级响应外部与实时信息**：
   - 你已完全具备接入互联网的实时搜索能力（通过 `web_search` 工具）；
   - **严禁在未调用 web_search 之前声称“无法获取外部实时信息/不掌握实时新闻”或拒绝回答**！
   - 当用户询问任何“新闻”、“实时资讯”、“最新热点”、“行业动态”、“最新版本/发布”，或询问任何本地知识库未涉及的外部事实时，**必须第一时间直接调用 `web_search`** 全网检索最新公开资讯；
   - 若本地知识库 `search_knowledge` 未能检索到有效结果，必须主动调用 `web_search` 检索外部资料进行补充；
   - 检索完成后，结合搜索结果提炼核心要点进行权威解答，或作为素材驱动 `generate_ppt` / `generate_document` 生产交付。

6. **最终回复规范**：
   - 当生成了 PPT 或文档后，下载卡片由系统自动挂载，回复正文提供高可读性的 Markdown 概述与要点提炼，不要输出冗长的大段文件代码。

7. **任务清单 (todo_write) —— 复杂任务的进度台账**：
   - 当任务包含多个环节或阶段（如多路检索、批量治理、先调研再生成等），开始执行前先调用 `todo_write` 一次性列出全部事项（status=pending），随后随执行进度实时整表更新：正在做的标 in_progress，做完的立即标 completed；
   - 简单问答、日常对话严禁使用任务清单。

8. **任务委派 (delegate_task) —— 可并行工作的分身**：
   - 当存在多项互不依赖、可独立完成的工作（如并行检索多个主题、批量补全多篇文档、独立生成配套文件），可在同一轮内多次调用 `delegate_task` 将它们委派给专项子代理并行处理；
   - 委派描述（description）必须自包含：写清背景、目标与约束，让子代理无需追问即可完成；
   - 有先后依赖的工作不要同时委派，应等前置结果返回后再委派后续任务。
"""


def _format_sse(event_type: str, data: dict[str, Any]) -> str:
    """封装标准 SSE 格式字符串。"""
    return f"event: {event_type}\ndata: {json.dumps(data, ensure_ascii=False)}\n\n"


async def run_harness_turn(
    db: AsyncSession,
    session_id: int,
    user_id: int,
    user_input: str,
    images: list[str] | None = None,
    override_model_id: str | None = None,
    usage_log_id: int | None = None,
) -> AsyncGenerator[str, None]:
    """执行一轮 ReAct 多步智能体生产循环，持续产出 SSE 事件流。"""
    start_time = time.time()
    session = await get_session(db, session_id, user_id)
    if not session:
        yield _format_sse("error", {"message": "会话不存在或无权访问"})
        return

    # 确定使用的模型：没有可用厂商配置时直接向前端报错，不做任何模型兜底
    model_id = override_model_id or session.model_id or ""
    if not model_id:
        yield _format_sse("error", {"message": "暂无可用模型，请先在后台 AI 配置中启用厂商与模型"})
        return
    try:
        api_url, api_key, model_id = await resolve_model_credentials(db, model_id)
    except ValueError as e:
        yield _format_sse("error", {"message": str(e)})
        return

    # 1. 记录用户消息到数据库
    user_msg = HarnessMessage(
        session_id=session_id,
        user_id=user_id,
        role="user",
        content=user_input,
        artifacts={"images": images} if images else None,
    )
    db.add(user_msg)
    # 更新会话标题（若是新会话，自动截取前 20 字作为标题）
    if session.title in ["新会话", "新任务"] and user_input:
        session.title = user_input.strip()[:24]
    session.status = "running"
    session.model_id = model_id
    await db.commit()

    # 2. 组装历史上下文消息
    from app.services.harness_service import list_messages
    past_messages = await list_messages(db, session_id, user_id)
    
    context_messages: list[dict[str, Any]] = [
        {"role": "system", "content": SYSTEM_PROMPT}
    ]
    # 保留最近 10 条历史消息作为上下文
    for m in past_messages[-10:]:
        if m.role == "user":
            user_imgs: list[str] = []
            if m.artifacts and isinstance(m.artifacts, dict) and "images" in m.artifacts:
                user_imgs = m.artifacts.get("images") or []

            if user_imgs:
                # 按照 OpenAI / DeepSeek 多模态视觉标准格式组装
                content_parts: list[dict[str, Any]] = []
                if m.content:
                    content_parts.append({"type": "text", "text": m.content})
                for img_url in user_imgs:
                    content_parts.append({
                        "type": "image_url",
                        "image_url": {"url": img_url},
                    })
                context_messages.append({"role": "user", "content": content_parts})
            elif m.content:
                context_messages.append({"role": "user", "content": m.content})
        elif m.role == "assistant" and m.content:
            context_messages.append({"role": "assistant", "content": m.content})

    tools_declaration = [tool.to_openai_tool() for tool in HARNESS_TOOLS]
    tools_declaration.append(DELEGATE_TOOL_SCHEMA)
    tools_declaration.append(TODO_WRITE_SCHEMA)

    # 初始化本轮追踪数据
    step = 0
    max_steps = 10
    total_prompt_tokens = 0
    total_completion_tokens = 0
    collected_reasoning: list[str] = []
    collected_content: list[str] = []
    recorded_tool_calls: list[dict[str, Any]] = []
    recorded_artifacts: list[dict[str, Any]] = []
    recorded_steps: list[dict[str, Any]] = []
    # 委派与自查状态：发生过委派的轮次在交卷前做一次质量自查
    delegation_used = False
    correction_used = 0
    max_final_corrections = 1
    work_digest_parts: list[str] = []

    try:
        async with httpx.AsyncClient(timeout=600.0) as client:
            while step < max_steps:
                step += 1
                step_title = f"步骤 {step}"
                yield _format_sse("step", {"step": step, "title": step_title})

                payload: dict[str, Any] = {
                    "model": model_id,
                    "messages": context_messages,
                    "tools": tools_declaration,
                    "tool_choice": "auto",
                    "stream": True,
                }
                headers = {
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                }
                endpoint = f"{api_url.rstrip('/')}/chat/completions"

                current_step_reasoning = ""
                current_step_content = ""
                tool_calls_accumulator: dict[int, dict[str, Any]] = {}
                has_detected_tool_calls = False
                pending_content_buffer: list[str] = []
                content_stream_active = False
                in_think_tag = False
                last_token_time = time.time()

                async with client.stream("POST", endpoint, headers=headers, json=payload) as response:
                    if response.status_code != 200:
                        err_text = await response.aread()
                        logger.error("LLM upstream error %s: %s", response.status_code, err_text.decode("utf-8", "ignore"))
                        yield _format_sse("error", {"message": f"模型服务响应异常 ({response.status_code})"})
                        return

                    async for line in response.aiter_lines():
                        now = time.time()
                        if now - last_token_time > 60.0:
                            # 连续 60s 没有任何 token 吐出
                            logger.warning("LLM token streaming inactivity timeout (60s)")
                            yield _format_sse("error", {"message": "模型流式输出卡死超时"})
                            return

                        line_str = line.strip()
                        if not line_str or not line_str.startswith("data:"):
                            continue

                        raw_data = line_str[5:].strip()
                        if raw_data == "[DONE]":
                            break

                        try:
                            chunk = json.loads(raw_data)
                        except Exception:
                            continue

                        choices = chunk.get("choices", [])
                        if not choices:
                            continue

                        delta = choices[0].get("delta", {})
                        last_token_time = time.time()

                        # 1. 思考链增量：逐 Token 实时推送给客户端
                        r_delta = delta.get("reasoning_content") or delta.get("reasoning")
                        if r_delta:
                            current_step_reasoning += r_delta
                            collected_reasoning.append(r_delta)
                            yield _format_sse("reasoning", {"delta": r_delta, "step": step})

                        # 2. 工具调用增量检测
                        tc_delta_list = delta.get("tool_calls", [])
                        if tc_delta_list:
                            has_detected_tool_calls = True
                            # 只要检测到工具调用，清空任何暂存正文，严防中间草稿泄漏到正文
                            pending_content_buffer.clear()
                            for tc_chunk in tc_delta_list:
                                raw_idx = tc_chunk.get("index")
                                idx = 0 if raw_idx is None else int(raw_idx)
                                if idx not in tool_calls_accumulator:
                                    tool_calls_accumulator[idx] = {
                                        "id": tc_chunk.get("id") or f"call_{idx}_{int(time.time())}",
                                        "name": "",
                                        "arguments": "",
                                    }
                                if tc_chunk.get("id"):
                                    tool_calls_accumulator[idx]["id"] = tc_chunk["id"]
                                func_chunk = tc_chunk.get("function") or {}
                                if func_chunk.get("name"):
                                    tool_calls_accumulator[idx]["name"] += func_chunk["name"]
                                if func_chunk.get("arguments"):
                                    tool_calls_accumulator[idx]["arguments"] += func_chunk["arguments"]

                        # 3. 文本回答增量（处理 <think> 分流，严防思考内容泄漏到正文）
                        c_delta = delta.get("content")
                        if c_delta:
                            # 3.1 兼容部分推理模型输出 <think>...</think> 标签，将其剥离并路由至 reasoning 思考流
                            if "<think>" in c_delta or in_think_tag:
                                text_rem = c_delta
                                while text_rem:
                                    if not in_think_tag:
                                        if "<think>" in text_rem:
                                            before, after = text_rem.split("<think>", 1)
                                            if before:
                                                current_step_content += before
                                                if not has_detected_tool_calls and not delegation_used and (step > 1 or content_stream_active):
                                                    collected_content.append(before)
                                                    yield _format_sse("content", {"delta": before, "step": step})
                                                elif not has_detected_tool_calls:
                                                    pending_content_buffer.append(before)
                                            in_think_tag = True
                                            text_rem = after
                                        else:
                                            current_step_content += text_rem
                                            if not has_detected_tool_calls and not delegation_used and (step > 1 or content_stream_active):
                                                collected_content.append(text_rem)
                                                yield _format_sse("content", {"delta": text_rem, "step": step})
                                            elif not has_detected_tool_calls:
                                                pending_content_buffer.append(text_rem)
                                            text_rem = ""
                                    else:
                                        if "</think>" in text_rem:
                                            thought_chunk, after_think = text_rem.split("</think>", 1)
                                            if thought_chunk:
                                                current_step_reasoning += thought_chunk
                                                collected_reasoning.append(thought_chunk)
                                                yield _format_sse("reasoning", {"delta": thought_chunk, "step": step})
                                            in_think_tag = False
                                            text_rem = after_think
                                        else:
                                            current_step_reasoning += text_rem
                                            collected_reasoning.append(text_rem)
                                            yield _format_sse("reasoning", {"delta": text_rem, "step": step})
                                            text_rem = ""
                                continue

                            current_step_content += c_delta
                            if has_detected_tool_calls:
                                # 本步已检测到工具调用，该过渡文本纯属内部思考规划，绝不推向正文
                                pass
                            else:
                                # 若是后续总结步 (step > 1) 或已确认为非工具纯文本回答，逐 Token 实时推送正文
                                # 委派轮全程缓冲：最终正文需等交卷自查通过后再流出
                                if not delegation_used and (step > 1 or content_stream_active):
                                    collected_content.append(c_delta)
                                    yield _format_sse("content", {"delta": c_delta, "step": step})
                                else:
                                    # step == 1 且有工具声明时，缓存文本，等待确认是否有 tool_calls 触发
                                    pending_content_buffer.append(c_delta)
                                    if not delegation_used and len("".join(pending_content_buffer)) >= 120:
                                        content_stream_active = True
                                        for p_chunk in pending_content_buffer:
                                            collected_content.append(p_chunk)
                                            yield _format_sse("content", {"delta": p_chunk, "step": step})
                                        pending_content_buffer.clear()

                        # 统计 Token
                        usage = chunk.get("usage")
                        if usage:
                            total_prompt_tokens += usage.get("prompt_tokens", 0)
                            total_completion_tokens += usage.get("completion_tokens", 0)

                # 判断本步是否有工具调用
                if not tool_calls_accumulator:
                    # 没有发起工具调用，本步为最终回答草稿
                    draft_text = current_step_content + "".join(pending_content_buffer)
                    pending_content_buffer.clear()
                    final_content = draft_text

                    # 委派轮：交卷前质量自查（最多修正一轮）
                    if delegation_used and correction_used < max_final_corrections:
                        review = await run_final_review(
                            db=db,
                            user_id=user_id,
                            api_url=api_url,
                            api_key=api_key,
                            model_id=model_id,
                            user_request=user_input,
                            work_digest="".join(work_digest_parts),
                            draft=draft_text,
                        )
                        if review["decision"] == "FAIL":
                            correction_used += 1
                            review_text = f"质量自查未通过：{review['feedback']}"
                            recorded_steps.append({
                                "step": step,
                                "title": f"步骤 {step}：草稿自查未通过",
                                "reasoning": current_step_reasoning,
                                "scratchpad": review_text,
                                "tool_calls": [],
                            })
                            yield _format_sse("step_thought", {"step": step, "text": review_text})
                            try:
                                from app.services.bad_case_collector import collect
                                await collect(
                                    db, user_id, user_input, draft_text, review["feedback"],
                                    "ReAct+delegate",
                                    total_prompt_tokens + total_completion_tokens,
                                    int((time.time() - start_time) * 1000),
                                )
                            except Exception:
                                pass
                            # 把草稿与审查意见回注上下文，进入修正轮
                            context_messages.append({"role": "assistant", "content": draft_text})
                            context_messages.append({"role": "user", "content": (
                                f"质量自查未通过：{review['feedback']}\n"
                                "请修正以上问题，重新给出完整、诚实的最终回答。"
                            )})
                            final_content = ""
                            continue

                    if delegation_used:
                        # 委派轮草稿此前全程缓冲，自查通过（或修正轮直接采信）后一次性流出
                        if draft_text:
                            collected_content.append(draft_text)
                            yield _format_sse("content", {"delta": draft_text, "step": step})
                    else:
                        # 普通轮：补发尚未流出的余量（如第一步缓冲未达阈值的尾部）
                        streamed_len = len("".join(collected_content))
                        tail = draft_text[streamed_len:]
                        if tail:
                            collected_content.append(tail)
                            yield _format_sse("content", {"delta": tail, "step": step})

                    recorded_steps.append({
                        "step": step,
                        "title": f"步骤 {step}：总结回答",
                        "reasoning": current_step_reasoning,
                        "scratchpad": "",
                        "tool_calls": [],
                    })
                    break

                # 本步发起了工具调用：清空任何未推送的正文缓存，严防草稿泄漏
                pending_content_buffer.clear()

                # 本步发起了工具调用：该步的 current_step_content 纯属思考规划/中间草稿，仅呈现在思考折叠框内
                if current_step_content.strip():
                    yield _format_sse("step_thought", {
                        "step": step,
                        "text": current_step_content,
                    })

                assistant_tool_calls_payload = []
                tool_executions = []
                seen_signatures = set()
                for idx, tc in sorted(tool_calls_accumulator.items(), key=lambda x: str(x[0])):
                    call_id = tc["id"]
                    fn_name = tc["name"].strip()
                    args_raw = tc["arguments"].strip()

                    if not fn_name:
                        continue
                    sig = (call_id, fn_name, args_raw)
                    if sig in seen_signatures:
                        continue
                    seen_signatures.add(sig)

                    assistant_tool_calls_payload.append({
                        "id": call_id,
                        "type": "function",
                        "function": {"name": fn_name, "arguments": args_raw},
                    })
                    tool_executions.append((call_id, fn_name, args_raw))

                # 按照 OpenAI 规范，先将带有全部 tool_calls 的 assistant 消息推入上下文
                context_messages.append({
                    "role": "assistant",
                    "content": current_step_content or None,
                    "tool_calls": assistant_tool_calls_payload,
                })

                # 依次执行各工具并记录在当前步骤的 tool_calls 中
                step_tool_calls = []
                delegate_runs: list[tuple[str, str, str, Any]] = []  # (call_id, label, description, task)
                for call_id, fn_name, args_raw in tool_executions:
                    # 解析工具参数
                    try:
                        args = json.loads(args_raw) if args_raw else {}
                    except Exception as e:
                        logger.warning("Failed to parse tool arguments: %s", e)
                        args = {}

                    # 内部工具：任务清单整表快照（不产生动作行，由前端清单面板呈现）
                    if fn_name == "todo_write":
                        todos = validate_todos(args.get("todos"))
                        if todos:
                            try:
                                await save_harness_todos(db, session_id, user_id, todos)
                            except Exception as e:
                                logger.warning("Persist harness todos failed: %s", e)
                            yield _format_sse("todo", {"todos": todos})
                            tool_content = f"任务清单已更新（共 {len(todos)} 项）"
                        else:
                            tool_content = (
                                "todo_write 参数无效：todos 需为非空数组，"
                                "每项包含 content 与 status(pending|in_progress|completed)"
                            )
                        context_messages.append({
                            "role": "tool",
                            "tool_call_id": call_id,
                            "content": tool_content,
                        })
                        continue

                    # 内部工具：委派子代理（动作行 label 使用模型给出的委派描述）
                    if fn_name == "delegate_task":
                        description = str(args.get("description") or "").strip()
                        if not description:
                            context_messages.append({
                                "role": "tool",
                                "tool_call_id": call_id,
                                "content": "delegate_task 参数无效：description 不能为空",
                            })
                            continue
                        delegation_used = True
                        label_name = description[:40]
                        yield _format_sse("tool_start", {
                            "call_id": call_id,
                            "tool": "delegate_task",
                            "label": label_name,
                            "step": step,
                        })
                        context_digest = f"用户总体请求：{user_input}"
                        if work_digest_parts:
                            context_digest += "\n已完成工作结果：\n" + "".join(work_digest_parts)[-4000:]
                        expected_output = str(args.get("expected_output") or "").strip() or None
                        task = asyncio.create_task(run_sub_agent_owned_session(
                            db=db,
                            user_id=user_id,
                            api_url=api_url,
                            api_key=api_key,
                            model_id=model_id,
                            description=description,
                            expected_output=expected_output,
                            context_digest=context_digest,
                        ))
                        delegate_runs.append((call_id, label_name, description, task))
                        continue

                    tool_instance = HARNESS_TOOL_MAP.get(fn_name)
                    label_name = tool_instance.description.split("，")[0] if tool_instance else fn_name
                    yield _format_sse("tool_start", {
                        "call_id": call_id,
                        "tool": fn_name,
                        "label": label_name,
                        "step": step,
                    })

                    # 执行工具
                    if tool_instance:
                        try:
                            tool_res = await tool_instance.execute(db=db, user_id=user_id, **args)
                        except Exception as ex:
                            logger.exception("Tool %s execution failed: %s", fn_name, ex)
                            tool_res = ToolResult(
                                success=False,
                                label=fn_name,
                                summary=f"执行异常: {ex}",
                                data=f"工具执行异常: {ex}",
                            )
                    else:
                        tool_res = ToolResult(
                            success=False,
                            label=fn_name,
                            summary="未找到对应工具",
                            data="未找到对应工具",
                        )

                    if tool_res.success:
                        # 若工具输出了 MinIO 交付物，推送 artifact 事件
                        if tool_res.artifact:
                            recorded_artifacts.append(tool_res.artifact)
                            yield _format_sse("artifact", tool_res.artifact)

                        tool_item = {
                            "call_id": call_id,
                            "tool": fn_name,
                            "label": tool_res.label,
                            "summary": tool_res.summary,
                            "citations": tool_res.citations,
                            "status": "success",
                        }
                        step_tool_calls.append(tool_item)
                        recorded_tool_calls.append(tool_item)

                        yield _format_sse("tool_done", {
                            "call_id": call_id,
                            "tool": fn_name,
                            "label": tool_res.label,
                            "summary": tool_res.summary,
                            "citations": tool_res.citations,
                            "status": "success",
                            "step": step,
                        })

                        tool_content_str = (
                            json.dumps(tool_res.data, ensure_ascii=False)
                            if not isinstance(tool_res.data, str)
                            else tool_res.data
                        )
                        context_messages.append({
                            "role": "tool",
                            "tool_call_id": call_id,
                            "content": tool_content_str,
                        })
                    else:
                        err_summary = tool_res.summary or "工具未执行成功"
                        tool_item = {
                            "call_id": call_id,
                            "tool": fn_name,
                            "label": tool_res.label,
                            "summary": err_summary,
                            "status": "error",
                        }
                        step_tool_calls.append(tool_item)
                        recorded_tool_calls.append(tool_item)

                        yield _format_sse("tool_done", {
                            "call_id": call_id,
                            "tool": fn_name,
                            "label": tool_res.label,
                            "summary": err_summary,
                            "status": "error",
                            "step": step,
                        })
                        context_messages.append({
                            "role": "tool",
                            "tool_call_id": call_id,
                            "content": f"工具执行提示: {err_summary}。请基于目前已有信息或通用知识继续回答，告知用户查询情况。",
                        })

                # 汇收并行委派结果
                if delegate_runs:
                    results = await asyncio.gather(
                        *[run[3] for run in delegate_runs], return_exceptions=True
                    )
                    for (call_id, label_name, description, _task), res in zip(delegate_runs, results):
                        if isinstance(res, Exception):
                            logger.exception("Delegate task failed: %s", res)
                            tool_item = {
                                "call_id": call_id,
                                "tool": "delegate_task",
                                "label": label_name,
                                "summary": f"委派执行异常: {res}",
                                "status": "error",
                            }
                            tool_content = f"委派任务执行异常: {res}"
                        elif res.get("ok"):
                            total_prompt_tokens += res.get("tokens_prompt", 0)
                            total_completion_tokens += res.get("tokens_completion", 0)
                            one_line = " ".join(str(res.get("content") or "").split())[:80]
                            tool_item = {
                                "call_id": call_id,
                                "tool": "delegate_task",
                                "label": label_name,
                                "summary": one_line or "委派任务完成",
                                "citations": [],
                                "status": "success",
                            }
                            for art in res.get("artifacts") or []:
                                recorded_artifacts.append(art)
                                yield _format_sse("artifact", art)
                            work_digest_parts.append(
                                f"\n### 委派：{description}\n{str(res.get('content') or '')[:4000]}"
                            )
                            tool_content = res.get("content") or "子代理未返回内容"
                        else:
                            total_prompt_tokens += res.get("tokens_prompt", 0)
                            total_completion_tokens += res.get("tokens_completion", 0)
                            err = res.get("error") or "子代理执行失败"
                            tool_item = {
                                "call_id": call_id,
                                "tool": "delegate_task",
                                "label": label_name,
                                "summary": err[:80],
                                "status": "error",
                            }
                            work_digest_parts.append(f"\n### 委派（失败）：{description}\n{err}")
                            tool_content = f"委派任务失败: {err}。请基于已有信息继续或调整方案。"
                        step_tool_calls.append(tool_item)
                        recorded_tool_calls.append(tool_item)
                        yield _format_sse("tool_done", {
                            "call_id": call_id,
                            "tool": "delegate_task",
                            "label": tool_item.get("label"),
                            "summary": tool_item.get("summary"),
                            "citations": tool_item.get("citations"),
                            "status": tool_item.get("status"),
                            "step": step,
                        })
                        context_messages.append({
                            "role": "tool",
                            "tool_call_id": call_id,
                            "content": tool_content,
                        })
                    delegate_runs.clear()

                # 记录本步骤的思考与工具调用集合
                recorded_steps.append({
                    "step": step,
                    "title": f"步骤 {step}",
                    "reasoning": current_step_reasoning,
                    "scratchpad": current_step_content,
                    "tool_calls": step_tool_calls,
                })

        # 循环结束，计算耗时与保存
        duration_ms = int((time.time() - start_time) * 1000)
        final_reasoning = "".join(collected_reasoning)
        if not final_content:
            final_content = current_step_content or "".join(collected_content)

        # 持久化 assistant 消息到数据库
        assistant_msg = HarnessMessage(
            session_id=session_id,
            user_id=user_id,
            role="assistant",
            content=final_content,
            reasoning_content=final_reasoning if final_reasoning else None,
            tool_calls=recorded_tool_calls if recorded_tool_calls else None,
            artifacts=recorded_artifacts if recorded_artifacts else None,
            step_details=recorded_steps if recorded_steps else None,
            tokens_prompt=total_prompt_tokens,
            tokens_completion=total_completion_tokens,
            duration_ms=duration_ms,
        )
        db.add(assistant_msg)
        session.status = "idle"
        if usage_log_id:
            await finish_log(
                db,
                usage_log_id,
                duration_ms=duration_ms,
                tokens_prompt=total_prompt_tokens,
                tokens_completion=total_completion_tokens,
            )
        await db.commit()

        # 记录执行指标（可观测性）
        try:
            from app.services.agent_metrics import record_execution
            tool_call_counts: dict[str, int] = {}
            for tc in recorded_tool_calls:
                tc_name = tc.get("tool") or "unknown"
                tool_call_counts[tc_name] = tool_call_counts.get(tc_name, 0) + 1
            if correction_used > 0:
                turn_review_decision = "FAIL"
            elif delegation_used:
                turn_review_decision = "PASS"
            else:
                turn_review_decision = None
            record_execution(
                tokens_in=total_prompt_tokens,
                tokens_out=total_completion_tokens,
                tool_calls=tool_call_counts,
                review_decision=turn_review_decision,
            )
        except Exception:
            pass

        # 推送完成信号
        yield _format_sse("done", {
            "duration_ms": duration_ms,
            "tokens_prompt": total_prompt_tokens,
            "tokens_completion": total_completion_tokens,
            "steps": step,
            "step_details": recorded_steps,
        })

    except asyncio.CancelledError:
        logger.info("Harness turn cancelled by client")
        session.status = "idle"
        if usage_log_id:
            await finish_log(db, usage_log_id, tokens_prompt=total_prompt_tokens, tokens_completion=total_completion_tokens)
        await db.commit()
    except Exception as e:
        logger.exception("Harness turn failed: %s", e)
        session.status = "error"
        if usage_log_id:
            await finish_log(db, usage_log_id, status="error")
        await db.commit()
        yield _format_sse("error", {"message": f"执行遇到错误: {str(e)}"})
