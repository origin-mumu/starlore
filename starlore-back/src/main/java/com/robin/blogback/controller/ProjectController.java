package com.robin.blogback.controller;

import com.robin.blogback.dto.ProjectInfo;
import com.robin.blogback.dto.ProjectRequest;
import com.robin.blogback.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @GetMapping
    public ResponseEntity<?> listProjects(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }
        List<ProjectInfo> list = projectService.listProjects(userId);
        return ResponseEntity.ok(Map.of("data", list));
    }

    @PostMapping
    public ResponseEntity<?> createProject(HttpServletRequest request, @RequestBody ProjectRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "项目名称不能为空"));
        }
        try {
            ProjectInfo project = projectService.createProject(request, req);
            return ResponseEntity.ok(Map.of("data", project, "message", "创建成功"));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(HttpServletRequest request, @PathVariable Integer id, @RequestBody ProjectRequest req) {
        try {
            ProjectInfo project = projectService.updateProject(request, id, req);
            return ResponseEntity.ok(Map.of("data", project, "message", "更新成功"));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(HttpServletRequest request, @PathVariable Integer id) {
        try {
            projectService.deleteProject(request, id);
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
