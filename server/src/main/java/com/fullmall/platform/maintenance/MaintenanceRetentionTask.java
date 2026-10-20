package com.fullmall.platform.maintenance;

import com.fullmall.platform.schedule.ScheduledJob;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/** 保留清理任务：图形化配置 cron，默认**关闭**（启用需在后台打开）。 */
@Component
public class MaintenanceRetentionTask implements ScheduledJob {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(MaintenanceRetentionTask.class);

    private final MaintenanceService maintenance;

    public MaintenanceRetentionTask(MaintenanceService maintenance) {
        this.maintenance = maintenance;
    }

    @Override
    public String code() {
        return "maintenance.retention";
    }

    @Override
    public String name() {
        return "数据保留清理（消息/导出记录/审计快照）";
    }

    @Override
    public String defaultCron() {
        return "0 30 4 * * *";
    }

    @Override
    public boolean enabledByDefault() {
        return false;
    }

    @Override
    public void run() {
        var report = maintenance.runRetentionWithDefaults();
        log.info("保留清理完成：消息 {} 条、导出记录 {} 条、审计 commit {} 条 / 快照 {} 条",
                report.notifications(), report.exportJobs(),
                report.auditCommits(), report.auditSnapshots());
    }
}
