package org.aeryzhao.springaidemo.chat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StructuredOutputControllerTest {

    @Test
    void bookSummaryShouldReturnStructuredEntity() {
        OpenAiChatModel chatModel = mock(OpenAiChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("""
                {
                  "name": "深入理解Java虚拟机",
                  "author": "周志明",
                  "summary": "本书系统讲解 JVM 运行机制、类加载、字节码执行与性能调优。",
                  "tags": ["JVM", "Java", "性能调优"]
                }
                """));

        StructuredOutputController controller = new StructuredOutputController(chatModel);

        StructuredOutputController.BookSummary summary = controller.bookSummary("深入理解Java虚拟机");

        assertThat(summary.name()).isEqualTo("深入理解Java虚拟机");
        assertThat(summary.author()).isEqualTo("周志明");
        assertThat(summary.summary()).contains("JVM");
        assertThat(summary.tags()).containsExactly("JVM", "Java", "性能调优");
    }

    private ChatResponse chatResponse(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }
}
