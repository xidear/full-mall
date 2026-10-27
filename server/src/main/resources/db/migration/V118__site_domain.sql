-- 对外访问域名（host[:port]）。演示/验收环境在数据初始化后由部署脚本写入真实域名。
INSERT INTO system_setting (setting_key, setting_value)
VALUES ('site_domain', '')
ON CONFLICT (setting_key) DO NOTHING;
