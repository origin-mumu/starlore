package com.robin.blogback.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robin.blogback.entity.AiConfig;
import com.robin.blogback.service.AiConfigService;
import com.robin.blogback.service.AiStreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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

@Service
public class AiStreamServiceImpl implements AiStreamService {

    @Autowired
    private AiConfigService aiConfigService;

    @Value("${spring.ai.openai.api-key:}")
    private String localApiKey;

    @Value("${spring.ai.openai.base-url:https://api.deepseek.com/v1}")
    private String localBaseUrl;

    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String localModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private record ModelConfig(String apiUrl, String modelName, String apiKey) {}

    private ModelConfig resolveModelConfig(String model) {
        // 优先从数据库读取配置
        AiConfig dbConfig = aiConfigService.getConfigByKey(model);
        if (dbConfig != null && dbConfig.getEnabled() && StringUtils.hasText(dbConfig.getApiKey())) {
            return new ModelConfig(dbConfig.getApiUrl(), dbConfig.getModelId(), dbConfig.getApiKey());
        }

        // 回退到 application.yaml 本地配置
        if (StringUtils.hasText(localApiKey)) {
            String apiUrl = localBaseUrl.endsWith("/chat/completions") ? localBaseUrl
                    : localBaseUrl.replaceAll("/+$", "") + "/chat/completions";
            return new ModelConfig(apiUrl, model, localApiKey);
        }

        return new ModelConfig(null, model, null);
    }

    @Override
    public SseEmitter streamChat(String model, List<Map<String, String>> messages) {
        SseEmitter emitter = new SseEmitter(120_000L);

        CompletableFuture.runAsync(() -> {
            try {
                ModelConfig config = resolveModelConfig(model);
                if (config.apiKey() == null) {
                    emitter.send(SseEmitter.event()
                            .data(objectMapper.writeValueAsString(Map.of("error", "未配置 " + model + " 对应的 API 密钥"))));
                    emitter.complete();
                    return;
                }

                Map<String, Object> body = Map.of(
                        "model", config.modelName(),
                        "messages", messages,
                        "stream", true
                );

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(config.apiUrl()))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + config.apiKey())
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                        .build();

                HttpResponse<InputStream> response = client.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                    emitter.send(SseEmitter.event()
                            .data(objectMapper.writeValueAsString(Map.of("error", "API " + response.statusCode() + ": " + errBody))));
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
                            String chunk = node.at("/choices/0/delta/content").asText("");
                            if (!chunk.isEmpty()) {
                                emitter.send(SseEmitter.event()
                                        .data(objectMapper.writeValueAsString(Map.of("content", chunk))));
                            }
                        } catch (Exception e) {
                            // Skip unparseable lines
                        }
                    }
                    emitter.send(SseEmitter.event()
                            .data(objectMapper.writeValueAsString(Map.of("done", true))));
                    emitter.complete();
                }
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .data(objectMapper.writeValueAsString(Map.of("error", e.getMessage() != null ? e.getMessage() : "未知错误"))));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> emitter.complete());
        return emitter;
    }
}
