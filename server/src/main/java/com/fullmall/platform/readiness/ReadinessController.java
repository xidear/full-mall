package com.fullmall.platform.readiness;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 能力就绪状态（管理端与运维脚本用）。 */
@RestController
public class ReadinessController {

    private final CapabilityReadinessService readiness;

    public ReadinessController(CapabilityReadinessService readiness) {
        this.readiness = readiness;
    }

    @GetMapping("/admin/v1/readiness")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public CapabilityReadinessService.Report readiness() {
        return readiness.check();
    }
}
