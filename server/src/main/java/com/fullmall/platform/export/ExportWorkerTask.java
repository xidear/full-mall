package com.fullmall.platform.export;

import com.fullmall.platform.schedule.ScheduledJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 导出 worker 任务（图形化配置 cron，默认关闭）。 */
@Component
public class ExportWorkerTask implements ScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(ExportWorkerTask.class);

    private final ExportJobScheduler worker;

    public ExportWorkerTask(ExportJobScheduler worker) {
        this.worker = worker;
    }

    @Override
    public String code() {
        return "export.worker";
    }

    @Override
    public String name() {
        return "导出任务执行（轮询待处理任务）";
    }

    @Override
    public String defaultCron() {
        return "*/10 * * * * *";
    }

    @Override
    public boolean enabledByDefault() {
        // 导出是"发起后必须有人处理"的流水线：默认关闭会导致任务永远停在 PENDING，
        // 用户以为系统坏了。处理是幂等且可重入的，默认开启更安全。
        return true;
    }

    @Override
    public void run() {
        worker.run();
    }
}
