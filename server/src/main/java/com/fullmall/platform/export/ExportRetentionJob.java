package com.fullmall.platform.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 导出任务清理：删除已结束且超过保留期的记录。
 *
 * <p>**默认关闭**（`app.export.retention.enabled=false`）；保留天数默认取
 * {@link ExportJobService#RETENTION}（7 天）。
 *
 * <p>注意：这里只清理**数据库记录**，磁盘上的导出文件不在本轮范围内（本地存储目录需另行清理）。
 */
@Component
@ConditionalOnProperty(prefix = "app.export.retention", name = "enabled", havingValue = "true")
public class ExportRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(ExportRetentionJob.class);

    private final ExportJobService jobs;
    private final int days;

    public ExportRetentionJob(ExportJobService jobs,
                              @Value("${app.export.retention.days:7}") int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("导出保留天数必须大于 0，实际=" + days);
        }
        this.jobs = jobs;
        this.days = days;
    }

    @Scheduled(cron = "${app.export.retention.cron:0 15 4 * * *}")
    public void purge() {
        try {
            int removed = jobs.purgeFinishedBefore(days);
            if (removed > 0) {
                log.info("导出任务清理完成：删除 {} 条（保留 {} 天）", removed, days);
            }
        } catch (RuntimeException error) {
            log.error("导出任务清理失败（本轮跳过，下一轮重试）", error);
        }
    }
}
