package com.fullmall.distribution.api;

import com.fullmall.distribution.api.DistributionDtos.BindReferralRequest;
import com.fullmall.distribution.api.DistributionDtos.ReferralBindingView;
import com.fullmall.distribution.app.ReferralService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/referrals")
public class ReferralController {

    private final ReferralService referralService;

    public ReferralController(ReferralService referralService) {
        this.referralService = referralService;
    }

    @PostMapping("/bind")
    public ReferralBindingView bind(@Valid @RequestBody BindReferralRequest request) {
        return referralService.bind(request);
    }
}
