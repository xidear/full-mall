package com.fullmall.platform.readiness;

import com.fullmall.platform.secret.IntegrationConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 能力就绪状态：每个外部能力"现在到底能不能用、配置来自哪里"。
 *
 * <p>为什么必须有：以前页面把配置存进集成表，代码却读环境变量（或反过来），
 * 结果"页面显示已启用、实际却走默认值"。这里把**两个来源都读出来**：
 * 只有一边有 → 标出来源；两边都有且不一致 → 标 {@code MISMATCH}，不替用户选一个。
 */
@Service
public class CapabilityReadinessService {

    public record Item(String capability, String label, String state, String provider,
                       List<String> missing, String hint) {
    }

    public record Report(boolean ready, List<Item> items) {
    }

    private final IntegrationConfigService integrations;
    private final JdbcTemplate jdbc;
    private final String envPayment;
    private final String envSms;
    private final String envLogistics;

    public CapabilityReadinessService(IntegrationConfigService integrations, JdbcTemplate jdbc,
                                      @Value("${app.payment.provider:}") String envPayment,
                                      @Value("${app.sms.provider:}") String envSms,
                                      @Value("${app.logistics.provider:}") String envLogistics) {
        this.integrations = integrations;
        this.jdbc = jdbc;
        this.envPayment = envPayment;
        this.envSms = envSms;
        this.envLogistics = envLogistics;
    }

    public Report check() {
        List<Item> items = new ArrayList<>();
        items.add(fromIntegration("storage", "素材存储", true));
        items.add(fromIntegration("export", "导出文件存储", true));
        items.add(compareWithEnv("payment", "支付", envPayment));
        items.add(compareWithEnv("sms", "短信", envSms));
        items.add(compareWithEnv("logistics", "物流查询", envLogistics));
        items.add(fromIntegration("moderation", "内容审核", false));
        items.add(ai());
        boolean ready = items.stream().allMatch(i -> "CONFIGURED".equals(i.state()));
        return new Report(ready, items);
    }

    private Item fromIntegration(String capability, String label, boolean directoryRequired) {
        String provider = integrations.enabledProvider(capability);
        if (provider == null) {
            return new Item(capability, label, "MISSING", null,
                    List.of(directoryRequired ? "服务商与目录" : "服务商"),
                    "请到「第三方服务」配置并启用「" + label + "」");
        }
        Map<String, String> config = integrations.resolveAll(capability);
        List<String> missing = new ArrayList<>();
        if (directoryRequired && blank(config.get("directory"))) {
            missing.add("directory");
        }
        String state = missing.isEmpty() ? "CONFIGURED" : "MISSING";
        return new Item(capability, label, state, provider, missing,
                missing.isEmpty() ? null : "已选服务商，但缺必填项：" + String.join("、", missing));
    }

    /** 支付/短信/物流历史上读环境变量，集成页面也能配：两边不一致必须显式暴露。 */
    private Item compareWithEnv(String capability, String label, String envValue) {
        String configured = integrations.enabledProvider(capability);
        String env = blank(envValue) ? null : envValue.trim();
        if (configured == null && env == null) {
            return new Item(capability, label, "MISSING", null, List.of("服务商"),
                    "请到「第三方服务」配置，或显式设置环境变量");
        }
        if (configured != null && env != null && !configured.equalsIgnoreCase(env)) {
            return new Item(capability, label, "MISMATCH", configured, List.of(),
                    "集成配置=" + configured + " 与环境变量=" + env + " 冲突；代码当前使用环境变量");
        }
        String provider = configured != null ? configured : env;
        String hint = configured != null && env != null
                ? "集成配置与环境变量一致"
                : (configured != null ? "来源：集成配置" : "来源：环境变量（页面配置未生效）");
        return new Item(capability, label, "CONFIGURED", provider, List.of(), hint);
    }

    private Item ai() {
        List<String> providers = jdbc.queryForList(
                "SELECT code FROM ai_provider WHERE enabled", String.class);
        if (providers.isEmpty()) {
            return new Item("ai", "AI 模型", "MISSING", null, List.of("供应商"),
                    "请到「第三方服务 → AI 模型」配置，或显式开启 APP_AI_BOOTSTRAP 注入示例供应商");
        }
        return new Item("ai", "AI 模型", "CONFIGURED", providers.get(0), List.of(),
                "已启用 " + providers.size() + " 个供应商");
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
