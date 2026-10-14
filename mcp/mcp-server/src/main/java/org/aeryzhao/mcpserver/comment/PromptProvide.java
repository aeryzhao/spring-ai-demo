package org.aeryzhao.mcpserver.comment;

import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

/**
 * @author zhaoxg
 * @date 2026/3/27 11:50
 */
@Component
public class PromptProvide {
    @McpPrompt(
            name = "comment-summary-prompt",
            title = "评论总结提示词",
            description = "根据文章 ID 生成一段可直接发送给模型的评论处理提示词。"
    )
    public String commentSummaryPrompt(
            @McpArg(name = "articleId", description = "文章 ID", required = true) String articleId,
            @McpArg(name = "username", description = "评论用户名", required = true) String username,
            @McpArg(name = "content", description = "评论内容", required = true) String content) {
        return "你是评论助手。请先调用 saveComment 工具保存评论，参数如下：articleId=" + articleId
                + ", username=" + username
                + ", content=" + content
                + "。然后调用 listComments 工具查询 articleId=" + articleId
                + " 的评论列表，最后输出：1）保存结果；2）评论总数；3）一句中文总结。";
    }
}
