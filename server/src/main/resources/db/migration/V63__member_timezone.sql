-- 会员所在时区：生日礼等"按日"业务必须用会员本地日期，不能用服务器时区
ALTER TABLE member ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai';

CREATE INDEX member_birthday_idx ON member (birthday) WHERE birthday IS NOT NULL;
