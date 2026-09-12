package org.aeryzhao.langchain4j.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 「助手」接口：只声明方法签名，不写实现。
 *
 * <p>这是 LangChain4j 最核心的设计 —— <b>AiServices</b>：
 * 你像写 MyBatis Mapper 一样声明一个接口，LangChain4j 在运行期用动态代理生成实现，
 * 自动完成「拼装提示词 → 调用模型 → 解析返回值」的全过程。
 *
 * <p>对比 Spring AI：Spring AI 走的是 {@code ChatClient} 流式 API 链，写法更过程式；
 * LangChain4j 走的是声明式接口，更贴近 Java 开发者的直觉。
 *
 * <p>本接口由 {@code AiServicesConfig} 统一构建为 Bean。
 */
public interface Assistant {

    /**
     * 最简单的一问一答。方法名本身不参与提示词，只有参数会作为用户消息。
     */
    String chat(String userMessage);

    /**
     * 用 {@link SystemMessage} 固定角色设定。
     *
     * <p>{@code {{question}}} 是模板占位符，由方法参数自动填充（参数名需保留，编译时加 {@code -parameters}）。
     */
    @SystemMessage("""
            你是一位资深 Java 架构师，回答要求：
            1. 先给结论，再给理由；
            2. 控制在 200 字以内；
            3. 涉及代码时给出关键片段。
            """)
    String askJavaExpert(@UserMessage String question);

    /**
     * 用 {@link V} 显式绑定模板变量，不依赖参数名（更稳妥，推荐在生产中使用）。
     */
    @SystemMessage("你是一个翻译引擎，只输出译文，不要任何解释。")
    @UserMessage("把下面的文本翻译成{{targetLanguage}}：{{text}}")
    String translate(@V("text") String text, @V("targetLanguage") String targetLanguage);

}
