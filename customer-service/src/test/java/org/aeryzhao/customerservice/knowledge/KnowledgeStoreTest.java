package org.aeryzhao.customerservice.knowledge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeStoreTest {

    @Test
    @DisplayName("解析抓取器写入的 front matter")
    void parseFrontMatter() {
        String raw = """
                ---
                id: jd-tuihuanzhengce
                title: "我的商品是否可以退换货"
                category: "售后服务"
                source: https://in.m.jd.com/help/app/tuihuanzhengce.html
                fetchedAt: 2026-10-25T13:10:00+08:00
                ---

                # 我的商品是否可以退换货

                京东承诺符合以下情况……
                """;

        KnowledgeDocument document = KnowledgeStore.parse("jd-tuihuanzhengce.md", raw);

        assertThat(document.id()).isEqualTo("jd-tuihuanzhengce");
        assertThat(document.title()).isEqualTo("我的商品是否可以退换货");
        assertThat(document.category()).isEqualTo("售后服务");
        assertThat(document.source()).contains("in.m.jd.com");
        assertThat(document.fetchedAt()).startsWith("2026-10-25");
        assertThat(document.content()).startsWith("# 我的商品是否可以退换货");
        assertThat(document.content()).doesNotContain("fetchedAt");
    }

    @Test
    @DisplayName("没有 front matter 时用文件名兜底")
    void parseWithoutFrontMatter() {
        KnowledgeDocument document = KnowledgeStore.parse("plain.md", "# 标题\n\n正文内容");

        assertThat(document.id()).isEqualTo("plain");
        assertThat(document.title()).isEqualTo("plain");
        assertThat(document.category()).isEqualTo("未分类");
        assertThat(document.content()).isEqualTo("# 标题\n\n正文内容");
    }
}
