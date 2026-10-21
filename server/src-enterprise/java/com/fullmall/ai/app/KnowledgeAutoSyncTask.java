package com.fullmall.ai.app;

import com.fullmall.ai.config.AiProperties;
import com.fullmall.platform.schedule.ScheduledJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 商品变更自动同步知识库（每 30 秒巡检水位）。
 *
 * <p>用 `updated_at` 水位而不是事件：改价/改图/改规格/改标题/上下架入口太多，
 * 事件式埋点会漏；水位能覆盖所有写入路径。
 */
@Component
public class KnowledgeAutoSyncTask implements ScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeAutoSyncTask.class);

    private final KnowledgeBackfillService backfill;
    private final AiProperties properties;

    public KnowledgeAutoSyncTask(KnowledgeBackfillService backfill, AiProperties properties) {
        this.backfill = backfill;
        this.properties = properties;
    }

    @Override
    public String code() {
        return "ai.knowledge-sync";
    }

    @Override
    public String name() {
        return "AI 知识库增量同步（商品变更）";
    }

    @Override
    public String defaultCron() {
        return "*/30 * * * * *";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void run() {
        if (!properties.isEnabled()) {
            return;   // 模块未启用时不做任何事（明确判断，不靠异常）
        }
        var result = backfill.syncChanged();
        if (result.products() > 0) {
            log.info("AI 知识库增量同步完成：{} 个 SKU，{} 个知识块", result.products(), result.chunks());
        }
    }
}
