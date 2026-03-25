package org.aeryzhao.mcpserver;

import org.aeryzhao.mcpserver.comment.CommentTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpServerApplication {
    @Bean
    public ToolCallbackProvider commentToolCallbackProvider(CommentTools commentTools) {
        return MethodToolCallbackProvider.builder().toolObjects(commentTools).build();
    }

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

}
