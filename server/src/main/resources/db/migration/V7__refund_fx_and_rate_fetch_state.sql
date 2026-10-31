-- 退款汇率快照：退款金额始终是原币种（不做跨币种退款/分账），
-- 但允许退款时汇率与下单时不同；这里记录退款时点的汇率与折算额用于对账。
ALTER TABLE order_line_refund ADD COLUMN IF NOT EXISTS fx_rate numeric(20,10);
ALTER TABLE order_line_refund ADD COLUMN IF NOT EXISTS fx_base_currency char(3);
ALTER TABLE order_line_refund ADD COLUMN IF NOT EXISTS fx_base_amount bigint;
ALTER TABLE order_line_refund ADD COLUMN IF NOT EXISTS fx_rate_at timestamp with time zone;

COMMENT ON COLUMN order_line_refund.fx_rate IS '退款时 原币种→基准币种 的汇率快照；允许与订单快照不同';

-- 自动汇率拉取状态（单行）：未配置供应商时保持空，不影响手工汇率。
CREATE TABLE IF NOT EXISTS exchange_rate_fetch_state (
    id boolean PRIMARY KEY DEFAULT true,
    provider varchar(32),
    last_attempt_at timestamp with time zone,
    last_success_at timestamp with time zone,
    last_error varchar(500),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT exchange_rate_fetch_state_single CHECK (id)
);