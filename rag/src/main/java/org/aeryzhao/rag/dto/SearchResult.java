package org.aeryzhao.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜索结果项")
public class SearchResult {
    
    @Schema(description = "文档ID", example = "123456789")
    private Long id;
    
    @Schema(description = "文档内容", example = "人工智能在医疗领域的应用包括疾病诊断、药物研发、医疗影像分析等...")
    private String content;
    
    @Schema(description = "文档元数据", example = "{\"author\": \"李四\", \"source\": \"医疗期刊\", \"publishDate\": \"2024-01-15\"}")
    private Map<String, Object> metadata;
    
    @Schema(description = "相似度得分，范围0-1，越接近1表示越相似", example = "0.95")
    private Double score;
}
