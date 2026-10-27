-- 对外协议：自定义端口（如 company.binnarui.com:15443）可能是 HTTPS，不能只按端口猜。
INSERT INTO system_setting (setting_key, setting_value)
VALUES ('site_scheme', 'https')
ON CONFLICT (setting_key) DO NOTHING;
