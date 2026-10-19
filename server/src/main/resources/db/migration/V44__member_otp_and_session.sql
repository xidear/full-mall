-- 会员手机 OTP 登录（MEM-001）
CREATE TABLE member_otp (
    id         UUID PRIMARY KEY,
    phone      VARCHAR(32) NOT NULL,
    code_hash  VARCHAR(255) NOT NULL,   -- BCrypt，不存明文验证码
    attempts   INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX member_otp_phone_idx ON member_otp (phone, created_at DESC);

CREATE TABLE member_session (
    token_hash VARCHAR(64) PRIMARY KEY,  -- SHA-256(token)
    member_id  UUID NOT NULL REFERENCES member(id),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX member_session_member_idx ON member_session (member_id, expires_at);
