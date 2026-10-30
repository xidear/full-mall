-- SKU 编码的唯一性应该是"商品内唯一"，不是全库唯一。
-- 症状：两个商品套同一个规格模板（颜色×尺寸）时，第二个商品的 6 条 SKU 全部被
-- ON CONFLICT (sku_code) DO NOTHING 跳过，接口返回 created=0/skipped=6，模板复用失效。
ALTER TABLE sku DROP CONSTRAINT IF EXISTS sku_sku_code_key;
ALTER TABLE sku ADD CONSTRAINT sku_product_code_key UNIQUE (product_id, sku_code);
