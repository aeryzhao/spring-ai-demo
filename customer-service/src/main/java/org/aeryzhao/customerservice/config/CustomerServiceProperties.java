package org.aeryzhao.customerservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 智能客服模块配置（映射 application.yml 中的 {@code customer-service.*}）。
 *
 * @param dataDir      数据根目录，内部按用途分 knowledge/ 与若干 json 文件；
 *                     相对路径会由 {@link DataDirectoryResolver} 定位（优先模块目录，其次工作目录）
 * @param topK         每次检索返回的知识片段数量
 * @param chunkSize    知识文档切块的字符上限
 * @param chunkOverlap 相邻切块的重叠字符数
 * @param memoryWindow 单会话保留的历史消息条数
 * @param crawl        抓虫相关参数
 */
@ConfigurationProperties(prefix = "customer-service")
public record CustomerServiceProperties(
        String dataDir,
        int topK,
        int chunkSize,
        int chunkOverlap,
        int memoryWindow,
        Crawl crawl) {

    public CustomerServiceProperties {
        dataDir = DataDirectoryResolver.resolve(dataDir);
        topK = topK <= 0 ? 4 : topK;
        chunkSize = chunkSize <= 0 ? 600 : chunkSize;
        chunkOverlap = chunkOverlap < 0 ? 100 : chunkOverlap;
        memoryWindow = memoryWindow <= 0 ? 20 : memoryWindow;
        crawl = crawl == null ? new Crawl(null, 20000, 800, false) : crawl;
    }

    public Path dataPath() {
        return Paths.get(dataDir).toAbsolutePath().normalize();
    }

    /** 抓取落地目录：data/knowledge。 */
    public Path knowledgePath() {
        return dataPath().resolve("knowledge");
    }

    /** 向量索引缓存文件，删除后重启会自动重建。 */
    public Path vectorStoreFile() {
        return dataPath().resolve("vector-store.json");
    }

    /**
     * 抓取参数。
     *
     * @param userAgent  请求头 UA，部分站点对默认 UA 会返回精简页
     * @param timeoutMs  单页请求超时
     * @param intervalMs 相邻两次请求的间隔，避免给对方站点造成压力
     * @param lenientTls 是否放宽 TLS 证书校验：部分国内站点的证书链不在 JDK 信任库中，
     *                   抓公开页面时可打开；生产环境对接自有系统请保持 false
     */
    public record Crawl(String userAgent, int timeoutMs, long intervalMs, boolean lenientTls) {

        public Crawl {
            userAgent = (userAgent == null || userAgent.isBlank())
                    ? "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
                    : userAgent;
            timeoutMs = timeoutMs <= 0 ? 20000 : timeoutMs;
            intervalMs = intervalMs < 0 ? 800 : intervalMs;
        }
    }
}
