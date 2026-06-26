package org.aeryzhao.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class DocumentParserService {

    /**
     * 解析文件并分块为 Spring AI Document 列表
     */
    public List<Document> parseDocumentToChunks(MultipartFile file, int chunkSize) throws IOException {
        String content = parseDocument(file);

        // 使用 Spring AI 的 TokenTextSplitter 进行分块
        TokenTextSplitter splitter = new TokenTextSplitter(
                chunkSize / 4,  // 默认每个 token 约 4 个字符，所以 chunkSize 字符 ≈ chunkSize/4 tokens
                chunkSize / 8,  // min chunk size
                5,              // min chunk length in tokens
                10000,          // max num chunks
                true            // keep separator
        );

        List<Document> documents = splitter.apply(List.of(new Document(UUID.randomUUID().toString(), content, Map.of())));
        log.info("Split content into {} chunks", documents.size());
        return documents;
    }

    /**
     * 解析文件为带元数据的 Spring AI Document 列表
     */
    public List<Document> parseDocumentToChunks(MultipartFile file, int chunkSize, Map<String, Object> metadata) throws IOException {
        List<Document> documents = parseDocumentToChunks(file, chunkSize);

        // 为每个 chunk 附加元数据
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            Map<String, Object> chunkMetadata = new HashMap<>(metadata);
            chunkMetadata.put("chunkIndex", i);
            chunkMetadata.put("totalChunks", documents.size());
            doc.getMetadata().putAll(chunkMetadata);
        }

        return documents;
    }

    /**
     * 解析文件内容为纯文本
     */
    public String parseDocument(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File name cannot be null");
        }

        String extension = getFileExtension(filename).toLowerCase();
        log.info("Parsing document: {} with extension: {}", filename, extension);

        return switch (extension) {
            case "pdf" -> parsePdf(file.getInputStream());
            case "doc" -> parseDoc(file.getInputStream());
            case "docx" -> parseDocx(file.getInputStream());
            case "xls" -> parseXls(file.getInputStream());
            case "xlsx" -> parseXlsx(file.getInputStream());
            case "txt", "md", "json", "xml", "csv" -> parseTextFile(file.getInputStream());
            default -> throw new UnsupportedOperationException(
                "Unsupported file format: " + extension +
                ". Supported formats: pdf, doc, docx, xls, xlsx, txt, md, json, xml, csv"
            );
        };
    }

    public boolean isSupportedFormat(String filename) {
        if (filename == null) {
            return false;
        }
        String extension = getFileExtension(filename).toLowerCase();
        return List.of("pdf", "doc", "docx", "xls", "xlsx", "txt", "md", "json", "xml", "csv")
                .contains(extension);
    }

    private String parsePdf(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(IOUtils.toByteArray(inputStream))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            log.debug("Extracted {} characters from PDF", text.length());
            return text.trim();
        }
    }

    private String parseDoc(InputStream inputStream) throws IOException {
        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            String text = extractor.getText();
            log.debug("Extracted {} characters from DOC", text.length());
            return text.trim();
        }
    }

    private String parseDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            log.debug("Extracted {} characters from DOCX", text.length());
            return text.trim();
        }
    }

    private String parseXls(InputStream inputStream) throws IOException {
        try (HSSFWorkbook workbook = new HSSFWorkbook(inputStream);
             ExcelExtractor extractor = new ExcelExtractor(workbook)) {
            extractor.setFormulasNotResults(false);
            extractor.setIncludeSheetNames(true);
            String text = extractor.getText();
            log.debug("Extracted {} characters from XLS", text.length());
            return text.trim();
        }
    }

    private String parseXlsx(InputStream inputStream) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
             XSSFExcelExtractor extractor = new XSSFExcelExtractor(workbook)) {
            extractor.setFormulasNotResults(false);
            extractor.setIncludeSheetNames(true);
            String text = extractor.getText();
            log.debug("Extracted {} characters from XLSX", text.length());
            return text.trim();
        }
    }

    private String parseTextFile(InputStream inputStream) throws IOException {
        String text = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        log.debug("Extracted {} characters from text file", text.length());
        return text.trim();
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
}
