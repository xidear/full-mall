package com.fullmall.pos.api;

import com.fullmall.pos.app.PosAdminService;
import com.fullmall.pos.app.PosService;
import com.fullmall.pos.domain.MemberLevel;
import com.fullmall.pos.domain.PosCoupon;
import com.fullmall.pos.domain.PosMember;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/v1/pos")
public class AdminPosController {

    private final PosService posService;
    private final PosAdminService adminService;

    private final com.fullmall.pos.app.PosSalesExportService exportService;

    public AdminPosController(PosService posService, PosAdminService adminService,
                              com.fullmall.pos.app.PosSalesExportService exportService) {
        this.posService = posService;
        this.adminService = adminService;
        this.exportService = exportService;
    }

    /** 销售导出：是否包含成本/毛利列由权限决定，文件本身不含未授权列（SEC-001）。 */
    @org.springframework.web.bind.annotation.GetMapping(value = "/sales/export.csv", produces = "text/csv;charset=UTF-8")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public org.springframework.http.ResponseEntity<String> exportSales(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "500") int limit,
            org.springframework.security.core.Authentication authentication) {
        boolean canViewCost = authentication.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .anyMatch(authority -> com.fullmall.pos.app.PosSalesExportService.PERMISSION_VIEW_COST
                        .equals(authority) || "ROLE_ADMIN".equals(authority));
        var csv = exportService.export(limit, canViewCost, authentication.getName(), null);
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"pos-sales-export.csv\"")
                .header("X-Export-Projection", csv.projection())
                .body(csv.content());
    }

    @GetMapping("/reports/daily")
    public PosDailyReport dailyReport(@RequestParam UUID storeId,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return posService.dailyReport(storeId, date);
    }

    @GetMapping("/reports/cash")
    public PosDailyReport cashSummary(@RequestParam UUID storeId,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return posService.dailyReport(storeId, date);
    }

    @GetMapping("/reconciliation")
    public PosReconciliationReport reconciliation(@RequestParam UUID storeId,
                                                  @RequestParam(required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return posService.reconciliation(storeId, date);
    }

    @GetMapping("/sales")
    public List<PosSaleView> sales(@RequestParam UUID storeId,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return posService.findStoreSales(storeId, date);
    }

    @GetMapping("/members")
    public List<MemberAdminView> members(@RequestParam(required = false) String query) {
        return adminService.listMembers(query);
    }

    @PostMapping("/members")
    public PosMember createMember(@Valid @RequestBody CreateMemberRequest request) {
        return adminService.createMember(request);
    }

    @PostMapping("/members/{memberId}/points")
    public PosMember adjustPoints(@PathVariable UUID memberId,
                                  @Valid @RequestBody AdjustPointsRequest request) {
        return adminService.adjustPoints(memberId, request);
    }

    @PostMapping("/members/{memberId}/stored-value")
    public PosMember topUpStoredValue(@PathVariable UUID memberId,
                                      @Valid @RequestBody TopUpStoredValueRequest request) {
        return adminService.topUpStoredValue(memberId, request);
    }

    @GetMapping("/member-levels")
    public List<MemberLevel> memberLevels() {
        return adminService.listMemberLevels();
    }

    @PutMapping("/member-levels/{code}")
    public MemberLevel updateMemberLevel(@PathVariable String code,
                                         @Valid @RequestBody UpdateMemberLevelRequest request) {
        return adminService.updateMemberLevel(code, request);
    }

    @GetMapping("/coupons")
    public List<PosCoupon> coupons() {
        return adminService.listCoupons();
    }

    @PostMapping("/coupons")
    public PosCoupon createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        return adminService.createCoupon(request);
    }
}