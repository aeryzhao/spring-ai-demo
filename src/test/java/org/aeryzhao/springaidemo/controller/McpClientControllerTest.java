package org.aeryzhao.springaidemo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpClientControllerTest {

    @Test
    void currentTimeShouldReturnGeneratedContent() {
        OpenAiChatModel chatModel = mock(OpenAiChatModel.class);
        SyncMcpToolCallbackProvider toolCallbackProvider = mock(SyncMcpToolCallbackProvider.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("MCP 工具返回的 UTC 时间是 2026-03-17T13:00:00Z，上海时间可在此基础上换算为 UTC+8。"));

        McpClientController controller = new McpClientController(chatModel, toolCallbackProvider);

        String content = controller.currentTime("上海");

        assertThat(content).contains("UTC 时间");
    }

    private ChatResponse chatResponse(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }
}
