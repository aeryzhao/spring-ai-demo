package org.aeryzhao.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识库响应")
public class KnowledgeBaseResponse {

    @Schema(description = "知识库ID", example = "kb_123456789")
    private String id;

    @Schema(description = "知识库名称", example = "技术文档库")
    private String name;

    @Schema(description = "知识库描述", example = "存放技术相关文档")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
