package com.fullmall.platform.risk;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 风控告警查询（SEC-004）。只读接口；告警由系统派生，不提供人工造单入口。
 */
@RestController
public class RiskAlertController {

    private final RiskAlertService service;

    public RiskAlertController(RiskAlertService service) {
        this.service = service;
    }

    @GetMapping("/admin/v1/risk-alerts")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RiskAlertService.RiskAlert> recent(
            @RequestParam(defaultValue = "50") int limit) {
        return service.findRecent(limit);
    }

    /**
     * 标记恢复：值班人处理完之后必须能收掉告警，否则一直红着会训练大家忽略告警。
     * 恢复会同时发一条恢复通知（AGENTS §10.5）。
     */
    @PostMapping("/admin/v1/risk-alerts/{id}/recover")
    @PreAuthorize("hasRole('ADMIN')")
    public RiskAlertService.RiskAlert recover(@PathVariable UUID id) {
        return service.recover(id);
    }

    /**
     * 确认处理：值班人认领告警，升级巡检从此不再升级它。
     *
     * <p>与「恢复」的区别：确认=我看到了、正在处理（问题仍在）；恢复=问题已解决。
     */
    @PostMapping("/admin/v1/risk-alerts/{id}/ack")
    @PreAuthorize("hasRole('ADMIN')")
    public RiskAlertService.RiskAlert acknowledge(@PathVariable UUID id, Authentication authentication) {
        return service.acknowledge(id, authentication == null ? null : authentication.getName());
    }
}