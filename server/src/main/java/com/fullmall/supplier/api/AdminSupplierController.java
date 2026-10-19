package com.fullmall.supplier.api;

import com.fullmall.supplier.app.SupplierAdminService;
import com.fullmall.supplier.app.SupplierCredentialStatus;
import com.fullmall.supplier.app.SupplierTokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/v1/suppliers")
public class AdminSupplierController {

    private final SupplierAdminService adminService;
    private final SupplierTokenService tokenService;

    public AdminSupplierController(SupplierAdminService adminService, SupplierTokenService tokenService) {
        this.adminService = adminService;
        this.tokenService = tokenService;
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
    public List<SupplierCredentialView> suppliers() {
        return adminService.listSuppliers();
    }

    @PostMapping("/{supplierId}/credentials")
    public SupplierCredentialCreated rotateCredential(@PathVariable UUID supplierId) {
        return adminService.rotateCredential(supplierId);
    }
}
