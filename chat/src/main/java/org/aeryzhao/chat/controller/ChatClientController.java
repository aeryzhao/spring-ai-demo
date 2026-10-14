package org.aeryzhao.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author zhaoxg
 * @date 2026/1/19 17:01
 */
@Tag(name = "ChatClient 调用", description = "使用 ChatClient 门面调用 DeepSeek 模型：普通对话、带元数据的完整响应，以及 SSE 流式输出")
@RequestMapping("/ai/chat-client")
@RestController
public class ChatClientController {
    private final ChatClient chatClient;

    public ChatClientController(OpenAiChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Operation(summary = "生成内容",
            description = """
                    通过 ChatClient 发起一次同步对话，直接返回模型回答的纯文本内容。

                    适用于只关心最终答案、不需要 token 用量等元数据的场景；如果还需要元数据请使用 `/ai/chat-client/chatResponse`。\
                    该接口会真实调用配置的 DeepSeek 模型，需要可用的 api-key 与网络。
                    """)
    @GetMapping("/content")
    String content(@Parameter(description = "用户输入的问题或指令，默认值：讲个笑话", example = "用一句话介绍 Spring AI")
                   @RequestParam(value = "userInput", defaultValue = "讲个笑话") String userInput) {
        return this.chatClient.prompt()
                .user(userInput)
                .call()
                .content();
    }

    @Operation(summary = "生成内容，元数据返回",
            description = """
                    与 `/ai/content` 相同的同步对话，但返回完整的 ChatResponse 对象（含生成结果与元数据）。

                    返回体为 Spring AI 的 `ChatResponse`，包含 generations（回答内容、结束原因）、\
                    metadata（模型名、token 用量等），字段结构以实际响应为准。
                    """)
    @GetMapping("/chatResponse")
    ChatResponse chatResponse(@Parameter(description = "用户输入的问题或指令，默认值：讲个笑话", example = "介绍一下你自己")
                              @RequestParam(value = "userInput", defaultValue = "讲个笑话") String userInput) {
        return this.chatClient.prompt()
                .user(userInput)
                .call()
                .chatResponse();
    }

    @Operation(summary = "生成内容流式返回",
            description = """
                    以 SSE（`text/event-stream`）方式逐段返回模型生成的内容，返回类型为 `Flux<String>`，\
                    每个数据帧是增量文本。

                    **注意**：Swagger UI 对 SSE 流式响应的支持有限（通常会在流结束前一直等待、且不好观察增量过程），\
                    建议使用命令行体验：

                    ```bash
                    curl -N "http://localhost:8096/ai/chat-client/stream?userInput=讲个笑话"
                    ```

                    或在浏览器中直接打开该 URL 观察数据逐段输出。
                    """)
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> stream(@Parameter(description = "用户输入的问题或指令，默认值：讲个笑话", example = "用三句话讲讲 JVM 内存结构")
                        @RequestParam(value = "userInput", defaultValue = "讲个笑话") String userInput) {
        return this.chatClient.prompt()
                .user(userInput)
                .stream()
                .content();
    }
}
