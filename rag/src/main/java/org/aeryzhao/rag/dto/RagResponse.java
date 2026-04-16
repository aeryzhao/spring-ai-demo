package org.aeryzhao.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG问答响应")
public class RagResponse {
    
    @Schema(description = "AI生成的答案", example = "机器学习是人工智能的一个分支，它使计算机系统能够从数据中学习和改进，而无需明确编程...")
    private String answer;
    
    @Schema(description = "答案来源的文档内容列表", example = "[\"文档1：机器学习基础概念...\", \"文档2：机器学习应用场景...\"]")
    private List<String> sources;
}
