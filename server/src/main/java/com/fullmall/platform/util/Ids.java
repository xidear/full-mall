package com.fullmall.platform.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class Ids {

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private Ids() {
    }

    public static UUID newId() {
        return UUID.randomUUID();
    }

    public static String orderNo() {
        return "FM" + LocalDateTime.now().format(ORDER_NO_FORMAT) + String.format("%04d", RANDOM.nextInt(10_000));
    }

    public static String pickupCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}