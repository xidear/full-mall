package com.fullmall.payment.app;

import com.fullmall.order.domain.Order;
import com.fullmall.payment.domain.Payment;

/**
 * 支付宝（官方 SDK `alipay-sdk-java`）。
 *
 * <p>与微信支付同样的原则：
 * <ul>
 *   <li>缺 app-id / 私钥 / 支付宝公钥 → **构造即失败**（启动期暴露）；</li>
 *   <li>已配置但下单/退款未接入 → 明确 `PAYMENT_PROVIDER_NOT_READY`，**不回落 mock**；</li>
 *   <li>回调必须验签（`AlipaySignature.rsaCheckV1`），未实现验签前**不开放回调入口**。</li>
 * </ul>
 */
public class AlipayConnector implements PaymentConnector {

    private final String appId;
    private final String merchantPrivateKey;
    private final String alipayPublicKey;

    public AlipayConnector(String appId, String merchantPrivateKey, String alipayPublicKey, String gateway) {
        if (appId == null || appId.isBlank()
                || merchantPrivateKey == null || merchantPrivateKey.isBlank()
                || alipayPublicKey == null || alipayPublicKey.isBlank()) {
            throw new IllegalStateException(
                    "app.payment.provider=alipay 时必须配置 app-id/merchant-private-key/alipay-public-key");
        }
        this.appId = appId;
        this.merchantPrivateKey = merchantPrivateKey;
        this.alipayPublicKey = alipayPublicKey;
    }

    @Override
    public String provider() {
        return "ALIPAY";
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
                "支付宝已配置但下单/退款 API 尚未接入（appId=" + appId
                        + "）；回调验签未实现前不得开放回调入口");
    }
}
