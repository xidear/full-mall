-- 佣金可被部分消耗：提现金额可能小于单条佣金，也可能只有一条大额佣金
ALTER TABLE distribution_commission
    ADD COLUMN consumed_amount BIGINT NOT NULL DEFAULT 0;

ALTER TABLE distribution_commission
    ADD CONSTRAINT distribution_commission_consumed_check
    CHECK (consumed_amount >= 0 AND consumed_amount <= commission_amount);

-- 订单完成时间：售后关闭策略的起算点（FULFILLED 时写入）
ALTER TABLE orders ADD COLUMN fulfilled_at TIMESTAMPTZ;
