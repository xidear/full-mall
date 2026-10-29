-- 区划快照里的国家名来自 ADS，只有英文（实测 CN--zh 的 name 仍是 "CHINA"）。
-- 中文界面显示 "CHINA" 是 UX 缺陷，所以这里补 zh-CN 名称；en 与原始 key 保留，缺 zh-CN 时仍回退英文。
UPDATE region SET names = names || '{"zh-CN": "中国"}'::jsonb WHERE code = 'CN' AND NOT names ? 'zh-CN';
UPDATE region SET names = names || '{"zh-CN": "中国香港"}'::jsonb WHERE code = 'HK' AND NOT names ? 'zh-CN';
UPDATE region SET names = names || '{"zh-CN": "中国澳门"}'::jsonb WHERE code = 'MO' AND NOT names ? 'zh-CN';
UPDATE region SET names = names || '{"zh-CN": "中国台湾"}'::jsonb WHERE code = 'TW' AND NOT names ? 'zh-CN';
UPDATE region SET names = names || '{"zh-CN": "日本"}'::jsonb WHERE code = 'JP' AND NOT names ? 'zh-CN';
UPDATE region SET names = names || '{"zh-CN": "新加坡"}'::jsonb WHERE code = 'SG' AND NOT names ? 'zh-CN';
UPDATE region SET names = names || '{"zh-CN": "马来西亚"}'::jsonb WHERE code = 'MY' AND NOT names ? 'zh-CN';
UPDATE region SET names = names || '{"zh-CN": "泰国"}'::jsonb WHERE code = 'TH' AND NOT names ? 'zh-CN';
UPDATE region SET names = names || '{"zh-CN": "美国"}'::jsonb WHERE code = 'US' AND NOT names ? 'zh-CN';
