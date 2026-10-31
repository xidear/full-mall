-- 汇率（阶段 1：手工维护 + 精确换算；不自动改价、不回算历史订单）
-- rate 的含义：1 单位 base_currency = rate 单位 quote_currency。
CREATE TABLE IF NOT EXISTS exchange_rate (
    id uuid PRIMARY KEY,
    base_currency char(3) NOT NULL,
    quote_currency char(3) NOT NULL,
    rate numeric(20,10) NOT NULL,
    source varchar(32) NOT NULL,
    effective_at timestamp with time zone NOT NULL DEFAULT now(),
    created_by varchar(64) NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT exchange_rate_positive CHECK (rate > 0),
    CONSTRAINT exchange_rate_pair CHECK (base_currency <> quote_currency)
);

CREATE INDEX IF NOT EXISTS exchange_rate_pair_effective_idx
    ON exchange_rate (base_currency, quote_currency, effective_at DESC);

-- 同一对币种同一生效时刻只保留一条，避免"两条最新汇率"的歧义
CREATE UNIQUE INDEX IF NOT EXISTS exchange_rate_pair_effective_uk
    ON exchange_rate (base_currency, quote_currency, effective_at);