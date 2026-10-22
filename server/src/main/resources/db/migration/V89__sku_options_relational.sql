-- ADR-003：规格改关联表（真相来源），options_json 暂保留为投影
CREATE TABLE product_option (
    id          UUID PRIMARY KEY,
    product_id  UUID NOT NULL REFERENCES product(id),
    name        VARCHAR(64) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    CONSTRAINT product_option_unique UNIQUE (product_id, name)
);

CREATE TABLE product_option_value (
    id          UUID PRIMARY KEY,
    option_id   UUID NOT NULL REFERENCES product_option(id) ON DELETE CASCADE,
    name        VARCHAR(64) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    CONSTRAINT product_option_value_unique UNIQUE (option_id, name)
);

-- 每个 SKU 在每个维度只能有一个值：靠 option_id 冗余 + 唯一约束保证
CREATE TABLE sku_option_value (
    sku_id          UUID NOT NULL REFERENCES sku(id) ON DELETE CASCADE,
    option_id       UUID NOT NULL REFERENCES product_option(id),
    option_value_id UUID NOT NULL REFERENCES product_option_value(id),
    PRIMARY KEY (sku_id, option_id)
);

CREATE INDEX sku_option_value_value_idx ON sku_option_value (option_value_id);

-- 组合签名：同商品下不允许重复组合（由应用层按值 id 排序拼接生成）
ALTER TABLE sku ADD COLUMN combination_key VARCHAR(255);
CREATE UNIQUE INDEX sku_combination_unique ON sku (product_id, combination_key)
    WHERE combination_key IS NOT NULL;
