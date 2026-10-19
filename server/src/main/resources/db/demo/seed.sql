INSERT INTO market (code, name, currency, default_locale)
VALUES ('CN', '中国大陆', 'CNY', 'zh-CN')
ON CONFLICT (code) DO NOTHING;

INSERT INTO market (code, name, currency, default_locale)
VALUES ('HK', '香港', 'HKD', 'zh-HK')
ON CONFLICT (code) DO NOTHING;

INSERT INTO market (code, name, currency, default_locale)
VALUES ('MO', '澳门', 'MOP', 'zh-HK')
ON CONFLICT (code) DO NOTHING;

INSERT INTO market (code, name, currency, default_locale)
VALUES ('TW', '台湾地区', 'TWD', 'zh-TW')
ON CONFLICT (code) DO NOTHING;

INSERT INTO category (id, parent_id, code, name)
VALUES ('99999999-9999-9999-9999-999999999999', NULL, 'FRESH', '生鲜食品')
ON CONFLICT (id) DO NOTHING;

INSERT INTO category (id, parent_id, code, name)
VALUES ('99999999-9999-9999-9999-999999999998', NULL, 'HOME', '家居日用')
ON CONFLICT (id) DO NOTHING;

INSERT INTO product (id, title, description, category_id, status, inventory_tracking_level, shelf_life_managed)
VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '有机鲜牛奶', '每日鲜奶，冷链配送，保质期短，按批次管理。',
 '99999999-9999-9999-9999-999999999999', 'ACTIVE', 'VARIANT', TRUE),
('cccccccc-cccc-cccc-cccc-cccccccccccc', '手工陶瓷马克杯', '手工釉面马克杯，常规库存管理。',
 '99999999-9999-9999-9999-999999999998', 'ACTIVE', 'VARIANT', FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO product_translation (product_id, locale, title, description, slug, seo_title, seo_description)
VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'zh-CN', '有机鲜牛奶', '每日鲜奶，冷链配送，保质期短，按批次管理。', 'organic-fresh-milk', '有机鲜牛奶', '每日鲜奶，冷链配送'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'zh-TW', '有機鮮乳', '每日鮮乳，冷鏈配送，保存期限短，依批次管理。', 'organic-fresh-milk-tw', '有機鮮乳', '每日鮮乳，冷鏈配送'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'zh-CN', '手工陶瓷马克杯', '手工釉面马克杯，常规库存管理。', 'ceramic-mug', '手工陶瓷马克杯', '手工釉面马克杯'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'zh-TW', '手工陶瓷馬克杯', '手工釉面馬克杯，常規庫存管理。', 'ceramic-mug-tw', '手工陶瓷馬克杯', '手工釉面馬克杯')
ON CONFLICT (product_id, locale) DO NOTHING;

INSERT INTO sku (id, product_id, sku_code, barcode, price_amount, price_currency,
                 compare_at_amount, compare_at_currency, options_json, image_url, shelf_life_policy_json)
VALUES
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
 'MILK-1L', '690000000001', 1990, 'CNY', 2490, 'CNY', '{"规格":"1L"}'::jsonb,
 NULL,
 '{"batchTracking":true,"pickingStrategy":"FEFO","minRemainingShelfLifeDays":2,"nearExpiryThresholdDays":7,"nearExpiryAction":"DISCOUNT","autoBlockExpired":true}'::jsonb),
('dddddddd-dddd-dddd-dddd-dddddddddddd', 'cccccccc-cccc-cccc-cccc-cccccccccccc',
 'MUG-350ML', '690000000002', 3990, 'CNY', NULL, NULL, '{"颜色":"米白","容量":"350ml"}'::jsonb,
 NULL,
 '{"batchTracking":false,"pickingStrategy":"NONE","minRemainingShelfLifeDays":0,"nearExpiryThresholdDays":0,"nearExpiryAction":"NORMAL","autoBlockExpired":true}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO warehouse (id, market_code, name, type)
VALUES ('11111111-1111-1111-1111-111111111111', 'CN', '华东中心仓', 'CENTRAL')
ON CONFLICT (id) DO NOTHING;

INSERT INTO store (id, market_code, name, address, latitude, longitude, pickup_enabled, phone, opening_hours)
VALUES
('22222222-2222-2222-2222-222222222222', 'CN', '上海静安店', '上海市静安区南京西路 100 号', 31.230416, 121.473701, TRUE, '021-60000001', '09:00-21:00'),
('44444444-4444-4444-4444-444444444444', 'CN', '北京朝阳店', '北京市朝阳区建国路 88 号', 39.904200, 116.407400, TRUE, '010-60000002', '10:00-22:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO supplier (id, code, name, contact_name, contact_phone)
VALUES ('33333333-3333-3333-3333-333333333333', 'SUP-DEMO-001', '演示供应商', '张供应', '13800000000')
ON CONFLICT (id) DO NOTHING;

INSERT INTO supplier_sku (supplier_id, sku_id, supplier_sku_code, price_amount, price_currency)
VALUES ('33333333-3333-3333-3333-333333333333', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'SUP-MILK-1L', 1500, 'CNY')
ON CONFLICT (supplier_id, sku_id) DO NOTHING;

INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no, production_date, expiry_date,
                             quantity, reserved_quantity, status, unit_cost_amount, unit_cost_currency, received_at)
VALUES
('b1000000-0000-0000-0000-000000000001', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'WAREHOUSE',
 '11111111-1111-1111-1111-111111111111', 'MILK-WH-20261015', CURRENT_DATE - 3, CURRENT_DATE + 5,
 10, 0, 'AVAILABLE', 1200, 'CNY', now() - interval '2 days'),
('b1000000-0000-0000-0000-000000000002', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'WAREHOUSE',
 '11111111-1111-1111-1111-111111111111', 'MILK-WH-20261020', CURRENT_DATE - 1, CURRENT_DATE + 20,
 20, 0, 'AVAILABLE', 1200, 'CNY', now() - interval '1 day'),
('b1000000-0000-0000-0000-000000000003', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'STORE',
 '22222222-2222-2222-2222-222222222222', 'MILK-SH-20261012', CURRENT_DATE - 5, CURRENT_DATE + 3,
 5, 0, 'AVAILABLE', 1200, 'CNY', now() - interval '4 days'),
('b1000000-0000-0000-0000-000000000004', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'SUPPLIER',
 '33333333-3333-3333-3333-333333333333', 'MILK-SUP-20261018', CURRENT_DATE, CURRENT_DATE + 30,
 50, 0, 'AVAILABLE', 1000, 'CNY', now()),
('b2000000-0000-0000-0000-000000000001', 'dddddddd-dddd-dddd-dddd-dddddddddddd', 'WAREHOUSE',
 '11111111-1111-1111-1111-111111111111', 'MUG-WH-001', NULL, NULL,
 100, 0, 'AVAILABLE', 1800, 'CNY', now() - interval '7 days'),
('b2000000-0000-0000-0000-000000000002', 'dddddddd-dddd-dddd-dddd-dddddddddddd', 'STORE',
 '22222222-2222-2222-2222-222222222222', 'MUG-SH-001', NULL, NULL,
 10, 0, 'AVAILABLE', 1800, 'CNY', now() - interval '7 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO legal_document (id, code, type, market_code, legal_entity)
VALUES
('e1000000-0000-0000-0000-000000000001', 'CN-TERMS', 'TERMS', 'CN', '演示商城（中国）有限公司'),
('e1000000-0000-0000-0000-000000000002', 'CN-PRIVACY', 'PRIVACY', 'CN', '演示商城（中国）有限公司'),
('e1000000-0000-0000-0000-000000000003', 'CN-REFUND', 'REFUND', 'CN', '演示商城（中国）有限公司'),
('e1000000-0000-0000-0000-000000000004', 'CN-PICKUP', 'PICKUP', 'CN', '演示商城（中国）有限公司')
ON CONFLICT (id) DO NOTHING;

INSERT INTO legal_document_version (id, document_id, version, status, effective_from, effective_to, content_hash)
VALUES
('e1100000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001', '1.0', 'PUBLISHED', now() - interval '1 day', NULL, 'seed-terms-v1'),
('e1100000-0000-0000-0000-000000000002', 'e1000000-0000-0000-0000-000000000002', '1.0', 'PUBLISHED', now() - interval '1 day', NULL, 'seed-privacy-v1'),
('e1100000-0000-0000-0000-000000000003', 'e1000000-0000-0000-0000-000000000003', '1.0', 'PUBLISHED', now() - interval '1 day', NULL, 'seed-refund-v1'),
('e1100000-0000-0000-0000-000000000004', 'e1000000-0000-0000-0000-000000000004', '1.0', 'PUBLISHED', now() - interval '1 day', NULL, 'seed-pickup-v1')
ON CONFLICT (id) DO NOTHING;

INSERT INTO legal_document_localization (id, version_id, locale, title, content, content_hash, review_status)
VALUES
('e1200000-0000-0000-0000-000000000001', 'e1100000-0000-0000-0000-000000000001', 'zh-CN', '用户服务协议',
 '这是演示用户服务协议。正式上线前必须由法务审校并按市场发布不可变版本。', 'seed-terms-zh-cn-v1', 'APPROVED'),
('e1200000-0000-0000-0000-000000000002', 'e1100000-0000-0000-0000-000000000002', 'zh-CN', '隐私政策',
 '这是演示隐私政策。正式上线前必须由法务审校并完成个人信息保护影响评估。', 'seed-privacy-zh-cn-v1', 'APPROVED'),
('e1200000-0000-0000-0000-000000000003', 'e1100000-0000-0000-0000-000000000003', 'zh-CN', '退换货政策',
 '这是演示退换货政策。实际规则以商品类目、法规和市场配置为准。', 'seed-refund-zh-cn-v1', 'APPROVED'),
('e1200000-0000-0000-0000-000000000004', 'e1100000-0000-0000-0000-000000000004', 'zh-CN', '门店自提规则',
 '这是演示门店自提规则。自提需核验提货码或下单手机号，超时未提货按规则释放库存。', 'seed-pickup-zh-cn-v1', 'APPROVED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO category (id, parent_id, code, name)
VALUES
('99999999-9999-9999-9999-999999999997', NULL, 'DIGITAL', '数码家电'),
('99999999-9999-9999-9999-999999999996', NULL, 'INDUSTRIAL', '工业品'),
('99999999-9999-9999-9999-999999999995', NULL, 'BEAUTY', '美妆个护'),
('99999999-9999-9999-9999-999999999994', NULL, 'SPORTS', '运动户外')
ON CONFLICT (id) DO NOTHING;

INSERT INTO product (id, title, description, category_id, status, inventory_tracking_level, shelf_life_managed)
VALUES
('aaaa0000-0000-0000-0000-000000000004', '无线蓝牙耳机', '主动降噪，长续航，适合通勤和运动。',
 '99999999-9999-9999-9999-999999999997', 'ACTIVE', 'VARIANT', FALSE),
('aaaa0000-0000-0000-0000-000000000005', '便携榨汁杯', '无线便携，一键榨汁，办公室和户外都适用。',
 '99999999-9999-9999-9999-999999999998', 'ACTIVE', 'VARIANT', FALSE),
('aaaa0000-0000-0000-0000-000000000006', '工业级步进电机', 'NEMA17 步进电机，适用于自动化设备和 3D 打印机。',
 '99999999-9999-9999-9999-999999999996', 'ACTIVE', 'VARIANT', FALSE),
('aaaa0000-0000-0000-0000-000000000007', '不锈钢法兰盘', '304 不锈钢法兰，DN50 标准件，工业管道连接。',
 '99999999-9999-9999-9999-999999999996', 'ACTIVE', 'VARIANT', FALSE),
('aaaa0000-0000-0000-0000-000000000008', '氨基酸洁面乳', '温和清洁，敏感肌适用，按批次管理。',
 '99999999-9999-9999-9999-999999999995', 'ACTIVE', 'VARIANT', TRUE),
('aaaa0000-0000-0000-0000-000000000009', '加厚瑜伽垫', '防滑加厚，适合居家训练和普拉提。',
 '99999999-9999-9999-9999-999999999994', 'ACTIVE', 'VARIANT', FALSE),
('aaaa0000-0000-0000-0000-00000000000a', '智能运动手环', '心率、睡眠和运动记录，支持消息提醒。',
 '99999999-9999-9999-9999-999999999997', 'ACTIVE', 'VARIANT', FALSE),
('aaaa0000-0000-0000-0000-00000000000b', '有机红富士苹果', '产地直采，脆甜多汁，按批次和效期管理。',
 '99999999-9999-9999-9999-999999999999', 'ACTIVE', 'VARIANT', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO product_translation (product_id, locale, title, description, slug, seo_title, seo_description)
VALUES
('aaaa0000-0000-0000-0000-000000000004', 'zh-CN', '无线蓝牙耳机', '主动降噪，长续航，适合通勤和运动。', 'wireless-earbuds', '无线蓝牙耳机', '主动降噪蓝牙耳机'),
('aaaa0000-0000-0000-0000-000000000005', 'zh-CN', '便携榨汁杯', '无线便携，一键榨汁，办公室和户外都适用。', 'portable-juicer', '便携榨汁杯', '无线便携榨汁杯'),
('aaaa0000-0000-0000-0000-000000000006', 'zh-CN', '工业级步进电机', 'NEMA17 步进电机，适用于自动化设备和 3D 打印机。', 'stepper-motor', '工业级步进电机', 'NEMA17 步进电机'),
('aaaa0000-0000-0000-0000-000000000007', 'zh-CN', '不锈钢法兰盘', '304 不锈钢法兰，DN50 标准件，工业管道连接。', 'steel-flange', '不锈钢法兰盘', '304 不锈钢法兰'),
('aaaa0000-0000-0000-0000-000000000008', 'zh-CN', '氨基酸洁面乳', '温和清洁，敏感肌适用，按批次管理。', 'amino-cleanser', '氨基酸洁面乳', '温和氨基酸洁面乳'),
('aaaa0000-0000-0000-0000-000000000009', 'zh-CN', '加厚瑜伽垫', '防滑加厚，适合居家训练和普拉提。', 'yoga-mat', '加厚瑜伽垫', '防滑加厚瑜伽垫'),
('aaaa0000-0000-0000-0000-00000000000a', 'zh-CN', '智能运动手环', '心率、睡眠和运动记录，支持消息提醒。', 'smart-band', '智能运动手环', '心率睡眠运动手环'),
('aaaa0000-0000-0000-0000-00000000000b', 'zh-CN', '有机红富士苹果', '产地直采，脆甜多汁，按批次和效期管理。', 'organic-apple', '有机红富士苹果', '产地直采红富士苹果')
ON CONFLICT (product_id, locale) DO NOTHING;

INSERT INTO sku (id, product_id, sku_code, barcode, price_amount, price_currency,
                 compare_at_amount, compare_at_currency, options_json, image_url, shelf_life_policy_json)
VALUES
('bbbb0000-0000-0000-0000-000000000004', 'aaaa0000-0000-0000-0000-000000000004', 'HEADPHONE-ANC', '690000000004', 29900, 'CNY', 39900, 'CNY', '{"颜色":"曜石黑"}'::jsonb, NULL, '{}'::jsonb),
('bbbb0000-0000-0000-0000-000000000005', 'aaaa0000-0000-0000-0000-000000000005', 'JUICER-PORTABLE', '690000000005', 9900, 'CNY', 12900, 'CNY', '{"颜色":"奶油白"}'::jsonb, NULL, '{}'::jsonb),
('bbbb0000-0000-0000-0000-000000000006', 'aaaa0000-0000-0000-0000-000000000006', 'MOTOR-NEMA17', '690000000006', 16800, 'CNY', NULL, NULL, '{"规格":"42-40"}'::jsonb, NULL, '{}'::jsonb),
('bbbb0000-0000-0000-0000-000000000007', 'aaaa0000-0000-0000-0000-000000000007', 'FLANGE-DN50', '690000000007', 5600, 'CNY', NULL, NULL, '{"规格":"DN50"}'::jsonb, NULL, '{}'::jsonb),
('bbbb0000-0000-0000-0000-000000000008', 'aaaa0000-0000-0000-0000-000000000008', 'CLEANSER-120G', '690000000008', 6900, 'CNY', 8900, 'CNY', '{"规格":"120g"}'::jsonb, NULL,
 '{"batchTracking":true,"pickingStrategy":"FEFO","minRemainingShelfLifeDays":15,"nearExpiryThresholdDays":30,"nearExpiryAction":"DISCOUNT","autoBlockExpired":true}'::jsonb),
('bbbb0000-0000-0000-0000-000000000009', 'aaaa0000-0000-0000-0000-000000000009', 'YOGA-MAT-8MM', '690000000009', 7900, 'CNY', NULL, NULL, '{"颜色":"雾灰","厚度":"8mm"}'::jsonb, NULL, '{}'::jsonb),
('bbbb0000-0000-0000-0000-00000000000a', 'aaaa0000-0000-0000-0000-00000000000a', 'BAND-SPORT', '69000000000a', 19900, 'CNY', 24900, 'CNY', '{"颜色":"午夜蓝"}'::jsonb, NULL, '{}'::jsonb),
('bbbb0000-0000-0000-0000-00000000000b', 'aaaa0000-0000-0000-0000-00000000000b', 'APPLE-1KG', '69000000000b', 2990, 'CNY', 3990, 'CNY', '{"规格":"1kg"}'::jsonb, NULL,
 '{"batchTracking":true,"pickingStrategy":"FEFO","minRemainingShelfLifeDays":3,"nearExpiryThresholdDays":7,"nearExpiryAction":"DISCOUNT","autoBlockExpired":true}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no, production_date, expiry_date,
                             quantity, reserved_quantity, status, unit_cost_amount, unit_cost_currency, received_at)
VALUES
('d1000000-0000-0000-0000-000000000004', 'bbbb0000-0000-0000-0000-000000000004', 'WAREHOUSE', '11111111-1111-1111-1111-111111111111', 'HEADPHONE-WH-001', NULL, NULL, 80, 0, 'AVAILABLE', 18000, 'CNY', now() - interval '5 days'),
('d1000000-0000-0000-0000-000000000005', 'bbbb0000-0000-0000-0000-000000000005', 'WAREHOUSE', '11111111-1111-1111-1111-111111111111', 'JUICER-WH-001', NULL, NULL, 120, 0, 'AVAILABLE', 5000, 'CNY', now() - interval '8 days'),
('d1000000-0000-0000-0000-000000000006', 'bbbb0000-0000-0000-0000-000000000006', 'WAREHOUSE', '11111111-1111-1111-1111-111111111111', 'MOTOR-WH-001', NULL, NULL, 30, 0, 'AVAILABLE', 9000, 'CNY', now() - interval '10 days'),
('d1000000-0000-0000-0000-000000000007', 'bbbb0000-0000-0000-0000-000000000007', 'WAREHOUSE', '11111111-1111-1111-1111-111111111111', 'FLANGE-WH-001', NULL, NULL, 60, 0, 'AVAILABLE', 3000, 'CNY', now() - interval '12 days'),
('d1000000-0000-0000-0000-000000000008', 'bbbb0000-0000-0000-0000-000000000008', 'WAREHOUSE', '11111111-1111-1111-1111-111111111111', 'CLEANSER-WH-001', CURRENT_DATE - 10, CURRENT_DATE + 45, 200, 0, 'AVAILABLE', 3500, 'CNY', now() - interval '10 days'),
('d1000000-0000-0000-0000-000000000009', 'bbbb0000-0000-0000-0000-000000000009', 'WAREHOUSE', '11111111-1111-1111-1111-111111111111', 'YOGA-WH-001', NULL, NULL, 90, 0, 'AVAILABLE', 4000, 'CNY', now() - interval '6 days'),
('d1000000-0000-0000-0000-00000000000a', 'bbbb0000-0000-0000-0000-00000000000a', 'WAREHOUSE', '11111111-1111-1111-1111-111111111111', 'BAND-WH-001', NULL, NULL, 70, 0, 'AVAILABLE', 12000, 'CNY', now() - interval '4 days'),
('d1000000-0000-0000-0000-00000000000b', 'bbbb0000-0000-0000-0000-00000000000b', 'WAREHOUSE', '11111111-1111-1111-1111-111111111111', 'APPLE-WH-001', CURRENT_DATE - 3, CURRENT_DATE + 12, 150, 0, 'AVAILABLE', 1500, 'CNY', now() - interval '3 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no, production_date, expiry_date,
                             quantity, reserved_quantity, status, unit_cost_amount, unit_cost_currency, received_at)
VALUES
('d1000000-0000-0000-0000-00000000000c', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'STORE',
 '22222222-2222-2222-2222-222222222222', 'MILK-SH-BULK', CURRENT_DATE, CURRENT_DATE + 60,
 1000, 0, 'AVAILABLE', 1200, 'CNY', now())
ON CONFLICT (id) DO NOTHING;
INSERT INTO promotion (id, code, type, title, description, status, starts_at, ends_at, sku_id,
                       promo_price_amount, gift_sku_id, buy_quantity, gift_quantity, max_gift_per_order,
                       stock_limit, sold_quantity, per_user_limit, group_size, group_duration_hours, priority)
VALUES
('91000000-0000-0000-0000-000000000001', 'DEMO-FLASH-MUG', 'FLASH_SALE', '马克杯限时秒杀',
 '手工陶瓷马克杯限时 2990 分，每单最多 5 件。', 'ACTIVE', now() - interval '1 hour', now() + interval '30 days',
 'dddddddd-dddd-dddd-dddd-dddddddddddd', 2990, NULL, NULL, NULL, NULL, 30, 0, 5, NULL, NULL, 30),
('91000000-0000-0000-0000-000000000002', 'DEMO-GIFT-MILK', 'BUY_GIFT', '鲜奶买二赠一',
 '购买 2 件 1L 鲜奶赠送 1 件同款，每单最多赠 2 件。', 'ACTIVE', now() - interval '1 hour', now() + interval '30 days',
 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', NULL, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 2, 1, 2,
 20, 0, NULL, NULL, NULL, 20),
('91000000-0000-0000-0000-000000000003', 'DEMO-GROUP-MUG', 'GROUP_BUY', '马克杯 2 人拼团',
 '2 人成团，每人 2490 分，24 小时内未成团自动退款。', 'ACTIVE', now() - interval '1 hour', now() + interval '30 days',
 'dddddddd-dddd-dddd-dddd-dddddddddddd', 2490, NULL, NULL, NULL, NULL, NULL, 0, 1, 2, 24, 10)
ON CONFLICT (id) DO NOTHING;
