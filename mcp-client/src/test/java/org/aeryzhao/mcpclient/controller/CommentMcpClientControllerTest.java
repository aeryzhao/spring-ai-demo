package org.aeryzhao.mcpclient.controller;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentMcpClientControllerTest {

    @Test
    void shouldBuildPromptWithToolNames() throws Exception {
        CommentMcpClientController controller = new CommentMcpClientController(
                "http://localhost:8081",
                "/sse",
                Duration.ofSeconds(30)
        );

        Method method = CommentMcpClientController.class.getDeclaredMethod(
                "buildPrompt", String.class, String.class, String.class, List.class);
        method.setAccessible(true);

        String prompt = (String) method.invoke(controller,
                "article-1001", "alice", "测试评论", List.of("listComments", "saveComment"));

        assertTrue(prompt.contains("article-1001"));
        assertTrue(prompt.contains("saveComment"));
        assertTrue(prompt.contains("listComments"));
    }

    @Test
    void shouldReturnFailurePayloadWhenServerIsUnavailable() {
        CommentMcpClientController controller = new CommentMcpClientController(
                "http://127.0.0.1:65530",
                "/sse",
                Duration.ofSeconds(1)
        );

        Map<String, Object> response = controller.listTools("article-1001", "alice", "测试评论");

        assertEquals("FAILED", response.get("status"));
        assertEquals("http://127.0.0.1:65530", response.get("serverBaseUrl"));
        assertEquals("/sse", response.get("sseEndpoint"));
        assertTrue(response.get("errorType").toString().contains("Exception"));
    }
}
