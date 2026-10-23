package com.fullmall.platform.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 导出任务调度器：定时抢占并执行待处理任务。
 *
 * <p>默认**关闭**（`app.export.worker.enabled=false`），需要显式开启：
 * 避免在测试或不需要导出的实例上悄悄跑后台任务。生产 worker profile 打开即可。
 *
 * <p>单次最多处理 {@code batches} 个任务，避免长时间占用线程；处理不完下一轮继续。
 */
/**
 * 导出任务执行器（纯逻辑，不含调度）。
 *
 * <p>调度由 `ExportWorkerTask` 通过图形化注册表驱动；本类只负责"跑一批"，
 * 便于直接单测与手动触发。
 */
@Component
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
    public void run() {
        int failures = 0;
        RuntimeException firstError = null;
        for (int i = 0; i < batches; i++) {
            try {
                if (!worker.runOnce()) {
                    break;
                }
            } catch (RuntimeException error) {
                failures++;
                if (firstError == null) {
                    firstError = error;
                }
            }
        }
        if (failures > 0) {
            // 每轮汇总一条，避免空转刷屏（H4 根因）
            log.error("本轮导出任务异常 {} 个（首个原因：{}）", failures,
                    firstError == null ? "未知" : firstError.getMessage(), firstError);
        }
    }
}
