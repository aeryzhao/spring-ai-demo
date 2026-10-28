package org.aeryzhao.customerservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据目录定位：无论从仓库根还是模块目录启动，都应落到 {@code customer-service/data}。
 */
class DataDirectoryResolverTest {

    @Test
    @DisplayName("能从启动类位置推断出模块根目录")
    void resolvesModuleRoot() {
        Path moduleRoot = DataDirectoryResolver.moduleRoot();

        assertThat(moduleRoot).isNotNull();
        assertThat(moduleRoot.getFileName().toString()).isEqualTo("customer-service");
    }

    @Test
    @DisplayName("相对路径解析到模块内的 data 目录（而不是工作目录）")
    void resolvesRelativeDataDirToModule() {
        String resolved = DataDirectoryResolver.resolve("data");
        Path path = Paths.get(resolved);

        assertThat(path.isAbsolute()).isTrue();
        assertThat(path.getFileName().toString()).isEqualTo("data");
        assertThat(path.getParent().getFileName().toString()).isEqualTo("customer-service");
        assertThat(Files.isDirectory(path.resolve("knowledge"))).isTrue();
    }

    @Test
    @DisplayName("绝对路径配置原样使用")
    void keepsAbsolutePath() {
        Path absolute = Paths.get(System.getProperty("java.io.tmpdir"), "cs-data-test").toAbsolutePath().normalize();

        assertThat(DataDirectoryResolver.resolve(absolute.toString())).isEqualTo(absolute.toString());
    }

    @Test
    @DisplayName("空配置回落到默认 data 目录")
    void fallsBackToDefault() {
        assertThat(DataDirectoryResolver.resolve(null)).endsWith("data");
        assertThat(DataDirectoryResolver.resolve("  ")).endsWith("data");
    }
}
