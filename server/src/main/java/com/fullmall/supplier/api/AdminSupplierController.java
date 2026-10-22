package com.fullmall.supplier.api;

import com.fullmall.supplier.app.SupplierAdminService;
import com.fullmall.supplier.app.SupplierCredentialStatus;
import com.fullmall.supplier.app.SupplierTokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import com.fullmall.supplier.infra.SupplierSettlementRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/v1/suppliers")
public class AdminSupplierController {

    private final SupplierAdminService adminService;
    private final SupplierTokenService tokenService;
    private final com.fullmall.supplier.app.SupplierSettlementService settlementService;
    private final com.fullmall.supplier.app.SupplierReturnService supplierReturnService;

    public AdminSupplierController(SupplierAdminService adminService, SupplierTokenService tokenService,
                                   com.fullmall.supplier.app.SupplierSettlementService settlementService,
                                   com.fullmall.supplier.app.SupplierReturnService supplierReturnService) {
        this.adminService = adminService;
        this.tokenService = tokenService;
        this.settlementService = settlementService;
        this.supplierReturnService = supplierReturnService;
    }

    public record TokenIssued(String token, Instant expiresAt) {
    }

    public record CredentialHealth(String authMode, String ipAllowlist, Instant tokenExpiresAt,
                                   boolean tokenRevoked, boolean active, List<String> warnings) {
    }

    /** 签发/轮换 Bearer Token：明文只在此响应中出现一次。 */
    @PostMapping("/{supplierId}/token")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public TokenIssued issueToken(@PathVariable UUID supplierId) {
        var issued = tokenService.issue(supplierId, Duration.ofDays(90));
        return new TokenIssued(issued.token(), issued.expiresAt());
    }

    @DeleteMapping("/{supplierId}/token")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public void revokeToken(@PathVariable UUID supplierId) {
        tokenService.revoke(supplierId);
    }

    /** 生成结算单（SUP-020）。退款暂按 0，佣金费率未配置时为 0。 */
    /** 结算单列表（后台查看）。 */
    @GetMapping("/{supplierId}/settlements")
    public java.util.List<com.fullmall.supplier.infra.SupplierSettlementRepository.SettlementRow>
            settlements(@PathVariable UUID supplierId,
                        @RequestParam(defaultValue = "20") int limit) {
        return settlementService.list(supplierId, limit);
    }

    /** 对账审计：已同意退货 vs 结算扣减，返回差额与是否一致。 */
    @GetMapping("/supplier-returns/reconciliation-audit")
    public java.util.Map<String, Object> reconciliationAudit(
            @RequestParam(defaultValue = "30") int days) {
        return supplierReturnService.reconciliationAudit(days);
    }

    /** 供应商退货对账（按供应商汇总已同意退货金额）。 */
    @GetMapping("/supplier-returns/reconciliation")
    public java.util.List<java.util.Map<String, Object>> returnReconciliation(
            @RequestParam(defaultValue = "30") int days) {
        return supplierReturnService.reconciliation(days);
    }

    @PostMapping("/{supplierId}/settlements")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public SupplierSettlementRepository.SettlementRow generateSettlement(
            @PathVariable UUID supplierId,
            @RequestParam String periodStart,
            @RequestParam String periodEnd,
            Authentication authentication) {
        return settlementService.generate(supplierId,
                Instant.parse(periodStart), Instant.parse(periodEnd), authentication.getName());
    }

    /** 凭证健康度与风险提示（SUP-031）。 */
    @GetMapping("/{supplierId}/credential-health")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public CredentialHealth credentialHealth(@PathVariable UUID supplierId) {
        var state = tokenService.state(supplierId);
        return new CredentialHealth(state.authMode(), state.ipAllowlist(), state.tokenExpiresAt(),
                state.tokenRevoked(), state.active(),
                SupplierCredentialStatus.warnings(state.authMode(), state.ipAllowlist(),
                        state.tokenExpiresAt(), Instant.now()));
    }

    @GetMapping
    public List<SupplierCredentialView> suppliers(@RequestParam(required = false) String sort,
                                                  @RequestParam(required = false) String direction) {
        return adminService.listSuppliers(sort, direction);
    }

    @PostMapping("/{supplierId}/credentials")
    public SupplierCredentialCreated rotateCredential(@PathVariable UUID supplierId) {
        return adminService.rotateCredential(supplierId);
    }
}
