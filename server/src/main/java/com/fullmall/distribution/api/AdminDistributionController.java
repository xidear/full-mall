package com.fullmall.distribution.api;

import com.fullmall.distribution.api.DistributionDtos.CommissionView;
import com.fullmall.distribution.api.DistributionDtos.ReferralBindingView;
import com.fullmall.distribution.app.ReferralService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/v1/distribution")
public class AdminDistributionController {

    private final ReferralService referralService;
    private final com.fullmall.distribution.app.DistributionReportService reportService;
    private final com.fullmall.distribution.app.WithdrawalService withdrawalService;

    public AdminDistributionController(ReferralService referralService,
                                       com.fullmall.distribution.app.WithdrawalService withdrawalService,
                                       com.fullmall.distribution.app.DistributionReportService reportService) {
        this.referralService = referralService;
        this.withdrawalService = withdrawalService;
        this.reportService = reportService;
    }

    /** 分销报表：按推广员汇总（金额单位：分）。 */
    @GetMapping("/report")
    public com.fullmall.distribution.app.DistributionReportService.Report report(
            @RequestParam String from, @RequestParam String to) {
        return reportService.report(java.time.LocalDate.parse(from), java.time.LocalDate.parse(to));
    }

    /** 维护推广员上级（二级分佣）。 */
    @PostMapping("/promoters/{promoterId}/upline")
    public Map<String, Object> linkUpline(@PathVariable UUID promoterId,
                                          @RequestBody Map<String, String> body) {
        String uplineId = body.get("uplineId");
        if (uplineId == null || uplineId.isBlank()) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "UPLINE_REQUIRED", "缺少上级 ID（uplineId）");
        }
        referralService.linkUpline(
                body.getOrDefault("promoterType", "STAFF"), promoterId,
                body.getOrDefault("uplineType", "STAFF"), UUID.fromString(uplineId));
        return Map.of("promoterId", promoterId.toString(), "uplineId", uplineId);
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
            @RequestBody java.util.Map<String, Object> body,
            org.springframework.security.core.Authentication authentication) {
        // 手工解析必须给出**明确的 400**：非法 UUID/金额不是"服务端错误"
        java.util.UUID promoterId;
        long amount;
        try {
            promoterId = java.util.UUID.fromString(String.valueOf(body.get("promoterId")));
            amount = Long.parseLong(String.valueOf(body.get("amount")));
        } catch (IllegalArgumentException error) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "INVALID_WITHDRAWAL_REQUEST",
                    "promoterId 必须是 UUID，amount 必须是整数：" + error.getMessage());
        }
        java.util.UUID id = withdrawalService.request(promoterId, amount,
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
                                 @RequestBody java.util.Map<String, String> body,
                                 org.springframework.security.core.Authentication authentication) {
        withdrawalService.reject(id, body.get("reason"), authentication.getName());
    }

    @PostMapping("/withdrawals/{id}/pay")
    public void payWithdrawal(@PathVariable java.util.UUID id,
                              @RequestBody java.util.Map<String, String> body,
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
