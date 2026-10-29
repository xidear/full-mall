-- 批次 F1：邮箱登录（海外用户很多国家短信不可达或成本高，手机号 OTP 不足以支撑跨境）
ALTER TABLE member ADD COLUMN IF NOT EXISTS email VARCHAR(254);

-- 邮箱唯一（不区分大小写），但允许为空：存量手机注册会员没有邮箱
CREATE UNIQUE INDEX IF NOT EXISTS member_email_idx ON member (lower(email)) WHERE email IS NOT NULL;

-- OTP 改成"手机号或邮箱二选一"：两者都不给就是脏数据
ALTER TABLE member_otp ALTER COLUMN phone DROP NOT NULL;
ALTER TABLE member_otp ADD COLUMN IF NOT EXISTS email VARCHAR(254);
ALTER TABLE member_otp DROP CONSTRAINT IF EXISTS member_otp_identifier_check;
ALTER TABLE member_otp ADD CONSTRAINT member_otp_identifier_check
    CHECK (phone IS NOT NULL OR email IS NOT NULL);
CREATE INDEX IF NOT EXISTS member_otp_email_idx
    ON member_otp (lower(email), created_at DESC) WHERE email IS NOT NULL;
