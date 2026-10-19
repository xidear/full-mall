package com.fullmall.supplier.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupplierIpAllowlistTest {

    @Test
    void emptyConfigMeansUnrestricted() {
        assertThat(SupplierIpAllowlist.parse(null).allows("203.0.113.9")).isTrue();
        assertThat(SupplierIpAllowlist.parse("  ").allows("203.0.113.9")).isTrue();
        assertThat(SupplierIpAllowlist.parse("").isConfigured()).isFalse();
    }

    @Test
    void matchesExactIpAndCidr() {
        var allowlist = SupplierIpAllowlist.parse("203.0.113.7, 10.0.0.0/8, 192.168.1.0/24");

        assertThat(allowlist.isConfigured()).isTrue();
        assertThat(allowlist.allows("203.0.113.7")).isTrue();
        assertThat(allowlist.allows("10.255.0.9")).isTrue();
        assertThat(allowlist.allows("192.168.1.42")).isTrue();
        assertThat(allowlist.allows("192.168.2.42")).isFalse();
        assertThat(allowlist.allows("11.0.0.1")).isFalse();
    }

    @Test
    void zeroPrefixMatchesEverything() {
        var allowlist = SupplierIpAllowlist.parse("0.0.0.0/0");
        assertThat(allowlist.allows("8.8.8.8")).isTrue();
        assertThat(allowlist.allows("255.255.255.255")).isTrue();
    }

    @Test
    void blankIpIsRejectedWhenAllowlistConfigured() {
        var allowlist = SupplierIpAllowlist.parse("10.0.0.0/8");
        assertThat(allowlist.allows(null)).isFalse();
        assertThat(allowlist.allows(" ")).isFalse();
    }

    @Test
    void ipv6IsNotPretendedToBeSupported() {
        var allowlist = SupplierIpAllowlist.parse("10.0.0.0/8");
        assertThatThrownBy(() -> allowlist.allows("2001:db8::1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IPv4");
    }

    @Test
    void malformedConfigFailsLoudly() {
        assertThatThrownBy(() -> SupplierIpAllowlist.parse("10.0.0.0/33"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0..32");
        assertThatThrownBy(() -> SupplierIpAllowlist.parse("10.0.0/8"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IPv4");
        assertThatThrownBy(() -> SupplierIpAllowlist.parse("10.0.0.0/abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不是数字");
        assertThatThrownBy(() -> SupplierIpAllowlist.parse("10.0.0.256"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0..255");
    }
}