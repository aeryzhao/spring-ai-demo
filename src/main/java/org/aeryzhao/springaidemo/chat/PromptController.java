package org.aeryzhao.springaidemo.chat;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhaoxg
 * @date 2026/3/17 16:44
 */
@RequestMapping("/ai/prompt")
@RestController
public class PromptController {
    private static final String DEFAULT_SYSTEM_PROMPT = "你是一名资深 Java 开发助手，请使用简体中文回答，并优先给出简洁、可执行的建议。";

    private final ChatClient chatClient;

    public PromptController(OpenAiChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Operation(summary = "使用系统提示词生成内容")
    @GetMapping("/system")
    String system(@RequestParam(value = "userInput", defaultValue = "如何设计一个缓存接口？") String userInput) {
        return this.chatClient.prompt()
                .system(DEFAULT_SYSTEM_PROMPT)
                .user(userInput)
                .call()
                .content();
    }

    @Operation(summary = "使用模板提示词生成内容")
    @GetMapping("/template")
    String template(
            @RequestParam(value = "topic", defaultValue = "Spring Boot") String topic,
            @RequestParam(value = "language", defaultValue = "Java") String language,
            @RequestParam(value = "level", defaultValue = "入门") String level) {
        return this.chatClient.prompt()
                .system(DEFAULT_SYSTEM_PROMPT)
                .user(u -> u.text("请用{language}给出一段关于{topic}的{level}示例代码，并补充两条说明。")
                        .param("language", language)
                        .param("topic", topic)
                        .param("level", level))
                .call()
                .content();
    }
}
