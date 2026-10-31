package com.fullmall.store.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 门店（后台管理 + 前台自提共用）。
 *
 * <p>{@code active} 与 {@code pickupEnabled} 是两件事：前者是门店是否营业/可被管理，
 * 后者是顾客能否选择到店自提。停用门店不会删除历史订单里的门店引用。
 */
public record Store(
        UUID id,
        String code,
        String marketCode,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean pickupEnabled,
        boolean active,
        String phone,
        String openingHours,
        Instant createdAt
) {
}