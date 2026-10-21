-- 部分售后期间冻结佣金：售后关闭前不得提现；关闭为退货则按比例回冲
ALTER TABLE distribution_commission
    ADD COLUMN reversed_amount BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN hold_after_sale_id UUID,
    ADD COLUMN hold_amount BIGINT NOT NULL DEFAULT 0;

ALTER TABLE distribution_commission ADD CONSTRAINT distribution_commission_reversed_check
    CHECK (reversed_amount >= 0 AND reversed_amount + consumed_amount <= commission_amount);

CREATE INDEX distribution_commission_hold_idx
    ON distribution_commission (hold_after_sale_id)
    WHERE hold_after_sale_id IS NOT NULL;
