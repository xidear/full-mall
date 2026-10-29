-- 品牌名唯一（大小写/首尾空格不敏感）：同名品牌会让"按品牌筛选/促销范围"出现重复项。
-- 先去重：同一名字只保留最早创建的一条，引用它的商品改指向保留项，再建唯一索引。
WITH ranked AS (
    SELECT id,
           first_value(id) OVER (PARTITION BY lower(btrim(name)) ORDER BY created_at, id) AS keeper,
           row_number() OVER (PARTITION BY lower(btrim(name)) ORDER BY created_at, id) AS rn
      FROM brand
)
UPDATE product p
   SET brand_id = r.keeper
  FROM ranked r
 WHERE p.brand_id = r.id AND r.rn > 1;

WITH ranked AS (
    SELECT id,
           row_number() OVER (PARTITION BY lower(btrim(name)) ORDER BY created_at, id) AS rn
      FROM brand
)
DELETE FROM brand b USING ranked r WHERE b.id = r.id AND r.rn > 1;

CREATE UNIQUE INDEX brand_name_unique ON brand (lower(btrim(name)));
