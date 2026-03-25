package org.aeryzhao.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "生成结构化书籍摘要")
    @GetMapping("/book-summary")
    BookSummary bookSummary(@RequestParam(value = "bookName", defaultValue = "深入理解Java虚拟机") String bookName) {
        return this.chatClient.prompt()
                .system(DEFAULT_SYSTEM_PROMPT)
                .user(u -> u.text("请为《{bookName}》生成书籍摘要。返回字段必须包含：name、author、summary、tags。tags 请返回 3 到 5 个关键词。")
                        .param("bookName", bookName))
                .call()
                .entity(BookSummary.class);
    }

    public record BookSummary(String name, String author, String summary, List<String> tags) {
    }
}
