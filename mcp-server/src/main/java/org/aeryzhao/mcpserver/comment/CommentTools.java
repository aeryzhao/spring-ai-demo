package org.aeryzhao.mcpserver.comment;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MCP Server 评论工具，模拟保存评论。
 */
@Component
public class CommentTools {

    private final List<SavedComment> comments = new CopyOnWriteArrayList<>();

    @McpTool(description = "模拟保存评论，返回生成的评论 ID、保存时间和状态。")
    public SaveCommentResponse saveComment(SaveCommentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        String articleId = normalizeRequired(request.articleId(), "articleId");
        String username = normalizeRequired(request.username(), "username");
        String content = normalizeRequired(request.content(), "content");

        SavedComment savedComment = new SavedComment(
                UUID.randomUUID().toString(),
                articleId,
                username,
                content,
                Instant.now().toString()
        );
        comments.add(savedComment);
        return new SaveCommentResponse(savedComment.id(), "SUCCESS", savedComment.savedAt(), comments.size());
    }

    @McpTool(description = "查询已经模拟保存的评论列表，可按 articleId 过滤。")
    public CommentListResponse listComments(ListCommentsRequest request) {
        String articleId = request == null ? null : normalizeOptional(request.articleId());
        List<SavedComment> result = comments.stream()
                .filter(comment -> articleId == null || articleId.equals(comment.articleId()))
                .toList();
        return new CommentListResponse(result.size(), result);
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record SaveCommentRequest(String articleId, String username, String content) {
    }

    public record SaveCommentResponse(String commentId, String status, String savedAt, int totalComments) {
    }

    public record ListCommentsRequest(String articleId) {
    }

    public record CommentListResponse(int total, List<SavedComment> comments) {
    }

    public record SavedComment(String id, String articleId, String username, String content, String savedAt) {
    }
}
