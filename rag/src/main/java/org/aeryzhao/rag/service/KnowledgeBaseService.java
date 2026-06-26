package org.aeryzhao.rag.service;

import org.aeryzhao.rag.dto.KnowledgeBaseRequest;
import org.aeryzhao.rag.dto.KnowledgeBaseResponse;
import org.aeryzhao.rag.model.KnowledgeBase;
import org.aeryzhao.rag.repository.KnowledgeBaseRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeBaseService {

    public static final String DEFAULT_KB_ID = "default";

    private final KnowledgeBaseRepository repository;
    private final VectorStore vectorStore;

    public KnowledgeBaseService(KnowledgeBaseRepository repository, VectorStore vectorStore) {
        this.repository = repository;
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void init() {
        if (!repository.existsById(DEFAULT_KB_ID)) {
            KnowledgeBase defaultKb = KnowledgeBase.builder()
                    .id(DEFAULT_KB_ID)
                    .name("默认知识库")
                    .description("系统默认知识库")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            repository.save(defaultKb);
            log.info("Default knowledge base created with id: {}", DEFAULT_KB_ID);
        }
    }

    public KnowledgeBaseResponse create(KnowledgeBaseRequest request) {
        String id = "kb_" + UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime now = LocalDateTime.now();

        KnowledgeBase kb = KnowledgeBase.builder()
                .id(id)
                .name(request.getName())
                .description(request.getDescription())
                .createdAt(now)
                .updatedAt(now)
                .build();

        repository.save(kb);
        log.info("Knowledge base created: {} ({})", kb.getName(), kb.getId());
        return toResponse(kb);
    }

    public List<KnowledgeBaseResponse> list() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public KnowledgeBaseResponse getById(String id) {
        KnowledgeBase kb = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Knowledge base not found: " + id));
        return toResponse(kb);
    }

    public KnowledgeBaseResponse update(String id, KnowledgeBaseRequest request) {
        KnowledgeBase kb = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Knowledge base not found: " + id));

        if (DEFAULT_KB_ID.equals(id)) {
            throw new RuntimeException("Cannot modify the default knowledge base");
        }

        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setUpdatedAt(LocalDateTime.now());

        repository.save(kb);
        log.info("Knowledge base updated: {} ({})", kb.getName(), kb.getId());
        return toResponse(kb);
    }

    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Knowledge base not found: " + id);
        }
        if (DEFAULT_KB_ID.equals(id)) {
            throw new RuntimeException("Cannot delete the default knowledge base");
        }

        // 删除该知识库下的所有文档
        String filterExpression = "knowledgeBaseId == '%s'".formatted(id);
        vectorStore.delete(filterExpression);
        log.info("Deleted all documents for knowledge base: {}", id);

        repository.deleteById(id);
        log.info("Knowledge base deleted: {}", id);
    }

    private KnowledgeBaseResponse toResponse(KnowledgeBase kb) {
        return KnowledgeBaseResponse.builder()
                .id(kb.getId())
                .name(kb.getName())
                .description(kb.getDescription())
                .createdAt(kb.getCreatedAt())
                .updatedAt(kb.getUpdatedAt())
                .build();
    }
}
