package org.aeryzhao.customerservice.dto;

import java.util.List;

/**
 * 一次完整回答。
 *
 * @param sessionId     会话 id
 * @param answer        模型回答
 * @param retrievalMode 本次使用的检索模式：VECTOR / KEYWORD / EMPTY
 * @param sources       命中的知识片段
 * @param toolCalls     本次回答调用过的工具（人可读描述）
 */
public record ChatAnswer(
        String sessionId,
        String answer,
        String retrievalMode,
        List<SourceRef> sources,
        List<String> toolCalls) {
}
