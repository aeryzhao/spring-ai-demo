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

class ToolCallControllerTest {

    @Test
    void travelPlanShouldReturnGeneratedContent() {
        OpenAiChatModel chatModel = mock(OpenAiChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("建议你第一天游西湖，第二天去灵隐寺，第三天安排市区美食和返程。"));

        ToolCallController controller = new ToolCallController(chatModel);

        String content = controller.travelPlan("杭州", 3, 3000);

        assertThat(content).contains("西湖");
    }

    private ChatResponse chatResponse(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }
}
