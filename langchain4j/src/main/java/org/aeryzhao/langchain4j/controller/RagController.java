package org.aeryzhao.langchain4j.controller;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 示例 7：RAG —— 自己动手实现「检索增强生成」的每个环节。
 *
 * <p>Spring AI 把 RAG 封装成了 {@code QuestionAnswerAdvisor}，一行 {@code .advisors(...)} 就能用；
 * LangChain4j 则更倾向于<b>把每个环节暴露给你</b>，方便按需替换：
 *
 * <pre>
 * 文档 → DocumentSplitter 切分 → EmbeddingModel 向量化 → EmbeddingStore 存储
 *                                                        ↓
 * 用户提问 → 向量化 → EmbeddingStore 相似度检索 → 拼进提示词 → ChatModel 生成
 * </pre>
 *
 * <p>本示例用 {@link InMemoryEmbeddingStore}（内存向量库，重启即丢）跑通全流程。
 * 生产环境换成 {@code PgVectorEmbeddingStore} / {@code MilvusEmbeddingStore} 等即可，
 * 接口完全一致，无需改动检索与生成代码。
 *
 * <p><b>设计说明</b>：知识库采用<b>懒加载</b>而不是 {@code @PostConstruct} 预加载。
 * 因为向量化需要调用 Embedding 接口，如果启动时就把网络调用写进生命周期，
 * 一旦 API Key 失效 / 网络不通，整个应用会直接启动失败 —— 连不依赖向量库的
 * Chat、AiServices、工具调用示例都跟着用不了。懒加载让每个示例可以独立工作、独立失败。
 */
@Slf4j
@RestController
@RequestMapping("/lc4j/rag")
@RequiredArgsConstructor
public class RagController {

    private final EmbeddingModel embeddingModel;
    private final dev.langchain4j.model.chat.ChatModel chatModel;

    /** 内存向量库：示例够用，生产请换持久化实现 */
    private final EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    /** 切分器：按段落递归切分，每段最多 300 字符，段间重叠 30 字符避免语义被割裂 */
    private final DocumentSplitter splitter = DocumentSplitters.recursive(300, 30);

    /** 内置知识是否已灌入（懒加载标记） */
    private final AtomicBoolean knowledgeLoaded = new AtomicBoolean(false);

    /**
     * 内置知识库文本。
     */
    private static final String BUILT_IN_KNOWLEDGE = """
            本仓库的 langchain4j 模块端口是 8097，chat 模块端口是 8096，rag 模块使用 Spring AI 实现。
            langchain4j 模块使用 LangChain4j 1.20.0 版本，Java 版本要求 21 及以上。

            LangChain4j 是一个面向 Java 的 LLM 应用开发框架，由 Dmytro Liubarskyi 创建。
            它的核心抽象包括 ChatModel、EmbeddingModel、ChatMemory 和 AiServices。
            AiServices 允许开发者只声明接口，由框架通过动态代理生成实现，从而把提示词、记忆、工具调用串起来。
            LangChain4j 同时支持 OpenAI、Anthropic、Gemini、Ollama 等二十多种模型提供方。

            向量检索的基本流程是：先把文档切分成片段，用 Embedding 模型把每个片段转成向量存进向量库；
            查询时把问题也转成向量，在向量库中找余弦相似度最高的若干片段，作为上下文交给大模型生成答案。

            RAG 的全称是 Retrieval Augmented Generation，中文叫检索增强生成。
            它能有效缓解大模型的幻觉问题，并让模型回答训练数据之外的私有知识。
            RAG 的两个关键参数是切分粒度和召回数量（maxResults），它们直接决定回答质量。
            """;

    /**
     * 懒加载内置知识：第一次用到知识库时才灌入，且只灌一次。
     *
     * <p>这样即使 Embedding 接口不可用，应用也能正常启动，只有 RAG 相关接口会报错。
     */
    private void ensureKnowledgeLoaded() {
        if (knowledgeLoaded.get()) {
            return;
        }
        synchronized (this) {
            if (knowledgeLoaded.get()) {
                return;
            }
            List<TextSegment> segments = splitter.split(Document.from(BUILT_IN_KNOWLEDGE));
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);
            knowledgeLoaded.set(true);
            log.info("RAG 示例知识库初始化完成，共切分 {} 个片段", segments.size());
        }
    }

    /**
     * 只做检索，不生成 —— 用于观察「检索质量」这一 RAG 中最关键的变量。
     *
     * <p>GET /lc4j/rag/search?question=langchain4j 模块用什么端口
     */
    @GetMapping("/search")
    public Map<String, Object> search(
            @RequestParam(defaultValue = "langchain4j 模块使用哪个端口？") String question,
            @RequestParam(defaultValue = "3") int maxResults) {

        ensureKnowledgeLoaded();

        Embedding queryEmbedding = embeddingModel.embed(question).content();

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(maxResults)
                        .minScore(0.3)          // 过滤掉明显不相关的片段
                        .build()
        ).matches();

        return Map.of(
                "question", question,
                "matchCount", matches.size(),
                "matches", matches.stream().map(m -> Map.of(
                        "score", Math.round(m.score() * 1000) / 1000.0,
                        "text", m.embedded().text()
                )).toList()
        );
    }

    /**
     * 完整 RAG 流程：检索 + 生成。
     *
     * <p>GET /lc4j/rag/ask?question=什么是 RAG？
     */
    @GetMapping("/ask")
    public Map<String, Object> ask(
            @RequestParam(defaultValue = "什么是 RAG？它解决了什么问题？") String question,
            @RequestParam(defaultValue = "3") int maxResults) {

        ensureKnowledgeLoaded();

        // 1) 检索：找出与问题最相关的片段
        Embedding queryEmbedding = embeddingModel.embed(question).content();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(maxResults)
                        .minScore(0.3)
                        .build()
        ).matches();

        if (matches.isEmpty()) {
            return Map.of("question", question, "answer", "知识库中没有找到相关内容。", "sources", List.of());
        }

        // 2) 拼上下文：把检索到的片段编号后放进提示词，并要求模型标注引用
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < matches.size(); i++) {
            context.append("【片段").append(i + 1).append("】")
                    .append(matches.get(i).embedded().text())
                    .append("\n\n");
        }

        // 3) 生成：严格约束模型「只依据资料回答」，这是抑制幻觉的关键
        String prompt = """
                请严格依据下面提供的资料回答问题。要求：
                1. 只使用资料中的信息，不要编造；
                2. 如果资料中没有答案，直接回答「资料中没有相关信息」；
                3. 回答末尾用【片段N】标注你引用了哪几段资料。

                资料：
                %s
                问题：%s
                """.formatted(context, question);

        String answer = chatModel.chat(prompt);

        return Map.of(
                "question", question,
                "answer", answer,
                "sourceCount", matches.size(),
                "sources", matches.stream().map(m -> Map.of(
                        "score", Math.round(m.score() * 1000) / 1000.0,
                        "text", m.embedded().text()
                )).toList()
        );
    }

    /**
     * 往知识库动态追加文档，演示 RAG 的「入库」环节。
     *
     * <p>POST /lc4j/rag/documents，Body 传纯文本。
     */
    @PostMapping("/documents")
    public Map<String, Object> addDocument(@RequestBody String text) {
        // 先确保内置知识已入库，避免顺序依赖
        ensureKnowledgeLoaded();

        List<TextSegment> segments = splitter.split(Document.from(text));
        embeddingStore.addAll(embeddingModel.embedAll(segments).content(), segments);

        return Map.of(
                "addedSegments", segments.size(),
                "segmentTexts", segments.stream().map(TextSegment::text).toList()
        );
    }

}
