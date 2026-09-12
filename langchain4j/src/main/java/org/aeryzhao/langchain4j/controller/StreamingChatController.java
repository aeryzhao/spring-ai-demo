package org.aeryzhao.langchain4j.controller;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
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
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(defaultValue = "用 30 个字介绍一下 LangChain4j") String message) {
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
