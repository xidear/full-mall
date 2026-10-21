-- 会员时区默认"跟随系统"：NULL/空 表示使用系统默认时区（服务器配置）
ALTER TABLE member ALTER COLUMN timezone DROP NOT NULL;
ALTER TABLE member ALTER COLUMN timezone SET DEFAULT NULL;
UPDATE member SET timezone = NULL WHERE timezone = 'Asia/Shanghai';

-- 生日为空表示"未设置"，生日礼跳过（避免每年 1 月 1 日给全部会员发积分）
ALTER TABLE member ALTER COLUMN birthday SET DEFAULT NULL;
