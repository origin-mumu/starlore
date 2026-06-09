package com.robin.blogback.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * 智谱 Embedding 模型（直接调用 Zhipu API，绕过 Spring AI 的 URL 拼接问题）
 */
public class ZhipuEmbeddingModel implements EmbeddingModel {

    private final String model;
    private final String apiKey;
    private final String embeddingsUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ZhipuEmbeddingModel(String baseUrl, String apiKey, String model) {
        this.model = model;
        this.apiKey = apiKey;
        this.embeddingsUrl = baseUrl + "/embeddings";
    }

    @Override
    public float[] embed(Document document) {
        return doEmbed(document.getText());
    }

    @Override
    public float[] embed(String text) {
        return doEmbed(text);
    }

    @Override
    @SuppressWarnings("unchecked")
    public EmbeddingResponse call(EmbeddingRequest request) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("input", request.getInstructions());

            String json = objectMapper.writeValueAsString(body);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(embeddingsUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> response = objectMapper.readValue(httpResponse.body(), Map.class);

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            List<Embedding> results = new ArrayList<>();

            for (Map<String, Object> item : data) {
                List<Double> embedding = (List<Double>) item.get("embedding");
                float[] floatArray = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    floatArray[i] = embedding.get(i).floatValue();
                }
                int index = item.get("index") != null ? ((Number) item.get("index")).intValue() : 0;
                results.add(new Embedding(floatArray, index));
            }

            return new EmbeddingResponse(results);
        } catch (Exception e) {
            throw new RuntimeException("智谱 Embedding API 调用失败: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private float[] doEmbed(String text) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("input", List.of(text));

            String json = objectMapper.writeValueAsString(body);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(embeddingsUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> response = objectMapper.readValue(httpResponse.body(), Map.class);

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            List<Double> embedding = (List<Double>) data.get(0).get("embedding");
            float[] result = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                result[i] = embedding.get(i).floatValue();
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("智谱 Embedding API 调用失败: " + e.getMessage(), e);
        }
    }
}
