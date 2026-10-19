package com.fullmall.migration;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用真实 PostgreSQL 跑完整 Flyway 迁移，验证迁移脚本本身可执行。
 *
 * <p>不依赖 Docker（Testcontainers 用例在无 Docker 环境会跳过，这里不会），
 * 因此 V22/V23 这类迁移问题能在本地和 CI 都被发现。
 */
class MigrationSmokeTest {

    @Test
    void appliesAllMigrationsOnRealPostgres() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder().start()) {
            DataSource dataSource = postgres.getPostgresDatabase();

            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load();
            var result = flyway.migrate();

            assertThat(result.migrationsExecuted)
                    .as("应当执行全部迁移（含 V23）")
                    .isGreaterThanOrEqualTo(23);
            assertThat(result.success).isTrue();

            JdbcTemplate jdbc = new JdbcTemplate(dataSource);

            // V22：三个布尔开关的初始行必须存在且默认开启
            List<String> enabled = jdbc.queryForList(
                    "SELECT code FROM feature_flag WHERE enabled ORDER BY code", String.class);
            assertThat(enabled).containsExactly(
                    "pos.pickup.enabled", "pos.sale.enabled", "supplier.enabled");

            // V23：V8 误插的演示会员/优惠券必须已被清理
            Integer members = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM member WHERE member_no IN ('M0001','M0002')", Integer.class);
            Integer coupons = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM coupon WHERE code IN ('POS10','MEMBER20')", Integer.class);
            assertThat(members).isZero();
            assertThat(coupons).isZero();

            // site_status 仍由 system_setting 单一存放
            String siteStatus = jdbc.queryForObject(
                    "SELECT setting_value FROM system_setting WHERE setting_key = 'site_status'", String.class);
            assertThat(siteStatus).isEqualTo("OPEN");
        }
    }

    @Test
    void demoSeedScriptsApplyAfterMigrations() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder().start()) {
            DataSource dataSource = postgres.getPostgresDatabase();
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();

            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                    new ClassPathResource("db/demo/seed.sql"),
                    new ClassPathResource("db/demo/staff.sql"));
            populator.setSqlScriptEncoding("UTF-8");
            populator.execute(dataSource);

            JdbcTemplate jdbc = new JdbcTemplate(dataSource);

            // 演示会员/优惠券现在由 demo seed 提供
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM member WHERE member_no IN ('M0001','M0002')", Integer.class))
                    .isEqualTo(2);
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM coupon WHERE code IN ('POS10','MEMBER20')", Integer.class))
                    .isEqualTo(2);
            // 供应商代发演示商品由 demo seed 提供
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM product WHERE id = 'aaaa0000-0000-0000-0000-0000000000c1'", Integer.class))
                    .isEqualTo(1);
            // 门店员工绑定由 demo staff seed 提供
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_store", Integer.class))
                    .isEqualTo(1);
        }
    }
}