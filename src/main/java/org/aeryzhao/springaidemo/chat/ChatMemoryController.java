package org.aeryzhao.springaidemo.chat;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhaoxg
 * @date 2026/3/17 21:21
 */
@RequestMapping("/ai/chat-memory")
@RestController
public class ChatMemoryController {

    private static final String DEFAULT_SYSTEM_PROMPT = "你是一名支持多轮上下文的 AI 助手，请结合历史对话简洁回答。";

    private final ChatClient chatClient;

    public ChatMemoryController(OpenAiChatModel chatModel) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();

        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Operation(summary = "基于内存仓库的聊天记忆示例")
    @GetMapping("/in-memory")
    String inMemoryChat(@RequestParam(value = "conversationId", defaultValue = "demo-session") String conversationId,
                        @RequestParam(value = "userInput", defaultValue = "我叫小明，请记住我的名字") String userInput) {
        return this.chatClient.prompt()
                .system(DEFAULT_SYSTEM_PROMPT)
                .user(userInput)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
