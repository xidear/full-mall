package com.fullmall.payment.wechat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 商家转账客户端装配：只有配置了 `app.payout.wechat.*` 才创建；未配置则不注册，
 * 提现打款接口会明确提示"未配置"而不是假装成功。
 */
@Configuration
public class WechatTransferConfig {

    @Bean
    public com.fullmall.distribution.app.PayoutGateway wechatPayoutGateway(
            org.springframework.beans.factory.ObjectProvider<WechatTransferClient> clients) {
        WechatTransferClient client = clients.getIfAvailable();
        return client == null ? null : new WechatPayoutGateway(client);
    }

    @Bean
    public WechatTransferClient wechatTransferClient(
            @Value("${app.payout.wechat.app-id:}") String appId,
            @Value("${app.payout.wechat.merchant-id:}") String merchantId,
            @Value("${app.payout.wechat.serial-number:}") String serialNumber,
            @Value("${app.payout.wechat.private-key:}") String privateKey,
            @Value("${app.payout.wechat.api-v3-key:}") String apiV3Key,
            @Value("${app.payout.wechat.base-url:https://api.mch.weixin.qq.com}") String baseUrl) {
        if (appId == null || appId.isBlank()) {
            return null;   // 未配置：不注册，调用方按"未配置"处理
        }
        return new WechatTransferClient(appId, merchantId, serialNumber, privateKey, apiV3Key, baseUrl);
    }
}
