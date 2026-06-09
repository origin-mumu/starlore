package com.robin.blogback.config;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在工具调用期间向 SSE 客户端发送 tool_start 事件
 */
public class SseContextHolder {

    private static final ThreadLocal<SseEmitter> emitterLocal = new ThreadLocal<>();
    private static final ConcurrentHashMap<String, SseEmitter> crossThreadEmitters = new ConcurrentHashMap<>();

    public static void setEmitter(String key, SseEmitter emitter) {
        emitterLocal.set(emitter);
        crossThreadEmitters.put(key, emitter);
    }

    public static SseEmitter getEmitter() {
        SseEmitter emitter = emitterLocal.get();
        if (emitter == null) {
            emitter = crossThreadEmitters.get("agent-stream");
        }
        return emitter;
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

    public static void clear(String key) {
        emitterLocal.remove();
        crossThreadEmitters.remove(key);
    }
}
