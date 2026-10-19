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
public class AdminDistributionController {

    private final ReferralService referralService;

    public AdminDistributionController(ReferralService referralService) {
        this.referralService = referralService;
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
