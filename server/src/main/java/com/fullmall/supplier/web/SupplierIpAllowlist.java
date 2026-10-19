package com.fullmall.supplier.web;

import java.util.ArrayList;
import java.util.List;

/**
 * 供应商 IP 白名单（SUP-030：IP allowlist，支持多段 CIDR）。
 *
 * <p>支持的写法：单个 IPv4（`10.0.0.5`）、CIDR（`10.0.0.0/8`）、逗号分隔多段。
 * 空白项被忽略；**非法配置直接抛错**，不做“跳过坏项继续放行”的兜底——
 * 白名单写错必须暴露，而不是悄悄变成全放行。
 *
 * <p>边界：只支持 IPv4。IPv6 与代理链（X-Forwarded-For 信任边界）另行处理，
 * 未支持时不会假装支持（IPv6 地址一律不匹配）。
 */
public final class SupplierIpAllowlist {

    private final List<Cidr> ranges;

    private SupplierIpAllowlist(List<Cidr> ranges) {
        this.ranges = List.copyOf(ranges);
    }

    public static SupplierIpAllowlist parse(String spec) {
        if (spec == null || spec.isBlank()) {
            return new SupplierIpAllowlist(List.of());
        }
        List<Cidr> parsed = new ArrayList<>();
        for (String raw : spec.split(",")) {
            String item = raw.trim();
            if (item.isEmpty()) {
                continue;
            }
            parsed.add(Cidr.parse(item));
        }
        return new SupplierIpAllowlist(parsed);
    }

    /** 未配置白名单时视为不限制（返回 true）；配置了则必须命中。 */
    public boolean isConfigured() {
        return !ranges.isEmpty();
    }

    public boolean allows(String ip) {
        if (ranges.isEmpty()) {
            return true;
        }
        if (ip == null || ip.isBlank()) {
            return false;
        }
        long candidate = ipv4ToLong(ip.trim());
        for (Cidr range : ranges) {
            if (range.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static long ipv4ToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("不是合法的 IPv4 地址: " + ip);
        }
        long value = 0;
        for (String part : parts) {
            int octet;
            try {
                octet = Integer.parseInt(part);
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("不是合法的 IPv4 地址: " + ip, error);
            }
            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException("IPv4 段超出 0..255: " + ip);
            }
            value = (value << 8) | octet;
        }
        return value;
    }

    private record Cidr(long network, int prefix) {

        static Cidr parse(String item) {
            int slash = item.indexOf('/');
            String address = slash < 0 ? item : item.substring(0, slash);
            int prefix = slash < 0 ? 32 : parsePrefix(item, slash);
            long network = ipv4ToLong(address);
            long mask = prefix == 0 ? 0L : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
            return new Cidr(network & mask, prefix);
        }

        private static int parsePrefix(String item, int slash) {
            try {
                int prefix = Integer.parseInt(item.substring(slash + 1).trim());
                if (prefix < 0 || prefix > 32) {
                    throw new IllegalArgumentException("CIDR 前缀必须在 0..32: " + item);
                }
                return prefix;
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("CIDR 前缀不是数字: " + item, error);
            }
        }

        boolean contains(long candidate) {
            long mask = prefix == 0 ? 0L : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
            return (candidate & mask) == network;
        }
    }
}