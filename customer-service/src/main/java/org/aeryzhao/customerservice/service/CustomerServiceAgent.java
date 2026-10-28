package org.aeryzhao.customerservice.service;

import lombok.extern.slf4j.Slf4j;
import org.aeryzhao.customerservice.config.CustomerServiceProperties;
import org.aeryzhao.customerservice.dto.ChatAnswer;
import org.aeryzhao.customerservice.dto.SourceRef;
import org.aeryzhao.customerservice.knowledge.KnowledgeIndexService;
import org.aeryzhao.customerservice.knowledge.RetrievedChunk;
import org.aeryzhao.customerservice.tool.CustomerServiceTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 智能客服 Agent：把「知识检索 + 工具调用 + 多轮记忆」组装成一次客服应答。
 *
 * <p>提示词分两层：</p>
 * <ol>
 *   <li>固定人设与规则（classpath:prompts/customer-service-system.md）</li>
 *   <li>每次提问动态拼接的【参考资料】编号片段，模型据此作答并给出 [1][2] 角标</li>
 * </ol>
 */
@Slf4j
@Service
public class CustomerServiceAgent {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss (XXX)");

    /**
     * 一次提问的准备结果：检索已完成，等待调用模型。
     */
    public record Prepared(
            String sessionId,
            String question,
            String systemPrompt,
            List<RetrievedChunk> chunks,
            String retrievalMode) {
    }

    private final ChatClient chatClient;
    private final KnowledgeIndexService indexService;
    private final CustomerServiceTools tools;
    private final ToolCallRecorder recorder;
    private final CustomerServiceProperties properties;
    private final String systemPromptTemplate;

    public CustomerServiceAgent(ChatClient.Builder chatClientBuilder,
                                ChatMemory chatMemory,
                                KnowledgeIndexService indexService,
                                CustomerServiceTools tools,
                                ToolCallRecorder recorder,
                                CustomerServiceProperties properties,
                                @Value("classpath:prompts/customer-service-system.md") Resource systemPromptResource) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.indexService = indexService;
        this.tools = tools;
        this.recorder = recorder;
        this.properties = properties;
        this.systemPromptTemplate = readPrompt(systemPromptResource);
    }

    /** 归一化会话 id（空则新建）。 */
    public String resolveSessionId(String sessionId) {
        return (sessionId == null || sessionId.isBlank()) ? UUID.randomUUID().toString() : sessionId.trim();
    }

    /** 检索知识 + 组装系统提示词。 */
    public Prepared prepare(String question, String sessionId) {
        String resolvedSessionId = resolveSessionId(sessionId);
        List<RetrievedChunk> chunks = List.of();
        try {
            chunks = indexService.search(question, properties.topK());
        } catch (Exception e) {
            log.warn("知识检索失败，本次不带参考资料回答：{}", e.getMessage());
        }
        String systemPrompt = systemPromptTemplate
                .replace("{{now}}", OffsetDateTime.now().format(TIME_FORMAT))
                .replace("{{sessionId}}", resolvedSessionId)
                + renderContext(chunks);
        return new Prepared(resolvedSessionId, question, systemPrompt, chunks, indexService.effectiveMode());
    }

    /** 同步回答。 */
    public ChatAnswer answer(Prepared prepared) {
        recorder.clear(prepared.sessionId());
        String content = chatClient.prompt()
                .system(prepared.systemPrompt())
                .user(prepared.question())
                .tools(tools)
                .toolContext(Map.of("sessionId", prepared.sessionId()))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, prepared.sessionId()))
                .call()
                .content();
        return toAnswer(prepared, content);
    }

    /** 流式回答：逐段增量文本。 */
    public Flux<String> stream(Prepared prepared) {
        recorder.clear(prepared.sessionId());
        return chatClient.prompt()
                .system(prepared.systemPrompt())
                .user(prepared.question())
                .tools(tools)
                .toolContext(Map.of("sessionId", prepared.sessionId()))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, prepared.sessionId()))
                .stream()
                .content();
    }

    public ChatAnswer toAnswer(Prepared prepared, String content) {
        return new ChatAnswer(
                prepared.sessionId(),
                content,
                prepared.retrievalMode(),
                toSources(prepared.chunks()),
                recorder.drain(prepared.sessionId()));
    }

    public List<String> drainToolCalls(String sessionId) {
        return recorder.drain(sessionId);
    }

    public List<SourceRef> toSources(List<RetrievedChunk> chunks) {
        return chunks.stream()
                .map(chunk -> new SourceRef(
                        chunk.chunkId(),
                        chunk.title(),
                        chunk.category(),
                        chunk.source(),
                        snippet(chunk.text()),
                        round(chunk.score())))
                .toList();
    }

    private String renderContext(List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return """

                    # 参考资料

                    （本次没有检索到相关资料，请按规则第 4 条处理：先说明需要确认，再调用 createHumanTicket 转人工。）
                    """;
        }
        StringBuilder sb = new StringBuilder("\n\n# 参考资料\n\n");
        int index = 1;
        for (RetrievedChunk chunk : chunks) {
            sb.append('[').append(index++).append("] 《").append(chunk.title()).append("》")
                    .append("（分类：").append(chunk.category()).append("）\n")
                    .append(chunk.text()).append("\n\n");
        }
        sb.append("引用规则：只允许使用上面的编号 [1]-[").append(chunks.size())
                .append("]，不要引入资料之外的条款或数字。\n");
        return sb.toString();
    }

    private String snippet(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 140 ? normalized : normalized.substring(0, 140) + "…";
    }

    private double round(double value) {
        return Math.round(value * 1000d) / 1000d;
    }

    private String readPrompt(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("读取系统提示词失败", e);
        }
    }
}
