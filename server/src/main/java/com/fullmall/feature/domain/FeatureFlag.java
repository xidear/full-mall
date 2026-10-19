package com.fullmall.feature.domain;

import java.time.Instant;

/**
 * 一个功能开关的当前状态。
 *
 * @param code      开关编码，形如 {@code supplier.enabled}
 * @param enabled   是否开启
 * @param updatedBy 最近一次修改人（未认证环境记录为 {@code anonymous}）
 * @param updatedAt 最近一次修改时间
 * @param reason    最近一次修改原因，非空
 */
public record FeatureFlag(String code, boolean enabled, String updatedBy, Instant updatedAt, String reason) {
}