package org.aeryzhao.langchain4j.controller;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * 示例 2：流式输出 —— {@link StreamingChatModel} + SSE。
 *
 * <p>和 Spring AI 的 {@code Flux<String>} 不同，LangChain4j 的核心是<b>回调式</b>的：
 * 你调用 {@code chat(...)} 并传入一个 {@link StreamingChatResponseHandler}，
 * 框架在收到每个 token 时回调 {@code onPartialResponse}，结束时回调 {@code onCompleteResponse}。
 *
 * <p>这里用 Spring MVC 的 {@link SseEmitter} 把回调桥接成浏览器可见的 SSE 流，
 * 这样用 curl 就能直接看到「一个字一个字吐出来」的效果。
 */
@Tag(name = "2. 流式输出（SSE）", description = """
        LangChain4j 的流式核心是**回调式**的：调用 `chat(...)` 时传入 `StreamingChatResponseHandler`，
        框架在收到每个 token 时回调 `onPartialResponse`，结束时回调 `onCompleteResponse`
        （对比 Spring AI 的 `Flux<String>` 响应式写法）。

        本接口用 Spring MVC 的 `SseEmitter` 把回调桥接成 SSE 流，用 `curl -N` 即可看到逐字输出效果。

        **SSE 事件约定**
        | event | data |
        | --- | --- |
        | `token` | 单个增量 token 文本 |
        | `done` | JSON：`{ "text": 完整文本, "totalTokens": N }` |
        | `error` | 错误信息字符串 |

        超时时间 120 秒，客户端断开连接属正常现象。

        > **Swagger UI 支持有限**：Swagger UI 对 `text/event-stream` 的流式渲染不完整，
        > 建议用命令行体验（见接口详情）。
        """)
@Slf4j
@RestController
@RequestMapping("/lc4j/stream")
@RequiredArgsConstructor
public class StreamingChatController {

    private final StreamingChatModel streamingChatModel;

    /**
     * 流式对话，返回 SSE。
     *
     * <p>命令行体验：{@code curl -N "http://localhost:8097/lc4j/stream?message=写一首五言绝句"}
     */
    @Operation(summary = "流式对话（SSE 逐字返回）", description = """
            以 SSE（`text/event-stream`）方式流式返回模型输出，返回类型为 `SseEmitter`。

            **命令行体验（推荐）**：

            ```bash
            curl -N "http://localhost:8097/lc4j/stream?message=写一首五言绝句"
            ```

            `-N` 关闭 curl 输出缓冲，才能实时看到逐字吐出的效果。

            事件流：多个 `event: token`（增量文本）→ 一个 `event: done`（完整文本 + Token 总量）；
            出错时推送 `event: error`。服务端超时时间 120 秒。

            > **Swagger UI 支持有限**：Swagger UI 无法完整渲染 SSE 流式响应，
            > 在本页点击 Execute 可能只能看到挂起或截断的结果，请以 `curl -N` 为准。
            """)
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @Parameter(description = "要生成的提示词", example = "写一首五言绝句")
            @RequestParam(defaultValue = "用 30 个字介绍一下 LangChain4j") String message) {
        // 超时时间给长一点，否则慢模型会被提前掐断
        SseEmitter emitter = new SseEmitter(120_000L);

        StringBuilder fullText = new StringBuilder();

        streamingChatModel.chat(message, new StreamingChatResponseHandler() {

            /** 每收到一个 token 就推送一次 */
            @Override
            public void onPartialResponse(String partialResponse) {
                fullText.append(partialResponse);
                try {
                    emitter.send(SseEmitter.event().name("token").data(partialResponse));
                } catch (IOException e) {
                    // 客户端断开连接是正常现象，直接结束即可
                    log.debug("SSE 客户端已断开: {}", e.getMessage());
                    emitter.complete();
                }
            }

            /** 全部生成完毕：推送完整文本 + Token 用量，然后关闭流 */
            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    emitter.send(SseEmitter.event().name("done").data(java.util.Map.of(
                            "text", fullText.toString(),
                            "totalTokens", completeResponse.tokenUsage() == null
                                    ? 0 : completeResponse.tokenUsage().totalTokenCount()
                    )));
                } catch (IOException e) {
                    log.debug("SSE 完成事件推送失败: {}", e.getMessage());
                }
                emitter.complete();
            }

            /** 出错时把错误信息推给前端，避免前端一直挂起 */
            @Override
            public void onError(Throwable error) {
                log.warn("流式对话失败", error);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data(String.valueOf(error.getMessage())));
                } catch (IOException ignored) {
                    // 连接已断开，无需再处理
                }
                emitter.completeWithError(error);
            }
        });

        return emitter;
    }

}
