package com.fullmall.logistics.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 快递100 实时查询响应解析（**纯解析，不发请求**，便于用假报文测试）。
 *
 * <p>官方报文形如：
 * <pre>{"message":"ok","state":"0","status":"200","data":[{"time":"2026-10-20 10:00:00","context":"已揽收","areaName":"上海"}]}</pre>
 *
 * <p>失败时 `status != "200"` 或含 `message`，必须**明确报错**而不是返回空轨迹
 * （空轨迹会让顾客以为"还没发货"）。
 */
public final class Kuaidi100Response {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private Kuaidi100Response() {
    }

    public static List<LogisticsTrackingService.TrackingEvent> parse(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("快递100 响应为空");
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(body);
        } catch (Exception error) {
            throw new IllegalArgumentException("快递100 响应不是合法 JSON：" + body, error);
        }

        String status = root.path("status").asText("");
        String message = root.path("message").asText("");
        if (!"200".equals(status)) {
            throw new IllegalArgumentException("快递100 查询失败：status=" + status
                    + (message.isBlank() ? "" : "，message=" + message));
        }

        JsonNode data = root.path("data");
        if (!data.isArray()) {
            throw new IllegalArgumentException("快递100 响应缺少 data 数组");
        }
        List<LogisticsTrackingService.TrackingEvent> events = new ArrayList<>();
        for (JsonNode node : data) {
            String time = node.path("time").asText("");
            String context = node.path("context").asText("");
            String area = node.path("areaName").asText("");
            if (time.isBlank() || context.isBlank()) {
                // 缺字段的条目不能静默丢弃：说明报文结构变了，必须暴露
                throw new IllegalArgumentException("快递100 轨迹条目缺少 time/context：" + node);
            }
            events.add(new LogisticsTrackingService.TrackingEvent(
                    parseTime(time), context, area));
        }
        return events;
    }

    private static Instant parseTime(String value) {
        String normalized = value.trim().replace('/', '-');
        try {
            return LocalDateTime.parse(normalized.replace(' ', 'T'))
                    .atZone(BUSINESS_ZONE).toInstant();
        } catch (java.time.format.DateTimeParseException error) {
            throw new IllegalArgumentException("快递100 时间格式无法解析：" + value, error);
        }
    }
}
