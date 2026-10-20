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
    private final DistributionConfig distributionConfig;

    public ReferralService(DistributionRepository repository,
                           DistributionConfig distributionConfig) {
        this.repository = repository;
        this.distributionConfig = distributionConfig;
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

    /**
     * 结算佣金：**二级分销 + 后台配置比例**。
     *
     * <p>一级：买家绑定的推广员；二级：该推广员的上级（`promoter_link` 里有才发）。
     * 买家和客服是两类账号，**买家永远不参与分佣**，只作为分佣的计算依据。
     *
     * <p>冻结：`distribution.freeze-policy=AFTER_SALE_CLOSED`（默认）时 `frozen_until` 留空，
     * 由释放任务在"订单完成 + 售后窗口"后置为可提现；`FIXED_DAYS` 时按配置天数。
     */
    @Transactional
    public void settleOrder(UUID orderId, long orderAmount, String currency) {
        UUID promoterId = repository.orderReferrer(orderId);
        if (promoterId == null || orderAmount <= 0) {
            return;
        }
        var config = distributionConfig.load();
        java.time.Instant frozenUntil = "FIXED_DAYS".equals(config.freezePolicy())
                ? java.time.Instant.now().plus(java.time.Duration.ofDays(config.freezeDays()))
                : null;

        long level1 = DistributionConfig.commission(config, 1, orderAmount);
        if (level1 > 0) {
            repository.insertCommission(orderId, promoterId, "STAFF", 1,
                    config.level1().mode().name(), config.level1().value(),
                    orderAmount, level1, currency, frozenUntil);
        }

        if (config.level2Enabled() && config.level2().value() > 0) {
            UUID upline = repository.findUpline("STAFF", promoterId);
            long level2 = DistributionConfig.commission(config, 2, orderAmount);
            if (upline != null && level2 > 0) {
                repository.insertCommission(orderId, upline, "STAFF", 2,
                        config.level2().mode().name(), config.level2().value(),
                        orderAmount, level2, currency, frozenUntil);
            }
        }
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
