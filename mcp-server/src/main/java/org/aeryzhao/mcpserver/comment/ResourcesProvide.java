package org.aeryzhao.mcpserver.comment;

import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

/**
 * MCP Server 资源与提示词示例。
 */
@Component
public class ResourcesProvide {

    @McpResource(
            name = "comment-guideline-resource",
            title = "评论规范资源",
            uri = "resource://comment/guideline",
            description = "返回评论场景的内容规范与审核建议。",
            mimeType = "application/json"
    )
    public String commentGuideline() {
        return """
                {
                  "scene": "comment",
                  "version": "1.0.0",
                  "rules": [
                    "评论内容应聚焦文章主题",
                    "避免辱骂、广告和无意义灌水",
                    "优先给出具体观点或改进建议"
                  ],
                  "recommendedFields": ["articleId", "username", "content"]
                }
                """;
    }
}
