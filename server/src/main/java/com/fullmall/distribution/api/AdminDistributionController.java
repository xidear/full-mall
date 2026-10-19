package com.fullmall.distribution.api;

import com.fullmall.distribution.api.DistributionDtos.CommissionView;
import com.fullmall.distribution.api.DistributionDtos.ReferralBindingView;
import com.fullmall.distribution.app.ReferralService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/v1/distribution")
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
public class AdminDistributionController {

    private final ReferralService referralService;
    private final com.fullmall.distribution.app.WithdrawalService withdrawalService;

    public AdminDistributionController(ReferralService referralService,
                                       com.fullmall.distribution.app.WithdrawalService withdrawalService) {
        this.referralService = referralService;
        this.withdrawalService = withdrawalService;
    }

    /** 提现单列表。 */
    @GetMapping("/withdrawals")
    public java.util.List<com.fullmall.distribution.app.WithdrawalService.Withdrawal> withdrawals(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return withdrawalService.list(status, limit);
    }

    /** 代推广员发起提现（推广员登录体系落地前由后台代操作）。 */
    @PostMapping("/withdrawals")
    public org.springframework.http.ResponseEntity<java.util.Map<String, java.util.UUID>> requestWithdrawal(
            @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, Object> body,
            org.springframework.security.core.Authentication authentication) {
        java.util.UUID id = withdrawalService.request(
                java.util.UUID.fromString(String.valueOf(body.get("promoterId"))),
                Long.parseLong(String.valueOf(body.get("amount"))),
                String.valueOf(body.getOrDefault("currency", "CNY")));
        return org.springframework.http.ResponseEntity.ok(java.util.Map.of("id", id));
    }

    @PostMapping("/withdrawals/{id}/approve")
    public void approveWithdrawal(@PathVariable java.util.UUID id,
                                  org.springframework.security.core.Authentication authentication) {
        withdrawalService.approve(id, authentication.getName());
    }

    @PostMapping("/withdrawals/{id}/reject")
    public void rejectWithdrawal(@PathVariable java.util.UUID id,
                                 @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> body,
                                 org.springframework.security.core.Authentication authentication) {
        withdrawalService.reject(id, body.get("reason"), authentication.getName());
    }

    @PostMapping("/withdrawals/{id}/pay")
    public void payWithdrawal(@PathVariable java.util.UUID id,
                              @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> body,
                              org.springframework.security.core.Authentication authentication) {
        withdrawalService.markPaid(id, body.get("paymentRef"), authentication.getName());
    }

    @GetMapping("/commissions")
    public List<CommissionView> commissions(@RequestParam(required = false) UUID promoterId,
                                            @RequestParam(required = false) String status) {
        return referralService.listCommissions(promoterId, status);
    }

    @PostMapping("/commissions/{commissionId}/settle")
    public void settle(@PathVariable UUID commissionId) {
        referralService.settleCommission(commissionId);
    }

    @GetMapping("/bindings")
    public List<ReferralBindingView> bindings(@RequestParam String phone) {
        return referralService.listBindings(phone);
    }
}
