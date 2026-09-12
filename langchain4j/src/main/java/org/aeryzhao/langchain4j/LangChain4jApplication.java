package org.aeryzhao.langchain4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LangChain4j 示例模块入口。
 *
 * <p>本模块与 {@code chat} 模块并列，用于对比 Spring AI 与 LangChain4j 这两套 Java AI 框架的用法差异。
 * 启动后访问 <a href="http://localhost:8097/swagger-ui.html">Swagger UI</a> 逐个调试示例。
 */
@SpringBootApplication
public class LangChain4jApplication {

    public static void main(String[] args) {
        SpringApplication.run(LangChain4jApplication.class, args);
    }

}
