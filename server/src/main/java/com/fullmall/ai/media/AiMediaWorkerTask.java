package com.fullmall.ai.media;

import com.fullmall.platform.schedule.ScheduledJob;
import org.springframework.stereotype.Component;

/**
 * AI 生成任务消费者：按固定间隔取一条 PENDING 任务执行。
 * 多实例安全由调度器的租约保证（TaskLease）。
 */
@Component
public class AiMediaWorkerTask implements ScheduledJob {

    private final AiMediaService service;

    public AiMediaWorkerTask(AiMediaService service) {
        this.service = service;
    }

    @Override
    public String code() {
        return "ai-media-worker";
    }

    @Override
    public String name() {
        return "AI 素材生成消费者";
    }

    @Override
    public String defaultCron() {
        return "*/10 * * * * *";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void run() {
        // 一次调度里连续消费若干条，避免任务堆积；失败由调度器记录 last_error
        for (int i = 0; i < 3; i++) {
            if (!service.runOne()) {
                return;
            }
        }
    }
}
