package com.robin.blogback.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robin.blogback.dto.*;
import com.robin.blogback.entity.AiConfig;
import com.robin.blogback.entity.AiMessage;
import com.robin.blogback.entity.AiSession;
import com.robin.blogback.mapper.AiMessageMapper;
import com.robin.blogback.mapper.AiSessionMapper;
import com.robin.blogback.service.AiConfigService;
import com.robin.blogback.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceImpl implements AiService {

    @Autowired
    private AiSessionMapper sessionMapper;
    @Autowired
    private AiMessageMapper messageMapper;
    @Autowired
    private AiConfigService aiConfigService;

    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String defaultModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AiModelsResponse getModels() {
        List<AiModelsResponse.ModelInfo> models = new ArrayList<>();
        List<AiConfig> configs = aiConfigService.getAllConfigs();

        if (!configs.isEmpty()) {
            // 从数据库读取模型列表
            for (AiConfig config : configs) {
                boolean configured = config.getEnabled()
                        && StringUtils.hasText(config.getApiKey());
                models.add(new AiModelsResponse.ModelInfo(
                        config.getModelKey(), config.getModelName(), configured));
            }
        } else {
            // 回退到环境变量检测
            boolean hasQwen = StringUtils.hasText(System.getenv("AI_KEY_QWEN"))
                    || StringUtils.hasText(System.getenv("VITE_API_KEY"));
            boolean hasMimo = StringUtils.hasText(System.getenv("AI_KEY_MIMO"))
                    || StringUtils.hasText(System.getenv("VITE_MIMO_API_KEY"));
            models.add(new AiModelsResponse.ModelInfo("qwen-plus", "通义千问 Plus", hasQwen));
            models.add(new AiModelsResponse.ModelInfo("mimo", "小米 MiMo", hasMimo));
        }
        return new AiModelsResponse(true, models);
    }

    @Override
    public AiCharacterCardsResponse getCharacterCards() {
        try {
            ClassPathResource resource = new ClassPathResource("aiCharacterCards.json");
            byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            String json = new String(bytes, StandardCharsets.UTF_8);
            List<AiCharacterCardsResponse.CharacterCard> cards = objectMapper.readValue(
                    json, new TypeReference<>() {});
            return new AiCharacterCardsResponse(true, cards);
        } catch (IOException e) {
            return new AiCharacterCardsResponse(true, new ArrayList<>());
        }
    }

    @Override
    public AiSessionListResponse listSessions(Integer userId) {
        List<AiSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<AiSession>()
                        .eq(AiSession::getUserId, userId)
                        .orderByDesc(AiSession::getUpdatedAt)
                        .last("LIMIT 80"));
        List<AiSessionListResponse.SessionItem> items = sessions.stream()
                .map(this::toSessionItem)
                .toList();
        return new AiSessionListResponse(true, items);
    }

    @Override
    public AiSessionResponse createSession(Integer userId, CreateSessionRequest request) {
        AiSession session = new AiSession();
        session.setUserId(userId);
        session.setTitle(request.getTitle() != null ? request.getTitle() : "新会话");
        session.setCharacterKey(request.getCharacterKey() != null ? request.getCharacterKey() : "default");
        session.setModelId(request.getModelId() != null ? request.getModelId() : defaultModel);
        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionMapper.insert(session);
        return new AiSessionResponse(true, toSessionData(session));
    }

    @Override
    public AiSessionResponse updateSession(Integer userId, Integer id, UpdateSessionRequest request) {
        AiSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<AiSession>()
                        .eq(AiSession::getId, id)
                        .eq(AiSession::getUserId, userId));
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (request.getTitle() != null) session.setTitle(request.getTitle().substring(0, Math.min(request.getTitle().length(), 255)));
        if (request.getCharacterKey() != null) session.setCharacterKey(request.getCharacterKey().substring(0, Math.min(request.getCharacterKey().length(), 64)));
        if (request.getModelId() != null) session.setModelId(request.getModelId().substring(0, Math.min(request.getModelId().length(), 32)));
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
        return new AiSessionResponse(true, toSessionData(session));
    }

    @Override
    public Map<String, Object> deleteSession(Integer userId, Integer id) {
        AiSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<AiSession>()
                        .eq(AiSession::getId, id)
                        .eq(AiSession::getUserId, userId));
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        // Cascade delete messages
        messageMapper.delete(new LambdaQueryWrapper<AiMessage>().eq(AiMessage::getSessionId, id));
        sessionMapper.deleteById(id);
        return Map.of("success", true);
    }

    @Override
    public AiMessageListResponse getMessages(Integer userId, Integer id) {
        AiSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<AiSession>()
                        .eq(AiSession::getId, id)
                        .eq(AiSession::getUserId, userId));
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        List<AiMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getSessionId, id)
                        .orderByAsc(AiMessage::getId));

        AiMessageListResponse.SessionInfo sessionInfo = new AiMessageListResponse.SessionInfo(
                session.getId(), session.getTitle(), session.getCharacterKey(), session.getModelId());
        List<AiMessageListResponse.MessageItem> items = messages.stream()
                .map(m -> new AiMessageListResponse.MessageItem(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();
        return new AiMessageListResponse(true, sessionInfo, items);
    }

    @Override
    public Map<String, Object> appendPair(Integer userId, Integer id, AppendPairRequest request) {
        if (!StringUtils.hasText(request.getUserContent()) || !StringUtils.hasText(request.getAssistantContent())) {
            throw new IllegalArgumentException("缺少 userContent 或 assistantContent");
        }
        AiSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<AiSession>()
                        .eq(AiSession::getId, id)
                        .eq(AiSession::getUserId, userId));
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }

        AiMessage userMsg = new AiMessage();
        userMsg.setSessionId(id);
        userMsg.setRole("user");
        userMsg.setContent(request.getUserContent());
        userMsg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(userMsg);

        AiMessage assistantMsg = new AiMessage();
        assistantMsg.setSessionId(id);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(request.getAssistantContent());
        assistantMsg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(assistantMsg);

        // Auto-rename session title
        if ("新会话".equals(session.getTitle()) && StringUtils.hasText(request.getUserContent())) {
            String content = request.getUserContent().trim();
            session.setTitle(content.length() > 28 ? content.substring(0, 28) + "…" : content);
        }
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        return Map.of("success", true);
    }

    private AiSessionListResponse.SessionItem toSessionItem(AiSession s) {
        return new AiSessionListResponse.SessionItem(
                s.getId(), s.getTitle(), s.getCharacterKey(), s.getModelId(),
                s.getCreatedAt(), s.getUpdatedAt());
    }

    private AiSessionResponse.SessionData toSessionData(AiSession s) {
        return new AiSessionResponse.SessionData(
                s.getId(), s.getTitle(), s.getCharacterKey(), s.getModelId(),
                s.getCreatedAt(), s.getUpdatedAt());
    }
}
