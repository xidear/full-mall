package com.fullmall.setting.app;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 首次配置检查：告诉运营"还差哪些决策项"，**不阻断使用**。
 *
 * <p>为什么不做成强制向导：管理员可能先要看数据、再决定服务商；强制拦截会把人挡在门外。
 * 因此只提供状态 + 常驻提示，随时可跳过或稍后继续。
 */
@Service
public class SetupService {

    /** 判定"已配置"所需的键（空或 mock 视为未完成）。 */
    private static final List<String> REQUIRED = List.of(
            "site_name", "default_locale", "default_currency",
            "payment_provider", "logistics_provider", "sms_provider",
            "distribution.level1-value", "member.points.expiry-mode");

    public record Item(String key, String value, boolean configured, String hint) {
    }

    public record SetupStatus(boolean completed, boolean ready, List<Item> items) {
    }

    private final SystemSettingService settings;

    public SetupService(SystemSettingService settings) {
        this.settings = settings;
    }

    public SetupStatus status() {
        Map<String, String> values = settings.adminSettings();
        List<Item> items = new ArrayList<>();
        for (String key : REQUIRED) {
            String raw = values.getOrDefault(key, "");
            boolean configured = raw != null && !raw.isBlank() && !"mock".equalsIgnoreCase(raw.trim());
            items.add(new Item(key, raw == null ? "" : raw, configured, hintOf(key, configured)));
        }
        boolean ready = items.stream().allMatch(Item::configured);
        boolean completed = "true".equalsIgnoreCase(values.getOrDefault("setup_completed", "false"));
        return new SetupStatus(completed, ready, items);
    }

    public void markCompleted() {
        settings.save(Map.of("setup_completed", "true"));
    }

    private String hintOf(String key, boolean configured) {
        if (configured) {
            return "已配置";
        }
        return switch (key) {
            case "payment_provider" -> "未配置真实支付（当前 mock 仅演示可用）";
            case "logistics_provider" -> "未配置真实物流（当前 mock 仅演示可用）";
            case "sms_provider" -> "未配置真实短信（当前 mock 仅演示可用）";
            case "distribution.level1-value" -> "未设置一级佣金比例";
            case "member.points.expiry-mode" -> "未选择积分过期规则（默认永不过期）";
            default -> "未填写";
        };
    }
}
