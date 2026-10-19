package com.fullmall.promotion.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 促销类型专属配置（服务端生成，用户不手填 JSON）。
 *
 * <p>数据库 `promotion.rules` 是唯一写入源，旧的类型字段是由它派生的**生成列**；
 * 新增促销类型只需扩展本类，不用改表结构。
 */
public final class PromotionRules {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PromotionRules() {
    }

    public static String toJson(Promotion promotion) {
        ObjectNode node = MAPPER.createObjectNode();
        putUuid(node, "skuId", promotion.skuId());
        putLong(node, "promoPriceAmount", promotion.promoPriceAmount());
        putUuid(node, "giftSkuId", promotion.giftSkuId());
        putInt(node, "buyQuantity", promotion.buyQuantity());
        putInt(node, "giftQuantity", promotion.giftQuantity());
        putInt(node, "maxGiftPerOrder", promotion.maxGiftPerOrder());
        putInt(node, "stockLimit", promotion.stockLimit());
        putInt(node, "perUserLimit", promotion.perUserLimit());
        putInt(node, "groupSize", promotion.groupSize());
        putInt(node, "groupDurationHours", promotion.groupDurationHours());
        try {
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("生成促销规则失败: " + promotion.code(), error);
        }
    }

    private static void putUuid(ObjectNode node, String field, java.util.UUID value) {
        if (value != null) {
            node.put(field, value.toString());
        }
    }

    private static void putLong(ObjectNode node, String field, Long value) {
        if (value != null) {
            node.put(field, value);
        }
    }

    private static void putInt(ObjectNode node, String field, Integer value) {
        if (value != null) {
            node.put(field, value);
        }
    }
}
