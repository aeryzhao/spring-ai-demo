package org.aeryzhao.customerservice.knowledge;

/**
 * 检索命中的知识片段。
 *
 * @param chunkId  片段 id（docId#序号）
 * @param docId    所属文档 id
 * @param title    文档标题
 * @param category 文档分类
 * @param source   原始网页地址
 * @param text     片段正文
 * @param score    相似度/关键词得分，仅用于排序与展示
 */
public record RetrievedChunk(
        String chunkId,
        String docId,
        String title,
        String category,
        String source,
        String text,
        double score) {
}
