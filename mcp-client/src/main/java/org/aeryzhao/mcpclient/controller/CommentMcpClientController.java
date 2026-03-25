package org.aeryzhao.mcpclient.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 演示查看 MCP Client 当前可用工具信息。
 */
@RestController
@RequestMapping("/comments")
public class CommentMcpClientController {
    private final ChatClient chatClient;
    private final ToolCallbackProvider toolCallbackProvider;

    public CommentMcpClientController(OpenAiChatModel openAiChatModel, ToolCallbackProvider toolCallbackProvider) {
        this.toolCallbackProvider = toolCallbackProvider;
        this.chatClient = ChatClient.builder(openAiChatModel)
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    @GetMapping("/tools")
    public Map<String, Object> listTools(@RequestParam(defaultValue = "") String articleId,
                                         @RequestParam(defaultValue = "demo-user") String username,
                                         @RequestParam(defaultValue = "这是一条来自 mcp-client 的评论") String content) {
        String[] toolNames = Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .map(callback -> callback.getToolDefinition().name())
                .sorted()
                .toArray(String[]::new);

        return Map.of(
                "serverBaseUrl", "http://localhost:8081",
                "toolCount", toolNames.length,
                "tools", toolNames,
                "exampleSaveCommentArguments", Map.of(
                        "articleId", articleId,
                        "username", username,
                        "content", content
                ),
                "examplePrompt", buildPrompt(articleId, username, content, toolNames)
        );
    }

    private String buildPrompt(String articleId, String username, String content, String[] toolNames) {
        String joinedTools = Arrays.stream(toolNames).collect(Collectors.joining(", "));
        return "请调用 MCP 工具保存评论，参数为：articleId=" + articleId
                + ", username=" + username
                + ", content=" + content
                + "。保存后再查询该 articleId 的评论列表并汇总返回。当前可用工具：" + joinedTools;
    }

    @GetMapping("/sse/client")
    String useCommentMCP(@RequestParam("message") String message) {
        return this.chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
