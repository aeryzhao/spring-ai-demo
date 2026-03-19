package org.aeryzhao.springaidemo.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MCP Server 时间工具示例。
 *
 * @author zhaoxg
 * @date 2026/3/19 11:36
 */
@Component
public class TimeMcpServerTools {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Shanghai");

    @Tool(description = "查询指定时区的当前时间。zoneId 为空时默认返回 Asia/Shanghai 时区时间。")
    public TimeResult currentTime(TimeRequest request) {
        String zoneIdText = request == null ? null : request.zoneId();
        ZoneId zoneId = resolveZoneId(zoneIdText);
        ZonedDateTime zonedDateTime = Instant.now().atZone(zoneId);
        return new TimeResult(zoneId.getId(), FORMATTER.format(zonedDateTime), FORMATTER.format(zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"))));
    }

    private ZoneId resolveZoneId(String zoneIdText) {
        if (zoneIdText == null || zoneIdText.isBlank()) {
            return DEFAULT_ZONE_ID;
        }
        return ZoneId.of(zoneIdText.trim());
    }

    public record TimeRequest(String zoneId) {
    }

    public record TimeResult(String zoneId, String localTime, String utcTime) {
    }
}
