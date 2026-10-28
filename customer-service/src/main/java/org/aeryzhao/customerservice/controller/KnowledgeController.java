package org.aeryzhao.customerservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.aeryzhao.customerservice.crawl.CrawlResult;
import org.aeryzhao.customerservice.crawl.KnowledgeCrawler;
import org.aeryzhao.customerservice.dto.KnowledgeDocView;
import org.aeryzhao.customerservice.knowledge.KnowledgeDocument;
import org.aeryzhao.customerservice.knowledge.KnowledgeIndexService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库运维接口：看有哪些文档、索引状态如何、重新抓取、重建索引。
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@Tag(name = "知识库", description = "抓取结果、索引状态与重建入口")
public class KnowledgeController {

    private final KnowledgeCrawler crawler;
    private final KnowledgeIndexService indexService;

    public KnowledgeController(KnowledgeCrawler crawler, KnowledgeIndexService indexService) {
        this.crawler = crawler;
        this.indexService = indexService;
    }

    @Operation(summary = "知识文档清单", description = "列出 data/knowledge 下所有已抓取文档（不含正文）")
    @GetMapping("/documents")
    public List<KnowledgeDocView> documents() {
        return indexService.documents().stream()
                .map(this::toView)
                .toList();
    }

    @Operation(summary = "索引状态", description = "当前检索模式（VECTOR/KEYWORD）、文档与片段数量、索引构建时间")
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return indexService.stats();
    }

    @Operation(summary = "重新抓取并重建索引",
            description = """
                    按 `crawl/sources.json` 重新抓取网页并落盘，随后重建向量索引。\
                    `force=false` 时只补齐本地缺失的文档。抓取为阻塞操作，通常需要 20-60 秒。
                    """)
    @PostMapping("/crawl")
    public Map<String, Object> crawl(@RequestParam(value = "force", defaultValue = "false") boolean force) {
        long start = System.currentTimeMillis();
        List<CrawlResult> results = crawler.crawlAll(force);
        indexService.rebuild(true);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", results.stream().filter(r -> r.ok() && !r.skipped()).count());
        response.put("skipped", results.stream().filter(CrawlResult::skipped).count());
        response.put("failed", results.stream().filter(r -> !r.ok()).count());
        response.put("elapsedMs", System.currentTimeMillis() - start);
        response.put("details", results);
        response.put("index", indexService.stats());
        return response;
    }

    @Operation(summary = "重建索引", description = "不重新抓取，仅按当前 data/knowledge 内容重建向量索引")
    @PostMapping("/reindex")
    public Map<String, Object> reindex() {
        indexService.rebuild(true);
        return indexService.stats();
    }

    private KnowledgeDocView toView(KnowledgeDocument document) {
        return new KnowledgeDocView(
                document.id(),
                document.title(),
                document.category(),
                document.source(),
                document.fetchedAt(),
                document.content().length());
    }
}
