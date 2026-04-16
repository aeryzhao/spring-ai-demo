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
@Schema(description = "文档搜索响应")
public class SearchResponse {
    
    @Schema(description = "搜索结果列表")
    private List<SearchResult> results;
}
