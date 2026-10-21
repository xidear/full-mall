package com.fullmall.platform.util;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 订单号时间必须按显式时区生成，不能隐式依赖服务器 TZ。 */
class OrderNoZoneTest {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Test
    void usesConfiguredZone() {
        Ids.configureZone("Asia/Shanghai");
        String expected = "FM" + ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).format(FMT);
        assertThat(Ids.orderNo()).startsWith(expected.substring(0, 12));

        Ids.configureZone("America/Los_Angeles");
        String expectedLa = "FM" + ZonedDateTime.now(ZoneId.of("America/Los_Angeles")).format(FMT);
        assertThat(Ids.orderNo()).startsWith(expectedLa.substring(0, 12));

        Ids.configureZone(ZoneId.systemDefault().getId());
    }

    @Test
    void invalidZoneFailsLoudly() {
        assertThatThrownBy(() -> Ids.configureZone("Mars/Olympus"))
                .isInstanceOf(java.time.DateTimeException.class);
        Ids.configureZone(ZoneId.systemDefault().getId());
    }
}
