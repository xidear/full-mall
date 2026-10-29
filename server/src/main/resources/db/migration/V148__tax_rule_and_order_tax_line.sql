-- 批次 E：税（按税务辖区/目的地）与订单税行
--
-- 税率用**基点**（1 bps = 0.01%）存整数，不用小数：浮点税率与金额相乘会出现 ±1 分错账（ADR-0004 同一原则）。
-- 计税口径：
--   price_includes_tax = TRUE  → 标价含税，税额从价内**反算**（subtract），订单总额不变；
--   price_includes_tax = FALSE → 标价是净价，税额**另加**（add），订单总额增加。
-- 查表顺序：目的地国家（目的地税，如 EU OSS / US ship-to）→ 市场 tax_jurisdiction（申报主体默认税率）。

CREATE TABLE IF NOT EXISTS tax_rule (
    id               UUID       PRIMARY KEY,
    -- 匹配键：ISO 3166-1 国家码，或市场的 tax_jurisdiction（如 CN-VAT / EU-OSS）
    jurisdiction     VARCHAR(32) NOT NULL,
    name             VARCHAR(64) NOT NULL,
    -- 基点：10000 = 100%（1300 = 13%）；上限卡死防止把"13% 写成 130000"这类数量级错误落库
    rate_bps         INT        NOT NULL CHECK (rate_bps >= 0 AND rate_bps <= 10000),
    price_includes_tax BOOLEAN   NOT NULL,
    active           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS tax_rule_jurisdiction_idx
    ON tax_rule (jurisdiction) WHERE active;

COMMENT ON COLUMN tax_rule.rate_bps IS '税率基点：1300 = 13%；10000 = 100%';

CREATE TABLE IF NOT EXISTS order_tax_line (
    id              UUID        PRIMARY KEY,
    order_id        UUID        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    jurisdiction    VARCHAR(32) NOT NULL,
    name            VARCHAR(64) NOT NULL,
    rate_bps        INT         NOT NULL,
    price_includes_tax BOOLEAN  NOT NULL,
    taxable_amount  BIGINT      NOT NULL,
    tax_amount      BIGINT      NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS order_tax_line_order_idx ON order_tax_line (order_id);

COMMENT ON TABLE order_tax_line IS '订单税行：下单时快照税率与税基，事后改税率不影响历史订单';
