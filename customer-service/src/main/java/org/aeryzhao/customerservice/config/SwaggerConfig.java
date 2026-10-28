package org.aeryzhao.customerservice.config;

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
 * 智能客服模块的 OpenAPI 文档配置。
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8098}")
    private String serverPort;

    @Bean
    public OpenAPI customerServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智能客服 Demo API")
                        .description("""
                                一个完整的智能客服案例：网页抓取 → 本地知识库 → 向量检索 → 大模型 + 工具调用 + 多轮记忆 + 流式输出。

                                ## 功能分组

                                - **对话**: `/api/chat` 同步问答，`/api/chat/stream` SSE 流式问答（带引用与工具调用轨迹）
                                - **知识库**: 文档清单、索引状态、重新抓取、重建索引
                                - **业务数据**: 订单 / 物流 / 工单查询（演示用模拟数据）

                                ## 使用说明

                                - 对话依赖 `local-config.yaml` 中的 DeepSeek 配置；向量化依赖本机 Ollama
                                （`qwen3-embedding`），Ollama 不可用时自动降级为关键词检索，功能仍可用。
                                - 订单、物流为本地模拟数据（`data/*.json`），知识库为从公开帮助中心/政策页面抓取的真实内容。
                                - 聊天记忆保存在进程内存中，重启即丢失；多次请求请保持相同的 `sessionId`。
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("customer-service module").email("support@aeryzhao.org"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(new Server()
                        .url("http://localhost:" + serverPort)
                        .description("本地开发服务器（智能客服模块默认端口 8098）")));
    }
}
