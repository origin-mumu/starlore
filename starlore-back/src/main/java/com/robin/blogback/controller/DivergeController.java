package com.robin.blogback.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robin.blogback.service.AiQuotaService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class DivergeController {

    private static final Logger log = LoggerFactory.getLogger(DivergeController.class);

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private AiQuotaService aiQuotaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
            "你是一个创意联想助手，擅长从一个词出发，沿着具体的方向（工具、场景、人物、风格、趋势等）找到生动且强相关的联想词。" +
            "你的联想让人感觉\"妙啊，确实是这样\"，而不是\"这有什么关系？\"。你只返回JSON数组，不返回其他内容。";

    @PostMapping("/diverge")
    public ResponseEntity<?> diverge(HttpServletRequest httpRequest, @RequestBody Map<String, String> body) {
        String word = body.get("word");
        if (word == null || word.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请输入一个词"));
        }

        // 检查 AI 配额
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        int remaining = aiQuotaService.getRemaining(userId);
        if (remaining == 0) {
            return ResponseEntity.status(429).body(Map.of("message", "今日 AI 对话次数已用尽，明天再来吧～"));
        }

        try {
            // 消耗一次配额
            aiQuotaService.tryConsume(userId);
            String userPrompt = String.format(
                "用户输入了\"%s\"，请围绕它联想8个词。\n\n" +
                "核心原则：每个词必须和\"%s\"强相关。联想可以巧妙、有趣，但不能牵强——如果别人看到这个词，应该能立刻明白\"为什么从%s想到了它\"。\n\n" +
                "联想方向建议（每个方向挑一两个即可，不用全部覆盖）：\n" +
                "- 工具/设备：%s常用什么工具\n" +
                "- 场景/空间：%s在什么环境下工作或出现\n" +
                "- 上下游：%s的上游输入或下游产出是什么\n" +
                "- 风格/流派：%s领域内有什么分支或风格\n" +
                "- 代表人物/品牌：行业内公认的名字\n" +
                "- 痛点/需求：%s面临什么困扰或用户需要什么\n" +
                "- 搭配/组合：%s常和什么一起出现\n" +
                "- 趋势/新事物：%s领域最近有什么新变化\n\n" +
                "要求：\n" +
                "1. 每个联想词必须是和\"%s\"直接相关的具体事物，不能是抽象概念\n" +
                "2. 优先选生动、有画面感的词，让人能\"看到\"它\n" +
                "3. 网感可以有，但不能为了网感牺牲相关性\n" +
                "4. 每个词包含 zh 和 en，严格按JSON数组返回，不要其他文字",
                word, word, word, word, word, word, word, word, word, word, word);

            log.info("Diverge request: word={}", word);

            // 使用 Spring AI ChatClient（与 echobot 共享相同配置）
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(SYSTEM_PROMPT));
            messages.add(new UserMessage(userPrompt));

            ChatResponse chatResponse = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .chatResponse();

            String content = chatResponse.getResult().getOutput().getText();
            log.info("Diverge response: {}", content);

            // 提取 JSON 数组（处理 markdown 代码块包裹）
            String json = extractJsonArray(content);
            if (json == null) {
                log.error("Failed to extract JSON from response: {}", content);
                return ResponseEntity.status(500).body(Map.of("error", "Failed to parse response"));
            }

            JsonNode pairsNode = objectMapper.readTree(json);
            List<Map<String, String>> pairs = new ArrayList<>();
            for (JsonNode pair : pairsNode) {
                Map<String, String> p = new LinkedHashMap<>();
                p.put("en", pair.has("en") ? pair.get("en").asText() : "");
                p.put("zh", pair.has("zh") ? pair.get("zh").asText() : "");
                pairs.add(p);
            }

            return ResponseEntity.ok(Map.of("pairs", pairs));

        } catch (Exception e) {
            log.error("Diverge error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    private String extractJsonArray(String content) {
        if (content == null || content.isBlank()) return null;

        // Try to find [...] substring (handles markdown ```json ... ``` wrapping)
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content.trim();
    }
}
