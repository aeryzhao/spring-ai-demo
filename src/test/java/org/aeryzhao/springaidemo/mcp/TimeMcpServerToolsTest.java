package org.aeryzhao.springaidemo.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeMcpServerToolsTest {

    private final TimeMcpServerTools tools = new TimeMcpServerTools();

    @Test
    void currentTimeShouldUseDefaultShanghaiZoneWhenZoneIsBlank() {
        TimeMcpServerTools.TimeResult result = tools.currentTime(new TimeMcpServerTools.TimeRequest(" "));

        assertThat(result.zoneId()).isEqualTo("Asia/Shanghai");
        assertThat(result.localTime()).isNotBlank();
        assertThat(result.utcTime()).endsWith("Z");
    }

    @Test
    void currentTimeShouldUseRequestedZone() {
        TimeMcpServerTools.TimeResult result = tools.currentTime(new TimeMcpServerTools.TimeRequest("UTC"));

        assertThat(result.zoneId()).isEqualTo("UTC");
        assertThat(result.localTime()).endsWith("Z");
        assertThat(result.utcTime()).endsWith("Z");
    }

    @Test
    void currentTimeShouldRejectInvalidZone() {
        assertThatThrownBy(() -> tools.currentTime(new TimeMcpServerTools.TimeRequest("Mars/Base")))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Unknown time-zone ID");
    }
}
