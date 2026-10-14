package org.aeryzhao.langchain4j.config;

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
 * langchain4j 模块的 OpenAPI 元数据配置。
 *
 * <p>Springdoc 会自动扫描本模块的 {@code @RestController}，本类只负责补上
 * 文档标题、描述、联系方式以及 server 地址（默认取 {@code server.port}，即 8097）。
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8097}")
    private String serverPort;

    @Bean
    public OpenAPI langChain4jOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LangChain4j 示例 API")
                        .description("""
                                LangChain4j 1.20.0 上手示例模块（端口 8097）—— 演示 Java 生态下 LLM 应用开发的核心能力。

                                ## 功能特性

                                - **基础对话（ChatModel）**: 字符串进字符串出、完整 ChatResponse、多轮消息列表
                                - **流式输出（SSE）**: `StreamingChatModel` 回调式流，`curl -N` 可直接观察逐字输出
                                - **AiServices 声明式接口**: 只声明接口，由框架动态代理生成实现，零实现体
                                - **结构化输出**: 自动生成 JSON Schema 并反序列化为 POJO（`Person` / `CodeReview`）
                                - **聊天记忆**: `ChatMemoryStore` / `ChatMemory` / `ChatMemoryProvider` 三层设计，支持多会话隔离
                                - **工具调用（Function Calling）**: 注册 Java 方法为工具，模型自行决定调用时机与参数
                                - **RAG**: 手动实现「切分 → 向量化 → 存储 → 检索 → 生成」全链路（内存向量库）

                                ## 通用约定

                                - **Base URL**: `http://localhost:8097`
                                - 所有接口均可直接通过浏览器或 `curl` 访问；GET 参数大多带默认值，可零参调用快速体验
                                - 请求模型为 OpenAI 兼容接口（默认指向 DeepSeek），也支持通过 `local-config.yaml` 切到本地 Ollama
                                - RAG 示例使用内存向量库，**重启即丢**；知识库为懒加载，首次调用 RAG 接口时才会向量化

                                ## 相关地址

                                - 本模块示例导航（返回所有示例接口清单）: `GET /lc4j`
                                - Swagger UI: `http://localhost:8097/swagger-ui.html`
                                - OpenAPI JSON: `http://localhost:8097/v3/api-docs`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("LangChain4j Demo Team")
                                .email("support@aeryzhao.org"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("本地开发服务器")
                ));
    }
}
