package org.aeryzhao.springaidemo.chat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatClientControllerTest {

    @Test
    void contentShouldReturnGeneratedText() {
        OpenAiChatModel chatModel = mock(OpenAiChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("你好，世界"));

        ChatClientController controller = new ChatClientController(chatModel);

        assertThat(controller.content("你好")).isEqualTo("你好，世界");
    }

    @Test
    void chatResponseShouldReturnMetadataResponse() {
        OpenAiChatModel chatModel = mock(OpenAiChatModel.class);
        ChatResponse expected = chatResponse("返回完整响应");
        when(chatModel.call(any(Prompt.class))).thenReturn(expected);

        ChatClientController controller = new ChatClientController(chatModel);

        assertThat(controller.chatResponse("你好")).isSameAs(expected);
    }

    @Test
    void streamShouldReturnStreamingContent() {
        OpenAiChatModel chatModel = mock(OpenAiChatModel.class);
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(
                chatResponse("你"),
                chatResponse("好")
        ));

        ChatClientController controller = new ChatClientController(chatModel);

        assertThat(controller.stream("你好").collectList().block()).containsExactly("你", "好");
    }

    private ChatResponse chatResponse(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }
}
