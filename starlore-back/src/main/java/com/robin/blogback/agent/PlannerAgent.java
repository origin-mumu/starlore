package com.robin.blogback.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Planner（规划者）Agent。
 * <p>
 * 职责：分析用户意图，将复杂任务拆解为可执行的子任务列表。
 * <p>
 * 输出结构化的 Plan：包含子任务描述、工具提示和依赖关系。
 */
public class PlannerAgent implements AgentNode {

    private static final Logger log = LoggerFactory.getLogger(PlannerAgent.class);

    private static final String PLANNER_SYSTEM_PROMPT =
            "你是 Starlore 知识库系统的任务规划专家（Planner）。\n\n"
            + "## 职责\n"
            + "分析用户的请求，将其拆解为一系列可执行的子任务。每个子任务应该足够具体，\n"
            + "以便执行者（Executor）能够独立完成。\n\n"
            + "## 可用工具\n"
            + "- searchArticles: 搜索知识库文章（语义搜索 + 关键词搜索）\n"
            + "- getArticleDetail: 获取文章详情\n"
            + "- getCategories: 获取所有分类\n"
            + "- getBlogStats: 获取知识库统计数据\n"
            + "- getRecentArticles: 获取最新文章\n"
            + "- writeArticle: 创建新文章\n"
            + "- updateArticle: 更新文章\n"
            + "- deleteArticle: 删除文章\n"
            + "- getAllTags: 获取所有标签\n"
            + "- getArticlesByCategory: 按分类获取文章\n"
            + "- createCategory: 创建新分类\n\n"
            + "## 输出格式\n"
            + "请严格按以下 JSON 格式输出，不要输出其他内容：\n\n"
            + "{\n"
            + "  \"summary\": \"任务概述（一句话）\",\n"
            + "  \"subtasks\": [\n"
            + "    {\n"
            + "      \"id\": 1,\n"
            + "      \"description\": \"子任务的具体描述\",\n"
            + "      \"toolHint\": \"建议使用的工具名（如不需要工具则为 null）\",\n"
            + "      \"dependencies\": []\n"
            + "    }\n"
            + "  ]\n"
            + "}\n\n"
            + "## 规则\n"
            + "1. 简单查询（如\"显示最新文章\"）只需 1 个子任务\n"
            + "2. 复杂任务（如\"统计知识库数据并写总结\"）拆解为 2-5 个子任务\n"
            + "3. 有依赖关系的子任务必须在 dependencies 中声明前置子任务的 id\n"
            + "4. 可并行执行的子任务不要设置依赖\n"
            + "5. 如果用户请求不需要工具（如闲聊），返回空的 subtasks 数组";

    private final ChatClient chatClient;
    private final LangSmithTracer tracer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlannerAgent(ChatClient chatClient, LangSmithTracer tracer) {
        this.chatClient = chatClient;
        this.tracer = tracer;
    }

    @Override
    public String name() { return "Planner"; }

    @Override
    public AgentState execute(AgentState state) throws Exception {
        log.info("[Planner] Analyzing user query: {}",
                state.getUserQuery().length() > 100
                        ? state.getUserQuery().substring(0, 100) + "..."
                        : state.getUserQuery());

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(PLANNER_SYSTEM_PROMPT));

        // 注入 Bad Case 作为 Few-Shot 示例（如果有）
        if (state.getSystemPromptOverride() != null) {
            messages.add(new SystemMessage(state.getSystemPromptOverride()));
        }

        messages.add(new UserMessage(state.getUserQuery()));

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

        // 记录 tracing
        if (tracer != null) {
            tracer.traceAgentCall("Planner", state.getUserQuery(), output, tokensIn, tokensOut, latencyMs);
        }

        // 解析 Plan
        try {
            // 清理可能的 markdown 代码块包裹
            String json = output.strip();
            if (json.startsWith("```")) {
                json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }

            Map<String, Object> plan = objectMapper.readValue(json, new TypeReference<>() {});
            state.setPlanSummary((String) plan.get("summary"));

            List<Map<String, Object>> subtaskList = (List<Map<String, Object>>) plan.get("subtasks");
            if (subtaskList != null) {
                List<AgentState.Subtask> subtasks = new ArrayList<>();
                for (Map<String, Object> st : subtaskList) {
                    int id = st.get("id") instanceof Number ? ((Number) st.get("id")).intValue() : 0;
                    String desc = (String) st.get("description");
                    String toolHint = (String) st.get("toolHint");
                    List<Integer> deps = st.get("dependencies") instanceof List
                            ? (List<Integer>) st.get("dependencies")
                            : new ArrayList<>();
                    subtasks.add(new AgentState.Subtask(id, desc, toolHint, deps));
                }
                state.setSubtasks(subtasks);
            }

            log.info("[Planner] Plan created: {} subtasks - {}",
                    state.getSubtasks().size(), state.getPlanSummary());

        } catch (Exception e) {
            log.warn("[Planner] Failed to parse plan JSON, treating as single task: {}", e.getMessage());
            // 降级：将整个用户查询作为一个子任务
            state.setPlanSummary("直接回答用户问题");
            state.setSubtasks(List.of(
                    new AgentState.Subtask(1, state.getUserQuery(), null, List.of())
            ));
        }

        state.addMessage("assistant", "[Plan] " + state.getPlanSummary()
                + " (" + state.getSubtasks().size() + " subtasks)");

        return state;
    }
}
