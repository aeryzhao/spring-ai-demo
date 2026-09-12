package org.aeryzhao.langchain4j.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 模型装配。
 *
 * <p>LangChain4j 与 Spring AI 最大的差异之一是：它<b>不依赖 Spring Boot 自动装配</b>，
 * 模型对象需要自己用 {@code builder()} 显式创建，然后交给 Spring 容器管理。
 * 好处是模型实例完全可控、可同时创建多个（不同厂商/不同参数），坏处是没有 starter 帮你兜底。
 *
 * <p>这里把 {@link ChatModel}（同步）与 {@link StreamingChatModel}（流式）都注册成 Bean，
 * 后续示例按需注入即可。
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LangChain4jProperties.class)
public class ModelConfig {

    /**
     * 创建同步对话模型。
     *
     * <p>这里用 {@code provider} 开关同时支持两种后端：
     * <ul>
     *   <li>{@code openai}：任何 OpenAI 兼容接口（OpenAI / DeepSeek / 各类网关）</li>
     *   <li>{@code ollama}：本地 Ollama，无需 API Key，离线可跑</li>
     * </ul>
     */
    @Bean
    public ChatModel chatModel(LangChain4jProperties props) {
        String provider = props.getProvider() == null ? "openai" : props.getProvider().toLowerCase();
        log.info("初始化 LangChain4j ChatModel, provider={}, model={}", provider, props.getChatModel());

        return switch (provider) {
            case "ollama" -> OllamaChatModel.builder()
                    .baseUrl(props.getBaseUrl())
                    .modelName(props.getChatModel())
                    .temperature(props.getTemperature())
                    .timeout(props.getTimeout())
                    .build();
            default -> OpenAiChatModel.builder()
                    .apiKey(props.getApiKey())
                    .baseUrl(props.getBaseUrl())
                    .modelName(props.getChatModel())
                    .temperature(props.getTemperature())
                    .timeout(props.getTimeout())
                    .logRequests(props.isLogRequests())
                    .logResponses(props.isLogResponses())
                    .build();
        };
    }

    /**
     * 创建流式对话模型。
     *
     * <p>注意 LangChain4j 中流式与同步是<b>两个不同的接口</b>（{@link StreamingChatModel} 不继承 {@link ChatModel}），
     * 想做流式输出就必须单独建一个模型实例，这一点和 Spring AI 的 {@code stream()} 调用方式不同。
     */
    @Bean
    public StreamingChatModel streamingChatModel(LangChain4jProperties props) {
        String provider = props.getProvider() == null ? "openai" : props.getProvider().toLowerCase();

        return switch (provider) {
            case "ollama" -> OllamaStreamingChatModel.builder()
                    .baseUrl(props.getBaseUrl())
                    .modelName(props.getChatModel())
                    .temperature(props.getTemperature())
                    .timeout(props.getTimeout())
                    .build();
            default -> OpenAiStreamingChatModel.builder()
                    .apiKey(props.getApiKey())
                    .baseUrl(props.getBaseUrl())
                    .modelName(props.getChatModel())
                    .temperature(props.getTemperature())
                    .timeout(props.getTimeout())
                    .logRequests(props.isLogRequests())
                    .build();
        };
    }

    /**
     * 向量模型，供 RAG 示例使用。
     *
     * <p>如果当前后端没有可用的 embedding 模型，可以把 RAG 示例关掉
     * （参见 {@code langchain4j.rag.enabled}），不影响其它示例启动。
     */
    @Bean
    public EmbeddingModel embeddingModel(LangChain4jProperties props) {
        String provider = props.getProvider() == null ? "openai" : props.getProvider().toLowerCase();

        return switch (provider) {
            case "ollama" -> OllamaEmbeddingModel.builder()
                    .baseUrl(props.getBaseUrl())
                    .modelName(props.getEmbeddingModel())
                    .timeout(props.getTimeout())
                    .build();
            default -> OpenAiEmbeddingModel.builder()
                    .apiKey(props.getApiKey())
                    .baseUrl(props.getBaseUrl())
                    .modelName(props.getEmbeddingModel())
                    .timeout(props.getTimeout())
                    .build();
        };
    }

}
