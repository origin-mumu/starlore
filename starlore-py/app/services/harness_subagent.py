"""Harness 子代理委派与质量自查。

单 agent ReAct 循环仍是唯一脊柱；委派子代理（delegate_task）与
任务清单（todo_write）是主 agent 可选的工具，循环本身不依赖它们。
子代理深度固定为 1：子代理的工具集中不含 delegate_task / todo_write。
"""

import json
import logging
from typing import Any

import httpx
from sqlalchemy.ext.asyncio import AsyncSession

from app.services.harness_content_tools import HARNESS_TOOLS, HARNESS_TOOL_MAP

logger = logging.getLogger(__name__)

SUB_AGENT_MAX_ROUNDS = 6
REVIEW_MAX_TOKENS = 800

DELEGATE_TOOL_SCHEMA = {
    "type": "function",
    "function": {
        "name": "delegate_task",
        "description": (
            "把一个明确、独立、可自行完成的子任务委派给一个专项执行子代理。"
            "适用于：多路独立检索、批量数据治理、独立生成 PPT/Word 等可并行的工作。"
            "同一轮可多次调用以并行推进多项互不依赖的工作；有依赖关系的工作请勿同时委派。"
        ),
        "parameters": {
            "type": "object",
            "properties": {
                "description": {
                    "type": "string",
                    "description": "子任务的完整描述，需包含必要的背景上下文与目标，让子代理无需追问即可独立完成",
                },
                "expected_output": {
                    "type": "string",
                    "description": "期望得到的产出形式，如'要点列表'、'对比结论'、'PPT 文件'等",
                },
            },
            "required": ["description"],
        },
    },
}

TODO_WRITE_SCHEMA = {
    "type": "function",
    "function": {
        "name": "todo_write",
        "description": (
            "写入或更新当前任务的清单（整表快照，每次调用覆盖上一版）。"
            "复杂/多阶段任务开始前先列出全部事项，随执行进度实时把事项标记为 in_progress/completed；"
            "简单对话不要使用。"
        ),
        "parameters": {
            "type": "object",
            "properties": {
                "todos": {
                    "type": "array",
                    "description": "完整清单快照",
                    "items": {
                        "type": "object",
                        "properties": {
                            "content": {"type": "string", "description": "一条简短的行动描述"},
                            "status": {
                                "type": "string",
                                "enum": ["pending", "in_progress", "completed"],
                                "description": "待处理 / 进行中 / 已完成",
                            },
                        },
                        "required": ["content", "status"],
                    },
                }
            },
            "required": ["todos"],
        },
    },
}

TODO_MAX_ITEMS = 20
TODO_STATUSES = {"pending", "in_progress", "completed"}


def validate_todos(raw: Any) -> list[dict]:
    """清洗模型提交的清单：仅保留合法三态条目，超过上限截断。"""
    if not isinstance(raw, list):
        return []
    todos: list[dict] = []
    for item in raw[:TODO_MAX_ITEMS]:
        if not isinstance(item, dict):
            continue
        content = str(item.get("content") or "").strip()
        status = str(item.get("status") or "").strip()
        if not content or status not in TODO_STATUSES:
            continue
        todos.append({"content": content[:200], "status": status})
    return todos


SUB_AGENT_SYSTEM_PROMPT = """你是 Starlore 智能体的专项任务执行者，只会被委派单一、明确的子任务。
专注完成委派给你的任务：按需调用可用工具获取真实数据或生成交付物，然后给出结论性回答。

【语言与排版规范】
1. 全程使用规范流畅的中文，严禁任何原生表情符号（Emoji）。
2. 保持简洁严谨的技术工作台风格，依靠 Markdown 语法组织信息层级。

【执行要求】
1. 必须通过工具获取真实数据，不得伪造结果。
2. 只做委派给你的任务，不要扩展范围。
3. 最终回答直接给出任务结果与关键细节，不要重复任务描述。"""


REVIEWER_SYSTEM_PROMPT = """你是 Starlore 智能体的质量审查专家。
审查给定的最终回答草稿是否完整、准确、诚实地完成了用户请求。

## 审查标准
1. 完整性：是否覆盖了用户请求的所有部分与任务清单中的事项？
2. 准确性：结论是否与工具调用和委派任务的真实结果一致？是否夸大或虚构？
3. 诚实性：声称"已完成/已更新"的操作是否有对应的成功记录？

## 输出格式
严格按以下 JSON 输出，不要输出其他内容：
{"decision": "PASS 或 FAIL", "feedback": "若 FAIL，给出必须修正的具体问题；PASS 时为空字符串"}"""


def _tool_content_str(data: Any) -> str:
    if isinstance(data, str):
        return data
    try:
        return json.dumps(data, ensure_ascii=False)
    except (TypeError, ValueError):
        return str(data)


async def run_sub_agent_owned_session(
    db: AsyncSession,
    user_id: int,
    api_url: str,
    api_key: str,
    model_id: str,
    description: str,
    expected_output: str | None = None,
    context_digest: str = "",
) -> dict:
    """使用独立 DB 会话运行子代理：并行委派间互不共享会话，结束后自行提交。

    主请求的 AsyncSession 不允许被多个协程并发使用，因此每个子代理
    持有自己的会话（对齐原 multi-agent 路由的 workflow-owned session 模式）。
    """
    from app.database import async_session_factory

    async with async_session_factory() as sub_db:
        try:
            result = await run_sub_agent(
                db=sub_db,
                user_id=user_id,
                api_url=api_url,
                api_key=api_key,
                model_id=model_id,
                description=description,
                expected_output=expected_output,
                context_digest=context_digest,
            )
            await sub_db.commit()
            return result
        except Exception:
            await sub_db.rollback()
            raise


async def run_sub_agent(
    db: AsyncSession,
    user_id: int,
    api_url: str,
    api_key: str,
    model_id: str,
    description: str,
    expected_output: str | None = None,
    context_digest: str = "",
) -> dict:
    """运行一个子代理 ReAct 小循环，执行单个委派任务。

    返回 {"ok": bool, "content": str, "artifacts": list, "error": str|None,
          "tokens_prompt": int, "tokens_completion": int}。
    """
    task_prompt = f"## 委派任务\n{description}"
    if expected_output:
        task_prompt += f"\n\n## 期望产出\n{expected_output}"
    if context_digest:
        task_prompt += f"\n\n## 主任务上下文（仅供理解背景，不要超出范围处理）\n{context_digest}"

    messages: list[dict[str, Any]] = [
        {"role": "system", "content": SUB_AGENT_SYSTEM_PROMPT},
        {"role": "user", "content": task_prompt},
    ]
    tools_declaration = [tool.to_openai_tool() for tool in HARNESS_TOOLS]
    tokens_prompt = 0
    tokens_completion = 0
    artifacts: list[dict] = []

    async with httpx.AsyncClient(timeout=300.0) as client:
        endpoint = f"{api_url.rstrip('/')}/chat/completions"
        headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}

        for _ in range(SUB_AGENT_MAX_ROUNDS):
            payload = {
                "model": model_id,
                "messages": messages,
                "tools": tools_declaration,
                "tool_choice": "auto",
                "stream": False,
            }
            try:
                resp = await client.post(endpoint, headers=headers, json=payload)
            except httpx.HTTPError as e:
                return _sub_result(False, f"子代理网络异常: {e}", artifacts, tokens_prompt, tokens_completion)
            if resp.status_code != 200:
                return _sub_result(
                    False,
                    f"模型服务响应异常 ({resp.status_code})",
                    artifacts,
                    tokens_prompt,
                    tokens_completion,
                )

            data = resp.json()
            usage = data.get("usage") or {}
            tokens_prompt += usage.get("prompt_tokens", 0)
            tokens_completion += usage.get("completion_tokens", 0)

            choices = data.get("choices") or []
            if not choices:
                return _sub_result(False, "模型响应缺少 choices", artifacts, tokens_prompt, tokens_completion)
            message = choices[0].get("message") or {}
            tool_calls = message.get("tool_calls") or []

            if not tool_calls:
                return _sub_result(
                    True,
                    message.get("content") or "",
                    artifacts,
                    tokens_prompt,
                    tokens_completion,
                )

            messages.append({
                "role": "assistant",
                "content": message.get("content"),
                "tool_calls": tool_calls,
            })
            for tc in tool_calls:
                fn_name = ((tc.get("function") or {}).get("name") or "").strip()
                try:
                    args = json.loads((tc.get("function") or {}).get("arguments") or "{}")
                except (TypeError, ValueError):
                    args = {}
                tool = HARNESS_TOOL_MAP.get(fn_name)
                if tool is None:
                    content_str = json.dumps({"error": f"未知工具: {fn_name}"}, ensure_ascii=False)
                else:
                    try:
                        result = await tool.execute(db=db, user_id=user_id, **args)
                        if result.artifact:
                            artifacts.append(result.artifact)
                        content_str = (
                            result.data if isinstance(result.data, str) else _tool_content_str(result.data)
                        )
                        if not result.success:
                            content_str = f"工具执行提示: {result.summary or '执行未成功'}。{content_str or ''}"
                    except Exception as e:
                        logger.exception("Sub-agent tool %s failed: %s", fn_name, e)
                        content_str = f"工具执行异常: {e}"
                messages.append({"role": "tool", "tool_call_id": tc.get("id") or "", "content": content_str})

        # 轮次耗尽：去掉工具声明，强制给出结论
        payload = {"model": model_id, "messages": messages, "stream": False}
        try:
            resp = await client.post(endpoint, headers=headers, json=payload)
            if resp.status_code == 200:
                data = resp.json()
                usage = data.get("usage") or {}
                tokens_prompt += usage.get("prompt_tokens", 0)
                tokens_completion += usage.get("completion_tokens", 0)
                choices = data.get("choices") or []
                content = (choices[0].get("message") or {}).get("content") if choices else ""
                return _sub_result(True, content or "", artifacts, tokens_prompt, tokens_completion)
        except httpx.HTTPError:
            pass
        return _sub_result(False, "子代理工具轮次超过上限", artifacts, tokens_prompt, tokens_completion)


def _sub_result(
    ok: bool, content: str, artifacts: list[dict], tokens_prompt: int, tokens_completion: int,
    error: str | None = None,
) -> dict:
    return {
        "ok": ok,
        "content": content or "",
        "artifacts": artifacts,
        "error": error or (None if ok else (content or "子代理执行失败")),
        "tokens_prompt": tokens_prompt,
        "tokens_completion": tokens_completion,
    }


async def run_final_review(
    db: AsyncSession,
    user_id: int,
    api_url: str,
    api_key: str,
    model_id: str,
    user_request: str,
    work_digest: str,
    draft: str,
) -> dict:
    """交卷前质量自查。返回 {"decision": "PASS"|"FAIL", "feedback": str}。"""
    review_input = f"## 用户原始请求\n{user_request}"

    few_shot = ""
    try:
        from app.services.bad_case_collector import build_few_shot_prompt
        few_shot = await build_few_shot_prompt(db, user_request)
    except Exception:
        pass

    if work_digest:
        review_input += f"\n\n## 工具调用与委派任务的真实结果摘要\n{work_digest}"
    review_input += f"\n\n## 待审查的最终回答草稿\n{draft}"
    if few_shot:
        review_input += f"\n\n## 历史易错案例（供参考规避）\n{few_shot}"

    payload = {
        "model": model_id,
        "messages": [
            {"role": "system", "content": REVIEWER_SYSTEM_PROMPT},
            {"role": "user", "content": review_input},
        ],
        "stream": False,
        "max_tokens": REVIEW_MAX_TOKENS,
    }
    try:
        async with httpx.AsyncClient(timeout=120.0) as client:
            resp = await client.post(
                f"{api_url.rstrip('/')}/chat/completions",
                headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
                json=payload,
            )
        if resp.status_code != 200:
            return {"decision": "PASS", "feedback": ""}
        data = resp.json()
        output = ((data.get("choices") or [{}])[0].get("message") or {}).get("content") or ""
    except Exception as e:
        logger.warning("Final review failed, defaulting to PASS: %s", e)
        return {"decision": "PASS", "feedback": ""}

    try:
        json_str = output.strip()
        if json_str.startswith("```"):
            json_str = json_str.split("\n", 1)[-1].rsplit("```", 1)[0].strip()
        review = json.loads(json_str)
        decision = str(review.get("decision", "PASS")).upper()
        feedback = str(review.get("feedback") or "").strip()
        if decision not in ("PASS", "FAIL"):
            decision = "PASS"
        return {"decision": decision, "feedback": feedback}
    except (TypeError, ValueError):
        return {"decision": "PASS", "feedback": ""}
