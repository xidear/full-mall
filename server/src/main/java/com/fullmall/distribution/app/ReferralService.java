package com.fullmall.distribution.app;

import com.fullmall.distribution.api.DistributionDtos.BindReferralRequest;
import com.fullmall.distribution.api.DistributionDtos.CommissionView;
import com.fullmall.distribution.api.DistributionDtos.ReferralBindingView;
import com.fullmall.distribution.domain.ReferralMode;
import com.fullmall.distribution.infra.DistributionRepository;
import com.fullmall.platform.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReferralService {

    private final DistributionRepository repository;
    private final int commissionRate;

    public ReferralService(DistributionRepository repository,
                           @Value("${app.distribution.commission-rate:5}") int commissionRate) {
        this.repository = repository;
        this.commissionRate = commissionRate;
    }

    @Transactional
    public ReferralBindingView bind(BindReferralRequest request) {
        ReferralMode mode = parseMode(request.mode());
        String phone = request.phone().trim();
        Instant expiresAt = mode == ReferralMode.VISIT ? Instant.now().plus(Duration.ofDays(30)) : null;
        if (mode == ReferralMode.PERMANENT || mode == ReferralMode.REGISTER) {
            repository.deactivateBindings(phone);
        }
        repository.insertBinding(phone, request.promoterId(), mode.name(),
                request.source(), expiresAt);
        return repository.findActiveBinding(phone);
    }

    @Transactional
    public void attachCheckout(UUID orderId, String phone, UUID referrerId, String modeValue) {
        if (referrerId != null) {
            ReferralMode mode = parseMode(modeValue == null || modeValue.isBlank()
                    ? "ORDER" : modeValue);
            Instant expiresAt = mode == ReferralMode.VISIT ? Instant.now().plus(Duration.ofDays(30)) : null;
            if (mode == ReferralMode.PERMANENT || mode == ReferralMode.REGISTER) {
                repository.deactivateBindings(phone);
            }
            repository.insertBinding(phone, referrerId, mode.name(), "CHECKOUT", expiresAt);
            repository.attachOrder(orderId, referrerId, mode.name());
            return;
        }
        ReferralBindingView binding = repository.findActiveBinding(phone);
        if (binding != null) {
            repository.attachOrder(orderId, binding.promoterId(), binding.mode());
        }
    }

    @Transactional
    public void settleOrder(UUID orderId, long orderAmount, String currency) {
        UUID promoterId = repository.orderReferrer(orderId);
        if (promoterId == null || orderAmount <= 0) {
            return;
        }
        long commission = (orderAmount * commissionRate + 50) / 100;
        if (commission <= 0) {
            return;
        }
        repository.insertCommission(orderId, promoterId, orderAmount, commission, currency);
    }

    public List<CommissionView> listCommissions(UUID promoterId, String status) {
        return repository.listCommissions(promoterId, status);
    }

    @Transactional
    public void settleCommission(UUID commissionId) {
        repository.settle(commissionId);
    }

    public List<ReferralBindingView> listBindings(String phone) {
        if (phone == null || phone.isBlank()) {
            throw ApiException.badRequest("REFERRAL_PHONE_REQUIRED", "请输入客户手机号");
        }
        return repository.listBindings(phone.trim());
    }

    private ReferralMode parseMode(String value) {
        try {
            return ReferralMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("REFERRAL_MODE_INVALID",
                    "绑定模式必须是 ORDER/REGISTER/VISIT/ONE_TIME/PERMANENT，实际为 " + value);
        }
    }
}
