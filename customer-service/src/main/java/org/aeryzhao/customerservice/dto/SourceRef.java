package org.aeryzhao.customerservice.dto;

/**
 * 引用到的知识片段。
 *
 * @param chunkId  片段 id
 * @param title    文档标题
 * @param category 文档分类
 * @param source   原始网页地址
 * @param snippet  片段摘要（供前端展示）
 * @param score    相关度得分
 */
public record SourceRef(String chunkId, String title, String category, String source, String snippet, double score) {
}
