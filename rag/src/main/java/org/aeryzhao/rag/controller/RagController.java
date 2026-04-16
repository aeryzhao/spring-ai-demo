package org.aeryzhao.rag.controller;

import org.aeryzhao.rag.dto.RagRequest;
import org.aeryzhao.rag.dto.RagResponse;
import org.aeryzhao.rag.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "RAG问答", description = "检索增强生成(Retrieval-Augmented Generation)相关接口")
public class RagController {
    
    @Autowired
    private RagService ragService;
    
    @PostMapping("/ask")
    @Operation(
        summary = "RAG问答",
        description = "基于检索增强生成技术回答用户问题。系统会先从向量数据库中检索相关文档，然后结合检索结果生成答案"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "问答成功",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"answer\": \"机器学习是人工智能的一个分支...\", \"sources\": [\"文档1内容\", \"文档2内容\"]}"
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
    public ResponseEntity<RagResponse> ask(
            @Parameter(description = "RAG问答请求", required = true)
            @Valid @RequestBody RagRequest request) {
        log.info("Received RAG question: {}", request.getQuestion());
        
        RagResponse response = ragService.ask(request.getQuestion(), request.getTopK());
        
        log.info("RAG response generated with {} sources", 
                response.getSources() != null ? response.getSources().size() : 0);
        
        return ResponseEntity.ok(response);
    }
}
