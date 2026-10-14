package org.aeryzhao.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "提示词", description = "演示系统提示词（system prompt）与模板提示词（占位符替换）两种提示词组织方式")
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

    @Operation(summary = "使用系统提示词生成内容",
            description = """
                    固定使用模块内置的系统提示词（「你是一名资深 Java 开发助手，请使用简体中文回答，并优先给出简洁、\
                    可执行的建议。」）约束模型的角色与回答风格，再传入用户输入。

                    适合观察 system prompt 对输出语气与内容结构的影响；该接口会真实调用 DeepSeek 模型。
                    """)
    @GetMapping("/system")
    String system(@Parameter(description = "用户输入的问题或指令，默认值：如何设计一个缓存接口？",
                             example = "如何设计一个缓存接口？")
                  @RequestParam(value = "userInput", defaultValue = "如何设计一个缓存接口？") String userInput) {
        return this.chatClient.prompt()
                .system(DEFAULT_SYSTEM_PROMPT)
                .user(userInput)
                .call()
                .content();
    }

    @Operation(summary = "使用模板提示词生成内容",
            description = """
                    使用带占位符的提示词模板生成内容：模板内部会把 `{language}`、`{topic}`、`{level}` 三个占位符\
                    替换为请求参数后，再连同系统提示词一起发送给模型。

                    例如 topic=Spring Boot、language=Java、level=入门，会要求模型输出入门级 Java 示例代码并补充两条说明。\
                    该接口会真实调用 DeepSeek 模型。
                    """)
    @GetMapping("/template")
    String template(
            @Parameter(description = "示例代码的主题，默认值：Spring Boot", example = "Spring Boot")
            @RequestParam(value = "topic", defaultValue = "Spring Boot") String topic,
            @Parameter(description = "示例代码使用的编程语言，默认值：Java", example = "Java")
            @RequestParam(value = "language", defaultValue = "Java") String language,
            @Parameter(description = "示例代码的难度/受众层级，默认值：入门", example = "入门")
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
