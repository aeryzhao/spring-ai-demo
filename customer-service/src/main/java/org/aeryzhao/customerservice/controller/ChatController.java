package org.aeryzhao.customerservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.aeryzhao.customerservice.dto.ChatAnswer;
import org.aeryzhao.customerservice.dto.ChatRequest;
import org.aeryzhao.customerservice.service.CustomerServiceAgent;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * 客服对话入口：同步一次问答，或 SSE 流式问答。
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@Tag(name = "客服对话", description = "智能客服的同步 / 流式问答接口，返回答案、引用来源与工具调用轨迹")
public class ChatController {

    private static final long SSE_TIMEOUT_MS = Duration.ofMinutes(3).toMillis();

    private final CustomerServiceAgent agent;

    public ChatController(CustomerServiceAgent agent) {
        this.agent = agent;
    }

    @Operation(summary = "同步问答",
            description = """
                    一次请求拿到完整回答。响应里的 `sources` 是本次命中的知识片段（带来源链接），\
                    `toolCalls` 是模型这次调用过的业务工具，便于验证「没有编造订单信息」。
                    """)
    @PostMapping
    public ChatAnswer chat(@Valid @RequestBody ChatRequest request) {
        CustomerServiceAgent.Prepared prepared = agent.prepare(request.message(), request.sessionId());
        ChatAnswer answer = agent.answer(prepared);
        log.info("会话 {} 回答完成，模式 {}，引用 {} 条，工具 {} 次",
                answer.sessionId(), answer.retrievalMode(),
                answer.sources().size(), answer.toolCalls().size());
        return answer;
    }

    @Operation(summary = "流式问答（SSE）",
            description = """
                    以 `text/event-stream` 返回三类事件：

                    - `meta`：会话 id、引用来源、检索模式（首帧）
                    - `token`：增量文本
                    - `done`：结束，附带本次调用的工具列表

                    浏览器可用 `EventSource` 订阅；命令行用 `curl -N` 观察。
                    """)
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("message") String message,
                             @RequestParam(value = "sessionId", required = false) String sessionId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        try {
            CustomerServiceAgent.Prepared prepared = agent.prepare(message, sessionId);
            emitter.send(SseEmitter.event()
                    .name("meta")
                    .data(Map.of(
                            "sessionId", prepared.sessionId(),
                            "retrievalMode", prepared.retrievalMode(),
                            "sources", agent.toSources(prepared.chunks())),
                            MediaType.APPLICATION_JSON));

            Flux<String> tokens = agent.stream(prepared);
            Disposable subscription = tokens.subscribe(
                    token -> sendQuietly(emitter, SseEmitter.event().name("token").data(token)),
                    error -> {
                        log.warn("流式回答失败：{}", error.getMessage());
                        sendQuietly(emitter, SseEmitter.event().name("error")
                                .data(Map.of("message", String.valueOf(error.getMessage())),
                                        MediaType.APPLICATION_JSON));
                        emitter.complete();
                    },
                    () -> {
                        sendQuietly(emitter, SseEmitter.event().name("done")
                                .data(Map.of("toolCalls", agent.drainToolCalls(prepared.sessionId())),
                                        MediaType.APPLICATION_JSON));
                        emitter.complete();
                    });

            emitter.onCompletion(subscription::dispose);
            emitter.onTimeout(subscription::dispose);
            emitter.onError(throwable -> subscription.dispose());
        } catch (Exception e) {
            log.error("流式问答初始化失败", e);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    private void sendQuietly(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE 发送失败（客户端可能已断开）：{}", e.getMessage());
        }
    }
}
