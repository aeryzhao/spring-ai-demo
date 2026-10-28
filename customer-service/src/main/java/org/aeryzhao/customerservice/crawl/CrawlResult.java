package org.aeryzhao.customerservice.crawl;

/**
 * 单个抓取源的执行结果。
 *
 * @param slug    知识文档 id
 * @param title   页面标题（失败时为 null）
 * @param ok      是否成功
 * @param skipped 是否因为本地文件已存在而跳过
 * @param chars   正文有效字符数
 * @param message 失败原因或备注
 */
public record CrawlResult(String slug, String title, boolean ok, boolean skipped, int chars, String message) {
}
