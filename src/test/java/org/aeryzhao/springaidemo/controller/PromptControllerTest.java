package org.aeryzhao.springaidemo.controller;

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

class PromptControllerTest {

    @Test
    void systemShouldReturnGeneratedText() {
        OpenAiChatModel chatModel = mock(OpenAiChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("缓存接口建议"));

        PromptController controller = new PromptController(chatModel);

        assertThat(controller.system("如何设计缓存接口？")).isEqualTo("缓存接口建议");
    }

    @Test
    void templateShouldReturnGeneratedText() {
        OpenAiChatModel chatModel = mock(OpenAiChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("Spring Boot 示例代码"));

        PromptController controller = new PromptController(chatModel);

        assertThat(controller.template("Spring Boot", "Java", "入门")).isEqualTo("Spring Boot 示例代码");
    }

    private ChatResponse chatResponse(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }
}
