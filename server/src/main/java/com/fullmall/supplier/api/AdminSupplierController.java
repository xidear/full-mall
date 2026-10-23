package com.fullmall.supplier.api;

import org.springframework.web.bind.annotation.RequestBody;
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
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final com.fullmall.supplier.webhook.WebhookDispatcher webhookDispatcher;

    public AdminSupplierController(SupplierAdminService adminService, SupplierTokenService tokenService,
                                   com.fullmall.supplier.app.SupplierSettlementService settlementService,
                                   com.fullmall.supplier.app.SupplierReturnService supplierReturnService,
                                   org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
                                   com.fullmall.supplier.webhook.WebhookDispatcher webhookDispatcher) {
        this.adminService = adminService;
        this.tokenService = tokenService;
        this.settlementService = settlementService;
        this.supplierReturnService = supplierReturnService;
        this.jdbcTemplate = jdbcTemplate;
        this.webhookDispatcher = webhookDispatcher;
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

    public record WebhookRequest(String url, String secret) {
    }

    /** 注册/更新供应商 Webhook（同供应商同 URL 覆盖 secret）。 */
    @PostMapping("/{supplierId}/webhooks")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public java.util.Map<String, Object> registerWebhook(@PathVariable UUID supplierId,
                                                         @RequestBody WebhookRequest body) {
        if (body.url() == null || !body.url().startsWith("http")) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "WEBHOOK_URL_INVALID", "Webhook URL 必须是 http/https 地址");
        }
        if (body.secret() == null || body.secret().isBlank()) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "WEBHOOK_SECRET_REQUIRED", "必须设置签名密钥，否则对端无法校验来源");
        }
        jdbcTemplate.update("""
                INSERT INTO supplier_webhook (id, supplier_id, url, secret, active)
                VALUES (?, ?, ?, ?, TRUE)
                """, java.util.UUID.randomUUID(), supplierId, body.url().trim(), body.secret().trim());
        return java.util.Map.of("registered", true);
    }

    /** 该供应商的投递记录（排障）。 */
    @GetMapping("/{supplierId}/webhooks/deliveries")
    public java.util.List<java.util.Map<String, Object>> webhookDeliveries(
            @PathVariable UUID supplierId,
            @RequestParam(defaultValue = "50") int limit) {
        return webhookDispatcher.deliveries(supplierId, limit);
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
