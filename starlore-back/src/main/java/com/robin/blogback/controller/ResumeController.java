package com.robin.blogback.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robin.blogback.entity.Resume;
import com.robin.blogback.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    @Value("${pdf.service.url:http://localhost:3001}")
    private String pdfServiceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResumeController() {
        // Jackson for JSON request body, ByteArray for PDF response
        List<HttpMessageConverter<?>> converters = List.of(
            new MappingJackson2HttpMessageConverter(),
            new ByteArrayHttpMessageConverter()
        );
        this.restTemplate = new RestTemplate(converters);
    }

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

    @GetMapping("/{id}/export-pdf")
    public ResponseEntity<?> exportPdf(@PathVariable Integer id, HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "未登录"));

        Resume resume = resumeService.getById(id, userId);
        if (resume == null) return ResponseEntity.status(404).body(Map.of("message", "简历不存在"));

        try {
            // Parse content JSON string to object
            Object contentObj = resume.getContent() != null
                ? objectMapper.readValue(resume.getContent(), new TypeReference<Map<String, Object>>() {})
                : new HashMap<>();

            // Build request body for PDF service
            Map<String, Object> data = new HashMap<>();
            data.put("title", resume.getTitle() != null ? resume.getTitle() : "");
            data.put("name", resume.getName() != null ? resume.getName() : "");
            data.put("jobTitle", resume.getJobTitle() != null ? resume.getJobTitle() : "");
            data.put("phone", resume.getPhone() != null ? resume.getPhone() : "");
            data.put("email", resume.getEmail() != null ? resume.getEmail() : "");
            data.put("photoUrl", resume.getPhotoUrl() != null ? resume.getPhotoUrl() : "");
            data.put("content", contentObj);

            Map<String, Object> body = new HashMap<>();
            body.put("template", resume.getTemplate() != null ? resume.getTemplate() : "classic");
            body.put("data", data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // Call PDF service
            ResponseEntity<byte[]> pdfResponse = restTemplate.exchange(
                pdfServiceUrl + "/api/pdf/resume",
                HttpMethod.POST,
                entity,
                byte[].class
            );

            if (pdfResponse.getStatusCode().is2xxSuccessful() && pdfResponse.getBody() != null) {
                String fileName = (resume.getTitle() != null && !resume.getTitle().isBlank() ? resume.getTitle() : "简历") + ".pdf";
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8))
                    .body(pdfResponse.getBody());
            }

            return ResponseEntity.status(502).body(Map.of("message", "PDF 服务返回异常"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "PDF 导出失败: " + e.getMessage()));
        }
    }
}
