-- 促销演示数据（幂等）：即便商品已由迁移插入，也应能单独补齐促销
INSERT INTO promotion (id, code, type, title, description, status, starts_at, ends_at, rules, priority)
VALUES
('91000000-0000-0000-0000-000000000001', 'DEMO-FLASH-MUG', 'FLASH_SALE', '马克杯限时秒杀',
 '手工陶瓷马克杯限时 2990 分，每单最多 5 件。', 'ACTIVE', now() - interval '1 hour', now() + interval '30 days',
 '{"skuId":"dddddddd-dddd-dddd-dddd-dddddddddddd","promoPriceAmount":2990,"stockLimit":30,"perUserLimit":5}'::jsonb, 30),
('91000000-0000-0000-0000-000000000002', 'DEMO-GIFT-MILK', 'BUY_GIFT', '鲜奶买二赠一',
 '购买 2 件 1L 鲜奶赠送 1 件同款，每单最多赠 2 件。', 'ACTIVE', now() - interval '1 hour', now() + interval '30 days',
 '{"skuId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb","giftSkuId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb","buyQuantity":2,"giftQuantity":1,"maxGiftPerOrder":2,"stockLimit":20,"perUserLimit":20}'::jsonb, 20),
('91000000-0000-0000-0000-000000000003', 'DEMO-GROUP-MUG', 'GROUP_BUY', '马克杯 2 人拼团',
 '2 人成团，每人 2490 分，24 小时内未成团自动退款。', 'ACTIVE', now() - interval '1 hour', now() + interval '30 days',
 '{"skuId":"dddddddd-dddd-dddd-dddd-dddddddddddd","promoPriceAmount":2490,"groupSize":2,"groupDurationHours":24,"perUserLimit":1}'::jsonb, 10)
ON CONFLICT (id) DO NOTHING;
