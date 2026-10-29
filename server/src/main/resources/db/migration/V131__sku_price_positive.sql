-- 价格必须大于 0：DTO（@Positive）、批量改价、Admin 输入与数据库约束统一。
-- 先确认没有 0 元 SKU；若有，请先决定 0 元商品是否合法再执行。
ALTER TABLE sku DROP CONSTRAINT IF EXISTS sku_price_non_negative;
DO $$
BEGIN
    -- 演示库/本地手工执行过时也能安全重跑；PostgreSQL 不支持 ADD CONSTRAINT IF NOT EXISTS。
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sku_price_positive') THEN
        ALTER TABLE sku ADD CONSTRAINT sku_price_positive CHECK (price_amount > 0);
    END IF;
END $$;
