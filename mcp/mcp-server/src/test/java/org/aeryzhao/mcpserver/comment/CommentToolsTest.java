package org.aeryzhao.mcpserver.comment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommentToolsTest {

    private final CommentTools commentTools = new CommentTools();

    @Test
    void shouldSaveCommentSuccessfully() {
        CommentTools.SaveCommentResponse response = commentTools.saveComment(
                new CommentTools.SaveCommentRequest("article-1001", "alice", "这是一条测试评论")
        );

        assertNotNull(response.commentId());
        assertEquals("SUCCESS", response.status());
        assertEquals(1, response.totalComments());
    }

    @Test
    void shouldFilterCommentsByArticleId() {
        commentTools.saveComment(new CommentTools.SaveCommentRequest("article-1001", "alice", "第一条评论"));
        commentTools.saveComment(new CommentTools.SaveCommentRequest("article-1002", "bob", "第二条评论"));

        CommentTools.CommentListResponse response = commentTools.listComments(
                new CommentTools.ListCommentsRequest("article-1001")
        );

        assertEquals(1, response.total());
        assertEquals("article-1001", response.comments().get(0).articleId());
    }

    @Test
    void shouldRejectBlankContent() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> commentTools.saveComment(new CommentTools.SaveCommentRequest("article-1001", "alice", "  ")));

        assertEquals("content 不能为空", exception.getMessage());
    }
}
