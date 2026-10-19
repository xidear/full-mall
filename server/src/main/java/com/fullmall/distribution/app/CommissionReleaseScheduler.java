package com.fullmall.distribution.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 佣金解冻调度：把冻结期满的佣金置为可提现。
 *
 * <p>默认**关闭**（`app.distribution.release.enabled=false`），生产 worker 显式开启。
 * 不接这个调度，"冻结 7 天"就永远不会到期——佣金会永久卡在 FROZEN。
 */
@Component
@ConditionalOnProperty(prefix = "app.distribution.release", name = "enabled", havingValue = "true")
public class CommissionReleaseScheduler {

    private static final Logger log = LoggerFactory.getLogger(CommissionReleaseScheduler.class);

    private final CommissionLedgerService ledger;

    public CommissionReleaseScheduler(CommissionLedgerService ledger,
                                      @Value("${app.distribution.release.enabled:false}") boolean enabled) {
        this.ledger = ledger;
    }

    @Scheduled(fixedDelayString = "${app.distribution.release.interval-ms:60000}",
            initialDelayString = "${app.distribution.release.initial-delay-ms:30000}")
    public void release() {
        try {
            int released = ledger.releaseDue(Instant.now());
            if (released > 0) {
                log.info("佣金解冻完成，共 {} 条", released);
            }
        } catch (RuntimeException error) {
            log.error("佣金解冻失败（本轮跳过，下一轮重试）", error);
        }
    }
}
