package com.robin.blogback.agent;

import com.robin.blogback.graph.AgentState;
import com.robin.blogback.graph.AgentNode;
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
 * Synthesizer（合成器）Agent。
 * <p>
 * 职责：将 Executor 的子任务结果整合为连贯的最终回答。
 * - 过滤掉失败的子任务（ERROR 开头）
 * - 合并成功的子任务结果
 * - 生成自然、连贯的回答
 */
public class SynthesizerAgent implements AgentNode {

    private static final Logger log = LoggerFactory.getLogger(SynthesizerAgent.class);

    private static final String SYNTHESIZER_SYSTEM_PROMPT =
            "你是 Starlore 知识库系统的回答合成专家（Synthesizer）。\n\n"
            + "## 职责\n"
            + "将多个子任务的执行结果整合为一个连贯、自然、完整的最终回答。\n\n"
            + "## 规则\n"
            + "1. **过滤失败结果**：忽略所有以 \"ERROR:\" 开头的子任务结果，不要在回答中提及它们。\n"
            + "2. **合并成功结果**：将成功的子任务结果有机整合，去除重复内容。\n"
            + "3. **自然表达**：用自然、流畅的语言组织回答，不要出现 \"执行结果\"、\"子任务\" 等内部标签。\n"
            + "4. **保持完整**：不要丢失重要的数据和信息。\n"
            + "5. **全部失败时**：如果所有子任务都失败了，礼貌地告诉用户执行未成功，建议换个方式提问。\n\n"
            + "## 输出\n"
            + "直接输出最终回答内容，不要加任何前缀、标签或元信息。";

    private final ChatClient chatClient;
    private final LangSmithTracer tracer;

    public SynthesizerAgent(ChatClient chatClient, LangSmithTracer tracer) {
        this.chatClient = chatClient;
        this.tracer = tracer;
    }

    @Override
    public String name() { return "Synthesizer"; }

    @Override
    public AgentState execute(AgentState state) throws Exception {
        log.info("[Synthesizer] Synthesizing final answer");

        String synthesisInput = buildSynthesisInput(state);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYNTHESIZER_SYSTEM_PROMPT));
        messages.add(new UserMessage(synthesisInput));

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
            tracer.traceAgentCall("Synthesizer", synthesisInput, output, tokensIn, tokensOut, latencyMs);
        }

        state.setFinalAnswer(output);
        state.addMessage("assistant", output);

        log.info("[Synthesizer] Final answer generated ({} chars)", output.length());
        return state;
    }

    private String buildSynthesisInput(AgentState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 用户原始请求\n");
        sb.append(state.getUserQuery()).append("\n\n");

        sb.append("## 执行计划\n");
        sb.append(state.getPlanSummary()).append("\n\n");

        sb.append("## 子任务执行结果\n");
        Map<Integer, String> results = state.getExecutionResults();
        if (results.isEmpty()) {
            sb.append("（无执行结果）\n\n");
        } else {
            for (Map.Entry<Integer, String> entry : results.entrySet()) {
                sb.append("### 子任务 ").append(entry.getKey()).append("\n");
                sb.append(entry.getValue()).append("\n\n");
            }
        }

        String reviewDecision = state.getReviewDecision();
        if (reviewDecision != null) {
            sb.append("## 审查结果\n");
            sb.append("决策：").append(reviewDecision).append("\n");
            String feedback = state.getReviewFeedback();
            if (feedback != null && !feedback.isEmpty()) {
                sb.append("反馈：").append(feedback).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
