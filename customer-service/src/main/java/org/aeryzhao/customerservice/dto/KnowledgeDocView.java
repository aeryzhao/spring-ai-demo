package org.aeryzhao.customerservice.dto;

/**
 * 知识库文档视图（不含正文，避免接口返回过大）。
 *
 * @param id        文档 id
 * @param title     标题
 * @param category  分类
 * @param source    原始网页地址
 * @param fetchedAt 抓取时间
 * @param chars     正文字符数
 */
public record KnowledgeDocView(String id, String title, String category, String source, String fetchedAt, int chars) {
}
