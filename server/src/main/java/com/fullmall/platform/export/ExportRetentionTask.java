package com.fullmall.platform.export;

import com.fullmall.platform.schedule.ScheduledJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 导出任务记录清理（图形化配置 cron，默认关闭）。只清数据库记录，不清磁盘文件。 */
@Component
public class ExportRetentionTask implements ScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(ExportRetentionTask.class);

    private final ExportJobService jobs;
    private final int days;

    public ExportRetentionTask(ExportJobService jobs,
                               @Value("${app.export.retention.days:7}") int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("导出保留天数必须大于 0，实际=" + days);
        }
        this.jobs = jobs;
        this.days = days;
    }

    @Override
    public String code() {
        return "export.retention";
    }

    @Override
    public String name() {
        return "导出任务记录清理（保留 " + days + " 天）";
    }

    @Override
    public String defaultCron() {
        return "0 15 4 * * *";
    }

    @Override
    public boolean enabledByDefault() {
        return false;
    }

    @Override
    public void run() {
        int removed = jobs.purgeFinishedBefore(days);
        if (removed > 0) {
            log.info("导出任务清理完成：删除 {} 条（保留 {} 天）", removed, days);
        }
    }
}
