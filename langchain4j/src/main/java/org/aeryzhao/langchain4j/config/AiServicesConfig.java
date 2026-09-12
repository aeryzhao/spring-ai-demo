package org.aeryzhao.langchain4j.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.aeryzhao.langchain4j.assistant.Assistant;
import org.aeryzhao.langchain4j.assistant.ChatMemoryAssistant;
import org.aeryzhao.langchain4j.assistant.StructuredOutputAssistant;
import org.aeryzhao.langchain4j.assistant.TravelAssistant;
import org.aeryzhao.langchain4j.tool.TravelTools;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AiServices 装配：把「接口」变成可直接注入的 Bean。
 *
 * <p>这是整个模块最核心的一个类。LangChain4j 用<b>动态代理</b>在运行期生成接口实现，
 * 你只需要通过 builder 告诉它：用哪个模型、要不要记忆、挂哪些工具。
 *
 * <p>每个 Bean 展示一个不同的能力组合：
 * <ul>
 *   <li>{@link Assistant}：纯对话 + 提示词模板</li>
 *   <li>{@link StructuredOutputAssistant}：结构化输出（返回 POJO / Result）</li>
 *   <li>{@link ChatMemoryAssistant}：多会话记忆（按 memoryId 隔离）</li>
 *   <li>{@link TravelAssistant}：工具调用</li>
 * </ul>
 */
@Configuration
public class AiServicesConfig {

    /**
     * 最基础的助手：只绑定模型。
     *
     * <p>{@code AiServices.create(接口, 模型)} 是一行代码的简写形式。
     */
    @Bean
    public Assistant assistant(ChatModel chatModel) {
        return AiServices.create(Assistant.class, chatModel);
    }

    /**
     * 结构化输出助手：无需额外配置，返回类型是 POJO 时框架自动处理 Schema 与反序列化。
     */
    @Bean
    public StructuredOutputAssistant structuredOutputAssistant(ChatModel chatModel) {
        return AiServices.builder(StructuredOutputAssistant.class)
                .chatModel(chatModel)
                .build();
    }

    /**
     * 记忆存储：负责「按 ID 存取消息列表」。
     *
     * <p>{@link InMemoryChatMemoryStore} 存内存，重启即丢；生产环境可换成 Redis / 数据库实现，
     * 只要实现 {@link ChatMemoryStore} 接口即可（三个方法：查、存、删）。
     */
    @Bean
    public ChatMemoryStore chatMemoryStore() {
        return new InMemoryChatMemoryStore();
    }

    /**
     * 记忆策略：每个 memoryId 保留最近 N 条消息。
     *
     * <p>{@link MessageWindowChatMemory} 是「按条数」截断；
     * 如果更在意成本控制，可以换 {@code TokenWindowChatMemory}（按 Token 数截断）。
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore chatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)          // 每个会话最多保留 20 条消息
                .chatMemoryStore(chatMemoryStore)
                .build();
    }

    /**
     * 多会话记忆助手：每个 memoryId 一份独立记忆，由 {@link ChatMemoryProvider} 懒加载创建。
     */
    @Bean
    public ChatMemoryAssistant chatMemoryAssistant(ChatModel chatModel,
                                                   ChatMemoryProvider chatMemoryProvider) {
        return AiServices.builder(ChatMemoryAssistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }

    /**
     * 工具调用助手：把 {@link TravelTools} 交给模型使用。
     *
     * <p>可以继续追加：{@code .maxToolCallingRoundTrips(5)} 限制工具往返轮数，
     * {@code .executeToolsConcurrently()} 开启并行工具执行，
     * {@code .beforeToolExecution(...)} / {@code .afterToolExecution(...)} 做调用日志与埋点。
     */
    @Bean
    public TravelAssistant travelAssistant(ChatModel chatModel, TravelTools travelTools) {
        return AiServices.builder(TravelAssistant.class)
                .chatModel(chatModel)
                .tools(travelTools)                       // 注册工具，可传多个对象
                .maxToolCallingRoundTrips(5)              // 防止模型无限调工具
                .beforeToolExecution(execution ->
                        org.slf4j.LoggerFactory.getLogger(TravelAssistant.class)
                                .info("模型请求调用工具: {}", execution.request().name()))
                .build();
    }

}
