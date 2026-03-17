package org.aeryzhao.springaidemo.chat;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP Client 示例控制器。
 *
 * @author zhaoxg
 * @date 2026/3/17 21:52
 */
@RequestMapping("/ai/mcp")
@RestController
public class McpClientController {

    private static final String DEFAULT_SYSTEM_PROMPT = "你是一名支持 MCP 工具调用的助手。遇到时间查询类问题时，优先调用可用 MCP 工具，并用简体中文整理答案。";

    private final ChatClient chatClient;

    public McpClientController(OpenAiChatModel chatModel, SyncMcpToolCallbackProvider mcpToolCallbackProvider) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultToolCallbacks(mcpToolCallbackProvider)
                .build();
    }

    @Operation(summary = "通过 MCP Client 调用外部工具示例")
    @GetMapping("/time")
    String currentTime(@RequestParam(value = "city", defaultValue = "上海") String city) {
        return this.chatClient.prompt()
                .system(DEFAULT_SYSTEM_PROMPT)
                .user(u -> u.text("请调用可用的 MCP 时间工具，告诉我{city}相关的当前时间参考信息，并明确写出工具返回的 UTC 时间。")
                        .param("city", city))
                .call()
                .content();
    }
}
