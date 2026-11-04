-- 短信服务商与短信凭据已改为「按用途路由」（ADR-0018）：这些键不再被任何代码读取。
-- 留着会让设置页把历史行原样回传，然后在 INVALID_SETTING_KEY 上失败；这里直接清掉死数据。
DELETE FROM system_setting WHERE setting_key IN ('sms_provider', 'sms_access_key', 'sms_secret_key', 'sms_sign_name');
DELETE FROM market_setting WHERE setting_key IN ('sms_provider', 'sms_access_key', 'sms_secret_key', 'sms_sign_name');
