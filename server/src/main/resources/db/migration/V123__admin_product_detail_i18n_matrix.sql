-- 后台商品详情聚合 / 译文本地化图片 / 规格矩阵模板（2026-10-28）
--
-- 1) 译文可带本地化商品图（出海市场用当地语言主图）；
-- 2) 规格矩阵改为"可复用模板"：模板只描述维度与禁用组合，不含价格（未绑定商品时价格无意义），
--    商品可以选择一个模板生成 SKU，模板与商品多对一关联便于核对来源；
-- 3) 启用语言列表入库（默认 zh-CN 为源语言，en/ja 为目标语言）。

ALTER TABLE product_translation ADD COLUMN image_url VARCHAR(1024);

CREATE TABLE sku_matrix_template (
    id          UUID PRIMARY KEY,
    code        VARCHAR(64) NOT NULL UNIQUE,
    name        VARCHAR(128) NOT NULL,
    note        TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE sku_matrix_dimension (
    id          UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES sku_matrix_template(id) ON DELETE CASCADE,
    name        VARCHAR(64) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    CONSTRAINT sku_matrix_dimension_unique UNIQUE (template_id, name)
);

CREATE TABLE sku_matrix_dimension_value (
    id           UUID PRIMARY KEY,
    dimension_id UUID NOT NULL REFERENCES sku_matrix_dimension(id) ON DELETE CASCADE,
    name         VARCHAR(64) NOT NULL,
    sort_order   INT NOT NULL DEFAULT 0,
    CONSTRAINT sku_matrix_value_unique UNIQUE (dimension_id, name)
);

-- 禁用组合：{"颜色":"黑","存储":"512G"}，与 SkuMatrixMultiService 的 disabled 规则同构
CREATE TABLE sku_matrix_disabled (
    template_id      UUID NOT NULL REFERENCES sku_matrix_template(id) ON DELETE CASCADE,
    combination_json JSONB NOT NULL,
    PRIMARY KEY (template_id, combination_json)
);

ALTER TABLE product ADD COLUMN sku_matrix_template_id UUID REFERENCES sku_matrix_template(id);
CREATE INDEX product_sku_matrix_template_idx ON product (sku_matrix_template_id);

INSERT INTO system_setting (setting_key, setting_value)
VALUES ('enabled_locales', 'en,ja')
ON CONFLICT (setting_key) DO NOTHING;
