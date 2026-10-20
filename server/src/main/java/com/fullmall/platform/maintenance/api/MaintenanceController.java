package com.fullmall.platform.maintenance.api;

import com.fullmall.platform.maintenance.MaintenanceService;
import org.springframework.web.bind.annotation.*;

/**
 * 运维入口：数据保留清理。
 *
 * <p>权限由 `StaffAuthFilter` 统一判定（`/admin/v1/**` 下未知路径回落到 `permission.manage`，
 * 仅 ADMIN 拥有）。这是**破坏性操作**，因此只暴露 POST，且参数必须显式给出或使用默认。
 */
@RestController
@RequestMapping("/admin/v1/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenance;

    public MaintenanceController(MaintenanceService maintenance) {
        this.maintenance = maintenance;
    }

    @PostMapping("/retention")
    public MaintenanceService.Report runRetention(
            @RequestParam(required = false) Integer notificationDays,
            @RequestParam(required = false) Integer exportDays,
            @RequestParam(required = false) Integer auditDays) {
        if (notificationDays == null && exportDays == null && auditDays == null) {
            return maintenance.runRetentionWithDefaults();
        }
        return maintenance.runRetention(
                notificationDays == null ? 180 : notificationDays,
                exportDays == null ? 7 : exportDays,
                auditDays == null ? 365 : auditDays);
    }
}
