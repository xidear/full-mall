package com.fullmall.setting.api;

import com.fullmall.setting.app.SystemSettingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SystemSettingController {

    private final SystemSettingService service;
    private final com.fullmall.setting.app.MarketContext marketContext;

    public SystemSettingController(SystemSettingService service,
                                  com.fullmall.setting.app.MarketContext marketContext) {
        this.service = service;
        this.marketContext = marketContext;
    }

    /**
     * 站点公开设置：按当前市场返回（市场级覆盖全局）。
     *
     * <p>市场从请求入口解析（X-Market 或站点 default_market）。解析不到时只返回全局值——
     * 老调用方（如媒体基址解析）没有市场上下文，不能替它挑一个市场。
     */
    @GetMapping("/api/v1/site/settings")
    public Map<String, String> publicSettings(
            @org.springframework.web.bind.annotation.RequestHeader(
                    value = com.fullmall.setting.app.MarketContext.HEADER, required = false) String marketHeader) {
        String market = null;
        try {
            market = marketContext.resolve(marketHeader);
        } catch (com.fullmall.platform.error.ApiException ignored) {
            // 明确记录：没有市场上下文时退化为全局设置，而不是假装有市场
            market = null;
        }
        return service.publicSettings(market);
    }

    @GetMapping("/admin/v1/settings")
    public Map<String, String> adminSettings() {
        return service.adminSettings();
    }

    @PutMapping("/admin/v1/settings")
    public Map<String, String> save(@RequestBody Map<String, String> values) {
        return service.save(values);
    }
}