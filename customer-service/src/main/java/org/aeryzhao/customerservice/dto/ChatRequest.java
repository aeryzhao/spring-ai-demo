package org.aeryzhao.customerservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 一次提问。
 *
 * @param sessionId 会话 id，为空时服务端生成；同一 id 共享多轮记忆
 * @param message   用户问题
 */
public record ChatRequest(
        String sessionId,
        @NotBlank(message = "message 不能为空")
        @Size(max = 1000, message = "message 长度不能超过 1000")
        String message) {
}
