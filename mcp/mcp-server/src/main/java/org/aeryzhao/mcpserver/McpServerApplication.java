package org.aeryzhao.mcpserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP Server 子模块入口（暴露 MCP Server 能力，默认端口 8081）。
 *
 * <p>拆分为独立子模块后，本模块只包含 server 侧代码，不再有 client 代码可被误扫，
 * 因此使用裸 {@code @SpringBootApplication}（默认从本包向下扫描）即可，
 * 无需额外限定扫描包，也无需激活任何 profile。</p>
 */
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

}
