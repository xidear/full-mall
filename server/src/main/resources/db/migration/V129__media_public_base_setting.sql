-- 媒体/静态资源对外基址（CDN/对象存储/独立图片域名）。
-- 留空时由 MediaUrlResolver 跟随 site_domain + site_scheme；环境变量仅作为开发/CI 兜底。
INSERT INTO system_setting (setting_key, setting_value)
VALUES ('media_public_base_url', '')
ON CONFLICT (setting_key) DO NOTHING;
