package org.aeryzhao.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档插入响应")
public class DocumentResponse {
    
    @Schema(description = "插入的文档ID", example = "123456789")
    private Long id;
    
    @Schema(description = "操作结果消息", example = "Document inserted successfully")
    private String message;
}
