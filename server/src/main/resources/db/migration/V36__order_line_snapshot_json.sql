-- 商品快照改为单个 JSONB 字段（V35 的拆散列撤销）
ALTER TABLE order_line ADD COLUMN snapshot JSONB;

-- 把 V35 已建的分列合并进 snapshot，再删除分列
UPDATE order_line SET snapshot = jsonb_strip_nulls(jsonb_build_object(
    'image', image_snapshot,
    'locale', locale,
    'attributes', attributes_snapshot,
    'brand', brand_snapshot,
    'category', category_snapshot,
    'listPriceAmount', list_price_amount,
    'costAmount', cost_amount
)) WHERE snapshot IS NULL;

ALTER TABLE order_line
    DROP COLUMN image_snapshot,
    DROP COLUMN locale,
    DROP COLUMN attributes_snapshot,
    DROP COLUMN brand_snapshot,
    DROP COLUMN category_snapshot,
    DROP COLUMN list_price_amount,
    DROP COLUMN cost_amount;

COMMENT ON COLUMN order_line.snapshot IS
    '下单时的商品快照（服务端生成，用户不可输入）：image/attributes/brand/category/listPrice/cost/locale 等，字段可增不改表';
