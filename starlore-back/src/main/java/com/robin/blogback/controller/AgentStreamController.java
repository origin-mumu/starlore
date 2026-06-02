package com.robin.blogback.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robin.blogback.config.UserContext;
import com.robin.blogback.service.AiQuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.robin.blogback.service.BlogTools;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/ai")
public class AgentStreamController {

    private static final Logger log = LoggerFactory.getLogger(AgentStreamController.class);

    private static final String AGENT_SYSTEM_PROMPT =
            "你是 Starlore 博客助手，也是用户的专属 AI 伙伴。" +
            "优先查询博客数据来回答问题。如果博客中找不到相关内容，" +
            "可以用你自己的知识来回答，不要拒绝用户。" +
            "引用文章时请注明标题。回答简洁友好，可少量用「喵」。";

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private BlogTools blogTools;

    @Autowired
    private AiQuotaService aiQuotaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 单线程执行器，确保 UserContext (ThreadLocal) 在整个 Reactor 管道中可用
    private final ExecutorService agentExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "agent-stream");
        t.setDaemon(true);
        return t;
    });

    @PostMapping(value = "/agent-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAgent(HttpServletRequest request,
            @RequestParam(required = false) String model,
            @RequestBody String messages) {
        Integer userId = (Integer) request.getAttribute("userId");
        log.info("Agent SSE request - userId from attribute: {}, auth header: {}",
                userId, request.getHeader("Authorization") != null ? "present" : "missing");

        // 检查 AI 配额
        int remaining = aiQuotaService.getRemaining(userId);
        if (remaining == 0) {
            SseEmitter blocked = new SseEmitter(0L);
            try {
                blocked.send(Map.of("error", "今日 AI 对话次数已用尽，明天再来吧～"));
            } catch (IOException ignored) {}
            blocked.complete();
            return blocked;
        }

        SseEmitter emitter = new SseEmitter(300_000L);

        agentExecutor.execute(() -> {
            UserContext.setUserId(userId);
            UserContext.setCrossThreadUser("agent-stream", userId);
            // 消耗一次配额
            aiQuotaService.tryConsume(userId);
            try {
                List<Map<String, String>> rawMessages = objectMapper.readValue(
                        messages, new TypeReference<>() {});

                // 构建 Spring AI 消息列表
                List<Message> springMessages = new ArrayList<>();
                springMessages.add(new SystemMessage(AGENT_SYSTEM_PROMPT));

                for (Map<String, String> msg : rawMessages) {
                    String role = msg.get("role");
                    String content = msg.get("content");
                    if (content == null || content.isEmpty()) continue;

                    switch (role) {
                        case "system" -> springMessages.add(new SystemMessage(content));
                        case "user" -> springMessages.add(new UserMessage(content));
                        case "assistant" -> springMessages.add(new AssistantMessage(content));
                    }
                }

                log.info("Agent SSE called, messages count: {}", springMessages.size());

                // 使用 ChatClient 流式调用，传递工具
                chatClient.prompt()
                        .messages(springMessages)
                        .tools(blogTools)
                        .stream()
                        .chatResponse()
                        .contextWrite(ctx -> ctx.put("userId", userId))  // 在 Reactor Context 中传递 userId
                        .publishOn(Schedulers.immediate())
                        .doOnNext(response -> {
                            try {
                                if (response.getResult() != null && response.getResult().getOutput() != null) {
                                    String text = response.getResult().getOutput().getText();
                                    if (text != null && !text.isEmpty()) {
                                        emitter.send(Map.of("content", text));
                                    }
                                }
                            } catch (IOException e) {
                                // 客户端已断开
                            }
                        })
                        .doOnError(error -> {
                            String msg = error.getMessage();
                            if (msg == null || msg.isEmpty()) msg = error.getClass().getSimpleName();
                            if (msg.contains("Connection reset")) {
                                msg = "DeepSeek API 连接被重置，可能是请求内容过长或网络不稳定，请稍后重试";
                            } else if (msg.contains("timeout") || msg.contains("Timeout")) {
                                msg = "DeepSeek API 请求超时，请稍后重试";
                            } else if (msg.contains("429")) {
                                msg = "DeepSeek API 请求频率过高，请稍后重试";
                            }
                            log.error("Agent stream error: {}", msg, error);
                            try {
                                emitter.send(Map.of("error", msg));
                            } catch (IOException e) {
                                // 忽略
                            }
                            UserContext.clear();
                            UserContext.clearCrossThread("agent-stream");
                        })
                        .doOnComplete(() -> {
                            try {
                                emitter.send(Map.of("done", "true"));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.complete();
                            }
                            UserContext.clear();
                            UserContext.clearCrossThread("agent-stream");
                        })
                        .subscribe();

            } catch (Exception e) {
                log.error("Agent error: {}", e.getMessage(), e);
                try {
                    emitter.send(Map.of("error", "Agent 错误: " + e.getMessage()));
                } catch (IOException ex) {
                    // 忽略
                }
                emitter.completeWithError(e);
                UserContext.clear();
                UserContext.clearCrossThread("agent-stream");
            }
        });

        emitter.onTimeout(emitter::complete);
        return emitter;
    }
}
