package com.fullmall.member;

import com.fullmall.member.auth.MemberAuthService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=false",
                "app.member.otp.return-code=true"
        })
@ActiveProfiles("test")
class MemberAuthServiceTest {

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
    MemberAuthService auth;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM member_session");
        jdbc.update("DELETE FROM member_otp");
    }

    @Test
    void verifyCreatesMemberAndSessionOnFirstLogin() {
        var otp = auth.requestOtp("13900000001");
        var session = auth.verifyOtp("13900000001", otp.code());

        assertThat(session.token()).isNotBlank();
        assertThat(session.memberNo()).startsWith("M");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM member WHERE phone = '13900000001'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM member_session WHERE member_id = ?", Integer.class, session.memberId()))
                .isEqualTo(1);
    }

    @Test
    void wrongCodeIsRejectedAndCountsAttempt() {
        var otp = auth.requestOtp("13900000002");
        String wrong = otp.code().equals("000000") ? "111111" : "000000";

        assertThatThrownBy(() -> auth.verifyOtp("13900000002", wrong))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("不正确");
        assertThat(jdbc.queryForObject(
                "SELECT attempts FROM member_otp WHERE phone = '13900000002'", Integer.class)).isEqualTo(1);
    }

    @Test
    void codeCannotBeReused() {
        var otp = auth.requestOtp("13900000003");
        auth.verifyOtp("13900000003", otp.code());

        assertThatThrownBy(() -> auth.verifyOtp("13900000003", otp.code()))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("请先获取验证码");
    }

    @Test
    void otpRequestsAreRateLimitedPerPhone() {
        for (int i = 0; i < 5; i++) {
            auth.requestOtp("13900000004");
        }

        assertThatThrownBy(() -> auth.requestOtp("13900000004"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("频繁");

        // 其他号码不受影响
        assertThat(auth.requestOtp("13900000005").code()).isNotBlank();
    }

    @Test
    void invalidPhoneIsRejected() {
        assertThatThrownBy(() -> auth.requestOtp("abc"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("手机号");
    }
}
