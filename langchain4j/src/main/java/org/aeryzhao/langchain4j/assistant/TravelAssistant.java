package org.aeryzhao.langchain4j.assistant;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 工具调用助手：把 {@code TravelTools} 交给它使用。
 *
 * <p>注册方式见 {@code AiServicesConfig}：{@code AiServices.builder(TravelAssistant.class).tools(travelTools)}。
 * 注册后，模型会在需要时自己决定调用哪个工具、传什么参数，LangChain4j 负责执行并把结果回灌给模型。
 *
 * <p>返回类型用 {@link Result} 是为了拿到 {@code toolExecutions()} ——
 * 这样接口能明确告诉我们「模型到底调了哪些工具」，非常利于调试。
 */
public interface TravelAssistant {

    @SystemMessage("""
            你是一个旅行规划助手。你可以调用工具查询天气、计算天数、估算预算。
            规则：
            1. 涉及日期计算、预算数字时，必须调用工具，不要自己心算；
            2. 最终回答用中文，分点罗列，简洁清晰。
            """)
    @UserMessage("{{request}}")
    Result<String> plan(@V("request") String request);

}
