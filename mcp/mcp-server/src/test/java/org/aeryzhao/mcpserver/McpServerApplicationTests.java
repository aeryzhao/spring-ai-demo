package org.aeryzhao.mcpserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * MCP Server 侧上下文冒烟测试。
 *
 * <p>拆分为独立子模块后，本模块只有一个 {@code @SpringBootApplication} 入口，
 * 但仍显式指定 {@code classes} 以明确测试目标。单一角色不再需要 profile。</p>
 *
 * <p>该测试只做 contextLoads，不依赖外部 MCP 服务（server 侧只暴露服务端，
 * 不会去连接 8081）也不调用真实模型。</p>
 */
@SpringBootTest(classes = McpServerApplication.class)
class McpServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
