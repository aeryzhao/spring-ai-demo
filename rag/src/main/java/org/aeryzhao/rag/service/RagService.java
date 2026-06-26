package org.aeryzhao.rag.service;

import org.aeryzhao.rag.dto.RagResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;

        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore).build();

        this.chatClient = chatClientBuilder
                .defaultAdvisors(qaAdvisor)
                .build();
    }

    public RagResponse ask(String question, int topK) {
        return ask(question, topK, null);
    }

    public RagResponse ask(String question, int topK, String knowledgeBaseId) {
        try {
            String kbId = StringUtils.hasText(knowledgeBaseId)
                    ? knowledgeBaseId
                    : KnowledgeBaseService.DEFAULT_KB_ID;
            log.info("Processing RAG question with topK: {}, knowledgeBaseId: {}", topK, kbId);

            String filterExpression = "knowledgeBaseId == '%s'".formatted(kbId);

            // 1. 使用 VectorStore 检索相关文档（用于返回 sources）
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(question)
                    .topK(topK)
                    .filterExpression(filterExpression)
                    .build();
            List<Document> relevantDocs = vectorStore.similaritySearch(searchRequest);
            log.info("Found {} relevant documents", relevantDocs.size());

            // 2. 使用 ChatClient + QuestionAnswerAdvisor 生成答案
            //    通过 advisor context 传入 filterExpression，让 Advisor 也按知识库过滤
            String answer = chatClient.prompt()
                    .user(question)
                    .advisors(a -> a.param(
                            QuestionAnswerAdvisor.FILTER_EXPRESSION,
                            filterExpression
                    ))
                    .call()
                    .content();

            // 3. 构建 sources 列表
            List<String> sources = relevantDocs.stream()
                    .map(doc -> String.format("[文档ID: %s]\n%s", doc.getId(), doc.getText()))
                    .collect(Collectors.toList());

            log.info("RAG answer generated successfully");

            return RagResponse.builder()
                    .answer(answer)
                    .sources(sources)
                    .build();

        } catch (Exception e) {
            log.error("Error processing RAG question: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process question: " + e.getMessage(), e);
        }
    }
}
