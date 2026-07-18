package com.robin.blogback.config;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * 在工具调用期间向 SSE 客户端发送 tool_start 事件
 */
public class SseContextHolder {

    private static final ThreadLocal<SseEmitter> emitterLocal = new ThreadLocal<>();
    public static void setEmitter(String key, SseEmitter emitter) {
        emitterLocal.set(emitter);
    }

    public static void setEmitter(SseEmitter emitter) {
        emitterLocal.set(emitter);
    }

    public static SseEmitter getEmitter() {
        return emitterLocal.get();
    }

    public static void sendToolStart(String toolName) {
        SseEmitter emitter = getEmitter();
        if (emitter != null) {
            try {
                emitter.send(Map.of("tool_start", toolName));
            } catch (IOException ignored) {
            }
        }
    }

    public static void sendEvent(String type, Map<String, Object> data) {
        SseEmitter emitter = getEmitter();
        if (emitter != null) {
            try {
                Map<String, Object> event = new java.util.LinkedHashMap<>();
                event.put("type", type);
                event.putAll(data);
                emitter.send(event);
            } catch (IOException ignored) {
            }
        }
    }

    public static void clear(String key) {
        emitterLocal.remove();
    }
}
