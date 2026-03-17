package org.aeryzhao.springaidemo.chat;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author zhaoxg
 * @date 2026/1/19 17:01
 */
@RequestMapping("/ai/chat-client")
@RestController
public class ChatClientController {
    private final ChatClient chatClient;

    public ChatClientController(OpenAiChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Operation(summary = "生成内容")
    @GetMapping("/content")
    String content(@RequestParam(value = "userInput", defaultValue = "讲个笑话") String userInput) {
        return this.chatClient.prompt()
                .user(userInput)
                .call()
                .content();
    }

    @Operation(summary = "生成内容，元数据返回")
    @GetMapping("/chatResponse")
    ChatResponse chatResponse(@RequestParam(value = "userInput", defaultValue = "讲个笑话") String userInput) {
        return this.chatClient.prompt()
                .user(userInput)
                .call()
                .chatResponse();
    }

    @Operation(summary = "生成内容流式返回")
    @GetMapping("/stream")
    Flux<String> stream(@RequestParam(value = "userInput", defaultValue = "讲个笑话") String userInput) {
        return this.chatClient.prompt()
                .user(userInput)
                .stream()
                .content();
    }
}
