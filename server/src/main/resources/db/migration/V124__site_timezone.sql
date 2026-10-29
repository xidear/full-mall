-- 站点默认时区：未登录顾客看到的时间必须与站点所在地区一致，
-- 而不是服务器时区（否则 SSR 与浏览器显示不同）。
INSERT INTO system_setting (setting_key, setting_value)
VALUES ('app.timezone', 'Asia/Shanghai')
ON CONFLICT (setting_key) DO NOTHING;
