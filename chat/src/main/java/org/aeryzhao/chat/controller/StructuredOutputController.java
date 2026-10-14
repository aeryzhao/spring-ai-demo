package org.aeryzhao.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author zhaoxg
 * @date 2026/3/17 21:10
 */
@Tag(name = "结构化输出", description = "演示让模型直接返回强类型结构（Java record），由 Spring AI 完成 JSON 到对象的转换")
@RequestMapping("/ai/structured-output")
@RestController
public class StructuredOutputController {

    private static final String DEFAULT_SYSTEM_PROMPT = "你是一名资深阅读助手，请使用简体中文输出内容。";

    private final ChatClient chatClient;

    public StructuredOutputController(OpenAiChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Operation(summary = "生成结构化书籍摘要",
            description = """
                    通过 `ChatClient...call().entity(BookSummary.class)` 让模型按 `BookSummary` 结构返回书籍摘要，\
                    响应为 JSON 对象而非纯文本：包含书名、作者、摘要与 3~5 个关键词标签。

                    提示词已明确要求返回 name、author、summary、tags 四个字段；若模型返回的内容无法映射到该结构，\
                    接口会抛出解析异常。该接口会真实调用 DeepSeek 模型。
                    """)
    @GetMapping("/book-summary")
    BookSummary bookSummary(@Parameter(description = "要生成摘要的书名，默认值：深入理解Java虚拟机",
                                      example = "深入理解Java虚拟机")
                            @RequestParam(value = "bookName", defaultValue = "深入理解Java虚拟机") String bookName) {
        return this.chatClient.prompt()
                .system(DEFAULT_SYSTEM_PROMPT)
                .user(u -> u.text("请为《{bookName}》生成书籍摘要。返回字段必须包含：name、author、summary、tags。tags 请返回 3 到 5 个关键词。")
                        .param("bookName", bookName))
                .call()
                .entity(BookSummary.class);
    }

    @Schema(name = "BookSummary", description = "书籍摘要结构化输出模型，由模型按该结构生成，Swagger UI 中的响应示例即按此结构展示")
    public record BookSummary(
            @Schema(description = "书名", example = "深入理解Java虚拟机")
            String name,
            @Schema(description = "作者", example = "周志明")
            String author,
            @Schema(description = "书籍内容摘要，通常为一段简述", example = "本书系统讲解 JVM 的内存结构、垃圾回收与类加载机制，是 Java 进阶的经典读物。")
            String summary,
            @Schema(description = "3~5 个关键词标签", example = "[\"JVM\", \"垃圾回收\", \"类加载\", \"性能调优\"]")
            List<String> tags) {
    }
}
