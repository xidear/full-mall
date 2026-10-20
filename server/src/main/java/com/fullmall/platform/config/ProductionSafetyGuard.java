package com.fullmall.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 生产模式下的适配器校验：**mock 绝不能进生产**。
 *
 * <p>为什么要在启动时就炸：mock 的价值是"永不失败"，一旦在生产生效，
 * 会出现"短信显示已发送、实际没人收到""支付显示成功、实际没扣款"这类**静默错误**——
 * 比直接报错危险得多。所以这里 fail-fast，宁可起不来，也不带病运行。
 *
 * <p>演示/开发模式（`app.mode=dev|demo`）不做限制，允许全 mock。
 */
@Component
public class ProductionSafetyGuard {

    /** 生产可接受的真实适配器白名单（按服务商名）。 */
    private static final Set<String> REAL_PROVIDERS = Set.of(
            "wechat", "alipay", "stripe", "palipay",
            "aliyun", "tencent", "yunpian",
            "kuaidi100", "kuaidiniao", "aftership",
            "s3", "seaweedfs", "oss", "cos");

    private final String mode;

    public ProductionSafetyGuard(
            @Value("${app.mode:dev}") String mode,
            @Value("${app.payment.provider:mock}") String paymentProvider,
            @Value("${app.logistics.provider:mock}") String logisticsProvider,
            @Value("${app.sms.provider:mock}") String smsProvider,
            @Value("${app.storage.provider:local}") String storageProvider) {
        this.mode = mode == null ? "dev" : mode.trim().toLowerCase(Locale.ROOT);

        if (!"prod".equals(this.mode)) {
            return;   // 演示/开发：允许 mock
        }

        List<String> problems = new ArrayList<>();
        requireReal("app.payment.provider", paymentProvider, problems);
        requireReal("app.logistics.provider", logisticsProvider, problems);
        requireReal("app.sms.provider", smsProvider, problems);
        // 本地磁盘存储在生产可用（不是 mock），但显式记录以便运维确认
        if (storageProvider == null || storageProvider.isBlank()) {
            problems.add("app.storage.provider 为空：生产必须显式指定存储实现");
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "生产模式禁止使用 mock/未配置适配器，启动中止：\n  - " + String.join("\n  - ", problems));
        }
    }

    public boolean productionMode() {
        return "prod".equals(mode);
    }

    private void requireReal(String key, String value, List<String> problems) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            problems.add(key + " 未配置（生产不允许留空）");
            return;
        }
        if ("mock".equals(normalized)) {
            problems.add(key + "=mock（生产禁止 mock，会伪造成功报文）");
            return;
        }
        if (!REAL_PROVIDERS.contains(normalized)) {
            problems.add(key + "=" + value + " 不是已知的真实适配器（白名单：" + REAL_PROVIDERS + "）");
        }
    }
}
