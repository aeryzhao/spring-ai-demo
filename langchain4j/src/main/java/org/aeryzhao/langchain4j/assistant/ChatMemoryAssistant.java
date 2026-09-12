package org.aeryzhao.langchain4j.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 「有记忆的助手」：配合 {@code chatMemoryProvider} 使用，每个会话 ID 一份独立记忆。
 *
 * <p>关键点是 {@link MemoryId} 注解：它标记的参数<b>不会</b>发给模型，
 * 而是被 LangChain4j 用来作为「取哪一份记忆」的 key —— 一个接口就能服务成百上千个用户会话。
 */
public interface ChatMemoryAssistant {

    /**
     * @param memoryId    会话标识（如用户 ID、会话 ID），同一个 ID 共享上下文
     * @param userMessage 用户本轮输入
     */
    @SystemMessage("你是一个贴心的私人助理，回答简洁友好，会主动记住用户之前提到的偏好。")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);

}
