package com.zhixun.kb.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentParseService {

    public String extractText(Path filePath, String filename) throws IOException {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv")) {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        }
        if (lower.endsWith(".pdf")) {
            try (PDDocument doc = PDDocument.load(filePath.toFile())) {
                return new PDFTextStripper().getText(doc);
            }
        }
        if (lower.endsWith(".docx")) {
            try (InputStream in = Files.newInputStream(filePath);
                 XWPFDocument doc = new XWPFDocument(in);
                 XWPFWordExtractor ex = new XWPFWordExtractor(doc)) {
                return ex.getText();
            }
        }
        // 兜底按文本读
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }

    public List<String> chunk(String text, int size, int overlap) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        List<String> chunks = new ArrayList<>();
        if (normalized.isEmpty()) return chunks;

        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + size, normalized.length());
            chunks.add(normalized.substring(start, end));
            if (end == normalized.length()) break;
            start = Math.max(0, end - overlap);
        }
        return chunks;
    }
}
