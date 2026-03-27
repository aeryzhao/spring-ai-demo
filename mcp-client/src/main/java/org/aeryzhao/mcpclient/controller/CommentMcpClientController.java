package org.aeryzhao.mcpclient.controller;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 演示查看 MCP Client 当前可用工具信息，以及 resource / prompt 的真实调用。
 */
@RestController
@RequestMapping("/comments")
public class CommentMcpClientController {
    private static final String RESOURCE_URI = "resource://comment/guideline";
    private static final String PROMPT_NAME = "comment-summary-prompt";

    private final ChatClient chatClient;
    private final ToolCallbackProvider toolCallbackProvider;
    private final List<McpSyncClient> mcpSyncClients;

    public CommentMcpClientController(OpenAiChatModel openAiChatModel,
                                      ToolCallbackProvider toolCallbackProvider,
                                      List<McpSyncClient> mcpSyncClients) {
        this.toolCallbackProvider = toolCallbackProvider;
        this.mcpSyncClients = mcpSyncClients;
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
                "examplePrompt", buildToolPrompt(articleId, username, content, toolNames),
                "resourceEndpoint", "/comments/resource",
                "promptEndpoint", "/comments/prompt",
                "demoEndpoint", "/comments/demo"
        );
    }

    @GetMapping("/resource")
    public Map<String, Object> readCommentGuidelineResource() {
        McpSyncClient client = getClient();
        McpSchema.ReadResourceResult result = client.readResource(new McpSchema.ReadResourceRequest(RESOURCE_URI));

        return Map.of(
                "serverInfo", client.getServerInfo(),
                "resourceUri", RESOURCE_URI,
                "contents", result.contents().stream().map(this::convertResourceContent).toList()
        );
    }

    @GetMapping("/prompt")
    public Map<String, Object> getCommentSummaryPrompt(@RequestParam(defaultValue = "article-001") String articleId,
                                                       @RequestParam(defaultValue = "demo-user") String username,
                                                       @RequestParam(defaultValue = "这是一条来自 mcp-client 的评论") String content) {
        McpSyncClient client = getClient();
        McpSchema.GetPromptResult result = client.getPrompt(new McpSchema.GetPromptRequest(
                PROMPT_NAME,
                Map.of(
                        "articleId", articleId,
                        "username", username,
                        "content", content
                )
        ));
        return Map.of(
                "serverInfo", client.getServerInfo(),
                "promptName", PROMPT_NAME,
                "messages", result.messages().stream().map(this::convertPromptMessage).toList()
        );
    }

    @GetMapping("/demo")
    public Map<String, Object> runCommentDemo(@RequestParam(defaultValue = "article-001") String articleId,
                                              @RequestParam(defaultValue = "demo-user") String username,
                                              @RequestParam(defaultValue = "这是一条来自 mcp-client 的评论") String content) {
        Map<String, Object> resource = readCommentGuidelineResource();
        Map<String, Object> prompt = getCommentSummaryPrompt(articleId, username, content);
        String promptText = extractPromptText(prompt);
        String answer = this.chatClient.prompt()
                .user(promptText)
                .call()
                .content();

        return Map.of(
                "resource", resource,
                "prompt", prompt,
                "modelAnswer", answer
        );
    }

    private String buildToolPrompt(String articleId, String username, String content, String[] toolNames) {
        String joinedTools = Arrays.stream(toolNames).collect(Collectors.joining(", "));
        return "请调用 MCP 工具保存评论，参数为：articleId=" + articleId
                + ", username=" + username
                + ", content=" + content
                + "。保存后再查询该 articleId 的评论列表并汇总返回。当前可用工具：" + joinedTools;
    }

    private McpSyncClient getClient() {
        if (mcpSyncClients == null || mcpSyncClients.isEmpty()) {
            throw new IllegalStateException("当前没有可用的 MCP Sync Client 连接");
        }
        return mcpSyncClients.getFirst();
    }

    private Map<String, Object> convertResourceContent(McpSchema.ResourceContents resourceContents) {
        if (resourceContents instanceof McpSchema.TextResourceContents textResourceContents) {
            return Map.of(
                    "uri", textResourceContents.uri(),
                    "mimeType", textResourceContents.mimeType(),
                    "text", textResourceContents.text()
            );
        }
        return Map.of("raw", resourceContents.toString());
    }

    private Map<String, Object> convertPromptMessage(McpSchema.PromptMessage promptMessage) {
        return Map.of(
                "role", promptMessage.role().name(),
                "content", convertPromptContent(promptMessage.content())
        );
    }

    private Object convertPromptContent(McpSchema.Content content) {
        if (content instanceof McpSchema.TextContent textContent) {
            return textContent.text();
        }
        if (content instanceof McpSchema.EmbeddedResource embeddedResource) {
            return convertResourceContent(embeddedResource.resource());
        }
        return content.toString();
    }

    private String extractPromptText(Map<String, Object> promptResult) {
        Object messages = promptResult.get("messages");
        if (messages instanceof List<?> messageList && !messageList.isEmpty()) {
            Object first = messageList.getFirst();
            if (first instanceof Map<?, ?> messageMap) {
                Object content = messageMap.get("content");
                if (content instanceof String text && !text.isBlank()) {
                    return text;
                }
            }
        }
        throw new IllegalStateException("未能从 MCP prompt 结果中提取文本内容");
    }

    @GetMapping("/mcp/list")
    public Map<String, Object> listResources() {
        McpSyncClient client = getClient();
        McpSchema.ListResourcesResult listResourcesResult = client.listResources();
        McpSchema.ListPromptsResult listPromptsResult = client.listPrompts();
        McpSchema.ListToolsResult listToolsResult = client.listTools();
        return Map.of(
                "serverInfo", client.getServerInfo(),
                "resources", listResourcesResult,
                "prompts", listPromptsResult,
                "tools", listToolsResult
        );
    }

    @GetMapping("/sse/client")
    String useCommentMCP(@RequestParam("message") String message) {
        return this.chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
