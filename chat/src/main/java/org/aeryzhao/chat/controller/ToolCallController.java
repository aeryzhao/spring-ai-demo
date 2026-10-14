package org.aeryzhao.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.aeryzhao.chat.tool.TravelPlanTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工具调用示例控制器。
 *
 * @author zhaoxg
 * @date 2026/3/17 21:38
 */
@Tag(name = "工具调用", description = "演示 Function Calling：模型根据用户意图自动调用本地 TravelPlanTools 工具，再基于工具结果整理回答")
@RequestMapping("/ai/tool")
@RestController
public class ToolCallController {

    private static final String DEFAULT_SYSTEM_PROMPT = "你是一名旅游助手。当用户提供城市、天数和预算时，优先调用可用工具生成旅行建议，再整理成简体中文答案。";

    private final ChatClient chatClient;

    public ToolCallController(OpenAiChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultTools(new TravelPlanTools())
                .build();
    }

    @Operation(summary = "基于带参数对象的工具调用生成旅游建议",
            description = """
                    演示工具（函数）调用：ChatClient 注册了 `TravelPlanTools` 作为默认工具，模型会根据用户消息判断\
                    是否需要调用它，并把城市、天数、预算作为参数传入，最后基于工具返回结果整理成中文建议。

                    与「工具调用（无参数）」不同，这里三个参数是真实传入工具的入参。参数值会先拼进提示词再交由模型决定调用；\
                    该接口会真实调用 DeepSeek 模型，响应时间通常比普通对话更长（包含工具调用与二次生成两轮）。
                    """)
    @GetMapping("/travel-plan")
    String travelPlan(@Parameter(description = "旅行目的地城市，默认值：杭州", example = "杭州")
                      @RequestParam(value = "city", defaultValue = "杭州") String city,
                      @Parameter(description = "旅行天数（天），默认值：3", example = "3")
                      @RequestParam(value = "days", defaultValue = "3") int days,
                      @Parameter(description = "总预算（元），默认值：3000", example = "3000")
                      @RequestParam(value = "budget", defaultValue = "3000") int budget) {
        return this.chatClient.prompt()
                .system(DEFAULT_SYSTEM_PROMPT)
                .user(u -> u.text("请使用旅游规划工具，为我生成一个旅行建议。城市是{city}，天数是{days}天，总预算是{budget}元。")
                        .param("city", city)
                        .param("days", days)
                        .param("budget", budget))
                .call()
                .content();
    }
}
