package org.aeryzhao.langchain4j.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LangChain4j 模块的模型配置，统一前缀 {@code langchain4j}。
 *
 * <p>配置示例（{@code application.yaml}）：
 * <pre>
 * langchain4j:
 *   provider: openai          # openai = OpenAI 兼容接口（DeepSeek 等）；ollama = 本地 Ollama
 *   api-key: ${deepseek.api-key}
 *   base-url: ${deepseek.base-url}
 *   chat-model: ${deepseek.chat-model}
 *   embedding-model: text-embedding-v3
 *   temperature: 0.7
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "langchain4j")
public class LangChain4jProperties {

    /** 模型提供方：{@code openai}（OpenAI 兼容 HTTP 接口）或 {@code ollama}（本地模型）。 */
    private String provider = "openai";

    /** OpenAI 兼容接口的 API Key；Ollama provider 下可为空。 */
    private String apiKey;

    /** OpenAI 兼容接口的 base-url，例如 {@code https://api.deepseek.com/v1}。 */
    private String baseUrl = "https://api.openai.com/v1";

    /** 对话模型名称，例如 {@code deepseek-chat}。 */
    private String chatModel = "gpt-4o-mini";

    /** 向量模型名称，仅 RAG 示例使用。 */
    private String embeddingModel = "text-embedding-3-small";

    /** 采样温度。 */
    private Double temperature = 0.7;

    /** 单次请求超时时间。 */
    private java.time.Duration timeout = java.time.Duration.ofSeconds(60);

    /** 是否打印请求报文，排查问题时打开。 */
    private boolean logRequests = false;

    /** 是否打印响应报文。 */
    private boolean logResponses = false;

}
