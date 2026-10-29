-- 批次 D（ADR-0013/0014 的下游）：商品跨境要素 + 按市场定价 + 运费阶梯
--
-- 定价模型：sku.price_amount 是**基础价**，sku_price 是**按市场的覆盖价**。
-- 有覆盖用覆盖（币种必须与市场币种一致），没有则用基础价——这是"价格表覆盖"的通行做法，
-- 不是静默兜底：基础价本身就是该商品的价，不是编造值。
-- 但跨市场**不能**拿基础价顶替：币种不同就直接拒绝，避免"人民币价当港币卖"。

ALTER TABLE sku
    ADD COLUMN IF NOT EXISTS hs_code            VARCHAR(16),
    ADD COLUMN IF NOT EXISTS origin_country     CHAR(2),
    ADD COLUMN IF NOT EXISTS net_weight_grams   INT,
    ADD COLUMN IF NOT EXISTS gross_weight_grams INT;

ALTER TABLE sku
    ADD CONSTRAINT sku_weight_non_negative CHECK (
        (net_weight_grams IS NULL OR net_weight_grams >= 0)
        AND (gross_weight_grams IS NULL OR gross_weight_grams >= 0));

COMMENT ON COLUMN sku.hs_code IS 'HS 编码（报关用）；为空时报关单导出会明确标注"缺失"而不是留空行';
COMMENT ON COLUMN sku.origin_country IS '原产地（ISO 3166-1 alpha-2），跨境报关必填项之一';

CREATE TABLE IF NOT EXISTS sku_price (
    sku_id            UUID       NOT NULL REFERENCES sku (id) ON DELETE CASCADE,
    market_code       VARCHAR(8) NOT NULL REFERENCES market (code) ON DELETE CASCADE,
    currency          CHAR(3)    NOT NULL,
    amount            BIGINT     NOT NULL CHECK (amount > 0),
    compare_at_amount BIGINT,
    active            BOOLEAN    NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (sku_id, market_code)
);

CREATE INDEX IF NOT EXISTS sku_price_market_idx ON sku_price (market_code) WHERE active;

-- 回填：只把"币种与该市场币种一致"的基础价复制成市场价。
-- 币种不一致的（例如 CNY 商品 vs HKD 市场）**故意不回填**：那需要一个真实定价决策，不能由迁移替他决定。
INSERT INTO sku_price (sku_id, market_code, currency, amount, compare_at_amount)
SELECT s.id, m.code, s.price_currency, s.price_amount, s.compare_at_amount
  FROM sku s
  JOIN market m ON m.currency = s.price_currency
ON CONFLICT (sku_id, market_code) DO NOTHING;

CREATE TABLE IF NOT EXISTS shipping_rate (
    id                  UUID       PRIMARY KEY,
    market_code         VARCHAR(8) NOT NULL REFERENCES market (code) ON DELETE CASCADE,
    -- NULL = 该市场的"其它目的地"默认档；非空 = 指定国家/地区的档位
    destination_country CHAR(2),
    min_weight_grams    INT        NOT NULL DEFAULT 0 CHECK (min_weight_grams >= 0),
    -- NULL = 无上限
    max_weight_grams    INT,
    fee_amount          BIGINT     NOT NULL CHECK (fee_amount >= 0),
    currency            CHAR(3)    NOT NULL,
    -- 满额免运费；NULL = 不免
    free_over_amount    BIGINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT shipping_rate_band_check CHECK (
        max_weight_grams IS NULL OR max_weight_grams > min_weight_grams)
);

-- 同一市场同一目的地同一重量档只能有一条。
-- 用 COALESCE 表达式而不是 NULLS NOT DISTINCT：后者要 PG 15+，而测试用的嵌入式 Postgres 更老，
-- 迁移在测试里会直接语法错误（生产是 PG 17 能过，但测试起不来等于没验证）。
CREATE UNIQUE INDEX IF NOT EXISTS shipping_rate_band_idx
    ON shipping_rate (market_code, COALESCE(destination_country, '*'), min_weight_grams);

COMMENT ON TABLE shipping_rate IS '运费阶梯：按 市场 × 目的地 × 重量档 计费；dest=market 国家且无规则时按"国内默认免运费"处理，跨国无规则则明确报错';

-- 现状对齐：当前运费恒为 0（FLAT_SHIPPING_FEE=0L），把它落成数据，让运营可以改
INSERT INTO shipping_rate (id, market_code, destination_country, min_weight_grams, max_weight_grams,
                          fee_amount, currency, free_over_amount)
SELECT gen_random_uuid(), m.code, m.country_code, 0, NULL, 0, m.currency, NULL
  FROM market m
ON CONFLICT DO NOTHING;

-- 订单快照：报关与运费复算都要用到总重量
ALTER TABLE orders ADD COLUMN IF NOT EXISTS total_weight_grams INT;
COMMENT ON COLUMN orders.total_weight_grams IS '下单时按 SKU 净重汇总的重量快照；缺重量数据的商品按 0 计并可在报关单里看到警告';
