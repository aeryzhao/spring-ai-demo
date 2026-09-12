package org.aeryzhao.langchain4j.assistant;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 结构化输出助手：让模型返回可直接反序列化的 Java 对象。
 *
 * <p>LangChain4j 支持两种方式：
 * <ol>
 *   <li><b>返回 POJO</b>：{@code Person extractPerson(String text)} —— 框架会自动生成 JSON Schema、
 *       要求模型输出 JSON，再反序列化成 {@code Person}。最省事，推荐。</li>
 *   <li><b>返回 {@link Result}</b>：在拿到内容的同时，还能拿到 Token 用量、工具调用记录等元数据。</li>
 * </ol>
 *
 * <p>对比 Spring AI：Spring AI 用 {@code .entity(Class)} 完成同样的事，思路一致，
 * 但 LangChain4j 把「要什么类型」直接写在方法返回值上，更直观。
 */
public interface StructuredOutputAssistant {

    /**
     * 抽取人员信息。返回类型是 POJO，框架自动处理 JSON Schema 与反序列化。
     */
    @SystemMessage("你是一个信息抽取引擎，只输出 JSON，不要输出任何解释性文字。")
    @UserMessage("从下面这段文本中抽取人员信息：\n{{text}}")
    Person extractPerson(@V("text") String text);

    /**
     * 返回 {@link Result}，可以同时拿到内容和元数据（Token 用量、结束原因等）。
     */
    @SystemMessage("你是一个严谨的评委，只输出 JSON。")
    @UserMessage("请评估下面这段代码的质量：\n{{code}}")
    Result<CodeReview> reviewCode(@V("code") String code);

}
