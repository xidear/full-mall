package com.fullmall.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 演示数据播种（seed 层）：只在 dev/demo profile 注册，启动完成后跑一次。
 *
 * <p>幂等键是"表里有没有数据"，不是脚本内的 ON CONFLICT：product / staff_user 为空才跑对应整段脚本，
 * 图片、扩容商品、促销每次都跑——已有演示库不会重跑 seed.sql，否则永远只有最初那几件商品。
 * 顺序有依赖：基础商品 → 图片 → 扩容 → 促销。
 */
@Component
@Profile({"dev", "demo"})
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final String appMode;

    public DemoDataSeeder(JdbcTemplate jdbcTemplate, DataSource dataSource,
                          @Value("${app.mode:dev}") String appMode) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.appMode = appMode;
    }

    /**
     * prod + dev/demo profile 属配置错误，直接启动失败而不是静默跳过；
     * 首次配置选了"完全空白"就整段跳过，否则每次重启都会把示例数据塞回来。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        // dev/demo profile 与 app.mode=prod 的组合是配置错误，必须启动失败而不是静默跳过
        if ("prod".equalsIgnoreCase(appMode)) {
            throw new IllegalStateException(
                    "演示数据种子不允许在 app.mode=prod 下启用：请移除 dev/demo profile "
                            + "或将 app.mode 改为 dev（当前 app.mode=" + appMode + "）");
        }
        // 首次配置里选了"完全空白"就不再播种——否则每次重启都会把示例数据塞回来
        if ("BLANK".equalsIgnoreCase(configuredDataMode())) {
            log.info("首次配置选择了『完全空白』，跳过演示数据播种");
            return;
        }
        Long productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product", Long.class);
        if (productCount == null) {
            throw new IllegalStateException("Product count query returned null during demo seeding");
        }
        ensurePromotionStock();
        if (productCount == 0) {
            execute("db/demo/seed.sql");
        } else {
            log.info("Demo product seed skipped because product table already has {} rows", productCount);
        }

        Long staffCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM staff_user", Long.class);
        if (staffCount == null) {
            throw new IllegalStateException("Staff count query returned null during demo seeding");
        }
        if (staffCount == 0) {
            execute("db/demo/staff.sql");
        }
        execute("db/demo/images.sql");
        // 扩容样本必须每次都执行：已有演示库不会重新跑 seed.sql，否则永远只有最初 11 件商品。
        execute("db/demo/extra_products.sql");

        // 促销演示数据单独幂等补齐：商品由迁移插入时，上面的整段 seed 会被跳过，
        // 但促销/拼团等演示数据仍需要存在（E2E 与本地演示都依赖它们）。
        execute("db/demo/promotions.sql");
        log.info("Demo seed completed");
    }

    /** 首次配置里"安装示例数据"按钮调用的入口（与启动播种同一套脚本，幂等）。 */
    public void installNow() {
        // 与启动播种同序：先基础数据，再图片/扩容/促销（后三者依赖已有商品与 SKU）
        Long productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product", Long.class);
        if (productCount != null && productCount == 0) {
            execute("db/demo/seed.sql");
        }
        Long staffCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM staff_user", Long.class);
        if (staffCount != null && staffCount == 0) {
            execute("db/demo/staff.sql");
        }
        execute("db/demo/images.sql");
        execute("db/demo/extra_products.sql");
        execute("db/demo/promotions.sql");
        log.info("示例数据已按首次配置安装");
    }

    /** 行缺失返回 null（未决定），与 BLANK 区分：null 仍会播种，BLANK 才是"不要示例数据"。 */
    private String configuredDataMode() {
        var rows = jdbcTemplate.query("""
                SELECT setting_value FROM system_setting WHERE setting_key = 'onboarding.data-mode'
                """, (rs, rowNum) -> rs.getString(1));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void execute(String classpathScript) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource(classpathScript));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
    }

    /**
     * 演示环境防止"买赠名额"被反复联调耗尽：库存接近上限时补足。
     * 只在非 prod 运行（seed 本身已保证），且仅在 sold_quantity 接近 stock_limit 时 +200。
     */
    private void ensurePromotionStock() {
        int updated = jdbcTemplate.update("""
                UPDATE promotion
                   SET rules = jsonb_set(rules, '{stockLimit}',
                                         to_jsonb((rules->>'stockLimit')::int + 200))
                 WHERE stock_limit IS NOT NULL
                   AND status = 'ACTIVE'
                   AND sold_quantity >= stock_limit - 5
                """);
        if (updated > 0) {
            log.info("演示促销名额已补足：{} 条", updated);
        }
    }
}
