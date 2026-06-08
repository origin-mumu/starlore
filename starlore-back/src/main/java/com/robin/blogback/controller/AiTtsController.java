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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * AI TTS 语音合成控制器
 * 使用 MiMo TTS 模型（mimo-v2.5-tts）将文字转换为语音
 *
 * API 格式参考：
 * messages: [user(语气指令), assistant(朗读内容)]
 * audio: { format: "wav", voice: "mimo_default" }
 * 响应音频: choices[0].message.audio.data (base64 WAV)
 */
@RestController
@RequestMapping("/api/ai")
public class AiTtsController {

    @Autowired
    private AiConfigService aiConfigService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /** MiMo TTS 的默认语气指令 */
    private static final String TTS_TONE_INSTRUCTION =
            "Warm, natural, conversational tone in Chinese Mandarin — like chatting with a close friend. "
                    + "Moderate pace, natural pauses, gentle and friendly voice. Slightly expressive but not exaggerated.";

    /**
     * 文字转语音
     * POST /api/ai/tts
     * Body: {"text": "要朗读的文字"}
     * Response: audio/wav 二进制流
     */
    @PostMapping("/tts")
    public ResponseEntity<StreamingResponseBody> textToSpeech(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (!StringUtils.hasText(text)) {
            return ResponseEntity.badRequest().build();
        }

        // 限制文本长度
        if (text.length() > 500) {
            text = text.substring(0, 500);
        }

        // 从数据库获取 MiMo TTS 配置
        AiConfig ttsConfig = aiConfigService.getConfigByKey("mimo-tts");
        if (ttsConfig == null || !ttsConfig.getEnabled() || !StringUtils.hasText(ttsConfig.getApiKey())) {
            return ResponseEntity.status(503)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(out -> out.write("{\"error\":\"未配置 mimo-tts 模型\"}".getBytes(StandardCharsets.UTF_8)));
        }

        String finalText = text;
        AiConfig config = ttsConfig;

        StreamingResponseBody streamBody = outputStream -> {
            try {
                String apiUrl = config.getApiUrl();
                if (!apiUrl.endsWith("/chat/completions")) {
                    apiUrl = apiUrl.replaceAll("/+$", "") + "/chat/completions";
                }

                // MiMo TTS 请求格式: user 放语气指令，assistant 放朗读文本
                Map<String, Object> requestBody = Map.of(
                        "model", config.getModelId(),
                        "messages", List.of(
                                Map.of("role", "user", "content", TTS_TONE_INSTRUCTION),
                                Map.of("role", "assistant", "content", finalText)
                        ),
                        "audio", Map.of(
                                "format", "wav",
                                "voice", "mimo_default"
                        )
                );

                String requestJson = objectMapper.writeValueAsString(requestBody);
                System.out.println("[TTS] 请求: " + finalText.substring(0, Math.min(80, finalText.length())) + "...");

                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Content-Type", "application/json")
                        .header("api-key", config.getApiKey())
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson));

                HttpResponse<String> response = httpClient.send(
                        builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (response.statusCode() != 200) {
                    System.err.println("[TTS] API " + response.statusCode() + ": " + response.body());
                    outputStream.write(("{\"error\":\"TTS API " + response.statusCode() + "\"}")
                            .getBytes(StandardCharsets.UTF_8));
                    return;
                }

                String responseBody = response.body();
                JsonNode json = objectMapper.readTree(responseBody);

                // 音频在 choices[0].message.audio.data (base64)
                JsonNode audioData = json.at("/choices/0/message/audio/data");
                if (audioData.isMissingNode() || !audioData.isTextual()) {
                    // 兜底：尝试 /audio/data 路径
                    audioData = json.at("/audio/data");
                }

                if (!audioData.isMissingNode() && audioData.isTextual()) {
                    byte[] wavBytes = Base64.getDecoder().decode(audioData.asText());
                    outputStream.write(wavBytes);
                    System.out.println("[TTS] 成功，WAV 音频 " + wavBytes.length + " bytes");
                } else {
                    System.err.println("[TTS] 未找到音频数据，响应: "
                            + responseBody.substring(0, Math.min(300, responseBody.length())));
                    outputStream.write(objectMapper.writeValueAsBytes(Map.of(
                            "error", "响应中未找到音频数据"
                    )));
                }

            } catch (Exception e) {
                System.err.println("[TTS] 异常: " + e.getMessage());
                e.printStackTrace();
                try {
                    outputStream.write(objectMapper.writeValueAsBytes(Map.of(
                            "error", "TTS 失败: " + e.getMessage()
                    )));
                } catch (Exception ignored) {}
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("audio/wav"))
                .body(streamBody);
    }
}
