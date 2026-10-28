package org.aeryzhao.customerservice.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.aeryzhao.customerservice.config.CustomerServiceProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 知识库索引与检索。
 *
 * <p>两种检索模式：</p>
 * <ul>
 *   <li>{@code VECTOR}：Ollama embedding + {@link SimpleVectorStore}（索引落盘为 data/vector-store.json，重启直接加载）</li>
 *   <li>{@code KEYWORD}：embedding 服务不可用时的兜底方案，中文按二元组 + 英文按词做覆盖度打分</li>
 * </ul>
 */
@Slf4j
@Service
public class KnowledgeIndexService {

    /** 检索模式。 */
    public enum Mode {VECTOR, KEYWORD, EMPTY}

    /**
     * 索引快照。
     *
     * @param mode      当前检索模式
     * @param chunks    全部知识片段（关键词检索用）
     * @param vectorStore 向量库（向量检索用，EMPTY/KEYWORD 时为 null）
     * @param builtAt   构建时间
     */
    public record IndexState(Mode mode, List<ChunkRecord> chunks, SimpleVectorStore vectorStore, Instant builtAt) {

        static IndexState empty() {
            return new IndexState(Mode.EMPTY, List.of(), null, Instant.now());
        }
    }

    /** 知识片段。 */
    public record ChunkRecord(String chunkId, String docId, String title, String category, String source, String text) {
    }

    private final CustomerServiceProperties properties;
    private final KnowledgeStore knowledgeStore;
    private final EmbeddingModel embeddingModel;

    private volatile IndexState state = IndexState.empty();
    private volatile String lastError;

    public KnowledgeIndexService(CustomerServiceProperties properties,
                                 KnowledgeStore knowledgeStore,
                                 EmbeddingModel embeddingModel) {
        this.properties = properties;
        this.knowledgeStore = knowledgeStore;
        this.embeddingModel = embeddingModel;
    }

    /** 应用启动后在虚拟线程里建索引，避免阻塞 Web 启动（embedding 服务不可用时自动降级）。 */
    @EventListener(ApplicationReadyEvent.class)
    public void indexOnStartup(ApplicationReadyEvent event) {
        if (event.getApplicationContext().getEnvironment().acceptsProfiles(Profiles.of("crawl"))) {
            return;
        }
        Thread.ofVirtual().name("kb-index").start(() -> {
            try {
                rebuild(false);
            } catch (Exception e) {
                log.warn("启动建索引失败，将在首次提问时重试：{}", e.getMessage());
            }
        });
    }

    public synchronized IndexState rebuild(boolean force) {
        long start = System.currentTimeMillis();
        List<KnowledgeDocument> documents = knowledgeStore.loadAll();
        if (documents.isEmpty()) {
            state = IndexState.empty();
            log.warn("知识库为空，请先抓取数据（data/knowledge 下没有任何 .md）");
            return state;
        }

        List<ChunkRecord> chunks = new ArrayList<>();
        for (KnowledgeDocument document : documents) {
            List<String> pieces = DocumentChunker.split(document.content(), properties.chunkSize(), properties.chunkOverlap());
            for (int i = 0; i < pieces.size(); i++) {
                chunks.add(new ChunkRecord(
                        document.id() + "#" + i,
                        document.id(),
                        document.title(),
                        document.category(),
                        document.source(),
                        pieces.get(i)));
            }
        }

        Path storeFile = properties.vectorStoreFile();
        if (!force && Files.exists(storeFile)) {
            try {
                SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
                store.load(storeFile.toFile());
                state = new IndexState(Mode.VECTOR, List.copyOf(chunks), store, Instant.now());
                lastError = null;
                log.info("已从 {} 加载向量索引：{} 篇文档 / {} 个片段，用时 {} ms",
                        storeFile.getFileName(), documents.size(), chunks.size(), System.currentTimeMillis() - start);
                return state;
            } catch (Exception e) {
                log.warn("加载向量索引缓存失败，将重建：{}", e.getMessage());
            }
        }

        SimpleVectorStore store = tryBuildVectorStore(chunks, storeFile);
        Mode mode = store != null ? Mode.VECTOR : Mode.KEYWORD;
        state = new IndexState(mode, List.copyOf(chunks), store, Instant.now());
        log.info("知识库索引完成：{} 篇文档 / {} 个片段，模式 {}，用时 {} ms",
                documents.size(), chunks.size(), mode, System.currentTimeMillis() - start);
        return state;
    }

    private SimpleVectorStore tryBuildVectorStore(List<ChunkRecord> chunks, Path storeFile) {
        try {
            // 先探活一次，避免把几十个片段全部失败一遍
            embeddingModel.embed("知识库健康检查");
            SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
            List<Document> documents = chunks.stream()
                    .map(chunk -> Document.builder()
                            .text(chunk.text())
                            .metadata(Map.of(
                                    "chunkId", chunk.chunkId(),
                                    "docId", chunk.docId(),
                                    "title", chunk.title(),
                                    "category", chunk.category(),
                                    "source", chunk.source()))
                            .build())
                    .toList();
            store.add(documents);
            Files.createDirectories(storeFile.getParent());
            store.save(storeFile.toFile());
            lastError = null;
            return store;
        } catch (Exception e) {
            lastError = "embedding 服务不可用，已降级为关键词检索：" + e.getMessage();
            log.warn(lastError);
            return null;
        }
    }

    /** 语义检索（必要时自动降级）。 */
    public List<RetrievedChunk> search(String query, int topK) {
        IndexState current = state;
        if (current.mode() == Mode.EMPTY) {
            current = rebuild(false);
        }
        if (current.chunks().isEmpty()) {
            return List.of();
        }
        if (current.mode() == Mode.VECTOR) {
            try {
                return vectorSearch(current, query, topK);
            } catch (Exception e) {
                lastError = "向量检索失败，已降级为关键词检索：" + e.getMessage();
                log.warn(lastError);
            }
        }
        return keywordSearch(current.chunks(), query, topK);
    }

    private List<RetrievedChunk> vectorSearch(IndexState current, String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        List<Document> documents = current.vectorStore().similaritySearch(request);
        List<RetrievedChunk> results = new ArrayList<>(documents.size());
        for (Document document : documents) {
            Map<String, Object> metadata = document.getMetadata();
            results.add(new RetrievedChunk(
                    asString(metadata.get("chunkId")),
                    asString(metadata.get("docId")),
                    asString(metadata.get("title")),
                    asString(metadata.get("category")),
                    asString(metadata.get("source")),
                    document.getText(),
                    document.getScore() == null ? 0d : document.getScore()));
        }
        return results;
    }

    /** 关键词兜底：中文二元组 + 英文单词的加权覆盖度。 */
    private List<RetrievedChunk> keywordSearch(List<ChunkRecord> chunks, String query, int topK) {
        List<String> terms = terms(query);
        if (terms.isEmpty()) {
            return List.of();
        }
        List<RetrievedChunk> scored = new ArrayList<>();
        for (ChunkRecord chunk : chunks) {
            String text = chunk.text().toLowerCase(Locale.ROOT);
            String title = chunk.title() == null ? "" : chunk.title().toLowerCase(Locale.ROOT);
            double score = 0;
            for (String term : terms) {
                score += countOccurrences(text, term);
                if (title.contains(term)) {
                    score += 3;
                }
            }
            if (score > 0) {
                scored.add(new RetrievedChunk(chunk.chunkId(), chunk.docId(), chunk.title(),
                        chunk.category(), chunk.source(), chunk.text(), score));
            }
        }
        return scored.stream()
                .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed())
                .limit(topK)
                .toList();
    }

    private static List<String> terms(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        Set<String> terms = new LinkedHashSet<>();
        StringBuilder cjkRun = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (isCjk(c)) {
                cjkRun.append(c);
            } else {
                addCjkTerms(cjkRun, terms);
                cjkRun.setLength(0);
            }
        }
        addCjkTerms(cjkRun, terms);
        for (String word : normalized.split("[^a-z0-9]+")) {
            if (word.length() >= 2) {
                terms.add(word);
            }
        }
        return List.copyOf(terms);
    }

    private static void addCjkTerms(StringBuilder run, Set<String> terms) {
        if (run.isEmpty()) {
            return;
        }
        String value = run.toString();
        if (value.length() == 1) {
            terms.add(value);
            return;
        }
        for (int i = 0; i + 1 < value.length(); i++) {
            terms.add(value.substring(i, i + 2));
        }
    }

    private static boolean isCjk(char c) {
        return Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN;
    }

    private static int countOccurrences(String text, String term) {
        int count = 0;
        int index = text.indexOf(term);
        while (index >= 0) {
            count++;
            index = text.indexOf(term, index + term.length());
        }
        return count;
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public IndexState state() {
        return state;
    }

    /** 实际生效的检索模式：运行期发生过降级时返回 KEYWORD。 */
    public String effectiveMode() {
        return lastError == null ? state.mode().name() : Mode.KEYWORD.name();
    }

    public String lastError() {
        return lastError;
    }

    /** 知识库概览，供前端展示与排查用。 */
    public List<KnowledgeDocument> documents() {
        return knowledgeStore.loadAll();
    }

    public Map<String, Object> stats() {
        IndexState current = state;
        Map<String, Object> stats = new HashMap<>();
        stats.put("mode", current.mode().name());
        stats.put("documents", documents().size());
        stats.put("chunks", current.chunks().size());
        stats.put("builtAt", current.builtAt().toString());
        stats.put("dataDir", properties.dataPath().toString());
        stats.put("knowledgeDir", properties.knowledgePath().toString());
        stats.put("vectorStoreFile", properties.vectorStoreFile().toString());
        if (lastError != null) {
            stats.put("lastError", lastError);
        }
        return stats;
    }

    /** 向量索引缓存文件是否存在。 */
    public boolean hasVectorCache() {
        File file = properties.vectorStoreFile().toFile();
        return file.isFile() && file.length() > 0;
    }
}
