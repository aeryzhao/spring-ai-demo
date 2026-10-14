package org.aeryzhao.mcpclient.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * mcp-client 模块的 OpenAPI / Swagger 文档配置。
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI mcpClientOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MCP Client 演示 API")
                        .version("0.0.1-SNAPSHOT")
                        .description("""
                                MCP Client 演示模块（端口 8090）。

                                本模块作为 MCP Client，通过 spring-ai-starter-mcp-client 连接本地 MCP Server
                                （http://localhost:8081/mcp），把 MCP Server 暴露的 tools 注册进 Spring AI 的
                                ToolCallbackProvider，并演示对 resource 与 prompt 的真实调用，最后用 ChatClient
                                完成一次带工具的对话。

                                使用前请确认本地 MCP Server 已启动在 8081 端口，且已配置可用的 OpenAI 兼容模型服务；
                                否则获取 MCP 连接的接口会抛出 IllegalStateException，对话接口也会因模型不可用而失败。

                                注意：/comments/sse/client 虽然路径含 sse，但返回的是普通字符串，不是 SSE 流式响应。
                                """))
                .servers(List.of(new Server()
                        .url("http://localhost:8090")
                        .description("本地开发环境（mcp-client，需 MCP Server 运行于 http://localhost:8081/mcp）")));
    }
}
