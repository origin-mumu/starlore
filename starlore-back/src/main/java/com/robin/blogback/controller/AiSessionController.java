package com.robin.blogback.controller;

import com.robin.blogback.dto.*;
import com.robin.blogback.service.AiQuotaService;
import com.robin.blogback.service.AiService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
