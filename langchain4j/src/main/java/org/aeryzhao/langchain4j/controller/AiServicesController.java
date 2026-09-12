package org.aeryzhao.langchain4j.controller;

import dev.langchain4j.service.Result;
import lombok.RequiredArgsConstructor;
import org.aeryzhao.langchain4j.assistant.Assistant;
import org.aeryzhao.langchain4j.assistant.CodeReview;
import org.aeryzhao.langchain4j.assistant.Person;
import org.aeryzhao.langchain4j.assistant.StructuredOutputAssistant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 示例 3 & 4：AiServices 声明式接口 + 结构化输出。
 *
 * <p>注意看这些接口方法的实现体 —— 一行都没有。所有逻辑都被 LangChain4j 的动态代理接管了。
 */
@RestController
@RequestMapping("/lc4j/ai-services")
@RequiredArgsConstructor
public class AiServicesController {

    private final Assistant assistant;
    private final StructuredOutputAssistant structuredOutputAssistant;

    // ---------------- 3. 声明式接口 ----------------

    /**
     * 最简 AiServices 调用。
     *
     * <p>GET /lc4j/ai-services/chat?message=你好
     */
    @GetMapping("/chat")
    public Map<String, Object> chat(@RequestParam(defaultValue = "用一句话介绍你自己") String message) {
        return Map.of("question", message, "answer", assistant.chat(message));
    }

    /**
     * 带 System 人设的调用。
     *
     * <p>GET /lc4j/ai-services/java-expert?question=什么是虚拟线程
     */
    @GetMapping("/java-expert")
    public Map<String, Object> javaExpert(
            @RequestParam(defaultValue = "Java 的虚拟线程和平台线程有什么区别？") String question) {
        return Map.of("question", question, "answer", assistant.askJavaExpert(question));
    }

    /**
     * 模板参数：{@code @V} 显式绑定，不依赖编译期参数名。
     *
     * <p>GET /lc4j/ai-services/translate?text=Hello%20World&targetLanguage=日语
     */
    @GetMapping("/translate")
    public Map<String, Object> translate(
            @RequestParam(defaultValue = "The quick brown fox jumps over the lazy dog") String text,
            @RequestParam(defaultValue = "中文") String targetLanguage) {
        return Map.of(
                "source", text,
                "targetLanguage", targetLanguage,
                "translation", assistant.translate(text, targetLanguage)
        );
    }

    // ---------------- 4. 结构化输出 ----------------

    /**
     * 让模型返回一个 {@link Person} 对象（框架自动生成 Schema 并反序列化）。
     *
     * <p>GET /lc4j/ai-services/extract-person?text=张三今年35岁，是杭州的一名后端工程师，擅长高并发系统设计。
     */
    @GetMapping("/extract-person")
    public Person extractPerson(@RequestParam(defaultValue =
            "张三今年35岁，是杭州的一名后端工程师，擅长高并发系统设计，业余喜欢爬山。") String text) {
        return structuredOutputAssistant.extractPerson(text);
    }

    /**
     * 返回 {@link Result}，同时拿到内容和元数据（Token 用量、结束原因等）。
     *
     * <p>POST /lc4j/ai-services/review-code，Body 传代码文本。
     */
    @PostMapping("/review-code")
    public Map<String, Object> reviewCode(@RequestBody(required = false) String code) {
        String target = (code == null || code.isBlank())
                ? "public class Demo { public static void main(String[] a) { int x = 1/0; } }"
                : code;

        Result<CodeReview> result = structuredOutputAssistant.reviewCode(target);

        return Map.of(
                "review", result.content(),
                "tokenUsage", Map.of(
                        "input", result.tokenUsage() == null ? 0 : result.tokenUsage().inputTokenCount(),
                        "output", result.tokenUsage() == null ? 0 : result.tokenUsage().outputTokenCount(),
                        "total", result.tokenUsage() == null ? 0 : result.tokenUsage().totalTokenCount()
                ),
                "finishReason", String.valueOf(result.finishReason())
        );
    }

    /**
     * 便捷版：用 query 参数传代码，方便直接在浏览器里试。
     *
     * <p>GET /lc4j/ai-services/review-code?code=int%20x%20%3D%201%3B
     */
    @GetMapping("/review-code")
    public Map<String, Object> reviewCodeGet(
            @RequestParam(defaultValue = "public int add(int a, int b) { return a - b; }") String code) {
        Result<CodeReview> result = structuredOutputAssistant.reviewCode(code);
        return Map.of(
                "code", code,
                "review", result.content(),
                "totalTokens", result.tokenUsage() == null ? 0 : result.tokenUsage().totalTokenCount()
        );
    }

}
