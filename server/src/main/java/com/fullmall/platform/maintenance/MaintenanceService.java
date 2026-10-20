package com.fullmall.platform.maintenance;

import com.fullmall.notification.app.NotificationService;
import com.fullmall.platform.audit.AuditRetentionService;
import com.fullmall.platform.export.ExportJobService;
import org.springframework.stereotype.Service;

/**
 * 数据保留清理的统一入口。
 *
 * <p>此前 notification / export_job / jv_snapshot 三套清理各自为政、**没有触发入口**，
 * 等于没做。这里把三者收成一个可被运维命令或定时任务调用的动作，并返回结构化报告。
 *
 * <p>默认参数：消息 180 天、导出记录 7 天、审计快照 365 天。
 * 不传参就用默认值；任何一项失败都会抛错（不静默跳过，避免"以为清完了"）。
 */
@Service
public class MaintenanceService {

    private final NotificationService notifications;
    private final ExportJobService exportJobs;
    private final AuditRetentionService audit;

    public MaintenanceService(NotificationService notifications,
                              ExportJobService exportJobs,
                              AuditRetentionService audit) {
        this.notifications = notifications;
        this.exportJobs = exportJobs;
        this.audit = audit;
    }

    public record Report(int notifications, int exportJobs, int auditCommits, int auditSnapshots) {
    }

    public Report runRetention(int notificationDays, int exportDays, int auditDays) {
        int removedNotifications = notifications.purgeOlderThan(notificationDays);
        int removedExports = exportJobs.purgeFinishedBefore(exportDays);
        var auditReport = audit.purgeOlderThan(auditDays);
        return new Report(removedNotifications, removedExports,
                auditReport.commits(), auditReport.snapshots());
    }

    /** 使用默认保留期。 */
    public Report runRetentionWithDefaults() {
        return runRetention(180, 7, 365);
    }
}
