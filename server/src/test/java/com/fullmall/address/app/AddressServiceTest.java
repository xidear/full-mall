package com.fullmall.address.app;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
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

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false"
        })
@ActiveProfiles("test")
class AddressServiceTest {

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
    AddressService addresses;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM member_address");
    }

    private static AddressService.AddressInput input(String recipient, String phone) {
        return new AddressService.AddressInput(recipient, phone, "310000", "上海", "上海", "静安",
                "南京西路 1 号", "200040");
    }

    @Test
    void firstAddressBecomesDefault() {
        UUID id = addresses.create("member:u1", input("张三", "13800000001"));

        assertThat(addresses.get("member:u1", id).isDefault()).isTrue();
    }

    @Test
    void listMasksPhoneButDetailKeepsIt() {
        UUID id = addresses.create("member:u2", input("李四", "13800000002"));

        assertThat(addresses.list("member:u2").get(0).phoneMasked()).isEqualTo("138****0002");
        assertThat(addresses.get("member:u2", id).phone()).isEqualTo("13800000002");
    }

    @Test
    void settingDefaultClearsPreviousOne() {
        UUID first = addresses.create("member:u3", input("甲", "13800000003"));
        UUID second = addresses.create("member:u3", input("乙", "13800000004"));

        addresses.setDefault("member:u3", second);

        assertThat(addresses.get("member:u3", first).isDefault()).isFalse();
        assertThat(addresses.get("member:u3", second).isDefault()).isTrue();
    }

    @Test
    void deletingDefaultWithOthersRequiresExplicitNewDefault() {
        UUID first = addresses.create("member:u4", input("甲", "13800000005"));
        addresses.create("member:u4", input("乙", "13800000006"));

        assertThatThrownBy(() -> addresses.delete("member:u4", first))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("默认地址");
    }

    @Test
    void otherUsersCannotReadAddress() {
        UUID id = addresses.create("member:u5", input("甲", "13800000007"));

        assertThatThrownBy(() -> addresses.get("member:other", id))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void invalidPhoneIsRejected() {
        assertThatThrownBy(() -> addresses.create("member:u6", input("甲", "abc")))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("手机号");
    }
}
