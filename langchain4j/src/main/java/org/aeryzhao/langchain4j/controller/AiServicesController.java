package org.aeryzhao.langchain4j.controller;

import dev.langchain4j.service.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.aeryzhao.langchain4j.assistant.Assistant;
import org.aeryzhao.langchain4j.assistant.CodeReview;
import org.aeryzhao.langchain4j.assistant.Person;
import org.aeryzhao.langchain4j.assistant.StructuredOutputAssistant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 示例 3 & 4：AiServices 声明式接口 + 结构化输出。
 *
 * <p>注意看这些接口方法的实现体 —— 一行都没有。所有逻辑都被 LangChain4j 的动态代理接管了。
 */
@Tag(name = "3/4. AiServices 声明式接口与结构化输出",
        description = """
                AiServices 是 LangChain4j 最核心的抽象：只声明接口，由框架通过动态代理生成实现，
                把提示词、记忆、工具调用串起来。本组接口的 Controller 方法体里「一行逻辑都没有」，
                全部委托给 Assistant / StructuredOutputAssistant 接口。

                同时演示结构化输出：框架依据 POJO 自动生成 JSON Schema 交给模型，
                再把模型返回的 JSON 反序列化成 `Person` / `CodeReview` 对象，
                并通过 `Result<T>` 暴露 Token 用量与结束原因等元数据。
                """)
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
    @Operation(summary = "最简 AiServices 调用",
            description = """
                    演示 AiServices 声明式接口的最小用法：`assistant.chat(message)` 一行调用，
                    接口实现完全由 LangChain4j 动态代理生成。

                    示例：`GET /lc4j/ai-services/chat?message=你好`

                    返回 `question` 与模型给出的 `answer`。
                    """)
    @GetMapping("/chat")
    public Map<String, Object> chat(
            @Parameter(description = "用户提问内容", example = "你好")
            @RequestParam(defaultValue = "用一句话介绍你自己") String message) {
        return Map.of("question", message, "answer", assistant.chat(message));
    }

    /**
     * 带 System 人设的调用。
     *
     * <p>GET /lc4j/ai-services/java-expert?question=什么是虚拟线程
     */
    @Operation(summary = "带 System 人设的调用（Java 专家）",
            description = """
                    演示通过接口上的 `@SystemMessage` 固定模型人设：同一个模型，
                    换成「资深 Java 架构师」的角色后再回答技术问题。

                    示例：`GET /lc4j/ai-services/java-expert?question=什么是虚拟线程`

                    返回 `question` 与专家口吻的 `answer`。
                    """)
    @GetMapping("/java-expert")
    public Map<String, Object> javaExpert(
            @Parameter(description = "要请教 Java 专家的技术问题", example = "什么是虚拟线程")
            @RequestParam(defaultValue = "Java 的虚拟线程和平台线程有什么区别？") String question) {
        return Map.of("question", question, "answer", assistant.askJavaExpert(question));
    }

    /**
     * 模板参数：{@code @V} 显式绑定，不依赖编译期参数名。
     *
     * <p>GET /lc4j/ai-services/translate?text=Hello%20World&targetLanguage=日语
     */
    @Operation(summary = "提示词模板参数绑定（翻译）",
            description = """
                    演示 `@UserMessage` 提示词模板 + `@V` 显式参数绑定：不依赖编译期参数名，
                    模板里写 `{{text}}` / `{{targetLanguage}}`，运行时由框架填充。

                    示例：`GET /lc4j/ai-services/translate?text=Hello%20World&targetLanguage=日语`

                    返回 `source`、`targetLanguage` 与 `translation`。
                    """)
    @GetMapping("/translate")
    public Map<String, Object> translate(
            @Parameter(description = "待翻译的源文本", example = "Hello World")
            @RequestParam(defaultValue = "The quick brown fox jumps over the lazy dog") String text,
            @Parameter(description = "目标语言（中文/英语/日语/法语…）", example = "日语")
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
    @Operation(summary = "结构化输出：从文本抽取人物信息为 Person 对象",
            description = """
                    演示结构化输出。框架根据返回类型 `Person`（record）自动生成 JSON Schema 塞进提示词，
                    再把模型返回的 JSON 反序列化成 Java 对象 —— 全程无需手写解析代码。

                    字段缺失时按 `@Description` 约定兜底：`age` 未提到填 `-1`，`city` / `occupation` 未提到填「未知」。

                    示例：`GET /lc4j/ai-services/extract-person?text=张三今年35岁，是杭州的一名后端工程师`

                    返回 `Person` 对象：`name` / `age` / `city` / `occupation` / `summary`。
                    """)
    @GetMapping("/extract-person")
    public Person extractPerson(
            @Parameter(description = "包含人物信息的自然语言文本",
                    example = "张三今年35岁，是杭州的一名后端工程师，擅长高并发系统设计。")
            @RequestParam(defaultValue =
            "张三今年35岁，是杭州的一名后端工程师，擅长高并发系统设计，业余喜欢爬山。") String text) {
        return structuredOutputAssistant.extractPerson(text);
    }

    /**
     * 返回 {@link Result}，同时拿到内容和元数据（Token 用量、结束原因等）。
     *
     * <p>POST /lc4j/ai-services/review-code，Body 传代码文本。
     */
    @Operation(summary = "POST 代码评审（结构化输出 + Token 用量）",
            description = """
                    提交一段代码文本，让模型以 `CodeReview` 结构返回评审结果，并附带 Token 用量与结束原因。

                    **请求体为裸字符串**（`Content-Type: text/plain`），不是 JSON 对象。
                    不传或传空白时，使用内置的示例代码（一段会抛 `ArithmeticException` 的除法代码）。

                    返回字段：
                    - `review`：`CodeReview` 对象（`score` / `approved` / `issues` / `suggestion`）
                    - `tokenUsage`：`input` / `output` / `total` 三个 Token 计数
                    - `finishReason`：模型结束原因

                    示例：
                    ```bash
                    curl -X POST http://localhost:8097/lc4j/ai-services/review-code \\
                      -H 'Content-Type: text/plain' \\
                      --data 'public int add(int a, int b) { return a - b; }'
                    ```

                    > 注意：本接口与 `GET /lc4j/ai-services/review-code` **共用同一路径**，仅 HTTP 方法不同。
                    > GET 版通过 query 参数传代码，方便直接在浏览器里试；两者返回结构略有差异。
                    """)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
            description = "待评审的源代码纯文本。不传或传空白时使用内置示例代码。",
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "text/plain",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string"),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "有 bug 的示例代码",
                            value = "public int add(int a, int b) { return a - b; }")))
    @PostMapping("/review-code")
    public Map<String, Object> reviewCode(
            @org.springframework.web.bind.annotation.RequestBody(required = false) String code) {
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
    @Operation(summary = "GET 代码评审（便捷版，query 传参）",
            description = """
                    与 POST 版同样的代码评审能力，但代码通过 query 参数传入 ——
                    可以直接把 URL 贴进浏览器地址栏体验，无需构造请求体。

                    返回字段：
                    - `code`：实际被评审的代码
                    - `review`：`CodeReview` 对象（`score` / `approved` / `issues` / `suggestion`）
                    - `totalTokens`：Token 总量

                    示例：`GET /lc4j/ai-services/review-code?code=public int add(int a,int b){return a-b;}`

                    > 注意：本接口与 `POST /lc4j/ai-services/review-code` **共用同一路径**，仅 HTTP 方法不同。
                    > POST 版额外返回 `tokenUsage` 明细与 `finishReason`。
                    """)
    @GetMapping("/review-code")
    public Map<String, Object> reviewCodeGet(
            @Parameter(description = "待评审的源代码文本（建议 URL 编码）",
                    example = "public int add(int a, int b) { return a - b; }")
            @RequestParam(defaultValue = "public int add(int a, int b) { return a - b; }") String code) {
        Result<CodeReview> result = structuredOutputAssistant.reviewCode(code);
        return Map.of(
                "code", code,
                "review", result.content(),
                "totalTokens", result.tokenUsage() == null ? 0 : result.tokenUsage().totalTokenCount()
        );
    }

}
