package com.fullmall.platform.util;

import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class Ids {

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 订单号使用的时区。
     *
     * <p>**不能隐式用服务器时区**：服务器 `TZ` 变化或容器默认 UTC 时，订单号里的时间会跳变，
     * 造成"同一分钟的单号看起来隔了 8 小时"。默认取系统时区（即服务器配置），
     * 可由 `app.timezone` 显式覆盖。
     */
    private static volatile ZoneId zone = ZoneId.systemDefault();

    private Ids() {
    }

    /** 启动时由配置注入（无效时区直接抛错，不静默回落）。 */
    public static void configureZone(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) {
            return;
        }
        zone = ZoneId.of(zoneId.trim());
    }

    static ZoneId zone() {
        return zone;
    }

    public static UUID newId() {
        return UUID.randomUUID();
    }

    public static String orderNo() {
        // 时间戳只到秒，并发下单会撞号；后缀用 8 位十六进制随机（约 43 亿组合）
        return "FM" + ZonedDateTime.now(zone).format(ORDER_NO_FORMAT)
                + String.format("%08x", RANDOM.nextInt());
    }

    public static String pickupCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}