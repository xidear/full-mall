package com.fullmall.member.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 独立事务记录验证码失败次数。
 *
 * <p>为什么必须独立事务：`verifyOtp` 抛异常会回滚主事务，
 * 如果次数更新在同一事务里，**失败尝试会被一起回滚**，等于没有限制——可被无限暴力猜码。
 */
@Service
public class OtpAttemptRecorder {

    private final JdbcTemplate jdbcTemplate;

    public OtpAttemptRecorder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID otpId) {
        int updated = jdbcTemplate.update(
                "UPDATE member_otp SET attempts = attempts + 1 WHERE id = ?", otpId);
        if (updated != 1) {
            throw new IllegalStateException("验证码失败次数更新失败: " + otpId);
        }
    }
}
