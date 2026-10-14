package org.aeryzhao.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "ChatModel 调用", description = "绕开 ChatClient 门面直接使用 ChatModel 的最底层调用方式：同步 call 与 SSE 流式 stream，便于对照 ChatClient 的能力差异")
@RequestMapping("/ai/chat-model")
@RestController
public class ChatModelController {
    private final ChatModel chatModel;

    public ChatModelController(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Operation(summary = "生成内容",
            description = """
                    直接调用 `ChatModel#call(String)` 发起同步对话，返回模型回答的纯文本。

                    这是最底层的调用方式，不带 ChatClient 的 Advisor、记忆等能力；\
                    适合用来验证模型通路是否正常。会真实调用配置的 DeepSeek 模型。
                    """)
    @GetMapping("/call")
    String call(@Parameter(description = "发送给模型的用户消息，默认值：讲个笑话", example = "你好，请自我介绍一下")
                @RequestParam(value = "message", defaultValue = "讲个笑话") String message) {
        return this.chatModel.call(message);
    }

    @Operation(summary = "生成内容流式返回",
            description = """
                    直接调用 `ChatModel#stream(Prompt)`，以 SSE（`text/event-stream`）方式返回 `Flux<ChatResponse>`，\
                    每个数据帧都是一个包含增量内容的完整 ChatResponse 对象（因而单帧信息比 `/ai/chat-client/stream` 更丰富）。

                    **注意**：Swagger UI 对 SSE 流式响应的支持有限，建议使用命令行体验：

                    ```bash
                    curl -N "http://localhost:8096/ai/chat-model/stream?message=讲个笑话"
                    ```

                    或在浏览器中直接打开该 URL 观察数据逐段输出。
                    """)
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<ChatResponse> stream(@Parameter(description = "发送给模型的用户消息，默认值：讲个笑话", example = "用三句话讲讲响应式编程")
                              @RequestParam(value = "message", defaultValue = "讲个笑话") String message) {
        Prompt prompt = new Prompt(new UserMessage(message));
        return this.chatModel.stream(prompt);
    }

}