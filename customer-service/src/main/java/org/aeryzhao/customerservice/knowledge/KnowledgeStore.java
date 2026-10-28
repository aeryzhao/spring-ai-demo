package org.aeryzhao.customerservice.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.aeryzhao.customerservice.config.CustomerServiceProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 读取 {@code data/knowledge/*.md}：解析 front matter + 正文。
 *
 * <p>front matter 由抓取器写入，形如：</p>
 * <pre>
 * ---
 * id: jd-tuihuanzhengce
 * title: "我的商品是否可以退换货"
 * category: "售后服务"
 * source: https://in.m.jd.com/help/app/tuihuanzhengce.html
 * fetchedAt: 2026-10-25T13:10:00+08:00
 * ---
 * </pre>
 */
@Slf4j
@Component
public class KnowledgeStore {

    private static final String SEPARATOR = "---";

    private final CustomerServiceProperties properties;

    public KnowledgeStore(CustomerServiceProperties properties) {
        this.properties = properties;
    }

    public Path directory() {
        return properties.knowledgePath();
    }

    /** 按文件名排序读取全部知识文档。 */
    public List<KnowledgeDocument> loadAll() {
        Path dir = directory();
        if (!Files.isDirectory(dir)) {
            log.warn("知识库目录不存在：{}（当前数据目录 {}）。请确认 customer-service.data-dir 指向模块的 data 目录，或先执行抓取：java -jar target/customer-service-*.jar --spring.profiles.active=crawl --force", dir, properties.dataPath());
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> markdownFiles = files
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();

            List<KnowledgeDocument> documents = new ArrayList<>(markdownFiles.size());
            for (Path file : markdownFiles) {
                try {
                    documents.add(parse(file));
                } catch (Exception e) {
                    log.warn("解析知识文档失败 {}: {}", file.getFileName(), e.getMessage());
                }
            }
            return documents;
        } catch (IOException e) {
            log.error("遍历知识库目录失败 {}", dir, e);
            return List.of();
        }
    }

    public KnowledgeDocument parse(Path file) throws IOException {
        String raw = Files.readString(file, StandardCharsets.UTF_8);
        return parse(file.getFileName().toString(), raw);
    }

    static KnowledgeDocument parse(String fileName, String raw) {
        String text = raw.replace("\r\n", "\n");
        String id = fileName.replaceFirst("\\.md$", "");
        String title = id;
        String category = "未分类";
        String source = "";
        String fetchedAt = "";
        String content = text;

        if (text.startsWith(SEPARATOR)) {
            int end = text.indexOf("\n" + SEPARATOR, SEPARATOR.length());
            if (end > 0) {
                String frontMatter = text.substring(SEPARATOR.length(), end);
                content = text.substring(end + SEPARATOR.length() + 1).trim();
                for (String line : frontMatter.split("\n")) {
                    int colon = line.indexOf(':');
                    if (colon <= 0) {
                        continue;
                    }
                    String key = line.substring(0, colon).trim();
                    String value = stripQuotes(line.substring(colon + 1).trim());
                    switch (key) {
                        case "id" -> id = value;
                        case "title" -> title = value;
                        case "category" -> category = value;
                        case "source" -> source = value;
                        case "fetchedAt" -> fetchedAt = value;
                        default -> {
                        }
                    }
                }
            }
        }
        return new KnowledgeDocument(id, title, category, source, fetchedAt, content, fileName);
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return value;
    }
}
