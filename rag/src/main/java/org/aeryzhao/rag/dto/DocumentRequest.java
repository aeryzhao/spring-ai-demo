package org.aeryzhao.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档插入请求")
public class DocumentRequest {
    
    @Schema(description = "文档内容", example = "这是一段关于人工智能的技术文档...", required = true)
    @NotBlank(message = "Content cannot be blank")
    @Size(max = 65535, message = "Content length cannot exceed 65535 characters")
    private String content;
    
    @Schema(description = "文档元数据，可包含作者、来源、创建时间等信息", 
            example = "{\"author\": \"张三\", \"source\": \"技术博客\", \"category\": \"AI\"}")
    private Map<String, Object> metadata;
}
