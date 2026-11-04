package com.fullmall.platform.config;

import com.fullmall.setting.app.SystemSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 生产就绪检查：**只报告，不阻断启动**。
 *
 * <p>为什么不再 fail-fast：生产环境的服务商与 AK 由管理员**在后台 web 配置**；
 * 如果缺配置就拒绝启动，管理员根本无法进入后台去配置——死锁。
 *
 * <p>因此策略改为：
 * <ul>
 *   <li>启动永远成功，缺失项在启动日志里以 WARN 列出；</li>
 *   <li>后台「系统就绪」接口（`/admin/v1/system/readiness`）随时可查；</li>
 *   <li>**运行时**真正调用该能力时才失败（不伪造成功），并写风控告警与日志。</li>
 * </ul>
 */
@Component
public class ProductionReadinessChecker implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(ProductionReadinessChecker.class);

    private static final Set<String> REAL_PROVIDERS = Set.of(
            "wechat", "alipay", "stripe",
            "aliyun", "tencent", "yunpian",
            "kuaidi100", "kuaidiniao", "aftership",
            "s3", "seaweedfs", "oss", "cos");

    private final SystemSettingService settings;
    private final String mode;

    public ProductionReadinessChecker(SystemSettingService settings,
                                      @Value("${app.mode:dev}") String mode) {
        this.settings = settings;
        this.mode = mode == null ? "dev" : mode.trim().toLowerCase(Locale.ROOT);
    }

    public record Item(String key, String value, boolean ready, String hint) {
    }

    public record Report(boolean production, List<Item> items) {
        public boolean ready() {
            return items.stream().allMatch(Item::ready);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Report report;
        try {
            report = check();
        } catch (org.springframework.dao.DataAccessException error) {
            // 设置表不可读（例如尚未迁移的测试库）：只警告，不影响启动。
            // 生产环境若看到这条日志，说明数据库初始化有问题，需立即处理。
            log.warn("无法读取系统设置，跳过生产就绪检查：{}", error.getMostSpecificCause().getMessage());
            return;
        }
        if (!report.production()) {
            return;
        }
        List<Item> missing = report.items().stream().filter(item -> !item.ready()).toList();
        if (!missing.isEmpty()) {
            log.warn("生产环境存在未配置的服务商（不影响启动，请在后台「系统设置」中补齐）：");
            missing.forEach(item -> log.warn("  - {} = {}（{}）", item.key(), item.value(), item.hint()));
        }
    }

    /** 供后台展示：当前各能力的配置状态。 */
    public Report check() {
        List<Item> items = new ArrayList<>();
        items.add(provider("payment", settings.rawValue("payment_provider"), "app.payment.provider"));
        items.add(provider("logistics", settings.rawValue("logistics_provider"), "app.logistics.provider"));
        // 短信/邮件/公众号/告警群是「多供应商 + 按用途路由」的能力（ADR-0018），
        // 就绪状态由 CapabilityReadinessService 按用途算；这里不再读 sms_provider（避免双源真相）。
        return new Report("prod".equals(mode), items);
    }

    /** 生产模式下该能力是否可用于真实交易（未配置/ mock 都视为不可用）。 */
    public boolean usableInProduction(String capability) {
        if (!"prod".equals(mode)) {
            return true;
        }
        String value = switch (capability) {
            case "payment" -> settings.rawValue("payment_provider");
            case "logistics" -> settings.rawValue("logistics_provider");
            default -> throw new IllegalArgumentException("未知能力: " + capability);
        };
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return REAL_PROVIDERS.contains(normalized);
    }

    public boolean productionMode() {
        return "prod".equals(mode);
    }

    private Item provider(String capability, String configured, String hintKey) {
        String normalized = configured == null ? "" : configured.trim().toLowerCase(Locale.ROOT);
        boolean ready = REAL_PROVIDERS.contains(normalized);
        String hint = ready ? "已配置真实服务商"
                : ("未配置或仍为 mock，真实调用会被拒绝并记录告警（配置项：" + hintKey + "）");
        return new Item(capability, normalized.isBlank() ? "(空)" : normalized, ready, hint);
    }
}
