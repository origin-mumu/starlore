package com.robin.blogback.agent;

import com.robin.blogback.graph.AgentState;
import com.robin.blogback.graph.AgentNode;
import com.robin.blogback.observability.BadCaseCollector;
import com.robin.blogback.observability.LangSmithTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Executor（执行者）Agent。
 * <p>
 * 职责：根据 Planner 生成的 Plan，逐个执行子任务。
 * 支持并行执行无依赖关系的子任务，支持 Few-Shot 动态反馈。
 */
public class ExecutorAgent implements AgentNode {

    private static final Logger log = LoggerFactory.getLogger(ExecutorAgent.class);

    private static final String EXECUTOR_SYSTEM_PROMPT =
            "你是 Starlore 知识库系统的执行专家（Executor）。\n\n"
            + "## 职责\n"
            + "根据分配的子任务，使用可用工具完成执行，并返回结构化的执行结果。\n\n"
            + "## 可用工具\n"
            + "- searchArticles(keyword, category?, tag?): 搜索知识库文章\n"
            + "- getArticleDetail(articleId): 获取文章详情\n"
            + "- getCategories(): 获取所有分类\n"
            + "- getBlogStats(): 获取知识库统计数据\n"
            + "- getRecentArticles(limit?): 获取最新文章\n"
            + "- writeArticle(title, content, category, tags?, description?, status?): 创建文章\n"
            + "- updateArticle(articleId, title?, content?, category?, tags?, description?, status?): 更新文章\n"
            + "- deleteArticle(articleId): 删除文章\n"
            + "- getAllTags(): 获取所有标签\n"
            + "- getArticlesByCategory(category): 按分类获取文章\n"
            + "- createCategory(name, description?, color?): 创建分类\n\n"
            + "## 输出格式\n"
            + "对于每个子任务，输出结构化的结果：\n"
            + "- 如果成功：直接输出结果内容\n"
            + "- 如果失败：输出 \"ERROR: [错误原因]\"\n\n"
            + "## 规则\n"
            + "1. 严格按照子任务描述执行，不要做多余操作\n"
            + "2. 如果子任务需要工具调用，优先使用 toolHint 建议的工具\n"
            + "3. 如果工具调用失败，尝试替代方案\n"
            + "4. 对于\"统计\"类任务，使用 getBlogStats 工具\n"
            + "5. 对于\"写文章\"类任务，使用 writeArticle 工具\n"
            + "6. 保持输出简洁，不要重复子任务描述";

    private final ChatClient chatClient;
    private final Object blogTools;  // BlogTools 实例，用于 Spring AI tool calling
    private final LangSmithTracer tracer;
    private final BadCaseCollector badCaseCollector;
    private final ExecutorService parallelExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "executor-parallel");
        t.setDaemon(true);
        return t;
    });

    public ExecutorAgent(ChatClient chatClient, Object blogTools,
                         LangSmithTracer tracer, BadCaseCollector badCaseCollector) {
        this.chatClient = chatClient;
        this.blogTools = blogTools;
        this.tracer = tracer;
        this.badCaseCollector = badCaseCollector;
    }

    @Override
    public String name() { return "Executor"; }

    @Override
    public AgentState execute(AgentState state) throws Exception {
        List<AgentState.Subtask> subtasks = state.getSubtasks();
        if (subtasks.isEmpty()) {
            // 没有子任务，直接用 LLM 回答
            executeDirectAnswer(state);
            return state;
        }

        log.info("[Executor] Executing {} subtasks", subtasks.size());

        // 分析依赖关系，将子任务分批执行
        List<List<AgentState.Subtask>> batches = buildExecutionBatches(subtasks);

        for (List<AgentState.Subtask> batch : batches) {
            if (batch.size() == 1) {
                // 单任务，串行执行
                executeSubtask(batch.get(0), state);
            } else {
                // 多任务，并行执行
                executeSubtasksParallel(batch, state);
            }
        }

        // 合并所有执行结果为最终答案
        mergeResults(state);

        return state;
    }

    /**
     * 无子任务时的直接回答模式。
     */
    private void executeDirectAnswer(AgentState state) throws Exception {
        log.info("[Executor] No subtasks, direct answer mode");

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(EXECUTOR_SYSTEM_PROMPT));
        messages.add(new UserMessage(state.getUserQuery()));

        long startTime = System.currentTimeMillis();
        ChatResponse response;
        if (blogTools != null) {
            response = chatClient.prompt()
                    .messages(messages)
                    .tools(blogTools)
                    .call()
                    .chatResponse();
        } else {
            response = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .chatResponse();
        }
        long latencyMs = System.currentTimeMillis() - startTime;

        String output = response.getResult().getOutput().getText();
        int tokensIn = response.getMetadata() != null && response.getMetadata().getUsage() != null
                ? response.getMetadata().getUsage().getPromptTokens() : 0;
        int tokensOut = response.getMetadata() != null && response.getMetadata().getUsage() != null
                ? response.getMetadata().getUsage().getCompletionTokens() : 0;
        state.addTokens(tokensIn, tokensOut);

        if (tracer != null) {
            tracer.traceAgentCall("Executor-Direct", state.getUserQuery(), output, tokensIn, tokensOut, latencyMs);
        }

        state.setFinalAnswer(output);
    }

    /**
     * 执行单个子任务。
     */
    private void executeSubtask(AgentState.Subtask subtask, AgentState state) throws Exception {
        log.info("[Executor] Executing subtask {}: {}", subtask.getId(), subtask.getDescription());

        // 发送实时子任务执行中状态
        com.robin.blogback.config.SseContextHolder.sendEvent("subtask_running", Map.of(
                "subtask_id", subtask.getId()
        ));

        // 构建带上下文的 prompt
        String context = buildSubtaskContext(subtask, state);
        String fewShotPrompt = buildFewShotPrompt(subtask);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(EXECUTOR_SYSTEM_PROMPT + fewShotPrompt));
        messages.add(new UserMessage(context));

        long startTime = System.currentTimeMillis();
        ChatResponse response;
        if (blogTools != null && subtask.getToolHint() != null) {
            // 有工具提示时使用 tool calling
            response = chatClient.prompt()
                    .messages(messages)
                    .tools(blogTools)
                    .call()
                    .chatResponse();
        } else {
            response = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .chatResponse();
        }
        long latencyMs = System.currentTimeMillis() - startTime;

        String output = response.getResult().getOutput().getText();
        int tokensIn = response.getMetadata() != null && response.getMetadata().getUsage() != null
                ? response.getMetadata().getUsage().getPromptTokens() : 0;
        int tokensOut = response.getMetadata() != null && response.getMetadata().getUsage() != null
                ? response.getMetadata().getUsage().getCompletionTokens() : 0;
        state.addTokens(tokensIn, tokensOut);

        state.addExecutionResult(subtask.getId(), output);
        state.addToolCallLog("subtask-" + subtask.getId() + ": " + subtask.getDescription());

        // 发送实时子任务执行完成状态
        com.robin.blogback.config.SseContextHolder.sendEvent("subtask_result", Map.of(
                "subtask_id", subtask.getId(),
                "result", output
        ));

        if (tracer != null) {
            tracer.traceAgentCall("Executor-Subtask-" + subtask.getId(),
                    subtask.getDescription(), output, tokensIn, tokensOut, latencyMs);
        }

        log.info("[Executor] Subtask {} completed in {}ms", subtask.getId(), latencyMs);
    }

    /**
     * 并行执行多个无依赖关系的子任务。
     */
    private void executeSubtasksParallel(List<AgentState.Subtask> batch, AgentState state) {
        log.info("[Executor] Parallel execution of {} subtasks", batch.size());

        List<Future<?>> futures = new ArrayList<>();
        for (AgentState.Subtask subtask : batch) {
            futures.add(parallelExecutor.submit(() -> {
                try {
                    executeSubtask(subtask, state);
                } catch (Exception e) {
                    log.error("[Executor] Parallel subtask {} failed: {}", subtask.getId(), e.getMessage());
                    state.addExecutionResult(subtask.getId(), "ERROR: " + e.getMessage());
                }
            }));
        }

        // 等待所有并行任务完成
        for (Future<?> future : futures) {
            try {
                future.get(60, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("[Executor] Parallel task error: {}", e.getMessage());
            }
        }
    }

    /**
     * 构建执行批次：按依赖关系分层，同层可并行。
     */
    private List<List<AgentState.Subtask>> buildExecutionBatches(List<AgentState.Subtask> subtasks) {
        List<List<AgentState.Subtask>> batches = new ArrayList<>();
        Set<Integer> completed = new HashSet<>();
        Set<Integer> remaining = subtasks.stream()
                .map(AgentState.Subtask::getId)
                .collect(Collectors.toSet());

        while (!remaining.isEmpty()) {
            List<AgentState.Subtask> batch = new ArrayList<>();
            for (AgentState.Subtask st : subtasks) {
                if (completed.contains(st.getId())) continue;
                if (completed.containsAll(st.getDependencies())) {
                    batch.add(st);
                }
            }
            if (batch.isEmpty()) {
                // 防止死循环：强制将剩余任务放入一批
                for (AgentState.Subtask st : subtasks) {
                    if (!completed.contains(st.getId())) {
                        batch.add(st);
                    }
                }
                batches.add(batch);
                break;
            }
            for (AgentState.Subtask st : batch) {
                completed.add(st.getId());
                remaining.remove(st.getId());
            }
            batches.add(batch);
        }
        return batches;
    }

    /**
     * 构建子任务上下文：包含之前的执行结果。
     */
    private String buildSubtaskContext(AgentState.Subtask subtask, AgentState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 当前子任务\n");
        sb.append("ID: ").append(subtask.getId()).append("\n");
        sb.append("描述: ").append(subtask.getDescription()).append("\n");
        if (subtask.getToolHint() != null) {
            sb.append("建议工具: ").append(subtask.getToolHint()).append("\n");
        }

        // 附带依赖任务的结果
        if (!subtask.getDependencies().isEmpty()) {
            sb.append("\n## 前置任务结果\n");
            for (int depId : subtask.getDependencies()) {
                String depResult = state.getExecutionResults().get(depId);
                if (depResult != null) {
                    sb.append("任务 ").append(depId).append(": ").append(depResult).append("\n");
                }
            }
        }

        sb.append("\n## 用户原始请求\n");
        sb.append(state.getUserQuery());

        return sb.toString();
    }

    /**
     * 从 Bad Case 库中检索相似案例，构建 Few-Shot 提示。
     */
    private String buildFewShotPrompt(AgentState.Subtask subtask) {
        if (badCaseCollector == null) return "";

        try {
            List<String> similarCases = badCaseCollector.getSimilarBadCases(subtask.getDescription(), 3);
            if (similarCases.isEmpty()) return "";

            StringBuilder sb = new StringBuilder("\n\n## 历史失败案例（请避免类似错误）\n");
            for (int i = 0; i < similarCases.size(); i++) {
                sb.append(i + 1).append(". ").append(similarCases.get(i)).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 合并所有子任务结果为最终答案。
     */
    private void mergeResults(AgentState state) {
        Map<Integer, String> results = state.getExecutionResults();
        if (results.isEmpty()) {
            state.setFinalAnswer("执行完成，但没有产生结果。");
            return;
        }

        StringBuilder merged = new StringBuilder();
        boolean hasSuccess = false;

        for (Map.Entry<Integer, String> entry : results.entrySet()) {
            String value = entry.getValue();
            // 跳过失败的子任务
            if (value != null && value.startsWith("ERROR:")) {
                continue;
            }
            if (!hasSuccess) {
                merged.append("## 执行结果\n\n");
                hasSuccess = true;
            }
            merged.append("### 子任务 ").append(entry.getKey()).append("\n");
            merged.append(value).append("\n\n");
        }

        // 如果全部失败，保留所有结果让 Reviewer 判断
        if (!hasSuccess) {
            merged.append("## 执行结果\n\n");
            for (Map.Entry<Integer, String> entry : results.entrySet()) {
                merged.append("### 子任务 ").append(entry.getKey()).append("\n");
                merged.append(entry.getValue()).append("\n\n");
            }
        }

        state.setFinalAnswer(merged.toString());
    }
}
