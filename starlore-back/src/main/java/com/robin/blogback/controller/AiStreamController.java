package com.robin.blogback.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robin.blogback.service.AiStreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiStreamController {

    @Autowired
    private AiStreamService aiStreamService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam String model,
            @RequestParam String messages) {
        try {
            List<Map<String, String>> parsedMessages = objectMapper.readValue(
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
}
