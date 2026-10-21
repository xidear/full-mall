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

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ReferralService.class);

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

    /** 后台维护推广员上级（二级分佣依据）。 */
    @Transactional
    public void linkUpline(String promoterType, UUID promoterId, String uplineType, UUID uplineId) {
        if (promoterId == null || uplineId == null) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "PROMOTER_REQUIRED", "推广员与上级都不能为空");
        }
        repository.linkUpline(
                promoterType == null ? "STAFF" : promoterType.trim().toUpperCase(),
                promoterId,
                uplineType == null ? "STAFF" : uplineType.trim().toUpperCase(),
                uplineId);
    }

    /**
     * 结算时绑定/归属：**绑定策略与时机会真正生效**（后台配置 `distribution.bind-mode` / `bind-timing`）。
     *
     * <ul>
     *   <li>`bind-timing=REGISTER`：结算不新建绑定，只沿用注册时已有的关系；</li>
     *   <li>`bind-timing=CART` 或 `VISIT`：结算可按推广链接新建绑定；</li>
     *   <li>`bind-mode=ONCE`：**后到覆盖先前**（一次性绑定：谁最后带来这笔单，佣金归谁）；</li>
     *   <li>`bind-mode=PERMANENT`：已有上级则**不覆盖**（终身绑定：先绑定的推广员长期受益）。</li>
     * </ul>
     */
    @Transactional
    public void attachCheckout(UUID orderId, String phone, UUID referrerId, String modeValue) {
        var config = distributionConfig.load();
        String timing = config.bindTiming();
        boolean once = "ONCE".equals(config.bindMode());
        ReferralBindingView existing = repository.findActiveBinding(phone);

        if (referrerId != null && !"REGISTER".equals(timing)) {
            // ONCE：后到覆盖先前（一次性绑定，最后一笔带来者得佣金）
            // PERMANENT：已有上级则保留原关系，不覆盖
            boolean shouldBind = once || existing == null;
            if (shouldBind) {
                ReferralMode mode = parseMode(modeValue == null || modeValue.isBlank()
                        ? "ORDER" : modeValue);
                Instant expiresAt = mode == ReferralMode.VISIT
                        ? Instant.now().plus(Duration.ofDays(30)) : null;
                if (once) {
                    repository.deactivateBindings(phone);   // 只停用**关系**，不动已结算佣金
                }
                repository.insertBinding(phone, referrerId, mode.name(), "CHECKOUT", expiresAt);
                repository.attachOrder(orderId, referrerId, mode.name());
                return;
            }
        }

        if (existing != null) {
            repository.attachOrder(orderId, existing.promoterId(), existing.mode());
            return;
        }

        // 配置为"注册时绑定"且没有历史关系：本单不归属任何推广员（不给不存在的上级发佣金）
        if (referrerId != null) {
            log.info("结算绑定被策略拒绝：timing={}，orderId={}，referrer={}", timing, orderId, referrerId);
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

        // 出款通道按身份决定：员工 → 工资/绩效（默认不进钱包），会员 → 钱包
        String channel = config.staffPayout();

        long level1 = DistributionConfig.commission(config, 1, orderAmount);
        if (level1 > 0) {
            repository.insertCommission(orderId, promoterId, "STAFF", 1,
                    config.level1().mode().name(), config.level1().value(),
                    orderAmount, level1, currency, frozenUntil, channel);
        }

        // 员工二级（如店长带队）默认关闭：员工的上级激励走组织绩效，不走用户分销链
        boolean staffChain = "STAFF".equals(promoterTypeOf(promoterId));
        if (config.level2Enabled() && config.level2().value() > 0
                && (!staffChain || config.staffLevel2Enabled())) {
            UUID upline = repository.findUpline("STAFF", promoterId);
            long level2 = DistributionConfig.commission(config, 2, orderAmount);
            if (upline != null && level2 > 0) {
                repository.insertCommission(orderId, upline, "STAFF", 2,
                        config.level2().mode().name(), config.level2().value(),
                        orderAmount, level2, currency, frozenUntil, channel);
            }
        }
    }

    /** 当前推广员只有员工主体；会员主体接入后按 id 来源区分。 */
    private String promoterTypeOf(UUID promoterId) {
        return "STAFF";
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
