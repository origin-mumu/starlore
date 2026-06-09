package com.robin.blogback.observability;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Agent 执行指标统计。
 * <p>
 * 追踪：Token 消耗、节点耗时、工具调用频率、重试次数等。
 * 数据保留在内存中，应用重启后重置。
 */
@Component
public class AgentMetrics {

    // ── 全局计数器 ──
    private final AtomicInteger totalExecutions = new AtomicInteger(0);
    private final AtomicInteger totalRetries = new AtomicInteger(0);
    private final AtomicInteger totalErrors = new AtomicInteger(0);
    private final AtomicLong totalTokensIn = new AtomicLong(0);
    private final AtomicLong totalTokensOut = new AtomicLong(0);
    private final AtomicLong totalLatencyMs = new AtomicLong(0);

    // ── 节点耗时 ──
    private final ConcurrentHashMap<String, List<Long>> nodeLatencies = new ConcurrentHashMap<>();

    // ── 工具调用频率 ──
    private final ConcurrentHashMap<String, AtomicInteger> toolCallCounts = new ConcurrentHashMap<>();

    // ── Reviewer 决策分布 ──
    private final ConcurrentHashMap<String, AtomicInteger> reviewDecisions = new ConcurrentHashMap<>();

    // ── 审查通过率（滑动窗口） ──
    private final List<Boolean> recentPasses = Collections.synchronizedList(new ArrayList<>());
    private static final int WINDOW_SIZE = 100;

    /**
     * 记录一次完整的 Graph 执行。
     */
    public void recordExecution(int tokensIn, int tokensOut, long latencyMs,
                                 Map<String, Long> nodeTimings, int retryCount, String reviewDecision) {
        totalExecutions.incrementAndGet();
        totalTokensIn.addAndGet(tokensIn);
        totalTokensOut.addAndGet(tokensOut);
        totalLatencyMs.addAndGet(latencyMs);
        totalRetries.addAndGet(retryCount);

        // 节点耗时
        if (nodeTimings != null) {
            for (Map.Entry<String, Long> entry : nodeTimings.entrySet()) {
                nodeLatencies.computeIfAbsent(entry.getKey(), k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(entry.getValue());
            }
        }

        // Reviewer 决策
        if (reviewDecision != null) {
            reviewDecisions.computeIfAbsent(reviewDecision, k -> new AtomicInteger(0)).incrementAndGet();
            recentPasses.add("PASS".equals(reviewDecision));
            // 维护滑动窗口
            while (recentPasses.size() > WINDOW_SIZE) {
                recentPasses.remove(0);
            }
        }
    }

    /**
     * 记录一次工具调用。
     */
    public void recordToolCall(String toolName) {
        toolCallCounts.computeIfAbsent(toolName, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * 记录一次错误。
     */
    public void recordError() {
        totalErrors.incrementAndGet();
    }

    /**
     * 获取指标快照。
     */
    public Map<String, Object> getSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("totalExecutions", totalExecutions.get());
        snapshot.put("totalErrors", totalErrors.get());
        snapshot.put("totalRetries", totalRetries.get());
        snapshot.put("totalTokensIn", totalTokensIn.get());
        snapshot.put("totalTokensOut", totalTokensOut.get());
        snapshot.put("avgLatencyMs", totalExecutions.get() > 0
                ? totalLatencyMs.get() / totalExecutions.get() : 0);

        // 节点平均耗时
        Map<String, Long> avgNodeLatency = new LinkedHashMap<>();
        for (Map.Entry<String, List<Long>> entry : nodeLatencies.entrySet()) {
            List<Long> latencies = entry.getValue();
            avgNodeLatency.put(entry.getKey(),
                    latencies.stream().mapToLong(Long::longValue).sum() / latencies.size());
        }
        snapshot.put("avgNodeLatencyMs", avgNodeLatency);

        // 工具调用统计
        Map<String, Integer> toolStats = new LinkedHashMap<>();
        toolCallCounts.forEach((k, v) -> toolStats.put(k, v.get()));
        snapshot.put("toolCallCounts", toolStats);

        // Reviewer 决策分布
        Map<String, Integer> reviewStats = new LinkedHashMap<>();
        reviewDecisions.forEach((k, v) -> reviewStats.put(k, v.get()));
        snapshot.put("reviewDecisions", reviewStats);

        // 审查通过率
        if (!recentPasses.isEmpty()) {
            long passCount = recentPasses.stream().filter(Boolean::booleanValue).count();
            snapshot.put("passRate", Math.round(passCount * 100.0 / recentPasses.size()) + "%");
        } else {
            snapshot.put("passRate", "N/A");
        }

        return snapshot;
    }

    /**
     * 重置所有指标。
     */
    public void reset() {
        totalExecutions.set(0);
        totalRetries.set(0);
        totalErrors.set(0);
        totalTokensIn.set(0);
        totalTokensOut.set(0);
        totalLatencyMs.set(0);
        nodeLatencies.clear();
        toolCallCounts.clear();
        reviewDecisions.clear();
        recentPasses.clear();
    }
}
