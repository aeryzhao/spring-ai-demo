package org.aeryzhao.langchain4j.controller;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.tool.ToolExecution;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.aeryzhao.langchain4j.assistant.TravelAssistant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 示例 6：工具调用（Function Calling）。
 *
 * <p>调用链：用户提问 → 模型判断需要工具 → LangChain4j 执行 Java 方法 → 结果回灌模型 → 模型生成最终回答。
 * 整个过程对业务代码是透明的，你只需要把工具注册到 {@code AiServices} 上。
 *
 * <p>本接口把 {@code toolExecutions()} 一并返回，方便直观看到「模型到底调了哪几个工具、传了什么参数」。
 */
@Tag(name = "6. 工具调用（Function Calling）", description = """
        演示让模型自主调用 Java 方法（Function Calling）。

        调用链：**用户提问 → 模型判断需要工具 → LangChain4j 执行 Java 方法 → 结果回灌模型 → 模型生成最终回答**。
        整个过程对业务代码透明，只需把工具（`TravelTools`）注册到 `AiServices` 上。

        可用工具包括查天气、算天数、估预算。本接口把 `toolExecutions()` 一并返回，
        方便直观看到「模型到底调了哪几个工具、传了什么参数」。
        """)
@RestController
@RequestMapping("/lc4j/tools")
@RequiredArgsConstructor
public class ToolCallController {

    private final TravelAssistant travelAssistant;

    /**
     * 一次触发多个工具的综合请求。
     *
     * <p>GET /lc4j/tools/plan?request=我下周一从北京出发去三亚玩5天，帮我看看天气和预算
     */
    @Operation(summary = "旅行规划：一次触发多个工具的调用链",
            description = """
                    提交一个旅行规划请求，模型会自行判断需要调用哪些工具（查天气 / 计算天数 / 估算预算），
                    LangChain4j 执行对应 Java 方法后把结果回灌给模型生成最终回答。

                    返回字段：
                    - `request`：原始请求
                    - `answer`：模型最终回答（中文，分点罗列）
                    - `toolCallCount`：实际发生的工具调用次数
                    - `toolCalls`：调用明细列表，每项含 `toolName` / `arguments` / `result`
                      —— 这是理解模型行为最直接的方式

                    示例：`GET /lc4j/tools/plan?request=我下周一从北京出发去三亚玩5天，帮我看看天气和预算`
                    """)
    @GetMapping("/plan")
    public Map<String, Object> plan(
            @Parameter(description = "自然语言的旅行规划请求（含出发地、目的地、日期等要素）",
                    example = "我下周一从北京出发去三亚玩5天，帮我看看天气和预算")
            @RequestParam(defaultValue =
            "我想 2026-10-01 从北京出发去三亚，2026-10-05 返回，帮我查下天气、算算几天、估个预算。") String request) {
        Result<String> result = travelAssistant.plan(request);

        // 把工具调用记录整理成可读结构，这是理解模型行为最直接的方式
        List<Map<String, Object>> toolCalls = result.toolExecutions().stream()
                .map(this::describe)
                .toList();

        return Map.of(
                "request", request,
                "answer", result.content(),
                "toolCallCount", toolCalls.size(),
                "toolCalls", toolCalls
        );
    }

    private Map<String, Object> describe(ToolExecution execution) {
        return Map.of(
                "toolName", execution.request().name(),
                "arguments", execution.request().arguments(),
                "result", execution.result()
        );
    }

}
