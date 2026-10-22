package com.fullmall.payment.app;

import com.fullmall.order.domain.Order;
import com.fullmall.payment.domain.Payment;

/**
 * 微信支付 v3（官方 SDK `wechatpay-java`）。
 *
 * <p>当前实现**只做配置校验与拒绝语义**：
 * 缺少商户号/证书/APIv3 密钥时构造即失败，避免"以为能收款，其实没配好"；
 * 真正的下单/退款 API 调用需要商户证书，联调阶段再补（本仓库无凭据）。
 *
 * <p>一旦启用 `app.payment.provider=wechat`，业务侧调用会得到
 * `PAYMENT_PROVIDER_NOT_READY`，而不是静默回落到 mock（会造成"没付钱却显示已付"）。
 */
public class WechatPayConnector implements PaymentConnector {

    private final String merchantId;
    private final String serialNumber;
    private final String apiV3Key;

    public WechatPayConnector(String merchantId, String serialNumber, String apiV3Key,
                              String privateKeyPath) {
        if (merchantId == null || merchantId.isBlank()
                || serialNumber == null || serialNumber.isBlank()
                || apiV3Key == null || apiV3Key.isBlank()
                || privateKeyPath == null || privateKeyPath.isBlank()) {
            throw new IllegalStateException(
                    "app.payment.provider=wechat 时必须配置 merchant-id/serial-number/api-v3-key/private-key-path");
        }
        this.merchantId = merchantId;
        this.serialNumber = serialNumber;
        this.apiV3Key = apiV3Key;
    }

    @Override
    public String provider() {
        return "WECHAT";
    }

    @Override
    public Payment createPayment(Order order) {
        throw notReady();
    }

    @Override
    public Payment confirm(Payment payment) {
        throw notReady();
    }

    @Override
    public Payment refund(Payment payment, long amount) {
        throw notReady();
    }

    private com.fullmall.platform.error.ApiException notReady() {
        return com.fullmall.platform.error.ApiException.conflict("PAYMENT_PROVIDER_NOT_READY",
                "微信支付已配置但下单/退款 API 尚未接入（merchantId="
                        + merchantId + ", serial=" + serialNumber + "）");
    }
}
