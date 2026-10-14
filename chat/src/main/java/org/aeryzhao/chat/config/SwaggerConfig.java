package org.aeryzhao.chat.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * chat 模块的 OpenAPI 文档配置。
 *
 * @author zhaoxg
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8096}")
    private String serverPort;

    @Bean
    public OpenAPI chatOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chat API - Spring AI 基础能力示例")
                        .description("""
                                Spring AI Chat 示例模块的接口文档：用一组最小可用的 HTTP 接口演示 ChatClient 调用、\
                                系统/模板提示词、结构化输出、聊天记忆与工具调用。

                                ## 功能分组

                                - **ChatClient 调用**: 最基础的模型调用、带元数据的调用与流式调用
                                - **ChatModel 调用**: 直接使用 ChatModel 的 call / stream 能力
                                - **聊天记忆**: 基于内存仓库与自定义仓库的多轮对话上下文
                                - **提示词**: 系统提示词与模板提示词（占位符替换）
                                - **结构化输出**: 让模型按 Java record 结构返回结果
                                - **工具调用**: 模型自动调用本地工具函数生成旅行建议

                                ## 使用前须知

                                - 本模块接口的响应来自 `application.yaml` 中配置的 DeepSeek 模型（OpenAI 兼容协议），\
                                **必须在 Swagger UI 中真实发起调用**，且需要可用的 `deepseek.api-key` 与网络；\
                                模型不可用时接口会返回错误，这属于外部依赖问题，不是文档问题。
                                - `GET /ai/chat-memory/**` 的会话上下文保存在**进程内存**中，应用重启即丢失，\
                                多次请求需保持相同的 `conversationId`。
                                - 两个流式接口（`/ai/chat-client/stream`、`/ai/chat-model/stream`）返回 SSE 数据流，\
                                **Swagger UI 对流式响应的支持有限**，建议使用 `curl -N` 或浏览器直接访问体验。

                                ## 技术栈

                                - Spring Boot 4.1.0
                                - Spring AI 2.0.0
                                - springdoc-openapi 3.1.0
                                - DeepSeek（OpenAI 兼容接口）
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("chat module")
                                .email("support@aeryzhao.org"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("本地开发服务器（chat 模块默认端口 8096）")
                ));
    }
}
