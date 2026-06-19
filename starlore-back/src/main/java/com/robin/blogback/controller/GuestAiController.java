package com.robin.blogback.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robin.blogback.entity.Article;
import com.robin.blogback.mapper.ArticleMapper;
import com.robin.blogback.service.ArticleEmbeddingService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/public/ai")
public class GuestAiController {

    private static final Logger log = LoggerFactory.getLogger(GuestAiController.class);

    @Autowired
    private ChatClient chatClient;

    @Autowired(required = false)
    private ArticleEmbeddingService articleEmbeddingService;

    @Autowired
    private ArticleMapper articleMapper;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:deepseek-v4-flash}")
    private String modelName;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // IP-based Rate Limiter (20 requests per IP per day)
    private final Map<String, AtomicInteger> ipRequestCounts = new ConcurrentHashMap<>();
    private final Map<String, LocalDate> ipLastRequestDates = new ConcurrentHashMap<>();

    private static final String DIVERGE_SYSTEM_PROMPT =
            "你是一个创意联想助手，擅长从一个词出发，沿着具体的方向（工具、场景、人物、风格、趋势等）找到生动且强相关的联想词。" +
            "你的联想让人感觉\"妙啊，确实是这样\"，而不是\"这有什么关系？\"。你只返回JSON数组，不返回其他内容。";

    private static final Map<String, String> CHARACTER_SYSTEM_PROMPTS = Map.of(
            "default", "你是一个名叫'星轮助手'的智能助理。你的态度和蔼、专业，对文学、科幻和本站(Starlore)的技术架构及内容有着深入的了解。",
            "philosopher", "你现在扮演哲学家庄子，说话充满道家智慧和哲理，善用寓言，物我两忘，逍遥自在，常以'吾'自称。",
            "explorer", "你现在扮演一名银河探索者，是一个热衷于探索未知星域的科幻领航员，说话带有机甲、星域、探索的科幻色彩。"
    );

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private synchronized int checkAndConsumeQuota(String ip, boolean consume) {
        LocalDate today = LocalDate.now();
        LocalDate lastDate = ipLastRequestDates.get(ip);
        if (lastDate == null || !lastDate.equals(today)) {
            ipLastRequestDates.put(ip, today);
            ipRequestCounts.put(ip, new AtomicInteger(0));
        }
        AtomicInteger count = ipRequestCounts.get(ip);
        if (consume) {
            if (count.get() >= 20) {
                return -1; // Exceeded
            }
            return count.incrementAndGet();
        } else {
            return count.get();
        }
    }

    @GetMapping("/guest-quota")
    public ResponseEntity<?> getGuestQuota(HttpServletRequest request) {
        String ip = getClientIp(request);
        int count = checkAndConsumeQuota(ip, false);
        int remaining = Math.max(0, 20 - count);
        return ResponseEntity.ok(Map.of("limit", 20, "remaining", remaining));
    }

    @PostMapping("/diverge")
    public ResponseEntity<?> guestDiverge(HttpServletRequest httpRequest, @RequestBody Map<String, String> body) {
        String word = body.get("word");
        if (word == null || word.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请输入一个词"));
        }

        String ip = getClientIp(httpRequest);
        int newCount = checkAndConsumeQuota(ip, true);
        if (newCount == -1) {
            return ResponseEntity.status(429).body(Map.of("message", "今日访客体验额度（20次）已用尽，登录后即可享受无限次数与专属 Agent 服务哦！"));
        }
        int remaining = Math.max(0, 20 - newCount);

        try {
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

            log.info("Guest Diverge request (IP={}): word={}", ip, word);

            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(DIVERGE_SYSTEM_PROMPT));
            messages.add(new UserMessage(userPrompt));

            ChatResponse chatResponse = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .chatResponse();

            String content = chatResponse.getResult().getOutput().getText();

            String json = extractJsonArray(content);
            if (json == null) {
                log.error("Failed to extract JSON from guest response: {}", content);
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

            return ResponseEntity.ok(Map.of("pairs", pairs, "remaining", remaining));

        } catch (Exception e) {
            log.error("Guest Diverge error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    @PostMapping("/guest-chat")
    public ResponseEntity<?> guestChat(HttpServletRequest httpRequest, @RequestBody Map<String, Object> body) {
        String message = (String) body.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "消息不能为空"));
        }

        String ip = getClientIp(httpRequest);
        int newCount = checkAndConsumeQuota(ip, true);
        if (newCount == -1) {
            return ResponseEntity.status(429).body(Map.of("message", "今日访客体验额度（20次）已用尽，登录后即可享受无限次数与专属 Agent 服务哦！"));
        }
        int remaining = Math.max(0, 20 - newCount);

        try {
            // 1. 公开数据语义搜索 (RAG)
            List<Article> matchedArticles = new ArrayList<>();
            if (articleEmbeddingService != null) {
                matchedArticles = articleEmbeddingService.searchSimilarPublic(message, 5);
            }
            if (matchedArticles.isEmpty()) {
                // 回退到数据库关键词搜索
                LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, "published")
                        .eq(Article::getIsPublic, true)
                        .and(w -> w.like(Article::getTitle, message).or().like(Article::getDescription, message))
                        .orderByDesc(Article::getCreatedAt)
                        .last("LIMIT 5");
                matchedArticles = articleMapper.selectList(wrapper);
            }

            // 2. 格式化上下文
            StringBuilder contextBuilder = new StringBuilder();
            if (!matchedArticles.isEmpty()) {
                contextBuilder.append("\n\n[参考公开知识库内容]\n");
                contextBuilder.append("以下是与用户提问相关的公开文章内容：\n");
                for (Article a : matchedArticles) {
                    contextBuilder.append(String.format("--- \n文章标题：《%s》\n分类：%s\n摘要：%s\n",
                            a.getTitle(), a.getCategory(), a.getDescription()));
                    String content = a.getContent();
                    if (content != null && !content.isEmpty()) {
                        String plain = content.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
                        if (plain.length() > 500) plain = plain.substring(0, 500) + "...";
                        contextBuilder.append("内容详情: ").append(plain).append("\n");
                    }
                }
                contextBuilder.append("\n在回答用户关于本站文章、星野分类或技术栈等问题时，请优先使用上述参考公开文章的内容作为事实根据。");
            }

            // 3. 构建 System Prompt
            String characterKey = (String) body.getOrDefault("character", "default");
            String charSystem = CHARACTER_SYSTEM_PROMPTS.getOrDefault(characterKey, CHARACTER_SYSTEM_PROMPTS.get("default"));

            String finalSystemPrompt = charSystem + contextBuilder.toString() +
                    "\n\n注意：你目前正在以'访客体验模式'与用户对话。用户每天有20次真实的AI对话额度。在回答完后，如果合适，请友好地提醒用户：'您可以随时登录，以解锁完整的个人云端空间、多Agent团队协作以及更高级的深度模型流式对话体验！'";

            // 4. 解析多轮对话历史
            List<Map<String, String>> apiMessages = new ArrayList<>();
            apiMessages.add(Map.of("role", "system", "content", finalSystemPrompt));

            List<Map<String, String>> history = (List<Map<String, String>>) body.get("history");
            if (history != null) {
                for (Map<String, String> histMsg : history) {
                    String role = histMsg.get("role");
                    String content = histMsg.get("content");
                    if (role != null && content != null && !content.trim().isEmpty()) {
                        apiMessages.add(Map.of("role", role, "content", content));
                    }
                }
            }
            apiMessages.add(Map.of("role", "user", "content", message));

            // 5. 构造原生 HTTP 请求，以便同时获取 content 和 reasoning_content
            Map<String, Object> requestBody = new java.util.LinkedHashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("messages", apiMessages);
            requestBody.put("stream", false);

            String finalApiUrl = baseUrl + "/v1/chat/completions";
            if (baseUrl.endsWith("/v1") || baseUrl.endsWith("/v1/")) {
                finalApiUrl = baseUrl + "/chat/completions";
            }

            String bodyJson = objectMapper.writeValueAsString(requestBody);
            log.info("Guest Chat request (IP={}): URL={}, model={}", ip, finalApiUrl, modelName);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(finalApiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                log.error("Guest AI Chat API error: status={}, body={}", response.statusCode(), response.body());
                return ResponseEntity.status(500).body(Map.of("error", "AI service error: HTTP " + response.statusCode()));
            }

            JsonNode responseNode = objectMapper.readTree(response.body());
            JsonNode messageNode = responseNode.at("/choices/0/message");
            String replyContent = messageNode.has("content") ? messageNode.get("content").asText("") : "";
            String reasoningContent = messageNode.has("reasoning_content") ? messageNode.get("reasoning_content").asText("") : "";

            return ResponseEntity.ok(Map.of(
                    "content", replyContent,
                    "reasoningContent", reasoningContent,
                    "remaining", remaining
            ));

        } catch (Exception e) {
            log.error("Guest Chat error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    private String extractJsonArray(String content) {
        if (content == null || content.isBlank()) return null;
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content.trim();
    }
}
