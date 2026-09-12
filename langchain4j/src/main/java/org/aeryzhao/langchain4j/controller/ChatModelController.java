package org.aeryzhao.langchain4j.controller;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 示例 1：最小可用对话 —— {@link ChatModel} 的三种调用姿势。
 *
 * <p>LangChain4j 的 {@link ChatModel} 处于最底层，相当于 Spring AI 的 {@code ChatModel}：
 * 它只负责「发消息、收消息」，不管理记忆、不组装工具。理解它是理解后面所有高级能力的前提。
 *
 * <p>三个接口分别演示：
 * <ol>
 *   <li>{@code chat(String)}：最简写法，一行代码拿到字符串</li>
 *   <li>{@code chat(ChatMessage...)}：自己拼装 System/User 消息，拿到完整 {@link ChatResponse}</li>
 *   <li>{@code chat(List&lt;ChatMessage&gt;)}：多轮消息列表，注意此处<b>无状态</b>，历史需自己带上</li>
 * </ol>
 */
@RestController
@RequestMapping("/lc4j/chat")
@RequiredArgsConstructor
public class ChatModelController {

    private final ChatModel chatModel;

    /**
     * 最简调用：字符串进、字符串出。
     *
     * <p>GET /lc4j/chat/simple?message=你好
     */
    @GetMapping("/simple")
    public Map<String, Object> simple(@RequestParam(defaultValue = "用一句话介绍你自己") String message) {
        // chat(String) 是便捷方法，内部等价于 chat(UserMessage.from(text))
        String answer = chatModel.chat(message);
        return Map.of("question", message, "answer", answer);
    }

    /**
     * 获取完整响应对象：除了文本，还能拿到 Token 用量、结束原因等元数据。
     *
     * <p>GET /lc4j/chat/detailed?message=你好
     */
    @GetMapping("/detailed")
    public Map<String, Object> detailed(@RequestParam(defaultValue = "用一句话介绍你自己") String message) {
        ChatResponse response = chatModel.chat(UserMessage.from(message));
        AiMessage aiMessage = response.aiMessage();
        TokenUsage usage = response.tokenUsage();

        return Map.of(
                "answer", aiMessage.text(),
                "finishReason", String.valueOf(response.finishReason()),
                "inputTokens", usage == null ? 0 : usage.inputTokenCount(),
                "outputTokens", usage == null ? 0 : usage.outputTokenCount(),
                "totalTokens", usage == null ? 0 : usage.totalTokenCount()
        );
    }

    /**
     * 自己拼装多轮消息：System 定角色，User/AI 交替构成历史。
     *
     * <p>关键点：{@link ChatModel} 是<b>无状态</b>的，它不会记住上一轮说了什么。
     * 这里的 "历史" 是硬编码进去的，真实场景要用「聊天记忆」来维护（见 {@code ChatMemoryController}）。
     *
     * <p>GET /lc4j/chat/multi-turn
     */
    @GetMapping("/multi-turn")
    public Map<String, Object> multiTurn() {
        List<ChatMessage> messages = List.of(
                SystemMessage.from("你是一个严谨的数学老师，回答只给结论和一行算式。"),
                UserMessage.from("3 的平方是多少？"),
                AiMessage.from("9。算式：3 × 3 = 9"),
                UserMessage.from("那再加 1 呢？")   // 模型能理解 "那" 指的是上一轮的 9
        );

        ChatResponse response = chatModel.chat(messages);
        return Map.of(
                "messages", messages.stream().map(m -> m.type().name()).toList(),
                "answer", response.aiMessage().text()
        );
    }

}
