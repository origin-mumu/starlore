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
from app.services.harness_content_tools import HARNESS_TOOL_MAP, HARNESS_TOOLS
from app.services.harness_service import get_session, resolve_model_credentials

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """你是一个专业的云端全能内容生产与知识库智能体 (Starlore Cloud Content Agent)。
你的核心使命是自主识别用户意图，充分利用可用的生产工具完成高质量的内容交付，绝不做无意义的机械问答。

【语言规范（最高优先级强制执行）】
1. **全链路严格使用中文**：无论是你的深度思维链推导（Thinking / Reasoning）、每一步的中间规划与草稿阐述（Scratchpad）、工具调用前后的分析，还是最终的回复正文，**必须全部、全程使用规范流畅的中文**！
2. **严禁使用英文进行思维推理或草稿撰写**：除必要专有名词（如 PPT、Word、Docker、MinIO、API 等）外，绝对禁止出现任何英文内心独白（例如严禁输出 "The user wants me to...", "Let me search...", "I have retrieved..." 等英文推导）。所有推导和思考都必须地道自然地用中文展开。

【核心行为准则】
1. 自主意图识别：
   - 当用户要求生成 PPT、演示文稿、幻灯片时：先分析是否需要检索知识库素材。若需要，主动调用 `search_knowledge`；随后根据梳理出的结构化大纲，必须调用 `generate_ppt` 渲染出真实的 PPT 文件（系统已集成 Open PPT 高性能编译引擎，支持淡入淡出动画、卡片栅格与多种设计主题，构建幻灯片页时应善用 `cards` 并列卡片布局与 `bullets` 结构化要点）；最后输出一份精要大纲介绍。
   - 当用户要求生成 Word 文档、技术方案、架构规范、演进报告时：先调用 `search_knowledge` 获取背景依据；随后必须调用 `generate_document` 渲染出排版规范的 Word 文档；最后给出章节精简总结。
   - 当用户询问知识库相关问题时：主动调用 `search_knowledge` 检索真实依据，基于切片做严谨引用和推导。
   - 当用户仅进行日常对话或概念探讨时：自然作答，无需滥用工具。

2. 思考与推导风格：
   - 展现深度的逻辑分析与大纲规划，使用中文清晰解释为何要设计这样的章节与页面版式。

3. 最终回复规范：
   - 当生成了 PPT 或文档后，文件下载卡片会由系统自动挂载，你在最终回复正文中只需提供高可读性的 Markdown 概述与要点提炼，不要输出冗长的大段原始文件代码。
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
) -> AsyncGenerator[str, None]:
    """执行一轮 ReAct 多步智能体生产循环，持续产出 SSE 事件流。"""
    start_time = time.time()
    session = await get_session(db, session_id, user_id)
    if not session:
        yield _format_sse("error", {"message": "会话不存在或无权访问"})
        return

    # 确定使用的模型
    model_id = override_model_id or session.model_id or "deepseek-chat"
    api_url, api_key, model_id = await resolve_model_credentials(db, model_id)

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
                                                if not has_detected_tool_calls and (step > 1 or content_stream_active):
                                                    collected_content.append(before)
                                                    yield _format_sse("content", {"delta": before, "step": step})
                                                elif not has_detected_tool_calls:
                                                    pending_content_buffer.append(before)
                                            in_think_tag = True
                                            text_rem = after
                                        else:
                                            current_step_content += text_rem
                                            if not has_detected_tool_calls and (step > 1 or content_stream_active):
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
                                if step > 1 or content_stream_active:
                                    collected_content.append(c_delta)
                                    yield _format_sse("content", {"delta": c_delta, "step": step})
                                else:
                                    # step == 1 且有工具声明时，缓存文本，等待确认是否有 tool_calls 触发
                                    pending_content_buffer.append(c_delta)
                                    if len("".join(pending_content_buffer)) >= 120:
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
                    # 没有发起工具调用，本步为最终回答！若有少量余留 buffer，立即推送
                    if pending_content_buffer:
                        for p_chunk in pending_content_buffer:
                            collected_content.append(p_chunk)
                            yield _format_sse("content", {"delta": p_chunk, "step": step})
                        pending_content_buffer.clear()

                    final_content = current_step_content
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
                for call_id, fn_name, args_raw in tool_executions:
                    # 解析工具参数
                    try:
                        args = json.loads(args_raw) if args_raw else {}
                    except Exception as e:
                        logger.warning("Failed to parse tool arguments: %s", e)
                        args = {}

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
        await db.commit()

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
        await db.commit()
    except Exception as e:
        logger.exception("Harness turn failed: %s", e)
        session.status = "error"
        await db.commit()
        yield _format_sse("error", {"message": f"执行遇到错误: {str(e)}"})
