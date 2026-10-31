package com.fullmall.store;

import com.fullmall.platform.error.ApiException;
import com.fullmall.store.app.StoreAdminService;
import com.fullmall.store.domain.Store;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 门店管理：编码唯一、市场必填、启停可逆且列表可区分、坐标范围校验。
 *
 * <p>用嵌入式 PostgreSQL 跑真实 Flyway 迁移：编码/市场唯一性与 NOT NULL 是数据库约束，
 * 用内存库或 mock 测不出来。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class StoreAdminServiceTest {

    private static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
        } catch (Exception error) {
            throw new IllegalStateException("无法启动嵌入式 PostgreSQL", error);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    StoreAdminService service;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM store WHERE code LIKE 'STORE-T%'");
    }

    private static StoreAdminService.Command command(String code) {
        return new StoreAdminService.Command(code, "CN", "测试门店", "上海市静安区测试路 1 号",
                new BigDecimal("31.230000"), new BigDecimal("121.470000"), true,
                "13800000000", "09:00-22:00");
    }

    @Test
    void createsStoreWithNormalizedCodeAndShowsItInActiveList() {
        Store created = service.create(command("store-t001"), "admin");

        assertThat(created.code()).isEqualTo("STORE-T001");
        assertThat(created.active()).isTrue();
        assertThat(created.marketCode()).isEqualTo("CN");
        assertThat(service.list(false)).extracting(Store::code).contains("STORE-T001");
    }

    @Test
    void duplicateCodeIsRejectedWithStableCode() {
        service.create(command("STORE-T002"), "admin");

        assertThatThrownBy(() -> service.create(command("STORE-T002"), "admin"))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("STORE_CODE_EXISTS"));
    }

    @Test
    void unknownMarketIsRejected() {
        StoreAdminService.Command bad = new StoreAdminService.Command("STORE-T003", "ZZ",
                "测试门店", "地址", null, null, true, null, null);

        assertThatThrownBy(() -> service.create(bad, "admin"))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("STORE_MARKET_NOT_FOUND"));
    }

    @Test
    void coordinateOutOfRangeIsRejected() {
        StoreAdminService.Command bad = new StoreAdminService.Command("STORE-T004", "CN",
                "测试门店", "地址", new BigDecimal("91"), BigDecimal.ZERO, true, null, null);

        assertThatThrownBy(() -> service.create(bad, "admin"))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("STORE_COORDINATES_INVALID"));
    }

    @Test
    void deactivationHidesStoreFromDefaultListButKeepsHistory() {
        Store created = service.create(command("STORE-T005"), "admin");
        Store disabled = service.setActive(created.id(), false, "admin");

        assertThat(disabled.active()).isFalse();
        assertThat(service.list(false)).extracting(Store::id).doesNotContain(created.id());
        List<Store> all = service.list(true);
        assertThat(all).filteredOn(store -> store.id().equals(created.id()))
                .singleElement()
                .satisfies(store -> assertThat(store.active()).isFalse());
    }

    @Test
    void updateChangesNameButKeepsCreatedAt() {
        Store created = service.create(command("STORE-T006"), "admin");
        StoreAdminService.Command renamed = new StoreAdminService.Command("STORE-T006", "CN",
                "改名后的门店", "新地址", null, null, false, null, "10:00-20:00");

        Store updated = service.update(created.id(), renamed, "admin");

        assertThat(updated.name()).isEqualTo("改名后的门店");
        assertThat(updated.pickupEnabled()).isFalse();
        // PostgreSQL timestamp 是微秒精度：与 Java Instant 的纳秒直接比会因截断失败
        assertThat(updated.createdAt())
                .isCloseTo(created.createdAt(), org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.MICROS));
    }
}