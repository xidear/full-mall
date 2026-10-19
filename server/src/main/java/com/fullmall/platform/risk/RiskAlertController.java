package com.fullmall.platform.risk;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}