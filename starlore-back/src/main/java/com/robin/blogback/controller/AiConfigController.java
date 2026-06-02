package com.robin.blogback.controller;

import com.robin.blogback.dto.AiConfigRequest;
import com.robin.blogback.entity.AiConfig;
import com.robin.blogback.service.AiConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-config")
public class AiConfigController {

    @Autowired
    private AiConfigService aiConfigService;

    @GetMapping
    public ResponseEntity<?> getAllConfigs() {
        List<AiConfig> configs = aiConfigService.getAllConfigs();
        return ResponseEntity.ok(Map.of("data", configs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getConfigById(@PathVariable Integer id) {
        try {
            AiConfig config = aiConfigService.getConfigById(id);
            return ResponseEntity.ok(Map.of("data", config));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createConfig(@RequestBody AiConfigRequest request) {
        try {
            AiConfig config = aiConfigService.createConfig(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "配置创建成功", "data", config));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateConfig(@PathVariable Integer id, @RequestBody AiConfigRequest request) {
        try {
            AiConfig config = aiConfigService.updateConfig(id, request);
            return ResponseEntity.ok(Map.of("message", "配置更新成功", "data", config));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConfig(@PathVariable Integer id) {
        try {
            aiConfigService.deleteConfig(id);
            return ResponseEntity.ok(Map.of("message", "配置删除成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }
}
