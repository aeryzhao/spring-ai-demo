package org.aeryzhao.springaidemo.repository;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentMapChatMemoryRepositoryTest {

    @Test
    void shouldSaveFindAndDeleteConversationMessages() {
        ConcurrentMapChatMemoryRepository repository = new ConcurrentMapChatMemoryRepository();

        repository.saveAll("session-1", List.of(
                new UserMessage("你好"),
                new AssistantMessage("你好，有什么可以帮你？")
        ));

        assertThat(repository.findConversationIds()).containsExactly("session-1");
        assertThat(repository.findByConversationId("session-1"))
                .extracting(message -> message.getText())
                .containsExactly("你好", "你好，有什么可以帮你？");

        repository.deleteByConversationId("session-1");

        assertThat(repository.findConversationIds()).isEmpty();
        assertThat(repository.findByConversationId("session-1")).isEmpty();
    }

    @Test
    void shouldReturnDefensiveCopyWhenReadingMessages() {
        ConcurrentMapChatMemoryRepository repository = new ConcurrentMapChatMemoryRepository();
        repository.saveAll("session-2", List.of(new UserMessage("请记住这句话")));

        List<?> messages = repository.findByConversationId("session-2");
        messages.clear();

        assertThat(repository.findByConversationId("session-2")).hasSize(1);
    }
}
