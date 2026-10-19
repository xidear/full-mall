package com.fullmall.order.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 订单行商品快照（服务端生成，用户不可输入）。
 *
 * <p>采用**单个 JSONB 字段**而不是每加一项就加一列：商品字段会持续演进，
 * 快照必须能跟着走而不改订单表结构。这与"后台禁止手填 JSON"不冲突——
 * 那条规则约束的是**输入层**，这里是服务端冻结的历史事实。
 *
 * <p>字段约定（可增不改）：image、attributes、listPriceAmount、costAmount、brand、category、locale。
 */
public final class OrderLineSnapshot {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OrderLineSnapshot() {
    }

    public static String toJson(String image, String attributes, Long listPriceAmount) {
        ObjectNode node = MAPPER.createObjectNode();
        if (image != null && !image.isBlank()) {
            node.put("image", image);
        }
        if (attributes != null && !attributes.isBlank()) {
            node.put("attributes", attributes);
        }
        if (listPriceAmount != null) {
            node.put("listPriceAmount", listPriceAmount);
        }
        try {
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("生成订单行快照失败", error);
        }
    }
}