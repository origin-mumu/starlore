package com.robin.blogback.controller;

import com.robin.blogback.entity.Resume;
import com.robin.blogback.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    private Integer getUserId(HttpServletRequest request) {
        return (Integer) request.getAttribute("userId");
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        List<Resume> list = resumeService.listByUser(userId);
        return ResponseEntity.ok(Map.of("data", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Integer id, HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        Resume resume = resumeService.getById(id, userId);
        if (resume == null) return ResponseEntity.status(404).body(Map.of("message", "简历不存在"));
        return ResponseEntity.ok(Map.of("data", resume));
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody Resume resume, HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        Resume created = resumeService.create(resume, userId);
        return ResponseEntity.ok(Map.of("data", created, "message", "创建成功"));
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody Resume resume, HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        try {
            Resume updated = resumeService.update(resume, userId);
            return ResponseEntity.ok(Map.of("data", updated, "message", "更新成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id, HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        try {
            resumeService.delete(id, userId);
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }
}
