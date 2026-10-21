package com.fullmall.promotion;

import com.fullmall.promotion.infra.PromotionRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真秒杀（少量库存、瞬时争抢）：并发下**不允许超卖**。
 *
 * <p>验证的是数据库层原子条件扣减：`sold_quantity + qty <= limit` 在一条 UPDATE 内完成，
 * 不存在"先查后扣"的窗口。20 个线程抢 5 件 → 恰好 5 个成功。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class FlashSaleConcurrencyTest {

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
    PromotionRepository promotions;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void concurrentClaimsNeverOversell() throws Exception {
        UUID promotionId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, title, status) VALUES (?, '秒杀品', 'ACTIVE')", productId);
        jdbc.update("""
                INSERT INTO sku (id, product_id, sku_code, price_amount, price_currency)
                VALUES (?, ?, ?, 10000, 'CNY')
                """, skuId, productId, "FS-" + skuId.toString().substring(0, 8));
        jdbc.update("""
                INSERT INTO promotion (id, code, title, type, status, starts_at, ends_at,
                                       rules, sold_quantity)
                VALUES (?, ?, '秒杀', 'FLASH_SALE', 'ACTIVE', now() - interval '1 hour',
                        now() + interval '1 hour',
                        jsonb_build_object('skuId', ?::text, 'promoPriceAmount', 100,
                                           'stockLimit', 5),
                        0)
                """, promotionId, "FS-" + promotionId.toString().substring(0, 8),
                skuId.toString());

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    promotions.claimStock(promotionId, 1, 5);
                    success.incrementAndGet();
                } catch (com.fullmall.platform.error.ApiException error) {
                    soldOut.incrementAndGet();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(success.get()).as("成功扣减必须恰好等于库存").isEqualTo(5);
        assertThat(soldOut.get()).isEqualTo(threads - 5);
        assertThat(jdbc.queryForObject("SELECT sold_quantity FROM promotion WHERE id = ?",
                Integer.class, promotionId)).isEqualTo(5);
    }
}
