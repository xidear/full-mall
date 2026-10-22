package com.fullmall.platform.secret.api;

import com.fullmall.platform.secret.IntegrationConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 第三方服务配置（ADR-005）。
 *
 * <p>只读列表：**永不返回 secret**，只给"已配置"标记与非敏感字段。
 * 保存：仅 ADMIN，敏感字段加密入库。
 */
@RestController
@RequestMapping("/admin/v1/integrations")
public class IntegrationConfigController {

    private final IntegrationConfigService configs;

    public IntegrationConfigController(IntegrationConfigService configs) {
        this.configs = configs;
    }

    public record SaveRequest(String capability, String provider,
                              Map<String, String> config, boolean enabled) {
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public List<IntegrationConfigService.View> list(
            @RequestParam(required = false) String capability) {
        return configs.list(capability);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public IntegrationConfigService.Saved save(@RequestBody SaveRequest body,
                                               Authentication authentication) {
        return configs.save(body.capability(), body.provider(), body.config(),
                body.enabled(), authentication.getName());
    }
}
