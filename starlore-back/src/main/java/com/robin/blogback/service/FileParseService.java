package com.robin.blogback.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件解析服务：从 txt/md/docx/pdf 中提取纯文本
 */
@Service
public class FileParseService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * 解析上传的文件，提取纯文本内容
     */
    public String extractText(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件过大（最大 10MB）");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("无法识别文件名");
        }

        String ext = getExtension(filename).toLowerCase();
        try (InputStream is = file.getInputStream()) {
            switch (ext) {
                case "txt":
                case "md":
                case "markdown":
                case "csv":
                case "json":
                case "xml":
                case "yaml":
                case "yml":
                    return readPlainText(is);
                case "docx":
                    return parseDocx(is);
                case "pdf":
                    return parsePdf(is, file.getSize());
                default:
                    throw new IllegalArgumentException("不支持的文件格式: " + ext);
            }
        }
    }

    /**
     * 纯文本文件直接读取
     */
    private String readPlainText(InputStream is) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * 解析 docx 文件
     */
    private String parseDocx(InputStream is) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(is)) {
            StringBuilder sb = new StringBuilder();
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            for (XWPFParagraph para : paragraphs) {
                String text = para.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text).append("\n");
                }
            }
            return sb.toString().trim();
        }
    }

    /**
     * 解析 PDF 文件
     */
    private String parsePdf(InputStream is, long size) throws Exception {
        // PDFBox 3.x 需要通过 byte[] 或文件加载
        byte[] bytes = is.readAllBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1);
    }
}
