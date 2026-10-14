package org.aeryzhao.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "聊天记忆（内存仓库）", description = "使用 Spring AI 内置 InMemoryChatMemoryRepository + MessageWindowChatMemory（窗口 20 条）实现多轮对话上下文")
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

    @Operation(summary = "基于内存仓库的聊天记忆示例",
            description = """
                    使用内置内存仓库（InMemoryChatMemoryRepository）保存会话上下文，配合 MessageWindowChatMemory（窗口 20 条）\
                    实现多轮对话：同一个 `conversationId` 下的历史消息会自动带入下一次请求。

                    **验证方式**：先用默认参数问「我叫小明，请记住我的名字」，再追加一句「我叫什么名字？」（保持\
                    `conversationId` 不变），模型应能答出「小明」。

                    **注意**：记忆保存在应用进程内存中，重启即丢失；该接口会真实调用 DeepSeek 模型。
                    """)
    @GetMapping("/in-memory")
    String inMemoryChat(@Parameter(description = "会话 ID，用于隔离不同对话的上下文；相同 ID 即共享历史记忆。默认值：demo-session",
                                 example = "demo-session-001")
                        @RequestParam(value = "conversationId", defaultValue = "demo-session") String conversationId,
                        @Parameter(description = "用户输入的问题或指令，默认值：我叫小明，请记住我的名字",
                                   example = "我叫小明，请记住我的名字")
                        @RequestParam(value = "userInput", defaultValue = "我叫小明，请记住我的名字") String userInput) {
        return this.chatClient.prompt()
                .system(DEFAULT_SYSTEM_PROMPT)
                .user(userInput)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
