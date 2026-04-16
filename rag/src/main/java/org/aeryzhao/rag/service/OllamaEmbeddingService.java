package org.aeryzhao.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class OllamaEmbeddingService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${rag.embedding.ollama.base-url:http://127.0.0.1:11434}")
    private String baseUrl;

    @Value("${rag.embedding.ollama.model:nomic-embed-text}")
    private String modelName;

    @Value("${rag.embedding.ollama.timeout-seconds:120}")
    private int timeoutSeconds;

    public OllamaEmbeddingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public List<Float> embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        try {
            JsonNode embedResponse = callEmbedApi(text);
            if (embedResponse != null) {
                return parseEmbedApiResponse(embedResponse);
            }

            JsonNode embeddingsResponse = callEmbeddingsApi(text);
            return parseEmbeddingsApiResponse(embeddingsResponse);
        } catch (Exception e) {
            log.error("Failed to generate embedding from Ollama: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate embedding from Ollama: " + e.getMessage(), e);
        }
    }

    private JsonNode callEmbedApi(String text) {
        String url = baseUrl.replaceAll("/$", "") + "/api/embed";
        String payload = "{\"model\":\"" + escapeJson(modelName) + "\",\"input\":\"" + escapeJson(text) + "\"}";
        try {
            String responseBody = postJson(url, payload);
            return objectMapper.readTree(responseBody);
        } catch (Exception e) {
            log.debug("Ollama /api/embed unavailable, fallback to /api/embeddings: {}", e.getMessage());
            return null;
        }
    }

    private JsonNode callEmbeddingsApi(String text) throws IOException, InterruptedException {
        String url = baseUrl.replaceAll("/$", "") + "/api/embeddings";
        String payload = "{\"model\":\"" + escapeJson(modelName) + "\",\"prompt\":\"" + escapeJson(text) + "\"}";
        String responseBody = postJson(url, payload);
        return objectMapper.readTree(responseBody);
    }

    private String postJson(String url, String payload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private List<Float> parseEmbedApiResponse(JsonNode root) {
        JsonNode embeddings = root.path("embeddings");
        if (!embeddings.isArray() || embeddings.isEmpty()) {
            throw new RuntimeException("Invalid Ollama /api/embed response: missing embeddings");
        }

        JsonNode vector = embeddings.get(0);
        if (!vector.isArray() || vector.isEmpty()) {
            throw new RuntimeException("Invalid Ollama /api/embed response: empty embedding vector");
        }

        List<Float> result = new ArrayList<>(vector.size());
        for (JsonNode node : vector) {
            result.add(node.floatValue());
        }
        return result;
    }

    private List<Float> parseEmbeddingsApiResponse(JsonNode root) {
        JsonNode vector = root.path("embedding");
        if (!vector.isArray() || vector.isEmpty()) {
            throw new RuntimeException("Invalid Ollama /api/embeddings response: missing embedding");
        }

        List<Float> result = new ArrayList<>(vector.size());
        for (JsonNode node : vector) {
            result.add(node.floatValue());
        }
        return result;
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
