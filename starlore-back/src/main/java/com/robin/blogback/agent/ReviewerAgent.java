package com.robin.blogback.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Reviewer（审查者）Agent。
 * <p>
 * 职责：审查 Executor 的执行结果，判断质量并给出决策：
 * - PASS：结果合格，可以输出
 * - REVISE：需要修正，附带修正建议（触发 Executor 重试）
 * - FAIL：严重失败，直接报告错误
 */
public class ReviewerAgent implements AgentNode {

    private static final Logger log = LoggerFactory.getLogger(ReviewerAgent.class);

    private static final String REVIEWER_SYSTEM_PROMPT =
            "你是 Starlore 知识库系统的审查专家（Reviewer）。\n\n"
            + "## 职责\n"
            + "审查执行结果的质量、完整性和准确性。给出明确的审查决策。\n\n"
            + "## 审查标准\n"
            + "1. **完整性**：结果是否完整回答了用户的问题？\n"
            + "2. **准确性**：数据是否准确？是否有明显的错误？\n"
            + "3. **格式**：输出格式是否合理？是否需要调整？\n"
            + "4. **错误处理**：是否有未处理的错误或异常？\n\n"
            + "## 重要说明\n"
            + "- 不是所有查询都需要调用工具。简单的问候、闲聊、通用知识问答等，Executor 可以直接由模型生成回答而无需调用任何工具，这是正常且正确的。\n"
            + "- 如果用户请求是简单的问候/自我介绍/闲聊，且执行计划合理，即使没有工具调用结果，也应判为 PASS。\n"
            + "- 只有当执行结果中存在明确错误、数据不一致、或未能回答用户问题时，才应判为 REVISE 或 FAIL。\n\n"
            + "## 输出格式\n"
            + "请严格按以下 JSON 格式输出，不要输出其他内容：\n\n"
            + "{\n"
            + "  \"decision\": \"PASS 或 REVISE 或 FAIL\",\n"
            + "  \"feedback\": \"审查意见（如果 decision 是 PASS，可以为空字符串）\",\n"
            + "  \"suggestions\": [\"修正建议1\", \"修正建议2\"]\n"
            + "}\n\n"
            + "## 决策规则\n"
            + "- PASS：结果完整、准确、格式合理；或者用户请求无需工具调用（如问候、闲聊），执行流程正常\n"
            + "- REVISE：结果基本正确但有小问题（如缺少数据、格式不佳），可修正\n"
            + "- FAIL：结果严重错误或完全无法使用\n\n"
            + "## 关键规则\n"
            + "- 如果用户请求需要工具调用（查询数据、搜索文章等），且任何子任务结果以 \"ERROR:\" 开头，说明该子任务执行失败，**必须判为 REVISE 或 FAIL**，绝对不能判为 PASS。\n"
            + "- 如果用户请求是问候/闲聊等不需要工具的场景，子任务中出现 ERROR 不影响判定，关注最终回答质量即可。\n"
            + "- 只有当工具调用类请求的所有子任务都成功完成时，才能判为 PASS。";

    private final ChatClient chatClient;
    private final LangSmithTracer tracer;
    private final BadCaseCollector badCaseCollector;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReviewerAgent(ChatClient chatClient, LangSmithTracer tracer, BadCaseCollector badCaseCollector) {
        this.chatClient = chatClient;
        this.tracer = tracer;
        this.badCaseCollector = badCaseCollector;
    }

    @Override
    public String name() { return "Reviewer"; }

    @Override
    public AgentState execute(AgentState state) throws Exception {
        log.info("[Reviewer] Reviewing execution results (retry #{})", state.getRetryCount());

        String reviewInput = buildReviewInput(state);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(REVIEWER_SYSTEM_PROMPT));
        messages.add(new UserMessage(reviewInput));

        long startTime = System.currentTimeMillis();
        ChatResponse response = chatClient.prompt()
                .messages(messages)
                .call()
                .chatResponse();
        long latencyMs = System.currentTimeMillis() - startTime;

        String output = response.getResult().getOutput().getText();
        int tokensIn = response.getMetadata() != null && response.getMetadata().getUsage() != null
                ? response.getMetadata().getUsage().getPromptTokens() : 0;
        int tokensOut = response.getMetadata() != null && response.getMetadata().getUsage() != null
                ? response.getMetadata().getUsage().getCompletionTokens() : 0;
        state.addTokens(tokensIn, tokensOut);

        if (tracer != null) {
            tracer.traceAgentCall("Reviewer", reviewInput, output, tokensIn, tokensOut, latencyMs);
        }

        // 解析审查结果
        parseReviewDecision(output, state);

        log.info("[Reviewer] Decision: {} - {}", state.getReviewDecision(), state.getReviewFeedback());

        // 如果是 FAIL 或重试耗尽的 REVISE，收集 Bad Case
        if ("FAIL".equals(state.getReviewDecision())
                || ("REVISE".equals(state.getReviewDecision()) && !state.canRetry())) {
            collectBadCase(state);
        }

        state.addMessage("assistant", "[Review] " + state.getReviewDecision()
                + (state.getReviewFeedback() != null ? ": " + state.getReviewFeedback() : ""));

        return state;
    }

    private String buildReviewInput(AgentState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 用户原始请求\n");
        sb.append(state.getUserQuery()).append("\n\n");

        sb.append("## 执行计划\n");
        sb.append(state.getPlanSummary()).append("\n\n");

        sb.append("## 执行结果\n");
        if (state.getExecutionResults().isEmpty()) {
            sb.append("（无需调用工具，由模型直接生成回答）\n\n");
        } else {
            for (Map.Entry<Integer, String> entry : state.getExecutionResults().entrySet()) {
                sb.append("### 子任务 ").append(entry.getKey()).append("\n");
                sb.append(entry.getValue()).append("\n\n");
            }
        }

        // 包含最终回答，让 Reviewer 能评估完整输出质量
        String finalAnswer = state.getFinalAnswer();
        if (finalAnswer != null && !finalAnswer.isEmpty()) {
            sb.append("## 最终回答\n");
            sb.append(finalAnswer).append("\n\n");
        }

        if (state.getRetryCount() > 0) {
            sb.append("## 之前的审查反馈\n");
            sb.append(state.getReviewFeedback()).append("\n");
            sb.append("（这是第 ").append(state.getRetryCount()).append(" 次重试）\n");
        }

        return sb.toString();
    }

    private void parseReviewDecision(String output, AgentState state) {
        try {
            String json = output.strip();
            if (json.startsWith("```")) {
                json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }

            Map<String, Object> review = objectMapper.readValue(json, new TypeReference<>() {});
            String decision = (String) review.get("decision");
            String feedback = (String) review.get("feedback");

            state.setReviewDecision(decision != null ? decision.toUpperCase() : "PASS");
            state.setReviewFeedback(feedback != null ? feedback : "");

            // 如果是 REVISE，构建包含修正建议的反馈
            if ("REVISE".equals(state.getReviewDecision()) && review.containsKey("suggestions")) {
                List<String> suggestions = (List<String>) review.get("suggestions");
                if (suggestions != null && !suggestions.isEmpty()) {
                    StringBuilder fullFeedback = new StringBuilder(feedback != null ? feedback : "");
                    fullFeedback.append("\n修正建议：");
                    for (int i = 0; i < suggestions.size(); i++) {
                        fullFeedback.append("\n").append(i + 1).append(". ").append(suggestions.get(i));
                    }
                    state.setReviewFeedback(fullFeedback.toString());
                }
            }

        } catch (Exception e) {
            log.warn("[Reviewer] Failed to parse review JSON, defaulting to PASS: {}", e.getMessage());
            // 降级：如果解析失败，默认通过
            state.setReviewDecision("PASS");
            state.setReviewFeedback("");
        }
    }

    /**
     * 收集 Bad Case 用于后续 Few-Shot 学习。
     */
    private void collectBadCase(AgentState state) {
        if (badCaseCollector == null) return;

        try {
            badCaseCollector.collect(
                    state.getUserId(),
                    state.getUserQuery(),
                    state.getFinalAnswer() != null ? state.getFinalAnswer() : "",
                    state.getReviewFeedback(),
                    "Planner->Executor->Reviewer",
                    state.getTotalTokensIn() + state.getTotalTokensOut(),
                    state.getNodeTimings().values().stream().mapToLong(Long::longValue).sum()
            );
            log.info("[Reviewer] Bad case collected for query: {}",
                    state.getUserQuery().substring(0, Math.min(50, state.getUserQuery().length())));
        } catch (Exception e) {
            log.warn("[Reviewer] Failed to collect bad case: {}", e.getMessage());
        }
    }
}
