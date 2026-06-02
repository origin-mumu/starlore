package com.robin.blogback.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai")
public class AiThinkingController {

    private static final Logger log = LoggerFactory.getLogger(AiThinkingController.class);

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:deepseek-v4-flash}")
    private String modelName;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @PostMapping(value = "/thinking-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamThinking(
            @RequestParam(defaultValue = "deepseek-v4-flash") String model,
            @RequestBody String messages) {
        SseEmitter emitter = new SseEmitter(300_000L);

        CompletableFuture.runAsync(() -> {
            try {
                List<Map<String, String>> parsedMessages = objectMapper.readValue(
                        messages, new TypeReference<>() {});

                String actualModel = model != null ? model : modelName;

                Map<String, Object> body = new java.util.LinkedHashMap<>();
                body.put("model", actualModel);
                body.put("messages", parsedMessages);
                body.put("stream", true);
                body.put("max_tokens", 65536);

                String apiUrl = baseUrl + "/v1/chat/completions";
                String bodyJson = objectMapper.writeValueAsString(body);
                log.info("Thinking request: POST {} model={} messages={}",
                        apiUrl, actualModel, parsedMessages.size());
                log.debug("Thinking body: {}", bodyJson);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                        .build();

                HttpResponse<InputStream> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                    log.error("Thinking API error: status={} body={}", response.statusCode(), errBody);
                    emitter.send(Map.of("error", "API " + response.statusCode() + ": " + errBody));
                    emitter.complete();
                    return;
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.equals("data: [DONE]")) continue;
                        if (!trimmed.startsWith("data: ")) continue;

                        String json = trimmed.substring(6);
                        try {
                            JsonNode node = objectMapper.readTree(json);
                            JsonNode delta = node.at("/choices/0/delta");
                            if (delta == null || delta.isMissingNode()) continue;

                            String reasoning = delta.has("reasoning_content")
                                    ? delta.get("reasoning_content").asText("") : "";
                            String content = delta.has("content")
                                    ? delta.get("content").asText("") : "";

                            if (!reasoning.isEmpty()) {
                                emitter.send(Map.of("reasoning_content", reasoning));
                            }
                            if (!content.isEmpty()) {
                                emitter.send(Map.of("content", content));
                            }
                        } catch (Exception e) {
                            // skip unparseable lines
                        }
                    }
                    emitter.send(Map.of("done", "true"));
                    emitter.complete();
                }
            } catch (Exception e) {
                log.error("Thinking SSE error: {}", e.getMessage(), e);
                try {
                    emitter.send(Map.of("error", e.getMessage() != null ? e.getMessage() : "未知错误"));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> emitter.complete());
        return emitter;
    }
}
