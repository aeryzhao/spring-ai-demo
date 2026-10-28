package org.aeryzhao.customerservice.knowledge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChunkerTest {

    @Test
    @DisplayName("按段落装箱，块的字符数不超过上限")
    void splitRespectsMaxChars() {
        String text = "第一段内容。\n第二段内容比较长一些，用来测试装箱效果。\n第三段内容。";
        List<String> chunks = DocumentChunker.split(text, 30, 5);

        assertThat(chunks).isNotEmpty();
        chunks.forEach(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(30 + 5));
        assertThat(String.join("\n", chunks)).contains("第一段内容", "第三段内容");
    }

    @Test
    @DisplayName("超长段落被切分且相邻块存在重叠")
    void splitLongParagraphWithOverlap() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            builder.append("客服政策条款").append(i).append('。');
        }
        List<String> chunks = DocumentChunker.split(builder.toString(), 100, 20);

        assertThat(chunks.size()).isGreaterThan(1);
        chunks.forEach(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(100));
    }

    @Test
    @DisplayName("空内容返回空列表")
    void splitBlankText() {
        assertThat(DocumentChunker.split("   \n\n  ", 100, 10)).isEmpty();
        assertThat(DocumentChunker.split(null, 100, 10)).isEmpty();
    }
}
