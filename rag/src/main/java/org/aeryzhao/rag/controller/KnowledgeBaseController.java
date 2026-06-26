package org.aeryzhao.rag.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.aeryzhao.rag.dto.KnowledgeBaseRequest;
import org.aeryzhao.rag.dto.KnowledgeBaseResponse;
import org.aeryzhao.rag.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/knowledge-bases")
@Tag(name = "知识库管理", description = "知识库的增删改查相关接口")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @PostMapping
    @Operation(summary = "创建知识库", description = "创建一个新的知识库")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    public ResponseEntity<KnowledgeBaseResponse> create(
            @Valid @RequestBody KnowledgeBaseRequest request) {
        log.info("Creating knowledge base: {}", request.getName());
        KnowledgeBaseResponse response = knowledgeBaseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "列出所有知识库", description = "获取所有知识库列表")
    public ResponseEntity<List<KnowledgeBaseResponse>> list() {
        List<KnowledgeBaseResponse> list = knowledgeBaseService.list();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取知识库详情", description = "根据ID获取知识库详情")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    public ResponseEntity<KnowledgeBaseResponse> getById(
            @Parameter(description = "知识库ID", required = true)
            @PathVariable String id) {
        KnowledgeBaseResponse response = knowledgeBaseService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新知识库", description = "更新知识库名称和描述")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    public ResponseEntity<KnowledgeBaseResponse> update(
            @Parameter(description = "知识库ID", required = true)
            @PathVariable String id,
            @Valid @RequestBody KnowledgeBaseRequest request) {
        log.info("Updating knowledge base: {}", id);
        KnowledgeBaseResponse response = knowledgeBaseService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识库", description = "删除知识库及其所有关联文档")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    public ResponseEntity<Map<String, String>> delete(
            @Parameter(description = "知识库ID", required = true)
            @PathVariable String id) {
        log.info("Deleting knowledge base: {}", id);
        knowledgeBaseService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Knowledge base deleted successfully"));
    }
}
