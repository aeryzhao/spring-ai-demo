package org.aeryzhao.customerservice.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记录模型在一次回答里调用过哪些工具，用于前端展示「这次答答案动用了哪些系统能力」。
 *
 * <p>工具通过 {@link org.springframework.ai.chat.model.ToolContext} 拿到 sessionId 后写在这里，
 * Agent 在回答结束时按会话取出。</p>
 */
@Component
public class ToolCallRecorder {

    private final Map<String, List<String>> notes = new ConcurrentHashMap<>();

    public void record(String sessionId, String note) {
        if (sessionId == null || sessionId.isBlank() || note == null) {
            return;
        }
        notes.computeIfAbsent(sessionId, key -> java.util.Collections.synchronizedList(new ArrayList<>())).add(note);
    }

    public List<String> drain(String sessionId) {
        if (sessionId == null) {
            return List.of();
        }
        List<String> recorded = notes.remove(sessionId);
        return recorded == null ? List.of() : List.copyOf(recorded);
    }

    public void clear(String sessionId) {
        if (sessionId != null) {
            notes.remove(sessionId);
        }
    }
}
