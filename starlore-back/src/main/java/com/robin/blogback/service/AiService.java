package com.robin.blogback.service;

import com.robin.blogback.dto.*;

import java.util.Map;

public interface AiService {
    AiModelsResponse getModels();
    AiCharacterCardsResponse getCharacterCards();
    AiSessionListResponse listSessions(Integer userId);
    AiSessionResponse createSession(Integer userId, CreateSessionRequest request);
    AiSessionResponse updateSession(Integer userId, Integer id, UpdateSessionRequest request);
    Map<String, Object> deleteSession(Integer userId, Integer id);
    AiMessageListResponse getMessages(Integer userId, Integer id);
    Map<String, Object> appendPair(Integer userId, Integer id, AppendPairRequest request);
}
