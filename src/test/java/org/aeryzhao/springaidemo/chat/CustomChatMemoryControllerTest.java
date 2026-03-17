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

class CustomChatMemoryControllerTest {

    @Test
    void chatShouldReturnGeneratedText() {
        OpenAiChatModel chatModel = mock(OpenAiChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("好的，我记住你最喜欢 Java 了。"));

        CustomChatMemoryController controller = new CustomChatMemoryController(chatModel);

        String content = controller.chat("custom-1", "我最喜欢的编程语言是Java，请记住");

        assertThat(content).isEqualTo("好的，我记住你最喜欢 Java 了。");
    }

    @Test
    void clearShouldRemoveConversationMemory() {
        OpenAiChatModel chatModel = mock(OpenAiChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("已记录。"));

        CustomChatMemoryController controller = new CustomChatMemoryController(chatModel);
        controller.chat("custom-2", "请记住我是后端开发");

        assertThat(controller.memorySize("custom-2")).isGreaterThan(0);
        assertThat(controller.clear("custom-2")).isEqualTo("已清空会话记忆: custom-2");
        assertThat(controller.memorySize("custom-2")).isZero();
    }

    private ChatResponse chatResponse(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }
}
