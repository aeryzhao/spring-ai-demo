package org.aeryzhao.customerservice.crawl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 抓取命令行入口：只在 {@code crawl} profile 下生效，跑完即退出，不会启动 Web 服务。
 *
 * <pre>
 * java -jar customer-service.jar --spring.profiles.active=crawl            # 增量抓取（已有文件跳过）
 * java -jar customer-service.jar --spring.profiles.active=crawl --force    # 全量覆盖
 * </pre>
 */
@Slf4j
@Component
@Profile("crawl")
public class CrawlRunner implements ApplicationRunner {

    private final KnowledgeCrawler crawler;
    private final ConfigurableApplicationContext context;

    public CrawlRunner(KnowledgeCrawler crawler, ConfigurableApplicationContext context) {
        this.crawler = crawler;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean force = args.containsOption("force");
        log.info("开始抓取知识库，force={}", force);

        List<CrawlResult> results = crawler.crawlAll(force);

        long ok = results.stream().filter(r -> r.ok() && !r.skipped()).count();
        long skipped = results.stream().filter(CrawlResult::skipped).count();
        long failed = results.stream().filter(r -> !r.ok()).count();

        System.out.printf("%n抓取结束：成功 %d / 跳过 %d / 失败 %d%n", ok, skipped, failed);
        for (CrawlResult result : results) {
            String flag = result.ok() ? (result.skipped() ? "SKIP" : " OK ") : "FAIL";
            System.out.printf("[%s] %-24s %-30s %s%n",
                    flag, result.slug(), abbreviate(result.title(), 28),
                    result.ok() ? result.chars() + " 字" : result.message());
        }

        int exitCode = failed > 0 ? 1 : 0;
        SpringApplication.exit(context, () -> exitCode);
        System.exit(exitCode);
    }

    private String abbreviate(String value, int max) {
        if (value == null) {
            return "-";
        }
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }
}
