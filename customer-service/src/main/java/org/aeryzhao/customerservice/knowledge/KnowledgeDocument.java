package org.aeryzhao.customerservice.knowledge;

/**
 * 本地知识库中的一篇文档（对应 data/knowledge/*.md）。
 *
 * @param id        文档 id，取自 front matter 或文件名
 * @param title     标题
 * @param category  分类
 * @param source    原始网页地址
 * @param fetchedAt 抓取时间
 * @param content   Markdown 正文（已去掉 front matter）
 * @param fileName  落盘文件名
 */
public record KnowledgeDocument(
        String id,
        String title,
        String category,
        String source,
        String fetchedAt,
        String content,
        String fileName) {
}
