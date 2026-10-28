package org.aeryzhao.customerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 智能客服 Demo 启动类。
 *
 * <p>核心链路：网页抓取 → 本地知识库 → 向量检索 → ChatClient（系统提示词 + 参考资料 + 工具调用 + 多轮记忆）→ 流式输出。</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }

}
