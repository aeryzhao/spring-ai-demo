package org.aeryzhao.springaidemo.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * @author zhaoxg
 * @date 2025/2/19 19:56
 */
@RequestMapping("/ai/chat-model")
@RestController
public class ChatModelController {
    private final ChatModel chatModel;

    public ChatModelController(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Operation(summary = "生成内容")
    @GetMapping("/call")
    String call(@RequestParam(value = "message", defaultValue = "讲个笑话") String message) {
        return this.chatModel.call(message);
    }

    @Operation(summary = "生成内容流式返回")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<ChatResponse> stream(@RequestParam(value = "message", defaultValue = "讲个笑话") String message) {
        Prompt prompt = new Prompt(new UserMessage(message));
        return this.chatModel.stream(prompt);
    }

}