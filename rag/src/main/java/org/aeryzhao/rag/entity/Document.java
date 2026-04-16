package org.aeryzhao.rag.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    
    private Long id;
    
    private String content;
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    private List<Float> embedding;
}
