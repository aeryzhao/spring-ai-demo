package org.aeryzhao.customerservice.crawl;

import lombok.extern.slf4j.Slf4j;
import org.aeryzhao.customerservice.config.CustomerServiceProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把公开网页抓成本地知识库：{@code data/knowledge/<slug>.md}。
 *
 * <p>抓取目标都是服务端直出 HTML 的公开帮助中心/政策页面，无需浏览器渲染。抓取链路：
 * {@link HttpClient} 取原始 HTML（对证书链不在 JDK 信任库中的站点可放宽 TLS 校验）→ Jsoup 解析 →
 * 抽取正文容器并转成 Markdown → 写入「YAML front matter + 正文」的 .md 文件。</p>
 */
@Slf4j
@Component
public class KnowledgeCrawler {

    /** 视为「独占一行」的块级标签，决定正文的换行点。 */
    private static final Set<String> BLOCK_TAGS = Set.of(
            "p", "div", "section", "article", "li", "ul", "ol", "dl", "dt", "dd",
            "h1", "h2", "h3", "h4", "h5", "h6", "table", "tbody", "tr", "td", "th",
            "blockquote", "pre", "br", "header", "footer", "aside");

    private static final Set<String> DROP_TAGS = Set.of("script", "style", "noscript", "iframe");

    /** 站点通用标题，命中时改用正文里的一级标题。 */
    private static final Set<String> GENERIC_TITLES = Set.of("帮助中心", "帮助中心-京东", "服务中心", "中国政府网");

    private static final Pattern CHARSET_PATTERN = Pattern.compile("charset=[\"']?([\\w-]+)");

    /** 去掉「5.1 」「2.1.3、」这类章节编号前缀。 */
    private static final Pattern SECTION_PREFIX = Pattern.compile("^\\d+(\\.\\d+)*[\\s.、:：]*");

    private final CustomerServiceProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public KnowledgeCrawler(CustomerServiceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = buildHttpClient(properties.crawl());
    }

    /** 读取抓取源清单。 */
    public List<CrawlSource> sources() {
        ClassPathResource resource = new ClassPathResource("crawl/sources.json");
        try (InputStream in = resource.getInputStream()) {
            SourcesFile file = objectMapper.readValue(in, SourcesFile.class);
            return file.sources() == null ? List.of() : file.sources();
        } catch (IOException | JacksonException e) {
            throw new IllegalStateException("读取抓取源清单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 全量抓取。
     *
     * @param force true 表示覆盖本地已有文件，false 表示已存在就跳过（增量）
     */
    public List<CrawlResult> crawlAll(boolean force) {
        List<CrawlSource> sources = sources();
        List<CrawlResult> results = new ArrayList<>(sources.size());
        Path knowledgeDir = properties.knowledgePath();

        try {
            Files.createDirectories(knowledgeDir);
        } catch (IOException e) {
            throw new IllegalStateException("创建知识库目录失败: " + knowledgeDir, e);
        }

        for (CrawlSource source : sources) {
            Path target = knowledgeDir.resolve(source.slug() + ".md");
            if (Files.exists(target) && !force) {
                results.add(new CrawlResult(source.slug(), readTitle(target), true, true, 0, "本地已存在，跳过"));
                continue;
            }
            try {
                Page page = fetch(source);
                if (page.lines().isEmpty()) {
                    results.add(new CrawlResult(source.slug(), page.title(), false, false, 0, "正文为空，可能页面结构变化"));
                    continue;
                }
                String markdown = renderMarkdown(source, page);
                Files.writeString(target, markdown, StandardCharsets.UTF_8);
                results.add(new CrawlResult(source.slug(), page.title(), true, false, markdown.length(), null));
                log.info("抓取完成 {} -> {} ({} 字)", source.url(), target.getFileName(), markdown.length());
            } catch (Exception e) {
                log.warn("抓取失败 {}: {}", source.url(), e.getMessage());
                results.add(new CrawlResult(source.slug(), null, false, false, 0, e.getMessage()));
            }
            sleep(properties.crawl().intervalMs());
        }
        return results;
    }

    /** 取页面并抽取标题与正文行。 */
    private Page fetch(CrawlSource source) throws IOException {
        Document document = loadDocument(source);

        String title = cleanTitle(document.title());
        Element container;

        if ("GOV_POLICY".equalsIgnoreCase(source.extractor())) {
            container = document.selectFirst("#UCAP-CONTENT");
            if (isBlank(title)) {
                title = cleanTitle(firstText(document, "h1", ".pages_title", ".article_title"));
            }
        } else {
            container = document.selectFirst("div.help-box");
            if (isBlank(title) || GENERIC_TITLES.contains(title)) {
                title = cleanTitle(firstText(document, "h3.help-tit01", "h1"));
            }
        }

        if (container == null) {
            container = document.body();
        }
        if (isBlank(title)) {
            title = source.slug();
        }

        List<String> lines = new ArrayList<>();
        if (container != null) {
            walk(container, lines, "");
        }
        return new Page(title, clean(lines, title));
    }

    /**
     * 主链路用 Jsoup（基于 HttpURLConnection）取页面：部分站点会针对非浏览器客户端
     * （如 Java HttpClient 的 TLS 指纹）返回精简版页面，Jsoup 拿到的内容更完整。
     * 只有遇到证书链不被信任时，才按配置改用宽松 TLS 的 HttpClient 兜底。
     */
    private Document loadDocument(CrawlSource source) throws IOException {
        try {
            return Jsoup.connect(source.url())
                    .userAgent(properties.crawl().userAgent())
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .timeout(properties.crawl().timeoutMs())
                    .followRedirects(true)
                    .get();
        } catch (IOException e) {
            if (!properties.crawl().lenientTls() || !isTlsFailure(e)) {
                throw e;
            }
            log.warn("TLS 证书校验失败，改用宽松校验重试 {}：{}", source.url(), e.getMessage());
            return Jsoup.parse(fetchHtmlLenient(source), source.url());
        }
    }

    private boolean isTlsFailure(Throwable error) {
        Throwable current = error;
        while (current != null && current.getCause() != current) {
            if (current instanceof SSLException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String fetchHtmlLenient(CrawlSource source) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(source.url()))
                .timeout(Duration.ofMillis(properties.crawl().timeoutMs()))
                .header("User-Agent", properties.crawl().userAgent())
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .header("Accept", "text/html,application/xhtml+xml")
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                throw new IOException("HTTP " + response.statusCode());
            }
            return new String(response.body(), charsetOf(response));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("抓取被中断", e);
        }
    }

    private HttpClient buildHttpClient(CustomerServiceProperties.Crawl crawl) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofMillis(crawl.timeoutMs()));
        if (crawl.lenientTls()) {
            builder.sslContext(lenientSslContext());
            log.warn("已启用宽松 TLS 校验（customer-service.crawl.lenient-tls=true），用于证书链不在 JDK 信任库中的公开站点");
        }
        return builder.build();
    }

    /** 信任任意证书的 SSLContext，供证书链不被 JDK 信任库覆盖的公开站点兜底使用。 */
    private SSLContext lenientSslContext() {
        TrustManager[] trustManagers = {new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustManagers, new SecureRandom());
            return context;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("初始化 TLS 上下文失败", e);
        }
    }

    /** 先看响应头 charset，再看 HTML 里的 meta charset，最后退回 UTF-8。 */
    private Charset charsetOf(HttpResponse<byte[]> response) {
        String contentType = response.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);
        Matcher headerMatcher = CHARSET_PATTERN.matcher(contentType);
        if (headerMatcher.find()) {
            return charsetOfName(headerMatcher.group(1));
        }
        byte[] body = response.body();
        String prefix = new String(body, 0, Math.min(body.length, 1024), StandardCharsets.ISO_8859_1)
                .toLowerCase(Locale.ROOT);
        Matcher metaMatcher = CHARSET_PATTERN.matcher(prefix);
        if (metaMatcher.find()) {
            return charsetOfName(metaMatcher.group(1));
        }
        return StandardCharsets.UTF_8;
    }

    private Charset charsetOfName(String name) {
        try {
            return Charset.forName(name);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private String cleanTitle(String raw) {
        if (raw == null) {
            return null;
        }
        String title = raw.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
        int underscore = title.indexOf('_');
        if (underscore > 0) {
            title = title.substring(0, underscore).trim();
        }
        title = SECTION_PREFIX.matcher(title).replaceFirst("").trim();
        return title.isEmpty() ? null : title;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstText(Document document, String... selectors) {
        for (String selector : selectors) {
            Element element = document.selectFirst(selector);
            if (element != null && !element.text().isBlank()) {
                return element.text().trim();
            }
        }
        return null;
    }

    /**
     * 递归遍历 DOM，遇到块级元素就换行，把行内内容拼成一行，尽量保留原文的层次。
     */
    private void walk(Element element, List<String> out, String prefix) {
        StringBuilder inline = new StringBuilder();
        String currentPrefix = prefix;
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode textNode) {
                inline.append(textNode.text());
            } else if (node instanceof Element child) {
                String tag = child.tagName().toLowerCase(Locale.ROOT);
                if (DROP_TAGS.contains(tag)) {
                    continue;
                }
                if (BLOCK_TAGS.contains(tag)) {
                    flush(out, currentPrefix, inline);
                    currentPrefix = "";
                    if ("br".equals(tag)) {
                        continue;
                    }
                    walk(child, out, childPrefix(child));
                } else {
                    inline.append(child.text());
                }
            }
        }
        flush(out, currentPrefix, inline);
    }

    private String childPrefix(Element element) {
        String tag = element.tagName().toLowerCase(Locale.ROOT);
        if ("li".equals(tag)) {
            return "- ";
        }
        if (tag.matches("h[1-6]")) {
            return "### ";
        }
        return "";
    }

    private void flush(List<String> out, String prefix, StringBuilder inline) {
        String line = inline.toString().replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
        inline.setLength(0);
        if (line.isEmpty()) {
            return;
        }
        out.add(prefix + line);
    }

    private List<String> clean(List<String> lines, String title) {
        List<String> result = new ArrayList<>(lines.size());
        String previous = null;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            // 页面标题会单独写进 front matter 与一级标题，正文里重复出现时去掉（忽略标点差异）
            if (result.isEmpty() && normalizeForCompare(line).equals(normalizeForCompare(title))) {
                continue;
            }
            if (line.equals(previous)) {
                continue;
            }
            result.add(line);
            previous = line;
        }
        return result;
    }

    /** 去掉 Markdown 前缀与首尾标点，仅用于标题去重比较。 */
    private String normalizeForCompare(String value) {
        if (value == null) {
            return "";
        }
        String text = value.replaceFirst("^#{1,6}\\s*", "").trim();
        return text.replaceAll("[\\s？?！!。.、，,：:；;]+$", "");
    }

    private String renderMarkdown(CrawlSource source, Page page) {
        String fetchedAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        StringBuilder sb = new StringBuilder(4096);
        sb.append("---\n");
        sb.append("id: ").append(source.slug()).append('\n');
        sb.append("title: ").append(quote(page.title())).append('\n');
        sb.append("category: ").append(quote(source.category())).append('\n');
        sb.append("source: ").append(source.url()).append('\n');
        sb.append("fetchedAt: ").append(fetchedAt).append('\n');
        sb.append("---\n\n");
        sb.append("# ").append(page.title()).append("\n\n");
        for (String line : page.lines()) {
            sb.append(line).append('\n');
            if (line.startsWith("- ") || line.startsWith("### ")) {
                continue;
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String quote(String value) {
        String safe = value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + safe + "\"";
    }

    private String readTitle(Path file) {
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.startsWith("title:")) {
                    return line.substring("title:".length()).trim().replaceAll("^\"|\"$", "");
                }
                if (line.startsWith("# ")) {
                    return line.substring(2).trim();
                }
            }
        } catch (IOException e) {
            log.debug("读取标题失败 {}: {}", file, e.getMessage());
        }
        return file.getFileName().toString();
    }

    private void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record Page(String title, List<String> lines) {
    }

    private record SourcesFile(List<CrawlSource> sources) {
    }
}
