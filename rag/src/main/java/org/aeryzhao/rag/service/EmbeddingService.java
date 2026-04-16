package org.aeryzhao.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EmbeddingService {

    @Autowired
    private OllamaEmbeddingService ollamaEmbeddingService;

    @Value("${rag.embedding.model-type:online}")
    private String modelType;
    
    public List<Float> embed(String text) {
        log.debug("Using ollama embedding model");
        return ollamaEmbeddingService.embed(text);
    }
}
