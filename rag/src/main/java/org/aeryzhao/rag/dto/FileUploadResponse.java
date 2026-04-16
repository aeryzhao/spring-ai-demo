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
@Schema(description = "文件上传响应")
public class FileUploadResponse {

    @Schema(description = "上传成功的文档ID列表")
    private List<Long> documentIds;

    @Schema(description = "处理的文档块数量")
    private Integer chunksCount;

    @Schema(description = "原始文件名")
    private String filename;

    @Schema(description = "操作结果消息")
    private String message;
}
