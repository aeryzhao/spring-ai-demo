package org.aeryzhao.rag.controller;

import org.aeryzhao.rag.dto.*;
import org.aeryzhao.rag.service.DocumentParserService;
import org.aeryzhao.rag.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@Tag(name = "文档管理", description = "文档的插入、检索和搜索相关接口")
public class DocumentController {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DocumentParserService documentParserService;

    @Value("${rag.document.chunk-size:1000}")
    private int defaultChunkSize;

    private String resolveKbId(String knowledgeBaseId) {
        return StringUtils.hasText(knowledgeBaseId)
                ? knowledgeBaseId
                : KnowledgeBaseService.DEFAULT_KB_ID;
    }

    @PostMapping
    @Operation(
        summary = "插入文档",
        description = "将文档内容插入到向量数据库中，系统会自动生成文档的向量表示并存储。支持指定知识库"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "文档插入成功",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"id\": \"doc_123456789\", \"message\": \"Document inserted successfully\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "请求参数错误",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500",
            description = "服务器内部错误",
            content = @Content
        )
    })
    public ResponseEntity<DocumentResponse> insertDocument(
            @Parameter(description = "文档插入请求", required = true)
            @Valid @RequestBody DocumentRequest request,
            @Parameter(description = "知识库ID，不指定则使用默认知识库")
            @RequestParam(required = false) String knowledgeBaseId) {
        log.info("Received document insertion request with content length: {}, knowledgeBaseId: {}",
                request.getContent() != null ? request.getContent().length() : 0, knowledgeBaseId);

        String kbId = resolveKbId(knowledgeBaseId);
        String docId = UUID.randomUUID().toString();

        Map<String, Object> metadata = request.getMetadata() != null
                ? new HashMap<>(request.getMetadata())
                : new HashMap<>();
        metadata.put("knowledgeBaseId", kbId);

        Document document = new Document(docId, request.getContent(), metadata);
        vectorStore.add(List.of(document));

        DocumentResponse response = DocumentResponse.builder()
                .id(docId)
                .message("Document inserted successfully")
                .build();

        log.info("Document inserted with ID: {} to knowledge base: {}", docId, kbId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/search")
    @Operation(
        summary = "搜索文档",
        description = "基于查询文本进行语义搜索，返回最相似的文档列表。支持按知识库过滤"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "搜索成功",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"results\": [{\"id\": \"doc_123\", \"content\": \"相关文档内容\", \"score\": 0.95}]}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "请求参数错误",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500",
            description = "服务器内部错误",
            content = @Content
        )
    })
    public ResponseEntity<SearchResponse> searchDocuments(
            @Parameter(description = "搜索查询文本", example = "人工智能应用", required = true)
            @RequestParam @NotBlank(message = "Query cannot be blank") String query,
            @Parameter(description = "返回结果数量，范围1-100", example = "5")
            @RequestParam(required = false, defaultValue = "5")
            @Min(value = 1, message = "topK must be at least 1")
            @Max(value = 100, message = "topK cannot exceed 100") Integer topK,
            @Parameter(description = "知识库ID，不指定则使用默认知识库")
            @RequestParam(required = false) String knowledgeBaseId) {

        String kbId = resolveKbId(knowledgeBaseId);
        log.info("Received search request with query length: {}, topK: {}, knowledgeBaseId: {}",
                query.length(), topK, kbId);

        String filterExpression = "knowledgeBaseId == '%s'".formatted(kbId);

        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression(filterExpression)
                .build();
        List<Document> searchResults = vectorStore.similaritySearch(searchRequest);

        List<SearchResult> results = searchResults.stream()
                .map(doc -> SearchResult.builder()
                        .id(doc.getId())
                        .content(doc.getText())
                        .metadata(doc.getMetadata())
                        .build())
                .collect(Collectors.toList());

        SearchResponse response = SearchResponse.builder()
                .results(results)
                .build();

        log.info("Search completed with {} results", results.size());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload")
    @Operation(
        summary = "上传文档文件",
        description = "上传多种格式的文档文件（PDF、Word、Excel、TXT等），系统会自动解析文档内容并存储到向量数据库。支持指定知识库"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "文件上传并处理成功",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"documentIds\": [\"doc_123\", \"doc_124\"], \"chunksCount\": 2, \"filename\": \"document.pdf\", \"message\": \"File uploaded and processed successfully\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "请求参数错误或不支持的文件格式",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500",
            description = "服务器内部错误",
            content = @Content
        )
    })
    public ResponseEntity<FileUploadResponse> uploadDocument(
            @Parameter(description = "上传的文档文件", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "文档分块大小，默认1000字符", example = "1000")
            @RequestParam(required = false) Integer chunkSize,
            @Parameter(description = "文档元数据（JSON格式）", example = "{\"author\": \"张三\", \"category\": \"技术文档\"}")
            @RequestParam(required = false) String metadata,
            @Parameter(description = "知识库ID，不指定则使用默认知识库")
            @RequestParam(required = false) String knowledgeBaseId) throws IOException {

        String filename = file.getOriginalFilename();
        String kbId = resolveKbId(knowledgeBaseId);
        log.info("Received file upload request: {}, size: {} bytes, knowledgeBaseId: {}",
                filename, file.getSize(), kbId);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (!documentParserService.isSupportedFormat(filename)) {
            throw new UnsupportedOperationException(
                "Unsupported file format. Supported formats: pdf, doc, docx, xls, xlsx, txt, md, json, xml, csv"
            );
        }

        int actualChunkSize = chunkSize != null ? chunkSize : defaultChunkSize;

        // 构建元数据
        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("filename", filename);
        metadataMap.put("contentType", file.getContentType());
        metadataMap.put("fileSize", file.getSize());
        metadataMap.put("knowledgeBaseId", kbId);
        if (metadata != null && !metadata.isEmpty()) {
            metadataMap.put("customMetadata", metadata);
        }

        // 解析文件并分块，自动附加元数据
        List<Document> documents = documentParserService.parseDocumentToChunks(file, actualChunkSize, metadataMap);

        if (documents.isEmpty()) {
            throw new IllegalArgumentException("No content extracted from the document");
        }

        // 批量存储到向量数据库
        vectorStore.add(documents);

        List<String> documentIds = documents.stream()
                .map(Document::getId)
                .collect(Collectors.toList());

        FileUploadResponse response = FileUploadResponse.builder()
                .documentIds(documentIds)
                .chunksCount(documentIds.size())
                .filename(filename)
                .message("File uploaded and processed successfully")
                .build();

        log.info("File processed successfully: {} chunks inserted to knowledge base: {}",
                documentIds.size(), kbId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/upload/batch")
    @Operation(
        summary = "批量上传文档文件",
        description = "批量上传多个文档文件，系统会自动解析所有文档内容并存储到向量数据库。支持指定知识库"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "文件批量上传并处理成功",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "400",
            description = "请求参数错误或不支持的文件格式",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500",
            description = "服务器内部错误",
            content = @Content
        )
    })
    public ResponseEntity<List<FileUploadResponse>> uploadDocuments(
            @Parameter(description = "上传的文档文件列表", required = true)
            @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "文档分块大小，默认1000字符", example = "1000")
            @RequestParam(required = false) Integer chunkSize,
            @Parameter(description = "知识库ID，不指定则使用默认知识库")
            @RequestParam(required = false) String knowledgeBaseId) throws IOException {

        String kbId = resolveKbId(knowledgeBaseId);
        log.info("Received batch file upload request: {} files, knowledgeBaseId: {}", files.length, kbId);

        List<FileUploadResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                String filename = file.getOriginalFilename();

                if (file.isEmpty()) {
                    log.warn("Skipping empty file: {}", filename);
                    continue;
                }

                if (!documentParserService.isSupportedFormat(filename)) {
                    log.warn("Skipping unsupported file format: {}", filename);
                    continue;
                }

                int actualChunkSize = chunkSize != null ? chunkSize : defaultChunkSize;

                // 构建元数据
                Map<String, Object> metadataMap = new HashMap<>();
                metadataMap.put("filename", filename);
                metadataMap.put("contentType", file.getContentType());
                metadataMap.put("fileSize", file.getSize());
                metadataMap.put("knowledgeBaseId", kbId);

                // 解析文件并分块
                List<Document> documents = documentParserService.parseDocumentToChunks(file, actualChunkSize, metadataMap);

                if (documents.isEmpty()) {
                    log.warn("No content extracted from file: {}", filename);
                    continue;
                }

                // 批量存储
                vectorStore.add(documents);

                List<String> documentIds = documents.stream()
                        .map(Document::getId)
                        .collect(Collectors.toList());

                FileUploadResponse response = FileUploadResponse.builder()
                        .documentIds(documentIds)
                        .chunksCount(documentIds.size())
                        .filename(filename)
                        .message("File uploaded and processed successfully")
                        .build();

                responses.add(response);
                log.info("File processed successfully: {} - {} chunks inserted to knowledge base: {}",
                        filename, documentIds.size(), kbId);

            } catch (Exception e) {
                log.error("Error processing file: {}", file.getOriginalFilename(), e);
            }
        }

        log.info("Batch upload completed: {} files processed", responses.size());

        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping
    @Operation(
        summary = "列出知识库中的文档",
        description = "列出指定知识库中的文档分块列表，支持分页"
    )
    public ResponseEntity<SearchResponse> listDocuments(
            @Parameter(description = "知识库ID，不指定则使用默认知识库")
            @RequestParam(required = false) String knowledgeBaseId,
            @Parameter(description = "返回结果数量，范围1-200", example = "50")
            @RequestParam(required = false, defaultValue = "50")
            @Min(value = 1, message = "limit must be at least 1")
            @Max(value = 200, message = "limit cannot exceed 200") Integer limit) {

        String kbId = resolveKbId(knowledgeBaseId);
        log.info("Listing documents for knowledge base: {}, limit: {}", kbId, limit);

        String filterExpression = "knowledgeBaseId == '%s'".formatted(kbId);

        // 使用宽泛查询 + filterExpression 获取该知识库下的文档
        SearchRequest searchRequest = SearchRequest.builder()
                .query("document text content information")
                .topK(limit)
                .filterExpression(filterExpression)
                .build();
        List<Document> docs = vectorStore.similaritySearch(searchRequest);

        List<SearchResult> results = docs.stream()
                .map(doc -> SearchResult.builder()
                        .id(doc.getId())
                        .content(doc.getText())
                        .metadata(doc.getMetadata())
                        .build())
                .collect(Collectors.toList());

        SearchResponse response = SearchResponse.builder()
                .results(results)
                .build();

        log.info("Found {} documents in knowledge base: {}", results.size(), kbId);

        return ResponseEntity.ok(response);
    }
}
