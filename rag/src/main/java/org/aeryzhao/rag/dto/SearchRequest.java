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
@Schema(description = "文档搜索请求")
public class SearchRequest {
    
    @Schema(description = "搜索查询文本", example = "人工智能在医疗领域的应用", required = true)
    @NotBlank(message = "Query cannot be blank")
    private String query;
    
    @Schema(description = "返回结果数量", example = "5", defaultValue = "5")
    @Min(value = 1, message = "topK must be at least 1")
    @Max(value = 100, message = "topK cannot exceed 100")
    @Builder.Default
    private Integer topK = 5;
}
