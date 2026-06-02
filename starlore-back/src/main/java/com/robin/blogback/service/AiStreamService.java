package com.robin.blogback.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface AiStreamService {
    SseEmitter streamChat(String model, List<Map<String, String>> messages);
}
