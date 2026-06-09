package com.robin.blogback.graph;

import java.util.*;

/**
 * Multi-Agent 共享状态，在 Graph 各节点间传递。
 */
public class AgentState {

    // ── 消息流 ──
    private final List<Map<String, Object>> messages = new ArrayList<>();
    private String userQuery;

    // ── Planner 输出 ──
    private String planSummary;
    private final List<Subtask> subtasks = new ArrayList<>();

    // ── Executor 状态 ──
    private int currentSubtaskIndex = 0;
    private final Map<Integer, String> executionResults = new LinkedHashMap<>();
    private final List<String> toolCallsLog = new ArrayList<>();

    // ── Reviewer 状态 ──
    private String reviewDecision;   // PASS / REVISE / FAIL
    private String reviewFeedback;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 2;

    // ── 最终输出 ──
    private String finalAnswer;

    // ── 可观测性 ──
    private String traceId;
    private final Map<String, Long> nodeTimings = new LinkedHashMap<>();
    private int totalTokensIn = 0;
    private int totalTokensOut = 0;

    // ── 上下文 ──
    private Integer userId;
    private String systemPromptOverride;

    // ========== 消息操作 ==========

    public List<Map<String, Object>> getMessages() { return messages; }

    public void addMessage(String role, String content) {
        messages.add(new LinkedHashMap<>() {{ put("role", role); put("content", content); }});
    }

    public String getLastAssistantMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("assistant".equals(messages.get(i).get("role"))) {
                return (String) messages.get(i).get("content");
            }
        }
        return null;
    }

    // ========== 用户查询 ==========

    public String getUserQuery() { return userQuery; }
    public void setUserQuery(String userQuery) { this.userQuery = userQuery; }

    // ========== Plan ==========

    public String getPlanSummary() { return planSummary; }
    public void setPlanSummary(String planSummary) { this.planSummary = planSummary; }

    public List<Subtask> getSubtasks() { return subtasks; }
    public void setSubtasks(List<Subtask> subtasks) {
        this.subtasks.clear();
        this.subtasks.addAll(subtasks);
    }

    // ========== Executor ==========

    public int getCurrentSubtaskIndex() { return currentSubtaskIndex; }
    public void setCurrentSubtaskIndex(int idx) { this.currentSubtaskIndex = idx; }

    public Subtask getCurrentSubtask() {
        if (currentSubtaskIndex >= 0 && currentSubtaskIndex < subtasks.size()) {
            return subtasks.get(currentSubtaskIndex);
        }
        return null;
    }

    public boolean hasMoreSubtasks() { return currentSubtaskIndex < subtasks.size(); }

    public void advanceSubtask() { currentSubtaskIndex++; }

    public Map<Integer, String> getExecutionResults() { return executionResults; }

    public void addExecutionResult(int subtaskIndex, String result) {
        executionResults.put(subtaskIndex, result);
    }

    public List<String> getToolCallsLog() { return toolCallsLog; }
    public void addToolCallLog(String log) { toolCallsLog.add(log); }

    // ========== Reviewer ==========

    public String getReviewDecision() { return reviewDecision; }
    public void setReviewDecision(String decision) { this.reviewDecision = decision; }

    public String getReviewFeedback() { return reviewFeedback; }
    public void setReviewFeedback(String feedback) { this.reviewFeedback = feedback; }

    public int getRetryCount() { return retryCount; }
    public void incrementRetryCount() { this.retryCount++; }
    public boolean canRetry() { return retryCount < MAX_RETRIES; }

    // ========== 最终输出 ==========

    public String getFinalAnswer() { return finalAnswer; }
    public void setFinalAnswer(String answer) { this.finalAnswer = answer; }

    // ========== 可观测性 ==========

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public void recordNodeTiming(String nodeId, long ms) { nodeTimings.put(nodeId, ms); }
    public Map<String, Long> getNodeTimings() { return nodeTimings; }

    public void addTokens(int in, int out) {
        this.totalTokensIn += in;
        this.totalTokensOut += out;
    }
    public int getTotalTokensIn() { return totalTokensIn; }
    public int getTotalTokensOut() { return totalTokensOut; }

    // ========== 上下文 ==========

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getSystemPromptOverride() { return systemPromptOverride; }
    public void setSystemPromptOverride(String prompt) { this.systemPromptOverride = prompt; }

    // ========== 子任务 DTO ==========

    public static class Subtask {
        private int id;
        private String description;
        private String toolHint;
        private List<Integer> dependencies = new ArrayList<>();

        public Subtask() {}

        public Subtask(int id, String description, String toolHint, List<Integer> dependencies) {
            this.id = id;
            this.description = description;
            this.toolHint = toolHint;
            this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getToolHint() { return toolHint; }
        public void setToolHint(String toolHint) { this.toolHint = toolHint; }
        public List<Integer> getDependencies() { return dependencies; }
        public void setDependencies(List<Integer> dependencies) { this.dependencies = dependencies; }

        @Override
        public String toString() {
            return "Subtask{id=" + id + ", desc='" + description + "', tool='" + toolHint + "', deps=" + dependencies + "}";
        }
    }
}
