package com.robin.blogback.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * LangSmith 全链路 Tracing。
 * <p>
 * 通过 LangSmith REST API 记录：
 * - 每次 Agent 调用的输入/输出/Token 消耗
 * - 工具调用的名称、参数、结果、耗时
 * - 整个 Graph 的执行路径
 * <p>
 * 当未配置 LangSmith API Key 时，仅做本地日志记录。
 */
@Component
public class LangSmithTracer {

    private static final Logger log = LoggerFactory.getLogger(LangSmithTracer.class);

    private final String apiKey;
    private final String projectName;
    private final String endpoint;
    private final boolean enabled;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService asyncSender = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "langsmith-trace");
        t.setDaemon(true);
        return t;
    });

    public LangSmithTracer(
            @Value("${langsmith.api-key:}") String apiKey,
            @Value("${langsmith.project:starlore-java}") String projectName,
            @Value("${langsmith.endpoint:https://api.smith.langchain.com}") String endpoint) {
        this.apiKey = apiKey;
        this.projectName = projectName;
        this.endpoint = endpoint;
        this.enabled = apiKey != null && !apiKey.isBlank();

        if (enabled) {
            log.info("[LangSmith] Tracing enabled, project: {}, endpoint: {}", projectName, endpoint);
        } else {
            log.info("[LangSmith] API key not configured, tracing disabled (local logging only)");
        }
    }

    // ========== Agent 调用追踪 ==========

    /**
     * 记录一次 Agent 调用。
     */
    public void traceAgentCall(String agentName, String input, String output,
                                int tokensIn, int tokensOut, long latencyMs) {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        log.info("[Trace] {} | tokens: {}/{} | latency: {}ms | traceId: {}",
                agentName, tokensIn, tokensOut, latencyMs, traceId);

        if (!enabled) return;

        try {
            ObjectNode run = objectMapper.createObjectNode();
            run.put("name", agentName);
            run.put("run_type", "chain");
            run.put("start_time", Instant.now().minusMillis(latencyMs).toString());
            run.put("end_time", Instant.now().toString());

            // inputs
            ObjectNode inputs = objectMapper.createObjectNode();
            inputs.put("input", truncate(input, 2000));
            run.set("inputs", inputs);

            // outputs
            ObjectNode outputs = objectMapper.createObjectNode();
            outputs.put("output", truncate(output, 4000));
            run.set("outputs", outputs);

            // extra metadata
            ObjectNode extra = objectMapper.createObjectNode();
            extra.put("tokens_in", tokensIn);
            extra.put("tokens_out", tokensOut);
            extra.put("latency_ms", latencyMs);
            extra.put("agent_name", agentName);
            run.set("extra", extra);

            sendRunAsync(run, traceId);

        } catch (Exception e) {
            log.warn("[LangSmith] Failed to trace agent call: {}", e.getMessage());
        }
    }

    /**
     * 记录工具调用。
     */
    public void traceToolCall(String toolName, String args, String result,
                               long latencyMs, String parentTraceId) {
        log.info("[Trace] Tool: {} | latency: {}ms | parent: {}", toolName, latencyMs, parentTraceId);

        if (!enabled) return;

        try {
            ObjectNode run = objectMapper.createObjectNode();
            run.put("name", toolName);
            run.put("run_type", "tool");
            run.put("start_time", Instant.now().minusMillis(latencyMs).toString());
            run.put("end_time", Instant.now().toString());

            ObjectNode inputs = objectMapper.createObjectNode();
            inputs.put("input", truncate(args, 2000));
            run.set("inputs", inputs);

            ObjectNode outputs = objectMapper.createObjectNode();
            outputs.put("output", truncate(result, 4000));
            run.set("outputs", outputs);

            ObjectNode extra = objectMapper.createObjectNode();
            extra.put("tool_name", toolName);
            extra.put("latency_ms", latencyMs);
            if (parentTraceId != null) extra.put("parent_trace_id", parentTraceId);
            run.set("extra", extra);

            sendRunAsync(run, parentTraceId != null ? parentTraceId : toolName);

        } catch (Exception e) {
            log.warn("[LangSmith] Failed to trace tool call: {}", e.getMessage());
        }
    }

    /**
     * 记录整个 Graph 执行。
     */
    public void traceGraphExecution(String graphName, String executionPath,
                                     int totalTokens, long totalLatencyMs,
                                     int retryCount, String traceId) {
        log.info("[Trace] Graph: {} | path: {} | tokens: {} | latency: {}ms | retries: {}",
                graphName, executionPath, totalTokens, totalLatencyMs, retryCount);

        if (!enabled) return;

        try {
            ObjectNode run = objectMapper.createObjectNode();
            run.put("name", graphName);
            run.put("run_type", "chain");
            run.put("start_time", Instant.now().minusMillis(totalLatencyMs).toString());
            run.put("end_time", Instant.now().toString());

            ObjectNode inputs = objectMapper.createObjectNode();
            inputs.put("graph", graphName);
            run.set("inputs", inputs);

            ObjectNode outputs = objectMapper.createObjectNode();
            outputs.put("execution_path", executionPath);
            outputs.put("retries", retryCount);
            run.set("outputs", outputs);

            ObjectNode extra = objectMapper.createObjectNode();
            extra.put("total_tokens", totalTokens);
            extra.put("total_latency_ms", totalLatencyMs);
            extra.put("retry_count", retryCount);
            run.set("extra", extra);

            sendRunAsync(run, traceId);

        } catch (Exception e) {
            log.warn("[LangSmith] Failed to trace graph execution: {}", e.getMessage());
        }
    }

    // ========== 内部方法 ==========

    private void sendRunAsync(ObjectNode run, String traceId) {
        asyncSender.submit(() -> {
            try {
                // 确保 project 存在
                ensureProject();
                // 发送 run
                run.put("trace_id", traceId);
                run.put("project_name", projectName);
                postJson(endpoint + "/runs", run.toString());
            } catch (Exception e) {
                log.debug("[LangSmith] Async send failed: {}", e.getMessage());
            }
        });
    }

    private void ensureProject() {
        try {
            ObjectNode project = objectMapper.createObjectNode();
            project.put("name", projectName);
            postJson(endpoint + "/projects", project.toString());
        } catch (Exception ignored) {
            // project 可能已存在
        }
    }

    private String postJson(String urlStr, String json) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-api-key", apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        conn.disconnect();
        return code >= 200 && code < 300 ? "ok" : "error:" + code;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    public boolean isEnabled() { return enabled; }
    public String getProjectName() { return projectName; }
}
