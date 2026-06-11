package com.robin.blogback.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robin.blogback.entity.AiConfig;
import com.robin.blogback.service.AiConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * AI ASR 语音转文字控制器 — MiMo mimo-v2.5-asr
 *
 * 两个端点：
 * - POST /api/ai/transcribe        非流式，一次性返回全文
 * - POST /api/ai/transcribe/stream SSE 流式，逐字返回（推荐）
 */
@RestController
@RequestMapping("/api/ai")
public class AiAsrController {

    @Autowired
    private AiConfigService aiConfigService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String ASR_MODEL = "mimo-v2.5-asr";

    /** 非流式转录 */
    @PostMapping("/transcribe")
    public ResponseEntity<Map<String, Object>> transcribe(@RequestBody Map<String, String> body) {
        String audioDataUri = body.get("audio");
        if (!StringUtils.hasText(audioDataUri)) {
            return ResponseEntity.badRequest().body(Map.of("error", "audio 不能为空"));
        }

        AiConfig config = getMimoConfig();
        if (config == null) return miMoNotConfigured();

        try {
            String requestJson = buildAsrRequest(audioDataUri, config, false);
            HttpResponse<String> response = callMiMo(config, requestJson);

            if (response.statusCode() != 200) {
                return ResponseEntity.status(502)
                        .body(Map.of("error", "MiMo ASR API " + response.statusCode()));
            }

            JsonNode json = objectMapper.readTree(response.body());
            String content = json.at("/choices/0/message/content").asText().trim();

            if (!StringUtils.hasText(content)) {
                return ResponseEntity.ok(Map.of("success", false, "error", "未识别到语音内容"));
            }

            System.out.println("[ASR] 转录成功: " + content.substring(0, Math.min(50, content.length())));
            return ResponseEntity.ok(Map.of("success", true, "text", content));

        } catch (Exception e) {
            System.err.println("[ASR] 异常: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "转录失败: " + e.getMessage()));
        }
    }

    /** SSE 流式转录 — 逐字返回识别结果 */
    @PostMapping(value = "/transcribe/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter transcribeStream(@RequestBody Map<String, String> body) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 分钟超时
        String audioDataUri = body.get("audio");

        if (!StringUtils.hasText(audioDataUri)) {
            completeWithError(emitter, "audio 不能为空");
            return emitter;
        }

        AiConfig config = getMimoConfig();
        if (config == null) {
            completeWithError(emitter, "MiMo 模型未配置");
            return emitter;
        }

        // 异步执行，避免阻塞
        new Thread(() -> {
            try {
                String requestJson = buildAsrRequest(audioDataUri, config, true);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(config.getApiUrl().replaceAll("/+$", "") + "/chat/completions"))
                        .header("Content-Type", "application/json")
                        .header("api-key", config.getApiKey())
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                        .build();

                HttpResponse<java.io.InputStream> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    emitter.send(SseEmitter.event()
                            .data(objectMapper.writeValueAsString(
                                    Map.of("error", "MiMo ASR API " + response.statusCode()))));
                    emitter.complete();
                    return;
                }

                // 逐行读取 MiMo SSE 响应
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body(), StandardCharsets.UTF_8));
                String line;
                StringBuilder fullText = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;

                    try {
                        JsonNode chunk = objectMapper.readTree(data);
                        String content = chunk.at("/choices/0/delta/content").asText(null);
                        String finishReason = chunk.at("/choices/0/finish_reason").asText(null);

                        if (content != null && !content.isEmpty()) {
                            fullText.append(content);
                            // 每收到一个 token 就推给前端
                            emitter.send(SseEmitter.event()
                                    .data(objectMapper.writeValueAsString(Map.of(
                                            "partial", content,
                                            "text", fullText.toString()
                                    ))));
                        }

                        if ("stop".equals(finishReason)) {
                            break; // 转录完成
                        }
                    } catch (Exception parseErr) {
                        // 跳过无法解析的行
                    }
                }

                // 发送最终完成事件
                emitter.send(SseEmitter.event()
                        .data(objectMapper.writeValueAsString(Map.of(
                                "text", fullText.toString(),
                                "done", true
                        ))));
                System.out.println("[ASR] SSE 转录完成: " + fullText);

            } catch (Exception e) {
                System.err.println("[ASR] SSE 异常: " + e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .data(objectMapper.writeValueAsString(
                                    Map.of("error", "转录失败: " + e.getMessage()))));
                } catch (Exception ignored) {}
            } finally {
                emitter.complete();
            }
        }).start();

        return emitter;
    }

    // ── 工具方法 ──

    private AiConfig getMimoConfig() {
        AiConfig config = aiConfigService.getConfigByKey("mimo");
        if (config == null || !config.getEnabled() || !StringUtils.hasText(config.getApiKey())) {
            return null;
        }
        return config;
    }

    private ResponseEntity<Map<String, Object>> miMoNotConfigured() {
        return ResponseEntity.status(503).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", "MiMo 未配置，请在 ai_configs 表中启用 modelKey='mimo'"));
    }

    private String buildAsrRequest(String audioDataUri, AiConfig config, boolean stream) throws Exception {
        Map<String, Object> audioContent = Map.of(
                "type", "input_audio",
                "input_audio", Map.of("data", audioDataUri)
        );

        Map<String, Object> requestBody = Map.of(
                "model", ASR_MODEL,
                "messages", List.of(
                        Map.of("role", "user", "content", List.of(audioContent))
                ),
                "asr_options", Map.of("language", "auto"),
                "stream", stream
        );

        return objectMapper.writeValueAsString(requestBody);
    }

    private HttpResponse<String> callMiMo(AiConfig config, String requestJson) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getApiUrl().replaceAll("/+$", "") + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("api-key", config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void completeWithError(SseEmitter emitter, String error) {
        try {
            emitter.send(SseEmitter.event()
                    .data("{\"error\":\"" + error + "\"}"));
        } catch (Exception ignored) {}
        emitter.complete();
    }
}
