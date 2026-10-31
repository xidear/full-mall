package com.fullmall.exchange.app;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 自动汇率定时拉取。
 *
 * <p>没配供应商时 {@link ExchangeRateFetchService#refresh} 直接返回，不会报错；
 * 配了但远端失败只记录状态，保留上次自动/手工汇率。测试环境通过 app.scheduling.enabled=false 关闭。
 */
@Component
@ConditionalOnProperty(prefix = "app.scheduling", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ExchangeRateRefreshTask {

    private final ExchangeRateFetchService fetcher;

    public ExchangeRateRefreshTask(ExchangeRateFetchService fetcher) {
        this.fetcher = fetcher;
    }

    @Scheduled(initialDelayString = "${app.fx.auto.initial-delay-ms:60000}",
            fixedDelayString = "${app.fx.auto.interval-ms:3600000}")
    public void refresh() {
        fetcher.refresh("scheduler");
    }
}