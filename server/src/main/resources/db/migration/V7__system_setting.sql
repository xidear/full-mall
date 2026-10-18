CREATE TABLE system_setting (
    setting_key VARCHAR(64) PRIMARY KEY,
    setting_value TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO system_setting (setting_key, setting_value) VALUES
('site_name', 'Full Mall'),
('company_name', '演示商城（中国）有限公司'),
('icp_number', '京ICP备00000000号-1'),
('public_security_record', '京公网安备 00000000000000号'),
('service_phone', '400-000-0000'),
('service_email', 'support@example.com'),
('site_status', 'OPEN'),
('maintenance_message', '系统维护中，请稍后再试。'),
('default_locale', 'zh-CN'),
('default_currency', 'CNY'),
('storage_provider', 'mock'),
('sms_provider', 'mock'),
('payment_provider', 'mock'),
('logistics_provider', 'mock')
ON CONFLICT (setting_key) DO NOTHING;