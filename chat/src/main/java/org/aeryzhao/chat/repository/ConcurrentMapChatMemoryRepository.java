package org.aeryzhao.chat.repository;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 ConcurrentHashMap 的自定义聊天记忆仓库示例。
 *
 * @author zhaoxg
 * @date 2026/3/17 21:25
 */
public class ConcurrentMapChatMemoryRepository implements ChatMemoryRepository {

    private final Map<String, List<Message>> store = new ConcurrentHashMap<>();

    @Override
    public List<String> findConversationIds() {
        return store.keySet().stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        return new ArrayList<>(store.getOrDefault(conversationId, List.of()));
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        store.put(conversationId, new ArrayList<>(messages));
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        store.remove(conversationId);
    }
}
