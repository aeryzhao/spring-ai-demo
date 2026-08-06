package org.aeryzhao.rag.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI ragOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RAG API")
                        .description("Retrieval-Augmented Generation API with Spring AI and Milvus\n\n" +
                                "## 功能特性\n\n" +
                                "- **文档管理**: 插入和管理文档到向量数据库\n" +
                                "- **语义搜索**: 基于向量相似度的文档检索\n" +
                                "- **RAG问答**: 结合检索和生成的智能问答系统\n\n" +
                                "## 技术栈\n\n" +
                                "- Spring Boot 4.1.0\n" +
                                "- Spring AI 2.0.0\n" +
                                "- Milvus Vector Database\n" +
                                "- OpenAI Embeddings")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("RAG Team")
                                .email("support@rag.com"))
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
