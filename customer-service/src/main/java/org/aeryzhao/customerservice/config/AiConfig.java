package org.aeryzhao.customerservice.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 会话记忆配置：每个 sessionId 保留最近 N 条消息，保证多轮追问「那这个订单呢」时上下文不丢。
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatMemory chatMemory(CustomerServiceProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(properties.memoryWindow())
                .build();
    }
}
