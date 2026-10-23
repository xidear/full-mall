package com.fullmall.supplier.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Webhook 推送与重试（G5）。
 *
 * <p>规则：
 * <ul>
 *   <li>事件按供应商投递；HTTP 2xx 视为成功，其它一律失败；</li>
 *   <li>失败按**指数退避**重试（1m、5m、30m、2h、6h），最多 5 次后置 FAILED；</li>
 *   <li>每次请求带 `X-Webhook-Timestamp` 与 `X-Webhook-Signature`（HMAC-SHA256）；</li>
 *   <li>失败原因写入 `last_error`，便于对端排查。</li>
 * </ul>
 */
@Service
public class WebhookDispatcher {

    private static final ObjectMapper MAPPER = com.fullmall.platform.json.Json.mapper();
    private static final int MAX_ATTEMPTS = 5;
    private static final long[] BACKOFF_SECONDS = {60, 300, 1800, 7200, 21600};

    private final JdbcTemplate jdbc;
    private final HttpClient http = com.fullmall.platform.http.HttpClients.shared();

    public WebhookDispatcher(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 某事件为供应商的所有启用 Webhook 建投递记录；无 Webhook 时什么也不做。 */
    @Transactional
    public int enqueue(UUID supplierId, String eventType, Map<String, Object> payload) {
        if (eventType == null || eventType.isBlank()) {
            throw ApiException.badRequest("WEBHOOK_EVENT_REQUIRED", "事件类型不能为空");
        }
        var hooks = jdbc.query("""
                SELECT id FROM supplier_webhook WHERE supplier_id = ? AND active
                """, (rs, rowNum) -> rs.getObject("id", UUID.class), supplierId);
        String json;
        try {
            json = MAPPER.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (Exception error) {
            throw ApiException.badRequest("WEBHOOK_PAYLOAD_INVALID", "payload 无法序列化");
        }
        int created = 0;
        for (UUID hookId : hooks) {
            jdbc.update("""
                    INSERT INTO webhook_delivery (id, webhook_id, event_type, payload)
                    VALUES (?, ?, ?, ?::jsonb)
                    """, UUID.randomUUID(), hookId, eventType.trim().toUpperCase(java.util.Locale.ROOT), json);
            created++;
        }
        return created;
    }

    /** 处理一批到期投递；返回成功条数。 */
    @Transactional
    public int deliverDue(int limit) {
        int safe = Math.min(Math.max(limit, 1), 50);
        var due = jdbc.query("""
                SELECT d.id, d.event_type, d.payload::text AS payload, d.attempts,
                       w.url, w.secret
                  FROM webhook_delivery d
                  JOIN supplier_webhook w ON w.id = d.webhook_id
                 WHERE d.status = 'PENDING' AND d.next_attempt_at <= now()
                 ORDER BY d.next_attempt_at
                 LIMIT ?
                """, (rs, rowNum) -> new LinkedHashMap<String, Object>(Map.of(
                "id", rs.getObject("id", UUID.class),
                "eventType", rs.getString("event_type"),
                "payload", rs.getString("payload"),
                "attempts", rs.getInt("attempts"),
                "url", rs.getString("url"),
                "secret", rs.getString("secret"))), safe);
        int succeeded = 0;
        for (Map<String, Object> row : due) {
            if (attempt(row)) {
                succeeded++;
            }
        }
        return succeeded;
    }

    private boolean attempt(Map<String, Object> row) {
        UUID id = (UUID) row.get("id");
        int attempts = (Integer) row.get("attempts") + 1;
        String body = (String) row.get("payload");
        long ts = Instant.now().getEpochSecond();
        String signature = WebhookSigner.sign((String) row.get("secret"), ts, body);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create((String) row.get("url")))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Timestamp", String.valueOf(ts))
                    .header("X-Webhook-Signature", signature)
                    .header("X-Webhook-Event", (String) row.get("eventType"))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 == 2) {
                jdbc.update("""
                        UPDATE webhook_delivery SET status = 'SUCCEEDED', attempts = ?, last_error = NULL
                         WHERE id = ?
                        """, attempts, id);
                return true;
            }
            scheduleRetry(id, attempts, "HTTP " + response.statusCode());
            return false;
        } catch (Exception error) {
            scheduleRetry(id, attempts, error.getMessage());
            return false;
        }
    }

    private void scheduleRetry(UUID id, int attempts, String error) {
        if (attempts >= MAX_ATTEMPTS) {
            jdbc.update("""
                    UPDATE webhook_delivery SET status = 'FAILED', attempts = ?, last_error = ?
                     WHERE id = ?
                    """, attempts, truncate(error), id);
            return;
        }
        long delay = BACKOFF_SECONDS[Math.min(attempts - 1, BACKOFF_SECONDS.length - 1)];
        jdbc.update("""
                UPDATE webhook_delivery
                   SET attempts = ?, last_error = ?, next_attempt_at = now() + (? || ' seconds')::interval
                 WHERE id = ?
                """, attempts, truncate(error), String.valueOf(delay), id);
    }

    private String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > 500 ? error.substring(0, 500) : error;
    }

    /** 供应商的投递记录（排障用）。 */
    public List<Map<String, Object>> deliveries(UUID supplierId, int limit) {
        return jdbc.query("""
                SELECT d.id, d.event_type, d.status, d.attempts, d.last_error, d.created_at
                  FROM webhook_delivery d
                  JOIN supplier_webhook w ON w.id = d.webhook_id
                 WHERE w.supplier_id = ?
                 ORDER BY d.created_at DESC
                 LIMIT ?
                """, (rs, rowNum) -> Map.of(
                "id", rs.getObject("id", UUID.class),
                "eventType", rs.getString("event_type"),
                "status", rs.getString("status"),
                "attempts", rs.getInt("attempts"),
                "lastError", rs.getString("last_error") == null ? "" : rs.getString("last_error"),
                "createdAt", rs.getTimestamp("created_at").toInstant().toString()),
                supplierId, Math.min(Math.max(limit, 1), 100));
    }
}
