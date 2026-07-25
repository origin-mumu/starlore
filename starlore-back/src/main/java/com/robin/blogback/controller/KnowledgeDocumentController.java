package com.robin.blogback.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.robin.blogback.entity.KnowledgeDocument;
import com.robin.blogback.entity.KnowledgeDocumentChunk;
import com.robin.blogback.mapper.KnowledgeDocumentChunkMapper;
import com.robin.blogback.mapper.KnowledgeDocumentMapper;
import com.robin.blogback.service.FileParseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class KnowledgeDocumentController {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeDocumentChunkMapper knowledgeDocumentChunkMapper;
    private final FileParseService fileParseService;

    @PostMapping("/parse-file")
    public ResponseEntity<?> parseFile(@RequestParam("file") MultipartFile file) {
        try {
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded_file.txt";
            String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1).toLowerCase() : "txt";
            String extractedText = fileParseService.extractText(file);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                extractedText = "【文件内容解析说明】文件《" + filename + "》已成功接收。";
            }

            int estimatedTokens = fileParseService.estimateTokens(extractedText);

            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("fileName", filename);
            res.put("fileType", ext);
            res.put("fileSize", file.getSize());
            res.put("extractedText", extractedText);
            res.put("estimatedTokens", estimatedTokens);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> errorRes = new HashMap<>();
            errorRes.put("success", false);
            errorRes.put("message", "文件解析失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorRes);
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<?> listDocuments(HttpServletRequest request) {
        Integer userIdInt = (Integer) request.getAttribute("userId");
        Long userId = userIdInt != null ? userIdInt.longValue() : 1L;

        List<KnowledgeDocument> docs = knowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getUserId, userId)
                        .orderByDesc(KnowledgeDocument::getId)
        );

        List<Map<String, Object>> docList = docs.stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId());
            map.put("fileName", d.getFileName());
            map.put("fileType", d.getFileType());
            map.put("fileSize", d.getFileSize());
            map.put("fileUrl", d.getFileUrl() != null ? d.getFileUrl() : "");
            map.put("extractedText", d.getExtractedText());
            map.put("status", d.getStatus());
            map.put("chunkCount", d.getChunkCount());
            map.put("createdAt", d.getCreatedAt() != null ? d.getCreatedAt().toString() : "");
            map.put("updatedAt", d.getUpdatedAt() != null ? d.getUpdatedAt().toString() : "");
            return map;
        }).toList();

        return ResponseEntity.ok(Map.of("success", true, "documents", docList));
    }

    @Data
    public static class ConfirmDocumentRequest {
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String extractedText;
    }

    @PostMapping("/documents/confirm")
    public ResponseEntity<?> confirmDocument(HttpServletRequest request, @RequestBody ConfirmDocumentRequest req) {
        Integer userIdInt = (Integer) request.getAttribute("userId");
        Long userId = userIdInt != null ? userIdInt.longValue() : 1L;

        String filename = req.getFileName() != null ? req.getFileName() : "file.txt";
        String fileExt = req.getFileType() != null ? req.getFileType() :
                (filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1).toLowerCase() : "txt");
        String extractedText = req.getExtractedText() != null ? req.getExtractedText() : "";

        if (extractedText.trim().isEmpty()) {
            extractedText = "【文件内容解析说明】文件《" + filename + "》已成功存储。";
        }

        List<String> chunks = fileParseService.splitText(extractedText, 800, 100);

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setUserId(userId);
        doc.setFileName(filename);
        doc.setFileType(fileExt);
        doc.setFileSize(req.getFileSize() != null ? req.getFileSize() : (long) extractedText.getBytes().length);
        doc.setFileUrl("");
        doc.setExtractedText(extractedText);
        doc.setStatus("indexed");
        doc.setChunkCount(chunks.size());
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());

        knowledgeDocumentMapper.insert(doc);

        for (int i = 0; i < chunks.size(); i++) {
            String cText = chunks.get(i);
            KnowledgeDocumentChunk chunk = new KnowledgeDocumentChunk();
            chunk.setDocumentId(doc.getId());
            chunk.setChunkIndex(i);
            chunk.setContent(cText);
            chunk.setTokenCount(fileParseService.estimateTokens(cText));
            chunk.setIsEnabled(1);
            chunk.setCreatedAt(LocalDateTime.now());
            knowledgeDocumentChunkMapper.insert(chunk);
        }

        Map<String, Object> docMap = new HashMap<>();
        docMap.put("id", doc.getId());
        docMap.put("fileName", doc.getFileName());
        docMap.put("fileType", doc.getFileType());
        docMap.put("fileSize", doc.getFileSize());
        docMap.put("extractedText", doc.getExtractedText());
        docMap.put("status", doc.getStatus());
        docMap.put("chunkCount", doc.getChunkCount());
        docMap.put("createdAt", doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : "");

        return ResponseEntity.ok(Map.of("success", true, "document", docMap));
    }

    @GetMapping("/documents/{docId}/chunks")
    public ResponseEntity<?> getDocumentChunks(@PathVariable Long docId) {
        List<KnowledgeDocumentChunk> chunks = knowledgeDocumentChunkMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocumentChunk>()
                        .eq(KnowledgeDocumentChunk::getDocumentId, docId)
                        .orderByAsc(KnowledgeDocumentChunk::getChunkIndex)
        );

        List<Map<String, Object>> list = chunks.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("documentId", c.getDocumentId());
            map.put("chunkIndex", c.getChunkIndex());
            map.put("content", c.getContent());
            map.put("tokenCount", c.getTokenCount());
            map.put("isEnabled", c.getIsEnabled());
            return map;
        }).toList();

        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }

    @DeleteMapping("/documents/{docId}")
    public ResponseEntity<?> deleteDocument(HttpServletRequest request, @PathVariable Long docId) {
        Integer userIdInt = (Integer) request.getAttribute("userId");
        Long userId = userIdInt != null ? userIdInt.longValue() : 1L;

        knowledgeDocumentChunkMapper.delete(
                new LambdaQueryWrapper<KnowledgeDocumentChunk>().eq(KnowledgeDocumentChunk::getDocumentId, docId)
        );
        knowledgeDocumentMapper.delete(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getId, docId)
                        .eq(KnowledgeDocument::getUserId, userId)
        );

        return ResponseEntity.ok(Map.of("success", true));
    }
}
