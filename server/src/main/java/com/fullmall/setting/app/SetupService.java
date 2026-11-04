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
            "site_name", "site_domain", "default_locale", "default_currency",
            "payment_provider", "logistics_provider",
            "distribution.level1-value", "member.points.expiry-mode");

    public record Item(String key, String value, boolean configured, String hint) {
    }

    public record SetupStatus(boolean completed, boolean ready, List<Item> items) {
    }

    /** 数据模式：DEMO=示例数据，BLANK=完全空白。 */
    public record DataStatus(String mode, boolean decided, long products, long orders,
                            long members, long stores, boolean seederAvailable) {
    }

    private final SystemSettingService settings;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final org.springframework.beans.factory.ObjectProvider<com.fullmall.seed.DemoDataSeeder> seeder;

    public SetupService(SystemSettingService settings,
                        org.springframework.jdbc.core.JdbcTemplate jdbc,
                        org.springframework.beans.factory.ObjectProvider<com.fullmall.seed.DemoDataSeeder> seeder) {
        this.settings = settings;
        this.jdbc = jdbc;
        this.seeder = seeder;
    }

    public DataStatus dataStatus() {
        String mode = settings.adminSettings().getOrDefault(
                SystemSettingService.ONBOARDING_DATA_MODE_KEY, "");
        long products = count("product");
        long orders = count("orders");
        long members = count("member");
        long stores = count("store");
        return new DataStatus(mode.isBlank() ? null : mode, !mode.isBlank(),
                products, orders, members, stores, seeder.getIfAvailable() != null);
    }

    /**
     * 首次进入时选择"示例数据 / 完全空白"。
     *
     * <p>DEMO：记下选择并调用演示种子（幂等，已有数据不会重复插）；
     * BLANK：先检查"还在首次配置阶段"（未标记完成，或没有任何订单/会员），再清空商业数据；
     * 否则已经开始运营，清空就是灾难，直接拒绝并说明原因。
     */
    @org.springframework.transaction.annotation.Transactional
    public DataStatus applyDataMode(String mode) {
        String normalized = mode == null ? "" : mode.trim().toUpperCase(java.util.Locale.ROOT);
        if (!normalized.equals("DEMO") && !normalized.equals("BLANK")) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "ONBOARDING_DATA_MODE_INVALID", "数据模式只能是 DEMO 或 BLANK");
        }
        if (normalized.equals("DEMO")) {
            var demo = seeder.getIfAvailable();
            if (demo == null) {
                throw com.fullmall.platform.error.ApiException.conflict(
                        "DEMO_SEEDER_UNAVAILABLE",
                        "当前环境未启用示例数据种子（需要 dev/demo profile），请手动导入商品");
            }
            settings.save(Map.of(SystemSettingService.ONBOARDING_DATA_MODE_KEY, "DEMO"));
            demo.installNow();
            return dataStatus();
        }

        boolean onboarding = !"true".equalsIgnoreCase(
                settings.adminSettings().getOrDefault("setup_completed", "false"));
        long orders = count("orders");
        long members = count("member");
        if (!onboarding && (orders > 0 || members > 0)) {
            throw com.fullmall.platform.error.ApiException.conflict(
                    "ONBOARDING_ALREADY_STARTED",
                    "已经有订单/会员，不能清空为空白商城（订单 " + orders + " 单、会员 " + members + " 人）");
        }
        // 清库与写模式标记在同一事务：中途失败整体回滚，可原样重试，不会留下"清了一半"的库
        purgeCommercialData();
        settings.save(Map.of(SystemSettingService.ONBOARDING_DATA_MODE_KEY, "BLANK"));
        return dataStatus();
    }

    /**
     * 清空"商业数据"：除"配置/身份/审计/字典"外的业务表全部清空。
     *
     * <p>用"按实际存在的表动态生成 + 保留名单"而不是硬编码表清单：
     * 表名会随迁移增减（例如 product_review_image 在早期迁移里并不存在），
     * 硬编码会在升级后直接 500。CASCADE 负责外键子表。
     */
    private void purgeCommercialData() {
        List<String> keep = List.of(
                // 迁移与配置
                "flyway_schema_history", "system_setting", "feature_flag", "market", "member_level",
                "scheduled_task", "scheduled_task_lease", "integration_config",
                // 身份与权限
                "staff_user", "staff_session", "permission", "role_permission",
                // 审计与事件
                "audit_log", "audit_event", "audit_outbox", "event_publication",
                "jv_commit", "jv_commit_property", "jv_global_id", "jv_snapshot",
                // 合规、许可、AI 基础设施、素材文件
                "legal_document", "legal_document_localization", "legal_document_version",
                "legal_acceptance", "license_state", "ai_provider", "asset",
                "knowledge_chunk", "knowledge_embedding", "knowledge_sync_state",
                "payment_provider_config", "payment_provider_config_version",
                "faq_entry");
        List<String> tables = jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class);
        List<String> targets = tables.stream()
                .filter(table -> !keep.contains(table))
                .sorted()
                .toList();
        if (targets.isEmpty()) {
            throw new IllegalStateException("没有可清空的业务表（pg_tables 查询为空），请检查数据库连接");
        }
        jdbc.execute("TRUNCATE TABLE " + String.join(", ", targets) + " CASCADE");
    }

    /** COUNT 返回 null 视为查询异常直接失败：把"查不到"当成 0，会让清库前的运营中检查静默失效。 */
    private long count(String table) {
        Long value = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        if (value == null) {
            throw new IllegalStateException("计数失败：" + table + " 返回 null");
        }
        return value;
    }

    /**
     * 只读探测，不写库：completed（运营手动标记）与 ready（决策项都填了）故意分开，
     * 强制向导会把"先看数据再决定服务商"的人挡在门外。
     * 占位值 mock 也算未配置——否则演示服务商会伪装成真实支付已接通。
     */
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
            case "distribution.level1-value" -> "未设置一级佣金比例";
            case "member.points.expiry-mode" -> "未选择积分过期规则（默认永不过期）";
            case "site_domain" -> "未配置对外访问域名（商城分享/SEO/支付回调需要）";
            default -> "未填写";
        };
    }
}
