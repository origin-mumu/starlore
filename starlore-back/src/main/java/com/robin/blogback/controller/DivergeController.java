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
            "你是一个头脑风暴创意联想助手。能够根据用户输入的词语，向外扩散联想出与之强关联的 8 个最典型、最生动、最具画面感的事物或概念。你必须严格以 JSON 数组形式返回结果，不能包含任何其他 Markdown 语法或额外解释。";

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
                "请输入词为：\"%s\"。请围绕它向外联想 8 个关联词语。\n\n" +
                "要求：\n" +
                "1. 每个联想词必须与输入词有强烈的直接关联，逻辑必须合乎常理、生动逼真（例如对于食物或动作，应联想相关器具、食材、流派、场景或直接相关联想词，避免生硬死板地套用无关概念）。\n" +
                "2. 每个联想词包含 zh（中文）和 en（英文翻译），必须使用 JSON 格式表示，例如：[{\"zh\":\"火锅\",\"en\":\"hotpot\"}, ...]。\n" +
                "3. 严禁返回任何 Markdown 代码块包裹（如 ```json）或多余的文字说明，仅返回纯粹的 JSON 数组。",
                word);

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
