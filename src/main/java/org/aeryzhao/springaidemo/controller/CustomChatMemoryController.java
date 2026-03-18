package org.aeryzhao.springaidemo.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.aeryzhao.springaidemo.repository.ConcurrentMapChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 自定义聊天记忆仓库演示。
 *
 * @author zhaoxg
 * @date 2026/3/17 21:28
 */
@RequestMapping("/ai/chat-memory/custom")
@RestController
public class CustomChatMemoryController {

    private static final String DEFAULT_SYSTEM_PROMPT = "你是一名支持多轮上下文的 AI 助手，请基于历史对话准确回答。";

    private final ChatClient chatClient;
    private final ConcurrentMapChatMemoryRepository chatMemoryRepository;

    public CustomChatMemoryController(OpenAiChatModel chatModel) {
        this.chatMemoryRepository = new ConcurrentMapChatMemoryRepository();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(this.chatMemoryRepository)
                .maxMessages(20)
                .build();

        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Operation(summary = "使用自定义仓库进行多轮对话")
    @GetMapping("/chat")
    String chat(@RequestParam(value = "conversationId", defaultValue = "custom-session") String conversationId,
                @RequestParam(value = "userInput", defaultValue = "我最喜欢的编程语言是Java，请记住") String userInput) {
        return this.chatClient.prompt()
                .system(DEFAULT_SYSTEM_PROMPT)
                .user(userInput)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    @Operation(summary = "查看当前会话的记忆条数")
    @GetMapping("/memory-size")
    int memorySize(@RequestParam(value = "conversationId", defaultValue = "custom-session") String conversationId) {
        return this.chatMemoryRepository.findByConversationId(conversationId).size();
    }

    @Operation(summary = "清空指定会话的记忆")
    @GetMapping("/clear")
    String clear(@RequestParam(value = "conversationId", defaultValue = "custom-session") String conversationId) {
        this.chatMemoryRepository.deleteByConversationId(conversationId);
        return "已清空会话记忆: " + conversationId;
    }
}
