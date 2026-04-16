package org.aeryzhao.rag.service;

import org.aeryzhao.rag.dto.RagResponse;
import org.aeryzhao.rag.entity.Document;
import org.aeryzhao.rag.service.DocumentService.SearchResultWithScore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RagService {
    
    @Autowired
    private EmbeddingService embeddingService;
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private ChatClient.Builder chatClientBuilder;
    
    public RagResponse ask(String question, int topK) {
        try {
            log.info("Processing RAG question with topK: {}", topK);
            
            List<Float> questionVector = embeddingService.embed(question);
            log.debug("Question vector generated with dimension: {}", questionVector.size());
            
            List<SearchResultWithScore> searchResults = documentService.searchSimilar(questionVector, topK);
            log.info("Found {} relevant documents", searchResults.size());
            
            List<String> sources = searchResults.stream()
                    .map(result -> {
                        Document doc = result.getDocument();
                        return String.format("[文档ID: %s]\n%s", 
                                doc.getId(), 
                                doc.getContent());
                    })
                    .collect(Collectors.toList());
            
            String prompt = buildPrompt(question, sources);
            log.debug("Prompt built with {} source documents", sources.size());
            
            ChatClient chatClient = chatClientBuilder.build();
            String answer = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
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
    
    private String buildPrompt(String question, List<String> sources) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("基于以下参考文档回答用户问题。如果参考文档中没有相关信息，请明确说明。\n\n");
        promptBuilder.append("参考文档：\n");
        
        for (int i = 0; i < sources.size(); i++) {
            promptBuilder.append(String.format("[文档%d]\n%s\n\n", i + 1, sources.get(i)));
        }
        
        promptBuilder.append("用户问题：").append(question).append("\n\n");
        promptBuilder.append("请提供详细、准确的回答：");
        
        return promptBuilder.toString();
    }
}
