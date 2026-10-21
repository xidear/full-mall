package com.fullmall.platform.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 审计 outbox 的写入与消费。
 *
 * <p>为什么需要：之前的审计写在**进程内队列**里，进程崩溃会丢；且 JaVers 共用一条连接，
 * 不能并发写。outbox 把"要记什么"先落库（与业务同事务），再由单消费者按类型还原实体提交 JaVers。
 *
 * <p>重放方式：存 `entity_type`（全类名）+ `payload`（JSON）。消费时 `Class.forName` + Jackson 还原。
 * **已知风险**：类被重命名/字段类型变化后，历史 payload 可能无法还原；届时该行会标记 FAILED 并保留，
 * 需要人工处理——**不静默丢弃**。
 */
@Service
public class AuditOutbox {

    private static final Logger log = LoggerFactory.getLogger(AuditOutbox.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final int MAX_ATTEMPTS = 5;

    private final JdbcTemplate jdbcTemplate;

    public AuditOutbox(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public UUID enqueue(String author, Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("审计对象不能为空");
        }
        Class<?> type = entity.getClass();
        Object id;
        String payload;
        try {
            payload = MAPPER.writeValueAsString(entity);
            id = readId(type, entity);
        } catch (ReflectiveOperationException | com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new IllegalStateException("审计 outbox 序列化失败: " + type.getName(), error);
        }
        UUID outboxId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO audit_outbox (id, entity_type, entity_id, author, payload)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb))
                """, outboxId, type.getName(), String.valueOf(id), author, payload);
        return outboxId;
    }

    /** 兼容 record 的 `id()` 与 JPA 实体的 `getId()`。 */
    private static Object readId(Class<?> type, Object entity) throws ReflectiveOperationException {
        try {
            return type.getMethod("id").invoke(entity);
        } catch (NoSuchMethodException ignored) {
            return type.getMethod("getId").invoke(entity);
        }
    }

    public record Pending(UUID id, String entityType, String entityId, String author,
                          String payload, int attempts) {
    }

    public List<Pending> claimPending(int limit) {
        if (limit <= 0 || limit > 200) {
            throw new IllegalArgumentException("批量必须 1..200，实际=" + limit);
        }
        return jdbcTemplate.query("""
                SELECT id, entity_type, entity_id, author, payload::text AS payload, attempts
                  FROM audit_outbox WHERE status = 'PENDING'
                 ORDER BY created_at LIMIT ?
                 FOR UPDATE SKIP LOCKED
                """, (rs, rowNum) -> new Pending(
                rs.getObject("id", UUID.class), rs.getString("entity_type"),
                rs.getString("entity_id"), rs.getString("author"),
                rs.getString("payload"), rs.getInt("attempts")), limit);
    }

    public void markDone(UUID id) {
        jdbcTemplate.update("""
                UPDATE audit_outbox SET status = 'DONE', processed_at = now(), last_error = NULL
                 WHERE id = ?
                """, id);
    }

    /** 失败累计；超过上限标记 FAILED（保留行等待人工处理，不删除）。 */
    public void markFailure(UUID id, String error) {
        String message = error == null ? "unknown" : error;
        if (message.length() > 500) {
            message = message.substring(0, 500);
        }
        jdbcTemplate.update("""
                UPDATE audit_outbox
                   SET attempts = attempts + 1,
                       last_error = ?,
                       status = CASE WHEN attempts + 1 >= ? THEN 'FAILED' ELSE 'PENDING' END
                 WHERE id = ?
                """, message, MAX_ATTEMPTS, id);
        log.error("审计 outbox 消费失败 id={} error={}", id, message);
    }

    /** 还原实体（供消费者使用）。 */
    public Object materialize(Pending pending) {
        try {
            Class<?> type = Class.forName(pending.entityType());
            return MAPPER.readValue(pending.payload(), type);
        } catch (ClassNotFoundException | com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new IllegalStateException(
                    "无法还原审计实体 type=" + pending.entityType() + " id=" + pending.id(), error);
        }
    }
}
