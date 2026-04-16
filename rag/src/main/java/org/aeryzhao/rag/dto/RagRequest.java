package org.aeryzhao.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG问答请求")
public class RagRequest {
    
    @Schema(description = "用户问题", example = "什么是机器学习？", required = true)
    @NotBlank(message = "Question cannot be blank")
    private String question;
    
    @Schema(description = "检索相关文档的数量", example = "3", defaultValue = "3")
    @Min(value = 1, message = "topK must be at least 1")
    @Max(value = 20, message = "topK cannot exceed 20")
    @Builder.Default
    private Integer topK = 3;
}
