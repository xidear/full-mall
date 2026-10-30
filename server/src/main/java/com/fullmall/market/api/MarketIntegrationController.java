package com.fullmall.market.api;

import com.fullmall.market.app.MarketIntegrationService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 按市场启用第三方通道（ADR-0014 §8.4）。
 *
 * <p>这只是"启用关系"；凭据仍在「第三方服务」页（ADR-005），两边分工见
 * {@link MarketIntegrationService} 的类注释。
 */
@RestController
public class MarketIntegrationController {

    private final MarketIntegrationService integrations;

    public MarketIntegrationController(MarketIntegrationService integrations) {
        this.integrations = integrations;
    }

    public record ToggleBody(@NotBlank String capability, @NotBlank String provider, boolean enabled) {
    }

    /** 能力 → 可选服务商：后台下拉的唯一来源（AGENTS §13，禁止手填枚举）。 */
    @GetMapping("/admin/v1/markets/integration-catalog")
    public java.util.Map<String, java.util.List<String>> catalog() {
        return integrations.catalog();
    }

    @GetMapping("/admin/v1/markets/{code}/integrations")
    public List<MarketIntegrationService.Row> list(@PathVariable String code) {
        return integrations.list(code);
    }

    @PutMapping("/admin/v1/markets/{code}/integrations")
    public List<MarketIntegrationService.Row> toggle(@PathVariable String code,
                                                   @RequestBody ToggleBody body) {
        return integrations.setEnabled(code, body.capability(), body.provider(), body.enabled());
    }
}
