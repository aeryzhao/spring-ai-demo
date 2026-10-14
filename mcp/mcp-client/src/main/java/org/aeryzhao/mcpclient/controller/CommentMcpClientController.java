package org.aeryzhao.mcpclient.controller;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "MCP 客户端演示", description = "演示 MCP Client 从本地 MCP Server 获取工具列表，并真实调用 resource / prompt，"
        + "以及把 MCP 工具接入 Spring AI ChatClient 进行对话。所有接口均需本地 MCP Server 已启动（http://localhost:8081/mcp），"
        + "否则会因无可用 MCP 连接而报错。")
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
                .defaultTools(toolCallbackProvider)
                .build();
    }

    @GetMapping("/tools")
    @Operation(summary = "列出当前可用的 MCP 工具",
            description = "从 Spring AI 的 ToolCallbackProvider 中读取已由 MCP Client 注册的工具定义，按工具名排序后返回工具数量与名称列表；"
                    + "同时根据传入的 articleId / username / content 拼装一个调用 saveComment 工具的示例参数和示例提示词（examplePrompt），"
                    + "方便直接复制去调用 /comments/sse/client。本接口只读取本地已注册的工具信息，不会真正调用 MCP Server。")
    public Map<String, Object> listTools(
            @Parameter(description = "文章 ID，用于生成示例工具参数。默认空字符串；传空时示例参数中的 articleId 也是空串",
                    example = "article-001")
            @RequestParam(defaultValue = "") String articleId,
            @Parameter(description = "用户名，用于生成示例工具参数", example = "demo-user")
            @RequestParam(defaultValue = "demo-user") String username,
            @Parameter(description = "评论内容，用于生成示例工具参数", example = "这是一条来自 mcp-client 的评论")
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
    @Operation(summary = "读取 MCP Server 的评论规范 resource",
            description = "取第一个可用的 MCP Sync Client，按固定 URI resource://comment/guideline 真实调用 MCP 的 readResource，"
                    + "返回 MCP Server 信息（serverInfo）、请求的资源 URI 以及解析后的资源内容列表（文本资源给出 uri / mimeType / text）。"
                    + "无任何入参；若当前没有可用的 MCP 连接将抛出 IllegalStateException。")
    public Map<String, Object> readCommentGuidelineResource() {
        McpSyncClient client = getClient();
        McpSchema.ReadResourceResult result = client.readResource(
                McpSchema.ReadResourceRequest.builder(RESOURCE_URI).build());

        return Map.of(
                "serverInfo", client.getServerInfo(),
                "resourceUri", RESOURCE_URI,
                "contents", result.contents().stream().map(this::convertResourceContent).toList()
        );
    }

    @GetMapping("/prompt")
    @Operation(summary = "获取 MCP Server 的评论摘要 prompt",
            description = "取第一个可用的 MCP Sync Client，按固定名称 comment-summary-prompt 真实调用 MCP 的 getPrompt，"
                    + "把 articleId / username / content 作为 prompt 参数传入，返回 MCP Server 信息、prompt 名称以及解析后的消息列表"
                    + "（每条消息含 role 与 content，文本内容直接给出字符串）。")
    public Map<String, Object> getCommentSummaryPrompt(
            @Parameter(description = "文章 ID，作为 prompt 参数传入", example = "article-001")
            @RequestParam(defaultValue = "article-001") String articleId,
            @Parameter(description = "用户名，作为 prompt 参数传入", example = "demo-user")
            @RequestParam(defaultValue = "demo-user") String username,
            @Parameter(description = "评论内容，作为 prompt 参数传入", example = "这是一条来自 mcp-client 的评论")
            @RequestParam(defaultValue = "这是一条来自 mcp-client 的评论") String content) {
        McpSyncClient client = getClient();
        McpSchema.GetPromptResult result = client.getPrompt(McpSchema.GetPromptRequest.builder(PROMPT_NAME)
                .arguments(Map.of(
                        "articleId", articleId,
                        "username", username,
                        "content", content
                ))
                .build());
        return Map.of(
                "serverInfo", client.getServerInfo(),
                "promptName", PROMPT_NAME,
                "messages", result.messages().stream().map(this::convertPromptMessage).toList()
        );
    }

    @GetMapping("/demo")
    @Operation(summary = "资源 + 提示词 + 大模型的全链路演示",
            description = "组合调用：先按 resource://comment/guideline 读取评论规范，再用 comment-summary-prompt 生成提示词，"
                    + "把该提示词交给内置的 ChatClient（已默认挂载 MCP 工具）向大模型提问，最后一次性返回 resource、prompt 与模型回答 modelAnswer。"
                    + "需要本地 MCP Server 与可用的 OpenAI 兼容模型服务，否则会报错。")
    public Map<String, Object> runCommentDemo(
            @Parameter(description = "文章 ID，透传给 prompt 调用", example = "article-001")
            @RequestParam(defaultValue = "article-001") String articleId,
            @Parameter(description = "用户名，透传给 prompt 调用", example = "demo-user")
            @RequestParam(defaultValue = "demo-user") String username,
            @Parameter(description = "评论内容，透传给 prompt 调用", example = "这是一条来自 mcp-client 的评论")
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
    @Operation(summary = "列出 MCP Server 的 resources / prompts / tools",
            description = "取第一个可用的 MCP Sync Client，依次真实调用 MCP 协议的 listResources、listPrompts、listTools 三个方法，"
                    + "把 serverInfo 与三份原始列表结果一并返回，用于确认 MCP Server 侧究竟暴露了哪些资源、提示词和工具。无任何入参。")
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
    @Operation(summary = "带 MCP 工具的普通对话（非 SSE）",
            description = "把用户消息直接交给已默认挂载 MCP 工具的 ChatClient，让模型自行决定是否调用 MCP 工具，最终返回模型回答（纯文本 String）。"
                    + "注意：路径虽然包含 sse，但本接口既不是 SSE、也不返回 text/event-stream，响应体是完整的普通字符串，"
                    + "请勿按流式接口使用。message 为必填参数，未传会返回 400。"
                    + "调用需要本地 MCP Server 与可用的 OpenAI 兼容模型服务。")
    String useCommentMCP(
            @Parameter(description = "发送给大模型的用户消息（必填，无默认值）", required = true,
                    example = "请帮我保存一条评论并汇总该文章的评论列表")
            @RequestParam("message") String message) {
        return this.chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
