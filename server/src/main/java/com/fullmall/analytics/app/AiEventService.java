package com.fullmall.analytics.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 事件采集（K7）：加购、搜索词、点击、浏览、购买。
 *
 * <p>用途：推荐排序、搜索词优化、知识库缺口发现。
 * 只存**行为**，不存无关个人信息（会员标识可空，匿名标识由前端生成）。
 */
@Service
public class AiEventService {

    private static final List<String> TYPES =
            List.of("VIEW", "ADD_TO_CART", "SEARCH", "CLICK", "PURCHASE");

    private final JdbcTemplate jdbc;

    public AiEventService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void record(String eventType, UUID memberId, String anonymousId, UUID skuId,
                       String keyword, Map<String, Object> payload) {
        String type = eventType == null ? "" : eventType.trim().toUpperCase(java.util.Locale.ROOT);
        if (!TYPES.contains(type)) {
            throw ApiException.badRequest("AI_EVENT_TYPE_INVALID",
                    "事件类型只能是 " + TYPES + "，收到：" + eventType);
        }
        if (skuId == null && (keyword == null || keyword.isBlank()) && type != "VIEW") {
            // 浏览可只带 SKU；其它事件至少要有关键维度，否则数据没有分析价值
            if (skuId == null && (keyword == null || keyword.isBlank())) {
                throw ApiException.badRequest("AI_EVENT_DIMENSION_REQUIRED",
                        "事件必须带 skuId 或 keyword 之一");
            }
        }
        String json;
        try {
            json = payload == null ? "{}" : new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(payload);
        } catch (Exception error) {
            throw ApiException.badRequest("AI_EVENT_PAYLOAD_INVALID", "payload 无法序列化");
        }
        jdbc.update("""
                INSERT INTO ai_event (id, event_type, member_id, anonymous_id, sku_id, keyword, payload)
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
                """, UUID.randomUUID(), type, memberId, anonymousId, skuId,
                keyword == null ? null : keyword.trim(), json);
    }

    /** 热搜词 Top N（供搜索优化与 AI 选品参考）。 */
    public List<Map<String, Object>> topKeywords(int limit) {
        int safe = Math.min(Math.max(limit, 1), 50);
        return jdbc.query("""
                SELECT keyword, COUNT(*) AS hits
                  FROM ai_event
                 WHERE event_type = 'SEARCH' AND keyword IS NOT NULL
                 GROUP BY keyword
                 ORDER BY hits DESC, keyword
                 LIMIT ?
                """, (rs, rowNum) -> Map.of(
                "keyword", rs.getString("keyword"),
                "hits", rs.getLong("hits")), safe);
    }

    /** 加购 Top N 商品（推荐冷启动用）。 */
    public List<Map<String, Object>> topAddedSkus(int limit) {
        int safe = Math.min(Math.max(limit, 1), 50);
        return jdbc.query("""
                SELECT sku_id, COUNT(*) AS hits
                  FROM ai_event
                 WHERE event_type = 'ADD_TO_CART' AND sku_id IS NOT NULL
                 GROUP BY sku_id
                 ORDER BY hits DESC
                 LIMIT ?
                """, (rs, rowNum) -> Map.of(
                "skuId", rs.getObject("sku_id", UUID.class),
                "hits", rs.getLong("hits")), safe);
    }
}
