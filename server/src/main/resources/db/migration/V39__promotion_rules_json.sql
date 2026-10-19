-- Promotion 收敛：类型专属字段统一进 rules JSONB（37 列 → 关键列 + JSON）
-- 读路径不变：旧列重建为**生成列**，由 rules 派生，现有查询零改动。

ALTER TABLE promotion ADD COLUMN rules JSONB;

UPDATE promotion SET rules = jsonb_strip_nulls(jsonb_build_object(
    'skuId', sku_id,
    'promoPriceAmount', promo_price_amount,
    'giftSkuId', gift_sku_id,
    'buyQuantity', buy_quantity,
    'giftQuantity', gift_quantity,
    'maxGiftPerOrder', max_gift_per_order,
    'stockLimit', stock_limit,
    'perUserLimit', per_user_limit,
    'groupSize', group_size,
    'groupDurationHours', group_duration_hours
)) WHERE rules IS NULL;

-- 旧 CHECK 依赖即将删除的列，先移除
ALTER TABLE promotion
    DROP CONSTRAINT promotion_flash_check,
    DROP CONSTRAINT promotion_gift_check,
    DROP CONSTRAINT promotion_group_check;

ALTER TABLE promotion
    DROP COLUMN sku_id,
    DROP COLUMN promo_price_amount,
    DROP COLUMN gift_sku_id,
    DROP COLUMN buy_quantity,
    DROP COLUMN gift_quantity,
    DROP COLUMN max_gift_per_order,
    DROP COLUMN stock_limit,
    DROP COLUMN per_user_limit,
    DROP COLUMN group_size,
    DROP COLUMN group_duration_hours;

-- 生成列：读代码无需改动
ALTER TABLE promotion
    ADD COLUMN sku_id UUID GENERATED ALWAYS AS ((rules->>'skuId')::uuid) STORED,
    ADD COLUMN promo_price_amount BIGINT GENERATED ALWAYS AS ((rules->>'promoPriceAmount')::bigint) STORED,
    ADD COLUMN gift_sku_id UUID GENERATED ALWAYS AS ((rules->>'giftSkuId')::uuid) STORED,
    ADD COLUMN buy_quantity INT GENERATED ALWAYS AS ((rules->>'buyQuantity')::int) STORED,
    ADD COLUMN gift_quantity INT GENERATED ALWAYS AS ((rules->>'giftQuantity')::int) STORED,
    ADD COLUMN max_gift_per_order INT GENERATED ALWAYS AS ((rules->>'maxGiftPerOrder')::int) STORED,
    ADD COLUMN stock_limit INT GENERATED ALWAYS AS ((rules->>'stockLimit')::int) STORED,
    ADD COLUMN per_user_limit INT GENERATED ALWAYS AS ((rules->>'perUserLimit')::int) STORED,
    ADD COLUMN group_size INT GENERATED ALWAYS AS ((rules->>'groupSize')::int) STORED,
    ADD COLUMN group_duration_hours INT GENERATED ALWAYS AS ((rules->>'groupDurationHours')::int) STORED;

-- 约束改为直接校验 JSON，避免依赖生成列
ALTER TABLE promotion
    ADD CONSTRAINT promotion_rules_required CHECK (rules IS NOT NULL),
    ADD CONSTRAINT promotion_flash_rules_check CHECK (
        type <> 'FLASH_SALE' OR ((rules->>'skuId') IS NOT NULL
            AND (rules->>'promoPriceAmount')::bigint > 0
            AND (rules->>'stockLimit')::int > 0)),
    ADD CONSTRAINT promotion_gift_rules_check CHECK (
        type <> 'BUY_GIFT' OR ((rules->>'giftSkuId') IS NOT NULL
            AND (rules->>'buyQuantity')::int > 0
            AND (rules->>'giftQuantity')::int > 0)),
    ADD CONSTRAINT promotion_group_rules_check CHECK (
        type <> 'GROUP_BUY' OR ((rules->>'promoPriceAmount')::bigint > 0
            AND (rules->>'groupSize')::int >= 2
            AND (rules->>'groupDurationHours')::int > 0));

COMMENT ON COLUMN promotion.rules IS
    '促销类型专属配置（服务端生成）；旧列为其生成列，新增促销类型无需改表';
