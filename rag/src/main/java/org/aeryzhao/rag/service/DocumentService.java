package org.aeryzhao.rag.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aeryzhao.rag.entity.Document;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class DocumentService {
    
    private static final String FIELD_ID = "id";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_EMBEDDING = "embedding";
    private static final String FIELD_METADATA = "metadata";
    
    private static final AtomicLong idGenerator = new AtomicLong(System.currentTimeMillis());
    
    @Data
    @AllArgsConstructor
    public static class SearchResultWithScore {
        private Document document;
        private double score;
    }
    
    @Autowired
    private MilvusServiceClient milvusClient;
    
    @Autowired
    private EmbeddingService embeddingService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${rag.vector.collection-name}")
    private String collectionName;
    
    @Value("${rag.vector.metric-type}")
    private String metricType;
    
    public Long insertDocument(Document document) {
        try {
            log.info("Inserting document with content length: {}", document.getContent().length());
            
            if (document.getId() == null) {
                document.setId(idGenerator.incrementAndGet());
            }
            
            List<Float> embedding = embeddingService.embed(document.getContent());
            document.setEmbedding(embedding);
            
            String metadataJson = convertMetadataToJson(document.getMetadata());
            
            List<Long> ids = Collections.singletonList(document.getId());
            List<String> contents = Collections.singletonList(document.getContent());
            List<List<Float>> embeddings = Collections.singletonList(embedding);
            List<String> metadatas = Collections.singletonList(metadataJson);
            
            List<InsertParam.Field> fields = new ArrayList<>();
            fields.add(new InsertParam.Field(FIELD_ID, ids));
            fields.add(new InsertParam.Field(FIELD_CONTENT, contents));
            fields.add(new InsertParam.Field(FIELD_EMBEDDING, embeddings));
            fields.add(new InsertParam.Field(FIELD_METADATA, metadatas));
            
            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build();
            
            R<MutationResult> response = milvusClient.insert(insertParam);
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("Document inserted successfully with ID: {}", document.getId());
                return document.getId();
            } else {
                String errorMsg = "Failed to insert document: " + response.getMessage();
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
        } catch (Exception e) {
            log.error("Error inserting document: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to insert document: " + e.getMessage(), e);
        }
    }
    
    public List<SearchResultWithScore> searchSimilar(List<Float> vector, int topK) {
        try {
            log.info("Searching for similar documents with topK: {}", topK);
            
            List<String> searchOutputFields = List.of(FIELD_ID, FIELD_CONTENT, FIELD_METADATA);
            
            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withMetricType(MetricType.valueOf(metricType))
                    .withTopK(topK)
                    .withVectors(Collections.singletonList(vector))
                    .withVectorFieldName(FIELD_EMBEDDING)
                    .withOutFields(searchOutputFields)
                    .build();
            
            R<SearchResults> response = milvusClient.search(searchParam);
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
                
                List<SearchResultWithScore> results = new ArrayList<>();
                
                List<SearchResultsWrapper.IDScore> idScores = wrapper.getIDScore(0);
                for (int i = 0; i < idScores.size(); i++) {
                    SearchResultsWrapper.IDScore idScore = idScores.get(i);
                    
                    Document doc = new Document();
                    doc.setId(idScore.getLongID());
                    
                    String content = (String) wrapper.getFieldData(FIELD_CONTENT, 0).get(i);
                    doc.setContent(content);
                    
                    String metadataJson = (String) wrapper.getFieldData(FIELD_METADATA, 0).get(i);
                    doc.setMetadata(parseMetadataFromJson(metadataJson));
                    
                    double score = idScore.getScore();
                    
                    results.add(new SearchResultWithScore(doc, score));
                }
                
                log.info("Found {} similar documents", results.size());
                return results;
                
            } else {
                String errorMsg = "Failed to search documents: " + response.getMessage();
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
        } catch (Exception e) {
            log.error("Error searching similar documents: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search similar documents: " + e.getMessage(), e);
        }
    }
    
    private String convertMetadataToJson(Map<String, Object> metadata) {
        try {
            if (metadata == null || metadata.isEmpty()) {
                return "{}";
            }
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert metadata to JSON: {}", e.getMessage());
            return "{}";
        }
    }
    
    private Map<String, Object> parseMetadataFromJson(String json) {
        try {
            if (json == null || json.isEmpty() || json.equals("{}")) {
                return Collections.emptyMap();
            }
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse metadata from JSON: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
