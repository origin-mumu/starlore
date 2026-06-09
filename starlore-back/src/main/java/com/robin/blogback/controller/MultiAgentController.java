package com.robin.blogback.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robin.blogback.config.SseContextHolder;
import com.robin.blogback.config.UserContext;
import com.robin.blogback.entity.AiBadCase;
import com.robin.blogback.graph.AgentGraph;
import com.robin.blogback.graph.AgentState;
import com.robin.blogback.observability.AgentMetrics;
import com.robin.blogback.observability.BadCaseCollector;
import com.robin.blogback.observability.LangSmithTracer;
import com.robin.blogback.service.AiQuotaService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Multi-Agent SSE 端点。
 * <p>
 * 实现 Planner-Executor-Reviewer 多智能体协作编排。
 * 支持子任务自动拆解、并行 Function 调用、结果自我纠错。
 */
@RestController
@RequestMapping("/api/ai")
public class MultiAgentController {

    private static final Logger log = LoggerFactory.getLogger(MultiAgentController.class);

    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是 Starlore 博客助手，也是用户的专属 AI 伙伴。" +
            "优先查询博客数据来回答问题。如果博客中找不到相关内容，" +
            "可以用你自己的知识来回答，不要拒绝用户。" +
            "引用文章时请注明标题。回答简洁友好，可少量用「喵」。" +
            "不要说'出了点小状况'、'接口有问题'之类的话，直接展示结果。";

    @Autowired
    @Qualifier("multiAgentGraph")
    private AgentGraph multiAgentGraph;

    @Autowired
    private AiQuotaService aiQuotaService;

    @Autowired(required = false)
    private LangSmithTracer langSmithTracer;

    @Autowired(required = false)
    private AgentMetrics agentMetrics;

    @Autowired(required = false)
    private BadCaseCollector badCaseCollector;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ExecutorService agentExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "multi-agent-stream");
        t.setDaemon(true);
        return t;
    });

    /**
     * Multi-Agent SSE 流式接口。
     * <p>
     * SSE 事件类型：
     * - plan: Planner 生成的执行计划
     * - subtask_start: 子任务开始执行
     * - subtask_result: 子任务执行结果
     * - review: Reviewer 审查结果
     * - content: 最终答案内容（流式）
     * - error: 错误信息
     * - done: 完成标记
     */
    @PostMapping(value = "/multi-agent-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMultiAgent(HttpServletRequest request,
                                        @RequestParam(required = false) String model,
                                        @RequestBody String messages) {
        Integer userId = (Integer) request.getAttribute("userId");
        log.info("Multi-Agent SSE request - userId: {}", userId);

        // 检查 AI 配额
        int remaining = aiQuotaService.getRemaining(userId);
        if (remaining == 0) {
            SseEmitter blocked = new SseEmitter(0L);
            try {
                blocked.send(Map.of("error", "今日 AI 对话次数已用尽，明天再来吧～"));
            } catch (IOException ignored) {}
            blocked.complete();
            return blocked;
        }

        SseEmitter emitter = new SseEmitter(300_000L);

        agentExecutor.execute(() -> {
            UserContext.setUserId(userId);
            UserContext.setCrossThreadUser("multi-agent-stream", userId);
            SseContextHolder.setEmitter("multi-agent-stream", emitter);
            aiQuotaService.tryConsume(userId);

            try {
                // 解析消息
                List<Map<String, Object>> rawMessages = objectMapper.readValue(
                        messages, new TypeReference<>() {});
                String userQuery = extractLastUserMessage(rawMessages);

                // 发送 plan_start 事件
                sendSseEvent(emitter, "plan_start", Map.of("query", userQuery));

                // 构建初始状态
                AgentState initialState = new AgentState();
                initialState.setUserQuery(userQuery);
                initialState.setUserId(userId);
                initialState.setSystemPromptOverride(DEFAULT_SYSTEM_PROMPT);
                initialState.setTraceId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));

                // 注册 SSE 事件监听器，实时推送 Graph 执行进度
                multiAgentGraph.addEvent((type, state, nodeId) -> {
                    try {
                        switch (type) {
                            case "node_start" -> sendSseEvent(emitter, "subtask_start",
                                    Map.of("node", nodeId != null ? nodeId : "unknown"));
                            case "node_end" -> {
                                if ("planner".equals(nodeId)) {
                                    sendSseEvent(emitter, "plan", Map.of(
                                            "summary", state.getPlanSummary() != null ? state.getPlanSummary() : "",
                                            "subtasks", state.getSubtasks().size()));
                                }
                            }
                            case "graph_end" -> { /* handled after execute */ }
                        }
                    } catch (IOException ignored) {}
                });

                // 执行 Graph
                long startTime = System.currentTimeMillis();
                AgentState finalState;
                try {
                    finalState = multiAgentGraph.execute(initialState);
                } catch (Exception e) {
                    log.error("Multi-Agent execution error: {}", e.getMessage(), e);
                    sendSseEvent(emitter, "error", Map.of("error", "Agent 执行错误: " + e.getMessage()));
                    emitter.complete();
                    return;
                }

                long totalLatency = System.currentTimeMillis() - startTime;

                // 发送 plan 事件
                sendSseEvent(emitter, "plan", Map.of(
                        "summary", finalState.getPlanSummary() != null ? finalState.getPlanSummary() : "",
                        "subtasks", finalState.getSubtasks().size()
                ));

                // 发送每个子任务的结果
                for (var entry : finalState.getExecutionResults().entrySet()) {
                    sendSseEvent(emitter, "subtask_result", Map.of(
                            "subtask_id", entry.getKey(),
                            "result", entry.getValue()
                    ));
                }

                // 发送 review 事件
                if (finalState.getReviewDecision() != null) {
                    sendSseEvent(emitter, "review", Map.of(
                            "decision", finalState.getReviewDecision(),
                            "feedback", finalState.getReviewFeedback() != null ? finalState.getReviewFeedback() : "",
                            "retry_count", finalState.getRetryCount()
                    ));
                }

                // 发送最终答案（分块流式）
                String finalAnswer = finalState.getFinalAnswer();
                if (finalAnswer != null && !finalAnswer.isEmpty()) {
                    // 模拟流式输出：按段落分块发送
                    String[] chunks = finalAnswer.split("(?<=\\n\\n)|(?<=。)|(?<=！)|(?<=？)");
                    for (String chunk : chunks) {
                        if (!chunk.isEmpty()) {
                            sendSseEvent(emitter, "content", Map.of("content", chunk));
                            Thread.sleep(50); // 模拟流式延迟
                        }
                    }
                }

                // 发送指标信息
                Map<String, Object> metrics = new LinkedHashMap<>();
                metrics.put("total_tokens_in", finalState.getTotalTokensIn());
                metrics.put("total_tokens_out", finalState.getTotalTokensOut());
                metrics.put("total_latency_ms", totalLatency);
                metrics.put("retry_count", finalState.getRetryCount());
                metrics.put("node_timings", finalState.getNodeTimings());
                metrics.put("trace_id", finalState.getTraceId());
                if (langSmithTracer != null && langSmithTracer.isEnabled()) {
                    metrics.put("langsmith_project", langSmithTracer.getProjectName());
                }
                sendSseEvent(emitter, "metrics", metrics);

                // LangSmith 完整链路追踪
                if (langSmithTracer != null) {
                    StringBuilder path = new StringBuilder();
                    path.append("Planner->Executor");
                    if (finalState.getRetryCount() > 0) {
                        path.append("(x").append(finalState.getRetryCount() + 1).append(")");
                    }
                    path.append("->Reviewer");
                    if ("PASS".equals(finalState.getReviewDecision())) {
                        path.append("->Synthesizer");
                    }
                    langSmithTracer.traceGraphExecution(
                            "MultiAgent-PlannerExecutorReviewer",
                            path.toString(),
                            finalState.getTotalTokensIn() + finalState.getTotalTokensOut(),
                            totalLatency,
                            finalState.getRetryCount(),
                            finalState.getTraceId()
                    );
                }

                sendSseEvent(emitter, "done", Map.of("done", "true"));
                emitter.complete();

            } catch (Exception e) {
                log.error("Multi-Agent error: {}", e.getMessage(), e);
                try {
                    sendSseEvent(emitter, "error", Map.of("error", "Agent 错误: " + e.getMessage()));
                } catch (IOException ex) { /* ignore */ }
                emitter.completeWithError(e);
            } finally {
                UserContext.clear();
                UserContext.clearCrossThread("multi-agent-stream");
                SseContextHolder.clear("multi-agent-stream");
            }
        });

        emitter.onTimeout(emitter::complete);
        return emitter;
    }

    /**
     * 查询 Agent 执行追踪记录。
     */
    @GetMapping("/agent-traces")
    public Map<String, Object> getAgentTraces(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (agentMetrics == null) {
            return Map.of("success", false, "message", "Metrics service not available");
        }
        return Map.of("success", true, "metrics", agentMetrics.getSnapshot());
    }

    /**
     * 查询 Bad Case 列表。
     */
    @GetMapping("/agent-bad-cases")
    public Map<String, Object> getBadCases(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (badCaseCollector == null) {
            return Map.of("success", false, "message", "BadCase collector not available");
        }
        List<AiBadCase> cases = badCaseCollector.getBadCases(page, size);
        long total = badCaseCollector.getBadCaseCount();
        return Map.of("success", true, "data", cases, "total", total, "page", page, "size", size);
    }

    /**
     * 获取 Agent 指标统计。
     */
    @GetMapping("/agent-metrics")
    public Map<String, Object> getAgentMetrics() {
        if (agentMetrics == null) {
            return Map.of("success", false, "message", "Metrics service not available");
        }
        return Map.of("success", true, "metrics", agentMetrics.getSnapshot());
    }

    // ========== 内部方法 ==========

    private String extractLastUserMessage(List<Map<String, Object>> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, Object> msg = messages.get(i);
            if ("user".equals(msg.get("role"))) {
                Object content = msg.get("content");
                if (content instanceof String) return (String) content;
                if (content instanceof List<?> parts) {
                    // 多模态消息：提取文本部分
                    for (Object part : parts) {
                        if (part instanceof Map<?, ?> p && "text".equals(p.get("type"))) {
                            return (String) p.get("text");
                        }
                    }
                }
            }
        }
        return "";
    }

    private void sendSseEvent(SseEmitter emitter, String type, Map<String, Object> data) throws IOException {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.putAll(data);
        emitter.send(event);
    }
}
