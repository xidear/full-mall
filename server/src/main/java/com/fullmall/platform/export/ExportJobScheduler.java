package com.fullmall.platform.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 导出任务调度器：定时抢占并执行待处理任务。
 *
 * <p>默认**关闭**（`app.export.worker.enabled=false`），需要显式开启：
 * 避免在测试或不需要导出的实例上悄悄跑后台任务。生产 worker profile 打开即可。
 *
 * <p>单次最多处理 {@code batches} 个任务，避免长时间占用线程；处理不完下一轮继续。
 */
@Component
@ConditionalOnProperty(prefix = "app.export.worker", name = "enabled", havingValue = "true")
public class ExportJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExportJobScheduler.class);

    private final ExportWorker worker;
    private final int batches;

    public ExportJobScheduler(ExportWorker worker,
                              @org.springframework.beans.factory.annotation.Value(
                                      "${app.export.worker.batches-per-run:5}") int batches) {
        if (batches <= 0) {
            throw new IllegalArgumentException("batches-per-run 必须大于 0，实际=" + batches);
        }
        this.worker = worker;
        this.batches = batches;
    }

    @Scheduled(fixedDelayString = "${app.export.worker.interval-ms:5000}",
            initialDelayString = "${app.export.worker.initial-delay-ms:10000}")
    public void run() {
        for (int i = 0; i < batches; i++) {
            try {
                if (!worker.runOnce()) {
                    return;
                }
            } catch (RuntimeException error) {
                // 单个任务失败不影响后续任务；worker 内部已把该任务置为 FAILED
                log.error("导出任务执行异常（已记录在任务上）", error);
            }
        }
    }
}
