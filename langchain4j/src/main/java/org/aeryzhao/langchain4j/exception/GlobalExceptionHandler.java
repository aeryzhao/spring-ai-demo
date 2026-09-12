package org.aeryzhao.langchain4j.exception;

import dev.langchain4j.exception.AuthenticationException;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一异常处理：把 LangChain4j 的异常翻译成可读的提示。
 *
 * <p>LangChain4j 把各家模型的 HTTP 错误统一映射成了自己的异常体系
 * （{@code dev.langchain4j.exception} 包下），所以业务代码不需要关心
 * 具体是 OpenAI 还是 DeepSeek，只要捕获这几个异常即可。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * API Key 无效 / 过期 —— 最常见的本地调试问题。
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuth(AuthenticationException e) {
        log.warn("模型鉴权失败: {}", e.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "模型鉴权失败", e,
                "请检查项目根目录 local-config.yaml 中的 deepseek.api-key 是否有效、是否已过期。");
    }

    /**
     * 模型服务不可达 / 返回非 2xx。
     */
    @ExceptionHandler(HttpException.class)
    public ResponseEntity<Map<String, Object>> handleHttp(HttpException e) {
        log.warn("模型接口调用失败: {}", e.getMessage());
        return build(HttpStatus.BAD_GATEWAY, "模型接口调用失败", e,
                "请确认 local-config.yaml 中的 base-url 与 chat-model 是否正确，以及网络是否可达。");
    }

    /**
     * 调用超时。
     */
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleTimeout(TimeoutException e) {
        log.warn("模型调用超时: {}", e.getMessage());
        return build(HttpStatus.GATEWAY_TIMEOUT, "模型调用超时", e,
                "可以调大 langchain4j.timeout，或换用更快的模型。");
    }

    /**
     * 其它 LangChain4j 异常兜底。
     */
    @ExceptionHandler(dev.langchain4j.exception.LangChain4jException.class)
    public ResponseEntity<Map<String, Object>> handleLangChain4j(dev.langchain4j.exception.LangChain4jException e) {
        log.warn("LangChain4j 调用异常: {}", e.getMessage());
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "LangChain4j 调用异常", e, null);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String title,
                                                      Exception e, String hint) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", title);
        body.put("message", e.getMessage());
        if (hint != null) {
            body.put("hint", hint);
        }
        return ResponseEntity.status(status).body(body);
    }

}
