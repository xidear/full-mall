package com.fullmall.setting.app;

import com.fullmall.market.app.MarketService;
import com.fullmall.platform.error.ApiException;
import org.springframework.stereotype.Service;

/**
 * 当前请求所属的市场（ADR-0014）。
 *
 * <p>解析顺序：显式请求头 {@code X-Market} → 站点配置的 {@code default_market}。
 * 两者都拿不到、或指向未启用的市场时**明确失败**：把未知市场当成 CN 继续跑，
 * 会让订单落到错误的市场（币种/税/主体全错），这是静默兜底，禁止。
 */
@Service
public class MarketContext {

    /** 前台/移动端切换市场时带这个头；不带则用站点默认市场。 */
    public static final String HEADER = "X-Market";

    private final MarketService markets;
    private final SystemSettingService settings;

    public MarketContext(MarketService markets, SystemSettingService settings) {
        this.markets = markets;
        this.settings = settings;
    }

    /** 请求上下文里的市场；无 HTTP 上下文（如定时任务）时回落到站点默认市场。 */
    public String current() {
        return resolve(headerValue());
    }

    /**
     * 解析市场码。{@code explicit} 为空时用站点默认市场；
     * 站点默认市场缺失同样报错（V7 迁移插入了该行，缺行属部署错误）。
     */
    public String resolve(String explicit) {
        if (explicit != null && !explicit.isBlank()) {
            return markets.requireActive(explicit).code();
        }
        String configured = settings.rawValue(SystemSettingService.DEFAULT_MARKET_KEY);
        if (configured == null || configured.isBlank()) {
            throw ApiException.badRequest("MARKET_UNRESOLVED",
                    "未解析到市场：请求头 " + HEADER + " 与站点 default_market 都为空");
        }
        return markets.requireActive(configured).code();
    }

    private String headerValue() {
        var attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servlet) {
            return servlet.getRequest().getHeader(HEADER);
        }
        return null;
    }
}
