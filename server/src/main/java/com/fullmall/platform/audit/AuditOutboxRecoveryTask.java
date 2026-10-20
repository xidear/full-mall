package com.fullmall.platform.audit;

import com.fullmall.platform.schedule.ScheduledJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 审计 outbox 崩溃恢复任务（图形化配置 cron，默认关闭）。
 *
 * <p>背景：审计采用"outbox 与业务同事务落库 + 异步提交 JaVers"。进程在两者之间崩溃时，
 * 会留下 `PENDING` 行；没有恢复任务就只能等人工发现，**审计链会静默缺口**。
 *
 * <p>为什么默认关闭：恢复走序列化还原路径，类结构变更后可能失败（会计入 `FAILED` 保留待查）；
 * 由运维在后台确认后开启，避免在结构变更期把失败记录刷多。
 */
@Component
public class AuditOutboxRecoveryTask implements ScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(AuditOutboxRecoveryTask.class);

    private final AuditTrail auditTrail;

    public AuditOutboxRecoveryTask(AuditTrail auditTrail) {
        this.auditTrail = auditTrail;
    }

    @Override
    public String code() {
        return "audit-outbox-recovery";
    }

    @Override
    public String name() {
        return "审计 outbox 崩溃恢复";
    }

    @Override
    public String defaultCron() {
        return "0 */10 * * * *";
    }

    @Override
    public boolean enabledByDefault() {
        return false;
    }

    @Override
    public void run() {
        long before = auditTrail.pendingRows();
        if (before == 0) {
            return;
        }
        auditTrail.recoverOrphans();
        long after = auditTrail.pendingRows();
        log.info("审计 outbox 恢复完成：待处理 {} → {}（成功 {} 条）", before, after,
                Math.max(before - after, 0));
    }
}
