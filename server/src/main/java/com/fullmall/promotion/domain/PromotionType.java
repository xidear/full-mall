package com.fullmall.promotion.domain;

/**
 * 促销类型。
 *
 * <p>`FULL_REDUCTION` / `FULL_DISCOUNT` 由 {@code PromotionEngine} 计算（满减/满折，取最优），
 * 与秒杀/买赠/拼团互斥；其余类型仍走原有下单逻辑。
 */
public enum PromotionType {
    FLASH_SALE,
    BUY_GIFT,
    GROUP_BUY,
    FULL_REDUCTION,
    FULL_DISCOUNT
}
