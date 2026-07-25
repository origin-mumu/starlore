package com.robin.blogback.controller;

import com.robin.blogback.dto.*;
import com.robin.blogback.service.AiQuotaService;
import com.robin.blogback.service.AiService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.Data;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiSessionController {

    @Autowired
    private AiService aiService;

    @Autowired
    private AiQuotaService aiQuotaService;

    @GetMapping("/models")
    public ResponseEntity<?> getModels() {
        return ResponseEntity.ok(aiService.getModels());
    }

    @GetMapping("/character-cards")
    public ResponseEntity<?> getCharacterCards() {
        return ResponseEntity.ok(aiService.getCharacterCards());
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> listSessions(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return ResponseEntity.ok(aiService.listSessions(userId));
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(HttpServletRequest request,
            @RequestBody(required = false) CreateSessionRequest sessionRequest) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (sessionRequest == null) sessionRequest = new CreateSessionRequest();
        return ResponseEntity.ok(aiService.createSession(userId, sessionRequest));
    }

    @PatchMapping("/sessions/{id}")
    public ResponseEntity<?> updateSession(HttpServletRequest request,
            @PathVariable Integer id, @RequestBody UpdateSessionRequest sessionRequest) {
        Integer userId = (Integer) request.getAttribute("userId");
        try {
            return ResponseEntity.ok(aiService.updateSession(userId, id, sessionRequest));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<?> deleteSession(HttpServletRequest request, @PathVariable Integer id) {
        Integer userId = (Integer) request.getAttribute("userId");
        try {
            return ResponseEntity.ok(aiService.deleteSession(userId, id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<?> getMessages(HttpServletRequest request, @PathVariable Integer id) {
        Integer userId = (Integer) request.getAttribute("userId");
        try {
            return ResponseEntity.ok(aiService.getMessages(userId, id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/sessions/{id}/append")
    public ResponseEntity<?> appendPair(HttpServletRequest request,
            @PathVariable Integer id, @RequestBody AppendPairRequest appendRequest) {
        Integer userId = (Integer) request.getAttribute("userId");
        try {
            return ResponseEntity.ok(aiService.appendPair(userId, id, appendRequest));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** 获取当前用户 AI 配额信息 */
    @GetMapping("/quota")
    public ResponseEntity<?> getQuota(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        AiQuotaService.QuotaInfo quota = aiQuotaService.getQuotaInfo(userId);
        return ResponseEntity.ok(Map.of("data", quota));
    }

    @Autowired
    private com.robin.blogback.mapper.AgentConfigMapper agentConfigMapper;

    @Autowired
    private com.robin.blogback.mapper.AiMessageFeedbackMapper aiMessageFeedbackMapper;

    @GetMapping("/agent-config")
    public Result<com.robin.blogback.entity.AgentConfig> getAgentConfig(HttpServletRequest request) {
        Integer userIdInt = (Integer) request.getAttribute("userId");
        Long userId = userIdInt != null ? userIdInt.longValue() : 1L;

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.robin.blogback.entity.AgentConfig> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.robin.blogback.entity.AgentConfig>()
                        .eq(com.robin.blogback.entity.AgentConfig::getUserId, userId);
        com.robin.blogback.entity.AgentConfig config = agentConfigMapper.selectOne(wrapper);
        if (config == null) {
            config = new com.robin.blogback.entity.AgentConfig();
            config.setUserId(userId);
            config.setModelName("deepseek-chat");
            config.setSimilarityThreshold(0.6);
            config.setTopK(5);
            config.setTemperature(0.7);
            config.setEnableRerank(1);
            agentConfigMapper.insert(config);
        }
        return Result.ok("获取Agent配置成功", config);
    }

    @Data
    public static class AgentConfigRequestDTO {
        private String modelName;
        private Double similarityThreshold;
        private Integer topK;
        private Double temperature;
        private Integer enableRerank;
    }

    @PutMapping("/agent-config")
    public Result<String> updateAgentConfig(HttpServletRequest request, @RequestBody AgentConfigRequestDTO req) {
        Integer userIdInt = (Integer) request.getAttribute("userId");
        Long userId = userIdInt != null ? userIdInt.longValue() : 1L;

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.robin.blogback.entity.AgentConfig> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.robin.blogback.entity.AgentConfig>()
                        .eq(com.robin.blogback.entity.AgentConfig::getUserId, userId);
        com.robin.blogback.entity.AgentConfig config = agentConfigMapper.selectOne(wrapper);
        if (config == null) {
            config = new com.robin.blogback.entity.AgentConfig();
            config.setUserId(userId);
            agentConfigMapper.insert(config);
        }
        if (req.getModelName() != null) config.setModelName(req.getModelName());
        if (req.getSimilarityThreshold() != null) config.setSimilarityThreshold(req.getSimilarityThreshold());
        if (req.getTopK() != null) config.setTopK(req.getTopK());
        if (req.getTemperature() != null) config.setTemperature(req.getTemperature());
        if (req.getEnableRerank() != null) config.setEnableRerank(req.getEnableRerank());
        agentConfigMapper.updateById(config);

        return Result.ok("Agent配置修改成功");
    }

    @Data
    public static class FeedbackRequestDTO {
        private Long sessionId;
        private String rating;
        private String feedbackType;
        private String comment;
    }

    @PostMapping("/messages/{messageId}/feedback")
    public Result<String> submitFeedback(HttpServletRequest request, @PathVariable Long messageId, @RequestBody FeedbackRequestDTO req) {
        Integer userIdInt = (Integer) request.getAttribute("userId");
        Long userId = userIdInt != null ? userIdInt.longValue() : 1L;

        com.robin.blogback.entity.AiMessageFeedback fb = new com.robin.blogback.entity.AiMessageFeedback();
        fb.setMessageId(messageId);
        fb.setSessionId(req.getSessionId() != null ? req.getSessionId() : 0L);
        fb.setUserId(userId);
        fb.setRating(req.getRating());
        fb.setFeedbackType(req.getFeedbackType());
        fb.setComment(req.getComment());
        aiMessageFeedbackMapper.insert(fb);

        return Result.ok("反馈提交成功，感谢您的评价！");
    }
}
