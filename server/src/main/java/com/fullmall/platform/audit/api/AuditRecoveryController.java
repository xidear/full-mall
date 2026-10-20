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

    public AuditRecoveryController(AuditTrail auditTrail) {
        this.auditTrail = auditTrail;
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
