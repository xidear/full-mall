package com.fullmall.platform.audit.api;

import com.fullmall.platform.audit.AuditTrail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 审计 outbox 的手工恢复入口（运维/事故处理用）。
 *
 * <p>为什么需要：定时任务默认关闭，事故时运维需要**立即**补提交积压的审计行，
 * 而不是等下一次调度或重启服务。
 */
@RestController
@RequestMapping("/admin/v1/audit/outbox")
public class AuditRecoveryController {

    private final AuditTrail auditTrail;
    private final com.fullmall.platform.audit.AuditEventService auditEvents;

    public AuditRecoveryController(AuditTrail auditTrail,
                                   com.fullmall.platform.audit.AuditEventService auditEvents) {
        this.auditTrail = auditTrail;
        this.auditEvents = auditEvents;
    }

    /** 操作审计查询（SEC-003）：按类型/操作人/时间范围。 */
    @GetMapping("/events")
    public java.util.Map<String, Object> events(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String eventType,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String actor,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            String from,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String to,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size) {
        return auditEvents.search(eventType, actor,
                from == null || from.isBlank() ? null : java.time.Instant.parse(from),
                to == null || to.isBlank() ? null : java.time.Instant.parse(to),
                page, size);
    }

    /** 审计 keyset 分页（深页推荐）：游标来自上一页的 nextCreatedAt/nextId。 */
    @GetMapping("/events/keyset")
    public java.util.Map<String, Object> eventsKeyset(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String eventType,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String actor,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String from,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String to,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String cursorCreatedAt,
            @org.springframework.web.bind.annotation.RequestParam(required = false) java.util.UUID cursorId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size) {
        return auditEvents.searchKeyset(
                eventType == null || eventType.isBlank() ? null : eventType.trim(),
                actor == null || actor.isBlank() ? null : actor.trim(),
                from == null || from.isBlank() ? null : java.time.Instant.parse(from),
                to == null || to.isBlank() ? null : java.time.Instant.parse(to),
                cursorCreatedAt == null || cursorCreatedAt.isBlank()
                        ? null : java.time.Instant.parse(cursorCreatedAt),
                cursorId, size);
    }

    @GetMapping("/pending")
    public Map<String, Object> pending() {
        return Map.of("pending", auditTrail.pendingRows());
    }

    @PostMapping("/recover")
    public Map<String, Object> recover() {
        long before = auditTrail.pendingRows();
        auditTrail.recoverOrphans();
        long after = auditTrail.pendingRows();
        return Map.of("before", before, "after", after, "recovered", Math.max(before - after, 0));
    }
}
