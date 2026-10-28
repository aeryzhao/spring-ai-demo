package org.aeryzhao.customerservice.config;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据目录定位器。
 *
 * <p>{@code customer-service.data-dir} 默认是相对路径 {@code data}，如果直接按「当前工作目录」解析，
 * 从仓库根用 IDE 启动（工作目录=仓库根）就会落到 {@code <repo>/data}，而不是模块自己的
 * {@code customer-service/data}。这里按下面的顺序找一个「看起来像数据目录」的候选位置：</p>
 *
 * <ol>
 *   <li>当前工作目录下的 {@code data-dir}</li>
 *   <li>模块根目录下的 {@code data-dir}（模块根由启动类的 code source 位置推断：
 *       {@code .../customer-service/target/classes} 或 {@code .../customer-service/target/app.jar} 都指向
 *       {@code .../customer-service}）</li>
 *   <li>当前工作目录下 {@code customer-service/data-dir}（从仓库根启动的另一种写法）</li>
 * </ol>
 *
 * <p>都不命中时，优先返回模块根下的路径（保证抓取产物落在模块里），否则返回工作目录下的路径，
 * 由调用方创建。命中结果会打一行 INFO 日志，便于排查「知识库目录不存在」这类问题。</p>
 */
@Slf4j
public final class DataDirectoryResolver {

    private static final String DEFAULT_DATA_DIR = "data";

    private DataDirectoryResolver() {
    }

    public static String resolve(String configured) {
        String value = (configured == null || configured.isBlank()) ? DEFAULT_DATA_DIR : configured.trim();
        Path configuredPath = Paths.get(value);

        if (configuredPath.isAbsolute()) {
            Path absolute = configuredPath.normalize();
            log.info("数据目录（绝对路径配置）：{}", absolute);
            return absolute.toString();
        }

        Path workingDir = Paths.get("").toAbsolutePath().normalize();
        Path moduleRoot = moduleRoot();

        Set<Path> candidates = new LinkedHashSet<>();
        candidates.add(workingDir.resolve(configuredPath).normalize());
        if (moduleRoot != null && moduleRoot.getFileSystem().equals(workingDir.getFileSystem())) {
            candidates.add(moduleRoot.resolve(configuredPath).normalize());
        }
        candidates.add(workingDir.resolve("customer-service").resolve(configuredPath).normalize());

        for (Path candidate : candidates) {
            if (looksLikeDataDirectory(candidate)) {
                log.info("数据目录：{}（工作目录：{}）", candidate, workingDir);
                return candidate.toString();
            }
        }

        Path fallback = (moduleRoot != null && moduleRoot.getFileSystem().equals(workingDir.getFileSystem()))
                ? moduleRoot.resolve(configuredPath).normalize()
                : workingDir.resolve(configuredPath).normalize();
        log.warn("未找到已存在的数据目录，将使用 {}（可用 customer-service.data-dir 覆盖）", fallback);
        return fallback.toString();
    }

    /** 目录存在，且包含 knowledge/ 或订单数据，才认为它是本模块的数据目录。 */
    private static boolean looksLikeDataDirectory(Path candidate) {
        if (candidate == null || !Files.isDirectory(candidate)) {
            return false;
        }
        return Files.isDirectory(candidate.resolve("knowledge"))
                || Files.exists(candidate.resolve("orders.json"))
                || Files.exists(candidate.resolve("vector-store.json"));
    }

    /**
     * 推断模块根目录（{@code customer-service/}）。
     *
     * <p>开发态：code source 是 {@code .../customer-service/target/classes}；
     * 打包运行：是 {@code jar:file:/.../target/customer-service-x.jar!/BOOT-INF/classes!/}。</p>
     *
     * <p>注意：Spring Boot 可执行 jar 的 code source 是 jar/zipfs 路径，与默认文件系统不是同一个
     * Provider，直接参与 {@code resolve} 会抛 {@code ProviderMismatchException}，
     * 因此这里统一换算成默认文件系统的路径，换算不了就返回 null。</p>
     */
    public static Path moduleRoot() {
        try {
            URL location = DataDirectoryResolver.class.getProtectionDomain().getCodeSource().getLocation();
            Path path = toDefaultFileSystemPath(location);
            if (path == null) {
                return null;
            }
            path = path.toAbsolutePath().normalize();

            List<Path> ancestors = new ArrayList<>();
            for (Path current = path; current != null; current = current.getParent()) {
                ancestors.add(current);
            }
            for (Path candidate : ancestors) {
                Path name = candidate.getFileName();
                if (name != null && "target".equals(name.toString()) && candidate.getParent() != null) {
                    return candidate.getParent();
                }
            }
            // 没有 target 目录：退回到 classes / jar 所在目录的上一级
            return Files.isDirectory(path) ? path : path.getParent();
        } catch (Exception e) {
            log.debug("推断模块根目录失败：{}", e.getMessage());
            return null;
        }
    }

    /** 把 code source 位置换算成默认文件系统的路径；jar:/nested: 链接会先剥掉内层路径。 */
    private static Path toDefaultFileSystemPath(URL location) throws java.net.URISyntaxException {
        if (location == null) {
            return null;
        }
        String spec = location.toString();
        if ("file".equalsIgnoreCase(location.getProtocol())) {
            return Paths.get(location.toURI());
        }
        // 形如 jar:file:/x/app.jar!/BOOT-INF/classes!/ 或 jar:nested:/x/app.jar/!BOOT-INF/classes!/
        int bang = spec.indexOf("!/");
        if (bang < 0) {
            bang = spec.indexOf("/!");
        }
        if (bang > 0) {
            spec = spec.substring(0, bang);
        }
        for (String prefix : new String[]{"jar:nested:", "jar:", "nested:"}) {
            if (spec.startsWith(prefix)) {
                spec = spec.substring(prefix.length());
                break;
            }
        }
        if (spec.startsWith("/")) {
            spec = "file:" + spec;
        }
        if (!spec.startsWith("file:")) {
            return null;
        }
        return Paths.get(URI.create(spec));
    }
}
