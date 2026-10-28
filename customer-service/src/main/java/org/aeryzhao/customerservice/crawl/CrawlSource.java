package org.aeryzhao.customerservice.crawl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 一条抓取源配置（对应 classpath:crawl/sources.json 中的一项）。
 *
 * @param slug      落地文件名前缀，同时作为知识文档 id
 * @param category  知识分类，用于元数据与前端展示
 * @param extractor 正文抽取器名称：{@code JD_HELP} / {@code GOV_POLICY}
 * @param url       页面地址
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CrawlSource(String slug, String category, String extractor, String url) {
}
