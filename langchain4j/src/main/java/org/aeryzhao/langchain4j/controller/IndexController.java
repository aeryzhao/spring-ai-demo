package org.aeryzhao.langchain4j.controller;

import org.aeryzhao.langchain4j.config.LangChain4jProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 示例导航：列出本模块所有示例接口，方便快速上手 & 自测。
 *
 * <p>GET /lc4j
 */
@RestController
@RequestMapping("/lc4j")
@RequiredArgsConstructor
public class IndexController {

    private final LangChain4jProperties props;

    /**
     * 返回所有示例接口清单。
     */
    @GetMapping
    public Map<String, Object> index() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("module", "langchain4j");
        result.put("currentModel", Map.of(
                "provider", props.getProvider(),
                "chatModel", props.getChatModel(),
                "baseUrl", props.getBaseUrl()
        ));
        result.put("examples", List.of(
                Map.of("no", 1, "name", "ChatModel 基础对话",
                        "endpoints", List.of(
                                "GET /lc4j/chat/simple?message=你好",
                                "GET /lc4j/chat/detailed?message=你好",
                                "GET /lc4j/chat/multi-turn")),
                Map.of("no", 2, "name", "流式输出（SSE）",
                        "endpoints", List.of("GET /lc4j/stream?message=写一首五言绝句（curl -N 体验）")),
                Map.of("no", 3, "name", "AiServices 声明式接口",
                        "endpoints", List.of(
                                "GET /lc4j/ai-services/chat?message=你好",
                                "GET /lc4j/ai-services/java-expert?question=什么是虚拟线程",
                                "GET /lc4j/ai-services/translate?text=Hello&targetLanguage=日语")),
                Map.of("no", 4, "name", "结构化输出",
                        "endpoints", List.of(
                                "GET /lc4j/ai-services/extract-person?text=张三今年35岁，杭州的后端工程师",
                                "GET /lc4j/ai-services/review-code?code=public int add(int a,int b){return a-b;}")),
                Map.of("no", 5, "name", "聊天记忆（多会话隔离）",
                        "endpoints", List.of(
                                "GET /lc4j/memory/chat?memoryId=user-A&message=我叫小明",
                                "GET /lc4j/memory/messages?memoryId=user-A",
                                "DELETE /lc4j/memory/clear?memoryId=user-A")),
                Map.of("no", 6, "name", "工具调用",
                        "endpoints", List.of(
                                "GET /lc4j/tools/plan?request=我10月1日去三亚玩5天，帮我查天气算预算")),
                Map.of("no", 7, "name", "RAG 检索增强生成",
                        "endpoints", List.of(
                                "GET /lc4j/rag/search?question=langchain4j 模块用什么端口",
                                "GET /lc4j/rag/ask?question=什么是 RAG",
                                "POST /lc4j/rag/documents"))
        ));
        return result;
    }

}
