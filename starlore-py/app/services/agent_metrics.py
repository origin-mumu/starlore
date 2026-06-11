"""Agent 内存级指标统计。

跟踪：
- 总执行次数、总 Token 消耗
- 各节点耗时
- 工具调用次数
- Reviewer 决策分布（PASS/REVISE/FAIL）
- 100 元素滑动窗口通过率
"""

import logging
import threading
import time
from collections import deque

logger = logging.getLogger(__name__)

_lock = threading.Lock()

# 计数器
_total_executions: int = 0
_total_tokens_in: int = 0
_total_tokens_out: int = 0

# 各节点耗时 {node_name: total_ms}
_node_latencies: dict[str, float] = {}

# 工具调用次数 {tool_name: count}
_tool_call_counts: dict[str, int] = {}

# Reviewer 决策分布 {decision: count}
_reviewer_decisions: dict[str, int] = {"PASS": 0, "REVISE": 0, "FAIL": 0}

# 100 元素滑动窗口：True=PASS, False=FAIL/REVISE
_pass_window: deque[bool] = deque(maxlen=100)


def record_execution(
    tokens_in: int = 0,
    tokens_out: int = 0,
    node_latencies: dict[str, float] | None = None,
    tool_calls: dict[str, int] | None = None,
    review_decision: str | None = None,
) -> None:
    """记录一次 Agent 执行的指标。"""
    global _total_executions, _total_tokens_in, _total_tokens_out

    with _lock:
        _total_executions += 1
        _total_tokens_in += tokens_in
        _total_tokens_out += tokens_out

        if node_latencies:
            for node, latency in node_latencies.items():
                _node_latencies[node] = _node_latencies.get(node, 0) + latency

        if tool_calls:
            for tool_name, count in tool_calls.items():
                _tool_call_counts[tool_name] = _tool_call_counts.get(tool_name, 0) + count

        if review_decision:
            decision = review_decision.upper()
            if decision in _reviewer_decisions:
                _reviewer_decisions[decision] += 1
            _pass_window.append(decision == "PASS")


def get_metrics() -> dict:
    """获取当前指标快照。"""
    with _lock:
        pass_rate = 0.0
        if _pass_window:
            pass_rate = sum(_pass_window) / len(_pass_window)

        return {
            "total_executions": _total_executions,
            "total_tokens_in": _total_tokens_in,
            "total_tokens_out": _total_tokens_out,
            "node_latencies": dict(_node_latencies),
            "tool_call_counts": dict(_tool_call_counts),
            "reviewer_decisions": dict(_reviewer_decisions),
            "pass_rate": round(pass_rate, 4),
            "pass_window_size": len(_pass_window),
            "timestamp": time.time(),
        }
