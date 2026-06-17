package com.robin.blogback.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robin.blogback.entity.AiConfig;
import com.robin.blogback.service.AiConfigService;
import com.robin.blogback.service.AiStreamService;
import com.robin.blogback.service.FileParseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiStreamController {

    @Autowired
    private AiStreamService aiStreamService;

    @Autowired
    private AiConfigService aiConfigService;

    @Autowired
    private FileParseService fileParseService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * 解析上传的文件，提取纯文本（支持 txt/md/docx/pdf）
     */
    @PostMapping("/parse-file")
    public Map<String, Object> parseFile(@RequestParam("file") MultipartFile file) {
        try {
            String text = fileParseService.extractText(file);
            String filename = file.getOriginalFilename();
            return Map.of("success", true, "filename", filename, "text", text);
        } catch (IllegalArgumentException e) {
            return Map.of("success", false, "error", e.getMessage());
        } catch (Exception e) {
            return Map.of("success", false, "error", "文件解析失败: " + e.getMessage());
        }
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam String model,
            @RequestParam String messages) {
        try {
            List<Map<String, Object>> parsedMessages = objectMapper.readValue(
                    messages, new TypeReference<>() {});
            return aiStreamService.streamChat(model, parsedMessages);
        } catch (Exception e) {
            SseEmitter errorEmitter = new SseEmitter();
            try {
                errorEmitter.send(SseEmitter.event()
                        .data(objectMapper.writeValueAsString(Map.of("error", "参数解析失败: " + e.getMessage()))));
            } catch (Exception ignored) {
            }
            errorEmitter.complete();
            return errorEmitter;
        }
    }

    /**
     * POST 方式支持多模态消息（图片等大数据不适合放在 URL 参数里）
     */
    @PostMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPost(
            @RequestParam String model,
            @RequestBody String messages) {
        try {
            List<Map<String, Object>> parsedMessages = objectMapper.readValue(
                    messages, new TypeReference<>() {});
            return aiStreamService.streamChat(model, parsedMessages);
        } catch (Exception e) {
            SseEmitter errorEmitter = new SseEmitter();
            try {
                errorEmitter.send(SseEmitter.event()
                        .data(objectMapper.writeValueAsString(Map.of("error", "参数解析失败: " + e.getMessage()))));
            } catch (Exception ignored) {
            }
            errorEmitter.complete();
            return errorEmitter;
        }
    }

    /**
     * 图片识别（流式）：用 MiMo 模型描述图片内容，SSE 逐字返回
     */
    @PostMapping(value = "/analyze-image/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAnalyzeImage(@RequestBody Map<String, String> body) {
        String base64Image = body.get("image");
        String userQuestion = body.getOrDefault("question", "请详细描述这张图片的内容");

        SseEmitter emitter = new SseEmitter(120_000L);

        if (!StringUtils.hasText(base64Image)) {
            try {
                emitter.send(SseEmitter.event()
                        .data(objectMapper.writeValueAsString(Map.of("error", "缺少图片数据"))));
            } catch (Exception ignored) {
            }
            emitter.complete();
            return emitter;
        }

        // 从数据库获取 MiMo 配置
        AiConfig mimoConfig = aiConfigService.getConfigByKey("mimo");
        if (mimoConfig == null || !mimoConfig.getEnabled() || !StringUtils.hasText(mimoConfig.getApiKey())) {
            try {
                emitter.send(SseEmitter.event()
                        .data(objectMapper.writeValueAsString(Map.of("error", "未配置 MiMo 模型"))));
            } catch (Exception ignored) {
            }
            emitter.complete();
            return emitter;
        }

        // 确保 base64 有 data URL 前缀
        if (!base64Image.startsWith("data:")) {
            base64Image = "data:image/jpeg;base64," + base64Image;
        }

        String apiUrl = mimoConfig.getApiUrl().endsWith("/chat/completions") ? mimoConfig.getApiUrl()
                : mimoConfig.getApiUrl().replaceAll("/+$", "") + "/chat/completions";

        String finalBase64Image = base64Image;

        // 异步处理
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                // 构造多模态流式请求
                String requestJson = objectMapper.writeValueAsString(Map.of(
                        "model", mimoConfig.getModelId(),
                        "messages", List.of(Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of("type", "text", "text", userQuestion),
                                        Map.of("type", "image_url", "image_url", Map.of("url", finalBase64Image))
                                )
                        )),
                        "stream", true,
                        "max_tokens", 1024
                ));

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson));

                // MiMo API 用 api-key 头
                if (apiUrl.contains("xiaomimimo.com")) {
                    requestBuilder.header("api-key", mimoConfig.getApiKey());
                } else {
                    requestBuilder.header("Authorization", "Bearer " + mimoConfig.getApiKey());
                }

                HttpRequest request = requestBuilder.build();
                HttpResponse<java.io.InputStream> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    String errBody = new String(response.body().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    emitter.send(SseEmitter.event()
                            .data(objectMapper.writeValueAsString(Map.of("error", "MiMo API 错误: " + response.statusCode() + " " + errBody))));
                    emitter.complete();
                    return;
                }

                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(response.body(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    int lineCount = 0;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.equals("data: [DONE]")) continue;

                        // MiMo 可能不支持流式，返回普通 JSON
                        if (!trimmed.startsWith("data: ") && trimmed.startsWith("{")) {
                            // 尝试解析为普通 JSON 响应
                            try {
                                JsonNode node = objectMapper.readTree(trimmed);
                                String content = node.at("/choices/0/message/content").asText("");
                                if (!content.isEmpty()) {
                                    System.out.println("[MiMo] 非流式响应，一次性返回: " + content.length() + " 字符");
                                    emitter.send(SseEmitter.event()
                                            .data(objectMapper.writeValueAsString(Map.of("content", content))));
                                }
                            } catch (Exception ignored) {}
                            continue;
                        }

                        if (!trimmed.startsWith("data: ")) continue;

                        String json = trimmed.substring(6);
                        try {
                            JsonNode node = objectMapper.readTree(json);
                            String chunk = node.at("/choices/0/delta/content").asText("");
                            if (!chunk.isEmpty()) {
                                lineCount++;
                                if (lineCount <= 3 || lineCount % 20 == 0) {
                                    System.out.println("[MiMo] 流式 chunk #" + lineCount + ": " + chunk.substring(0, Math.min(50, chunk.length())) + "...");
                                }
                                emitter.send(SseEmitter.event()
                                        .data(objectMapper.writeValueAsString(Map.of("content", chunk))));
                            }
                        } catch (Exception e) {
                            // Skip unparseable lines
                        }
                    }
                    System.out.println("[MiMo] 流式传输完成，共 " + lineCount + " 个 chunk");
                    emitter.send(SseEmitter.event()
                            .data(objectMapper.writeValueAsString(Map.of("done", true))));
                    emitter.complete();
                }
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .data(objectMapper.writeValueAsString(Map.of("error", e.getMessage() != null ? e.getMessage() : "图片识别失败"))));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> emitter.complete());
        return emitter;
    }

    /**
     * 图片识别：用 MiMo 模型描述图片内容，返回文字描述
     */
    @PostMapping("/analyze-image")
    public Map<String, Object> analyzeImage(@RequestBody Map<String, String> body) {
        String base64Image = body.get("image");
        String userQuestion = body.getOrDefault("question", "请详细描述这张图片的内容");

        if (!StringUtils.hasText(base64Image)) {
            return Map.of("success", false, "error", "缺少图片数据");
        }

        // 从数据库获取 MiMo 配置
        AiConfig mimoConfig = aiConfigService.getConfigByKey("mimo");
        if (mimoConfig == null || !mimoConfig.getEnabled() || !StringUtils.hasText(mimoConfig.getApiKey())) {
            return Map.of("success", false, "error", "未配置 MiMo 模型");
        }

        try {
            // 确保 base64 有 data URL 前缀
            if (!base64Image.startsWith("data:")) {
                base64Image = "data:image/jpeg;base64," + base64Image;
            }

            String apiUrl = mimoConfig.getApiUrl().endsWith("/chat/completions") ? mimoConfig.getApiUrl()
                    : mimoConfig.getApiUrl().replaceAll("/+$", "") + "/chat/completions";

            // 构造多模态请求
            String requestJson = objectMapper.writeValueAsString(Map.of(
                    "model", mimoConfig.getModelId(),
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of("type", "text", "text", userQuestion),
                                    Map.of("type", "image_url", "image_url", Map.of("url", base64Image))
                            )
                    )),
                    "stream", false,
                    "max_tokens", 1024
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("api-key", mimoConfig.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                return Map.of("success", false, "error", "MiMo API 错误: " + response.statusCode() + " " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            String description = json.at("/choices/0/message/content").asText("");

            return Map.of("success", true, "description", description);
        } catch (Exception e) {
            return Map.of("success", false, "error", "图片识别失败: " + e.getMessage());
        }
    }
}
