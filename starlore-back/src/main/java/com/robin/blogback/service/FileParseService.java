package com.robin.blogback.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileParseService {

    public String extractText(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded.txt";
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1).toLowerCase() : "txt";

        if ("pdf".equals(ext)) {
            try (InputStream is = file.getInputStream();
                 PDDocument document = Loader.loadPDF(is.readAllBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } else if ("docx".equals(ext) || "doc".equals(ext)) {
            try (InputStream is = file.getInputStream();
                 XWPFDocument document = new XWPFDocument(is)) {
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph p : document.getParagraphs()) {
                    if (p.getText() != null) {
                        sb.append(p.getText()).append("\n");
                    }
                }
                return sb.toString();
            }
        } else {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }
    }

    public List<String> splitText(String text, int maxChunkSize, int overlap) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        String[] lines = text.split("\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String line : lines) {
            if (currentChunk.length() + line.length() + 1 > maxChunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
            }
            currentChunk.append(line).append("\n");
        }
        if (currentChunk.length() > 0 && !currentChunk.toString().trim().isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }
        return chunks;
    }

    public int estimateTokens(String text) {
        if (text == null) return 0;
        return (int) Math.ceil(text.length() / 3.0);
    }
}
