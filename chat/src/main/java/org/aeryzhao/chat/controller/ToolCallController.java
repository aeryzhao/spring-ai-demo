package org.aeryzhao.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "基于带参数对象的工具调用生成旅游建议")
    @GetMapping("/travel-plan")
    String travelPlan(@RequestParam(value = "city", defaultValue = "杭州") String city,
                      @RequestParam(value = "days", defaultValue = "3") int days,
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
