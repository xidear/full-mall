package com.fullmall.feature.api;

import com.fullmall.feature.app.FeatureFlagService;
import com.fullmall.feature.domain.FeatureFlag;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class FeatureFlagController {

    private final FeatureFlagService service;

    public FeatureFlagController(FeatureFlagService service) {
        this.service = service;
    }

    public record UpdateFlagRequest(Boolean enabled, String reason) {
    }

    @GetMapping("/admin/v1/feature-flags")
    public List<FeatureFlag> list() {
        return service.findAll().values().stream().toList();
    }

    @PutMapping("/admin/v1/feature-flags/{code}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public FeatureFlag update(@PathVariable String code, @RequestBody UpdateFlagRequest request) {
        if (request == null || request.enabled() == null) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "ENABLED_REQUIRED", "必须提供 enabled 布尔值");
        }
        return service.update(code, request.enabled(), request.reason(), currentActor());
    }

    /**
     * 审计操作人：优先取认证主体；未认证（本地 dev 放开鉴权）时记录 anonymous，
     * 不伪造用户名。
     */
    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        String name = authentication.getName();
        return (name == null || name.isBlank() || "anonymousUser".equals(name)) ? "anonymous" : name;
    }
}