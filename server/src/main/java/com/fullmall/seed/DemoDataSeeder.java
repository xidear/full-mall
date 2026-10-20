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

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        // dev/demo profile 与 app.mode=prod 的组合是配置错误，必须启动失败而不是静默跳过
        if ("prod".equalsIgnoreCase(appMode)) {
            throw new IllegalStateException(
                    "演示数据种子不允许在 app.mode=prod 下启用：请移除 dev/demo profile "
                            + "或将 app.mode 改为 dev（当前 app.mode=" + appMode + "）");
        }
        Long productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product", Long.class);
        if (productCount == null) {
            throw new IllegalStateException("Product count query returned null during demo seeding");
        }
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

        // 促销演示数据单独幂等补齐：商品由迁移插入时，上面的整段 seed 会被跳过，
        // 但促销/拼团等演示数据仍需要存在（E2E 与本地演示都依赖它们）。
        execute("db/demo/promotions.sql");
        log.info("Demo seed completed");
    }

    private void execute(String classpathScript) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource(classpathScript));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
    }
}