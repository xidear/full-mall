package com.fullmall.order.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OrderLineSnapshotTest {

    @Test
    void includesOnlyPresentFields() {
        String json = OrderLineSnapshot.toJson("https://cdn/a.png", null, null);

        assertThat(json).contains("\"image\":\"https://cdn/a.png\"")
                .doesNotContain("attributes")
                .doesNotContain("listPriceAmount");
    }

    @Test
    void keepsAllProvidedFields() {
        String json = OrderLineSnapshot.toJson("img", "颜色:红;尺码:M", 9900L);

        assertThat(json).contains("image").contains("attributes").contains("listPriceAmount");
        assertThat(json).contains("9900");
    }

    @Test
    void attributesAreStableAndHumanReadable() {
        String text = OrderLine.attributesText(Map.of("尺码", "M", "颜色", "红"));

        assertThat(text).isEqualTo("尺码:M;颜色:红");
    }

    @Test
    void emptyOptionsYieldNoAttributes() {
        assertThat(OrderLine.attributesText(Map.of())).isNull();
        assertThat(OrderLine.attributesText(null)).isNull();
    }
}
