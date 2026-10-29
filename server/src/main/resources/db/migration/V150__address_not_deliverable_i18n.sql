-- 门禁文案：该市场未开通该目的地时的提示（前台与后端错误码 DESTINATION_NOT_DELIVERABLE 对应）
INSERT INTO ui_translation (locale, text_key, text_value) VALUES
('zh-CN', 'address.notDeliverable', '暂不支持配送到该国家/地区，请更换国家或联系客服'),
('en', 'address.notDeliverable', 'Delivery to this country/region is not available yet')
ON CONFLICT (locale, text_key) DO NOTHING;
