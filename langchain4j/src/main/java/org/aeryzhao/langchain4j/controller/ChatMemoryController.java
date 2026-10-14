package org.aeryzhao.langchain4j.controller;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.aeryzhao.langchain4j.assistant.ChatMemoryAssistant;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 示例 5：聊天记忆 —— 多会话隔离与手动管理。
 *
 * <p>LangChain4j 的记忆设计分三层，理解这三层就能完全掌控上下文：
 * <ol>
 *   <li>{@link ChatMemoryStore}：<b>存哪里</b> —— 按 ID 存取消息列表（内存 / Redis / DB）</li>
 *   <li>{@link ChatMemory}：<b>存多少</b> —— 截断策略（按条数 / 按 Token）</li>
 *   <li>{@link ChatMemoryProvider}：<b>存几份</b> —— 按 memoryId 懒加载，一个接口服务 N 个会话</li>
 * </ol>
 *
 * <p>关键体验：用同一个 memoryId 连续对话，模型能记住上文；换个 memoryId，上下文完全隔离。
 */
@Tag(name = "5. 聊天记忆（多会话隔离）",
        description = """
                LangChain4j 的记忆设计分三层，理解这三层就能完全掌控上下文：

                1. `ChatMemoryStore`：**存哪里** —— 按 ID 存取消息列表（内存 / Redis / DB）
                2. `ChatMemory`：**存多少** —— 截断策略（按条数 / 按 Token）
                3. `ChatMemoryProvider`：**存几份** —— 按 memoryId 懒加载，一个接口服务 N 个会话

                关键体验：用同一个 `memoryId` 连续对话，模型能记住上文；换个 `memoryId`，上下文完全隔离。

                推荐实验顺序：
                1. `GET /lc4j/memory/chat?memoryId=user-A&message=我叫小明，我喜欢喝美式`
                2. `GET /lc4j/memory/chat?memoryId=user-A&message=我喜欢喝什么？` → 能答出「美式」
                3. `GET /lc4j/memory/chat?memoryId=user-B&message=我喜欢喝什么？` → 完全不知道
                4. `GET /lc4j/memory/messages?memoryId=user-A` → 查看框架到底记了什么
                5. `DELETE /lc4j/memory/clear?memoryId=user-A` → 清空该会话
                """)
@RestController
@RequestMapping("/lc4j/memory")
@RequiredArgsConstructor
public class ChatMemoryController {

    private final ChatMemoryAssistant chatMemoryAssistant;
    private final ChatMemoryProvider chatMemoryProvider;
    private final ChatMemoryStore chatMemoryStore;

    /**
     * 带上记忆的多轮对话。
     *
     * <p>试一下：先用 user-A 说「我叫小明，我喜欢喝美式」，再用同一个 user-A 问「我喜欢喝什么？」
     * 最后换 user-B 问同样的问题，观察两者的差异。
     *
     * <p>GET /lc4j/memory/chat?memoryId=user-A&message=我叫小明，我喜欢喝美式
     */
    @Operation(summary = "带记忆的多轮对话",
            description = """
                    按 `memoryId` 维护独立会话上下文的对话接口，是体验「记忆」最直接的入口。

                    关键点：
                    - `memoryId` 有默认值 `user-A`，可用同一 ID 连续对话验证记忆生效；
                    - **`message` 无默认值，属于必填参数**（`required=true`），缺失时 Spring 返回 400。

                    推荐体验：先用 `user-A` 说「我叫小明，我喜欢喝美式」，再用同一个 `user-A` 问「我喜欢喝什么？」，
                    最后换 `user-B` 问同样的问题，观察两者的差异。

                    示例：`GET /lc4j/memory/chat?memoryId=user-A&message=我叫小明，我喜欢喝美式`

                    返回 `memoryId`、`question` 与 `answer`。
                    """)
    @GetMapping("/chat")
    public Map<String, Object> chat(
            @Parameter(description = "会话 ID，用于隔离不同用户的上下文", example = "user-A")
            @RequestParam(defaultValue = "user-A") String memoryId,
            @Parameter(description = "用户消息（必填，无默认值）", required = true,
                    example = "我叫小明，我喜欢喝美式")
            @RequestParam String message) {
        String answer = chatMemoryAssistant.chat(memoryId, message);
        return Map.of("memoryId", memoryId, "question", message, "answer", answer);
    }

    /**
     * 查看某个会话当前保存的原始消息列表 —— 直接看框架「记住了什么」，是排查记忆问题最有效的手段。
     *
     * <p>GET /lc4j/memory/messages?memoryId=user-A
     */
    @Operation(summary = "查看会话当前保存的原始消息列表",
            description = """
                    直接读取 `ChatMemoryStore` 中该 `memoryId` 的消息，看框架「到底记住了什么」——
                    排查记忆问题的第一手段。

                    返回 `memoryId`、`messageCount` 以及 `messages` 列表，
                    每条消息包含 `type`（SYSTEM / USER / AI / TOOL_EXECUTION_RESULT）与 `text`。

                    示例：`GET /lc4j/memory/messages?memoryId=user-A`
                    """)
    @GetMapping("/messages")
    public Map<String, Object> messages(
            @Parameter(description = "会话 ID", example = "user-A")
            @RequestParam(defaultValue = "user-A") String memoryId) {
        List<ChatMessage> messages = chatMemoryStore.getMessages(memoryId);
        return Map.of(
                "memoryId", memoryId,
                "messageCount", messages.size(),
                "messages", messages.stream().map(m -> Map.of(
                        "type", m.type().name(),
                        "text", extractText(m)
                )).toList()
        );
    }

    /**
     * 手动清空某个会话的记忆（等价于「新开会话」）。
     *
     * <p>DELETE /lc4j/memory/clear?memoryId=user-A
     */
    @Operation(summary = "清空某个会话的记忆（DELETE）",
            description = """
                    手动清空指定 `memoryId` 的记忆，等价于「新开会话」。
                    清空后模型将不再记得此前的任何对话内容。

                    注意：本接口使用 **HTTP DELETE** 方法，且带 query 参数 `memoryId`。

                    返回 `memoryId`、`cleared`（固定为 `true`）与 `remaining`（清空后剩余消息数，应为 0）。

                    示例：
                    ```bash
                    curl -X DELETE "http://localhost:8097/lc4j/memory/clear?memoryId=user-A"
                    ```
                    """)
    @DeleteMapping("/clear")
    public Map<String, Object> clear(
            @Parameter(description = "要清空的会话 ID", example = "user-A")
            @RequestParam(defaultValue = "user-A") String memoryId) {
        ChatMemory memory = chatMemoryProvider.get(memoryId);
        memory.clear();
        return Map.of("memoryId", memoryId, "cleared", true,
                "remaining", memory.messages().size());
    }

    /**
     * 不同类型消息的取文本方式不同，这里统一做一次兼容处理。
     */
    private String extractText(ChatMessage message) {
        return switch (message) {
            case dev.langchain4j.data.message.UserMessage m -> m.singleText();
            case dev.langchain4j.data.message.AiMessage m -> m.text();
            case dev.langchain4j.data.message.SystemMessage m -> m.text();
            case dev.langchain4j.data.message.ToolExecutionResultMessage m -> m.text();
            default -> message.toString();
        };
    }

}
