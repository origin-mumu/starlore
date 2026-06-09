package com.robin.blogback.config;

import com.robin.blogback.agent.ExecutorAgent;
import com.robin.blogback.agent.PlannerAgent;
import com.robin.blogback.agent.ReviewerAgent;
import com.robin.blogback.entity.AiConfig;
import com.robin.blogback.graph.AgentGraph;
import com.robin.blogback.graph.AgentState;
import com.robin.blogback.observability.AgentMetrics;
import com.robin.blogback.observability.BadCaseCollector;
import com.robin.blogback.observability.LangSmithTracer;
import com.robin.blogback.service.AiConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Multi-Agent 配置。
 * <p>
 * 创建 Planner、Executor、Reviewer 三个专用 ChatModel，
 * 并编排 AgentGraph 状态机。
 */
@Configuration
public class MultiAgentConfig {

    private static final Logger log = LoggerFactory.getLogger(MultiAgentConfig.class);

    @Autowired
    private AiConfigService aiConfigService;

    @Autowired
    private LangSmithTracer langSmithTracer;

    @Autowired(required = false)
    private BadCaseCollector badCaseCollector;

    @Autowired(required = false)
    private AgentMetrics agentMetrics;

    @Value("${spring.ai.openai.api-key:}")
    private String localApiKey;

    @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
    private String localBaseUrl;

    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String localModel;

    // ========== 专用 ChatModel Beans ==========

    /**
     * Planner 专用 ChatModel（低 temperature，确保规划稳定）。
     */
    @Bean("plannerChatModel")
    public ChatModel plannerChatModel() {
        return createChatModel("planner", 0.3);
    }

    /**
     * Executor 专用 ChatModel（中等 temperature，允许灵活执行）。
     */
    @Bean("executorChatModel")
    public ChatModel executorChatModel() {
        return createChatModel("executor", 0.5);
    }

    /**
     * Reviewer 专用 ChatModel（低 temperature，确保审查严格）。
     */
    @Bean("reviewerChatModel")
    public ChatModel reviewerChatModel() {
        return createChatModel("reviewer", 0.2);
    }

    // ========== ChatClient Beans ==========

    @Bean("plannerChatClient")
    public ChatClient plannerChatClient(ChatModel plannerChatModel) {
        return ChatClient.builder(plannerChatModel).build();
    }

    @Bean("executorChatClient")
    public ChatClient executorChatClient(ChatModel executorChatModel) {
        return ChatClient.builder(executorChatModel).build();
    }

    @Bean("reviewerChatClient")
    public ChatClient reviewerChatClient(ChatModel reviewerChatModel) {
        return ChatClient.builder(reviewerChatModel).build();
    }

    // ========== Agent Beans ==========

    @Bean("plannerAgent")
    public PlannerAgent plannerAgent(ChatClient plannerChatClient) {
        return new PlannerAgent(plannerChatClient, langSmithTracer);
    }

    @Bean("executorAgent")
    public ExecutorAgent executorAgent(ChatClient executorChatClient,
                                        com.robin.blogback.service.BlogTools blogTools) {
        return new ExecutorAgent(executorChatClient, blogTools, langSmithTracer, badCaseCollector);
    }

    @Bean("reviewerAgent")
    public ReviewerAgent reviewerAgent(ChatClient reviewerChatClient) {
        return new ReviewerAgent(reviewerChatClient, langSmithTracer, badCaseCollector);
    }

    // ========== AgentGraph Bean ==========

    /**
     * 编排 Multi-Agent Graph：
     * <pre>
     * [START] -> [Planner] -> [Executor] -> [Reviewer] ─┬─ PASS -> [Synthesizer] -> [END]
     *                                                    ├─ REVISE & canRetry -> [Executor]
     *                                                    └─ FAIL / retryExhausted -> [END]
     * </pre>
     */
    @Bean("multiAgentGraph")
    public AgentGraph multiAgentGraph(PlannerAgent plannerAgent,
                                       ExecutorAgent executorAgent,
                                       ReviewerAgent reviewerAgent) {
        return AgentGraph.builder()
                .node("planner", plannerAgent)
                .node("executor", executorAgent)
                .node("reviewer", reviewerAgent)
                .node("synthesizer", this::synthesizeFinalAnswer)
                .edge(AgentGraph.START, "planner")
                .edge("planner", "executor")
                .edge("executor", "reviewer")
                .conditionalEdge("reviewer", state -> {
                    String decision = state.getReviewDecision();
                    if ("PASS".equals(decision)) {
                        return "synthesizer";
                    } else if ("REVISE".equals(decision) && state.canRetry()) {
                        state.incrementRetryCount();
                        return "executor";
                    } else {
                        // FAIL 或重试耗尽
                        if (state.getFinalAnswer() == null || state.getFinalAnswer().isEmpty()) {
                            state.setFinalAnswer("执行失败：" + state.getReviewFeedback());
                        }
                        return AgentGraph.END;
                    }
                })
                .edge("synthesizer", AgentGraph.END)
                .onEvent((type, state, nodeId) -> {
                    if (agentMetrics != null && "graph_end".equals(type)) {
                        agentMetrics.recordExecution(
                                state.getTotalTokensIn(),
                                state.getTotalTokensOut(),
                                state.getNodeTimings().values().stream().mapToLong(Long::longValue).sum(),
                                state.getNodeTimings(),
                                state.getRetryCount(),
                                state.getReviewDecision()
                        );
                    }
                })
                .compile("planner");
    }

    // ========== 内部方法 ==========

    /**
     * 最终答案合成节点：将 Executor 的结果和 Reviewer 的反馈整合为最终输出。
     */
    private AgentState synthesizeFinalAnswer(AgentState state) {
        String answer = state.getFinalAnswer();
        if (answer == null || answer.isEmpty()) {
            // 从执行结果中组装答案
            StringBuilder sb = new StringBuilder();
            for (var entry : state.getExecutionResults().entrySet()) {
                sb.append(entry.getValue()).append("\n\n");
            }
            answer = sb.toString().trim();
        }
        state.setFinalAnswer(answer);
        state.addMessage("assistant", answer);
        return state;
    }

    private ChatModel createChatModel(String role, double temperature) {
        AiConfig dbConfig = aiConfigService.getConfigByKey("deepseek-v4-flash");

        String apiKey;
        String baseUrl;
        String model;

        if (dbConfig != null && dbConfig.getEnabled() && StringUtils.hasText(dbConfig.getApiKey())) {
            apiKey = dbConfig.getApiKey();
            baseUrl = dbConfig.getApiUrl().replaceAll("/chat/completions$", "");
            model = dbConfig.getModelId();
        } else {
            apiKey = localApiKey;
            baseUrl = localBaseUrl;
            model = localModel;
        }

        log.info("[MultiAgent] Creating {} ChatModel: model={}, temp={}", role, model, temperature);

        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build();
        return new OpenAiChatModel(openAiApi, options);
    }
}
