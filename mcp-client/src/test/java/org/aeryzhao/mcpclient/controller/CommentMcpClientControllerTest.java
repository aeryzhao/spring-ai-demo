package org.aeryzhao.mcpclient.controller;

import io.modelcontextprotocol.client.McpSyncClient;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CommentMcpClientControllerTest {

    @Test
    void shouldBuildPromptWithToolNames() throws Exception {
        CommentMcpClientController controller = new CommentMcpClientController(
                mock(OpenAiChatModel.class),
                mock(ToolCallbackProvider.class),
                List.of(mock(McpSyncClient.class))
        );

        Method method = CommentMcpClientController.class.getDeclaredMethod(
                "buildToolPrompt", String.class, String.class, String.class, String[].class);
        method.setAccessible(true);

        String prompt = (String) method.invoke(controller,
                "article-1001", "alice", "测试评论", new String[]{"listComments", "saveComment"});

        assertTrue(prompt.contains("article-1001"));
        assertTrue(prompt.contains("saveComment"));
        assertTrue(prompt.contains("listComments"));
    }
}
