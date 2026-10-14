package org.aeryzhao.mcpclient;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * client 角色上下文的冒烟测试。
 *
 * <p>该测试刻意不依赖任何外部服务：
 * <ul>
 *     <li>连接地址被显式覆盖为 {@code http://127.0.0.1:1/mcp}，即客户端会去连一个
 *     必然连不上的端口，但**不会**因此让上下文启动失败（MCP Client 自动配置只创建
 *     {@code McpSyncClient} bean，不要求握手成功）。</li>
 *     <li>这里**不能**用 {@code spring.ai.mcp.client.enabled=false} 来「避免连接」：
 *     关掉之后 {@code ToolCallbackProvider} 也不会再被注册，而
 *     {@code CommentMcpClientController} 的构造函数强依赖该 bean，
 *     上下文会直接启动失败。</li>
 *     <li>{@code spring.ai.openai.api-key}/{@code base-url}/{@code chat.model} 提供占位值，
 *     使 {@code OpenAiChatModel} 在未加载 {@code local-config.yaml} 时也能装配；
 *     这里只验证上下文可启动，不会发起任何真实模型调用。</li>
 * </ul>
 */
@SpringBootTest(classes = McpClientApplication.class, properties = {
        "spring.ai.mcp.client.streamable-http.connections.comment-server.url=http://127.0.0.1:1/mcp",
        "spring.ai.openai.api-key=test-key",
        "spring.ai.openai.base-url=http://127.0.0.1:1",
        "spring.ai.openai.chat.model=test-model"
})
class McpClientApplicationTests {

    @Test
    void contextLoads() {
    }

}
