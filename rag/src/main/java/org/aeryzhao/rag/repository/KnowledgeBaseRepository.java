package org.aeryzhao.rag.repository;

import org.aeryzhao.rag.model.KnowledgeBase;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class KnowledgeBaseRepository {

    private final Map<String, KnowledgeBase> store = new ConcurrentHashMap<>();

    public KnowledgeBase save(KnowledgeBase knowledgeBase) {
        store.put(knowledgeBase.getId(), knowledgeBase);
        return knowledgeBase;
    }

    public Optional<KnowledgeBase> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<KnowledgeBase> findAll() {
        return new ArrayList<>(store.values());
    }

    public void deleteById(String id) {
        store.remove(id);
    }

    public boolean existsById(String id) {
        return store.containsKey(id);
    }
}
