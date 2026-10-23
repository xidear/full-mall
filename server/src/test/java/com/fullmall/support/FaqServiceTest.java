package com.fullmall.support;

import com.fullmall.support.app.FaqService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** L5：命中返回答案；未命中记录并可成候选；采纳后同一问题命中。 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class FaqServiceTest {

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
    FaqService faq;

    @Test
    void missBecomesCandidateThenHitAfterAdopt() {
        String question = "怎么开电子发票-" + java.util.UUID.randomUUID().toString().substring(0, 6);

        assertThat(faq.search(question, null)).isNull();
        assertThat(faq.search(question, null)).isNull();

        var candidates = faq.candidates(30, 2);
        assertThat(candidates).anySatisfy(row ->
                assertThat(String.valueOf(row.get("question"))).contains(question));

        faq.adopt(question, "在订单详情页点「申请发票」即可。");

        var hit = faq.search(question, null);
        assertThat(hit).isNotNull();
        assertThat(hit.answer()).contains("申请发票");

        var after = faq.candidates(30, 2);
        assertThat(after).noneSatisfy(row ->
                assertThat(String.valueOf(row.get("question"))).contains(question));
    }

    @Test
    void rejectsEmptyInputs() {
        assertThatThrownBy(() -> faq.search(" ", null)).hasMessageContaining("问题不能为空");
        assertThatThrownBy(() -> faq.adopt("q", " ")).hasMessageContaining("问题与答案都不能为空");
    }
}
