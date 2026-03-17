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

class ChatMemoryControllerTest {

    @Test
    void inMemoryChatShouldReturnGeneratedText() {
        OpenAiChatModel chatModel = mock(OpenAiChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("你好，小明。我已经记住你的名字了。"));

        ChatMemoryController controller = new ChatMemoryController(chatModel);

        String content = controller.inMemoryChat("session-1", "我叫小明，请记住我的名字");

        assertThat(content).isEqualTo("你好，小明。我已经记住你的名字了。");
    }

    private ChatResponse chatResponse(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }
}
