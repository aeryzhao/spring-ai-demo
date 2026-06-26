package org.aeryzhao.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识库创建/更新请求")
public class KnowledgeBaseRequest {

    @Schema(description = "知识库名称", example = "技术文档库", required = true)
    @NotBlank(message = "Name cannot be blank")
    private String name;

    @Schema(description = "知识库描述", example = "存放技术相关文档")
    private String description;
}
