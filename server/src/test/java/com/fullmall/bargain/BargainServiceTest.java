package com.fullmall.bargain;

import com.fullmall.bargain.app.BargainService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 砍价：一人一次、不能自砍、最后一刀不破底价、人数够了锁定 READY。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class BargainServiceTest {

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
    BargainService bargain;

    @Autowired
    JdbcTemplate jdbc;

    private UUID member(String phone) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO member (id, member_no, name, phone, tier, points_balance,
                                    stored_value_balance, currency)
                VALUES (?, ?, '砍价用户', ?, 'STANDARD', 0, 0, 'CNY')
                """, id, "M-BG-" + id.toString().substring(0, 8), phone);
        return id;
    }

    private UUID activity(long originalPrice, long floorPrice, int minHelpers) {
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '砍价品', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, ?, 'CNY')
                """, skuId, productId, "BG-" + skuId.toString().substring(0, 8), originalPrice);
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bargain_activity (id, sku_id, floor_price, currency, max_cut, min_helpers,
                                              cut_min, cut_max, expires_hours, status, starts_at, ends_at)
                VALUES (?, ?, ?, 'CNY', ?, ?, 50, 100, 24, 'ACTIVE', now(), now() + interval '7 days')
                """, id, skuId, floorPrice, originalPrice - floorPrice, minHelpers);
        return id;
    }

    @Test
    void helpingCutsPriceAndLocksWhenEnoughHelpers() {
        UUID activityId = activity(1_000, 400, 3);
        UUID owner = member("13900001001");
        var session = bargain.start(activityId, owner);
        assertThat(session.currentPrice()).isEqualTo(1_000);

        bargain.help(session.id(), member("13900001002"));
        bargain.help(session.id(), member("13900001003"));
        var afterSecond = bargain.help(session.id(), member("13900001004"));

        assertThat(afterSecond.helperCount()).isEqualTo(3);
        assertThat(afterSecond.status()).isEqualTo("READY");
        assertThat(afterSecond.currentPrice()).isGreaterThanOrEqualTo(400);
    }

    @Test
    void samePersonCannotHelpTwiceNorHelpSelf() {
        UUID activityId = activity(1_000, 400, 3);
        UUID owner = member("13900002001");
        var session = bargain.start(activityId, owner);

        assertThatThrownBy(() -> bargain.help(session.id(), owner))
                .hasMessageContaining("不能给自己砍价");

        UUID helper = member("13900002002");
        bargain.help(session.id(), helper);
        assertThatThrownBy(() -> bargain.help(session.id(), helper))
                .hasMessageContaining("已经帮砍过了");
    }

    @Test
    void ownerCanOnlyStartOnce() {
        UUID activityId = activity(1_000, 400, 3);
        UUID owner = member("13900003001");
        bargain.start(activityId, owner);

        assertThatThrownBy(() -> bargain.start(activityId, owner))
                .hasMessageContaining("只能发起一次");
    }

    /** 只有 READY 可下单，且只能绑一次订单。 */
    @Test
    void onlyReadySessionCanBeOrderedOnce() {
        UUID activityId = activity(1_000, 400, 3);
        UUID owner = member("13900004001");
        var session = bargain.start(activityId, owner);

        assertThatThrownBy(() -> bargain.markOrdered(session.id(), UUID.randomUUID()))
                .hasMessageContaining("只有人数已够");

        bargain.help(session.id(), member("13900004002"));
        bargain.help(session.id(), member("13900004003"));
        bargain.help(session.id(), member("13900004004"));

        UUID orderId = UUID.randomUUID();
        var ordered = bargain.markOrdered(session.id(), orderId);
        assertThat(ordered.status()).isEqualTo("ORDERED");
        assertThat(jdbc.queryForObject("SELECT order_id FROM bargain_session WHERE id = ?",
                UUID.class, session.id())).isEqualTo(orderId);

        assertThatThrownBy(() -> bargain.markOrdered(session.id(), UUID.randomUUID()))
                .hasMessageContaining("不能重复下单");
    }

    /** 结算取价：只有本人的 READY 场次可参与，且不会回落原价。 */
    @Test
    void quoteOnlyForOwnReadySession() {
        UUID activityId = activity(1_000, 400, 3);
        UUID owner = member("13900005001");
        UUID other = member("13900005002");
        var session = bargain.start(activityId, owner);

        assertThatThrownBy(() -> bargain.quote(session.id(), owner))
                .as("未达成不能按活动价下单").hasMessageContaining("砍价尚未达成");
        assertThatThrownBy(() -> bargain.quote(session.id(), other))
                .hasMessageContaining("只能用自己的砍价场次");

        bargain.help(session.id(), member("13900005003"));
        bargain.help(session.id(), member("13900005004"));
        bargain.help(session.id(), member("13900005005"));

        var quote = bargain.quote(session.id(), owner);
        assertThat(quote.unitPrice()).isLessThan(1_000);
        assertThat(quote.unitPrice()).isGreaterThanOrEqualTo(400);
        assertThat(quote.skuId()).isNotNull();
    }
}
