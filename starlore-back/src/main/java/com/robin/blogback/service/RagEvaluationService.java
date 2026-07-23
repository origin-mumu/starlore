package com.robin.blogback.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robin.blogback.dto.RagEvaluationResponse;
import com.robin.blogback.entity.Article;
import com.robin.blogback.exception.BadRequestException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RAGAS-aligned LLM-as-judge evaluation for the production Java backend.
 *
 * The metric definitions follow the four common RAG evaluation dimensions while
 * keeping evaluation executable without a Python sidecar.
 */
@Service
public class RagEvaluationService {

    private static final String EVALUATION_PROMPT = """
            你是严格的 RAG 质量评估器。请只根据问题、回答、检索上下文和可选标准答案评分。

            四项指标均为 0 到 1：
            1. faithfulness：回答中的事实是否能被检索上下文支持。
            2. answerRelevance：回答是否直接、完整地回应问题。
            3. contextPrecision：检索上下文中与问题相关的信息比例是否高。
            4. contextRecall：检索上下文是否覆盖回答问题所需的信息；若提供标准答案，以标准答案为参照。

            只输出合法 JSON，不要 Markdown：
            {
              "faithfulness": 0.0,
              "answerRelevance": 0.0,
              "contextPrecision": 0.0,
              "contextRecall": 0.0
            }

            【问题】
            %s

            【待评估回答】
            %s

            【标准答案】
            %s

            【检索上下文】
            %s
            """;

    private final ArticleEmbeddingService articleEmbeddingService;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public RagEvaluationService(
            ArticleEmbeddingService articleEmbeddingService,
            @Qualifier("chatClient") ChatClient chatClient,
            ObjectMapper objectMapper) {
        this.articleEmbeddingService = articleEmbeddingService;
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    public RagEvaluationResponse evaluate(
            Integer userId, String question, String answer, String groundTruth, Integer requestedTopK) {
        int topK = requestedTopK == null ? 5 : Math.max(1, Math.min(requestedTopK, 10));
        List<Article> articles = articleEmbeddingService.searchSimilar(question, userId, topK);
        if (articles.isEmpty()) {
            throw new BadRequestException(
                    "上一轮问题没有检索到相关知识库文章，不能进行 RAG 评估。请先询问一个与你的文章内容相关的问题。");
        }

        StringBuilder contexts = new StringBuilder();
        List<RagEvaluationResponse.ContextSource> sources = new ArrayList<>();
        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);
            String content = article.getContent() == null ? "" : article.getContent();
            if (content.length() > 3000) content = content.substring(0, 3000);
            contexts.append("\n[上下文 ").append(i + 1).append("，文章《")
                    .append(article.getTitle()).append("》]\n").append(content).append("\n");
            sources.add(new RagEvaluationResponse.ContextSource(article.getId(), article.getTitle()));
        }

        String prompt = EVALUATION_PROMPT.formatted(
                question,
                answer,
                groundTruth == null || groundTruth.isBlank() ? "未提供，请根据问题判断必要信息覆盖度" : groundTruth,
                contexts
        );
        String raw = chatClient.prompt().user(prompt).call().content();
        RagEvaluationResponse.Scores scores = parseScores(raw);
        return new RagEvaluationResponse(true, "ragas-aligned-llm-judge", scores, sources);
    }

    private RagEvaluationResponse.Scores parseScores(String raw) {
        try {
            String json = raw == null ? "" : raw.trim();
            if (json.startsWith("```")) {
                json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
            }
            JsonNode node = objectMapper.readTree(json);
            double faithfulness = score(node, "faithfulness");
            double relevance = score(node, "answerRelevance");
            double precision = score(node, "contextPrecision");
            double recall = score(node, "contextRecall");
            double overall = clamp(
                    faithfulness * 0.35 + relevance * 0.25 + precision * 0.20 + recall * 0.20);
            return new RagEvaluationResponse.Scores(
                    round(faithfulness), round(relevance), round(precision), round(recall), round(overall));
        } catch (Exception e) {
            throw new IllegalStateException("RAG 评估结果解析失败: " + e.getMessage(), e);
        }
    }

    private double score(JsonNode node, String field) {
        if (node == null || !node.has(field) || !node.get(field).isNumber()) {
            throw new IllegalArgumentException("评估结果缺少数字字段: " + field);
        }
        return clamp(node.get(field).asDouble());
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }
}
