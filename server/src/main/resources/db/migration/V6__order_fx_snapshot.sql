-- 订单汇率快照（阶段 2）：下单时把“订单币种 → 基准币种”的汇率与折算额写死。
-- 只用于报表/对账/客服解释；订单金额与支付金额仍是订单币种，不因汇率变化回算。
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fx_rate numeric(20,10);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fx_base_currency char(3);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fx_base_total_amount bigint;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS fx_rate_at timestamp with time zone;

COMMENT ON COLUMN orders.fx_rate IS '下单时 订单币种→基准币种 的汇率快照；无可用汇率时为空（不阻塞下单）';
COMMENT ON COLUMN orders.fx_base_total_amount IS '按快照汇率折算的基准币种总额（最小单位）';