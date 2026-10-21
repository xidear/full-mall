-- 行退款的数量改为可空：按金额分摊时"退了几件"未必可知，不能瞎填导致数量累计超限
ALTER TABLE order_line_refund ALTER COLUMN quantity DROP NOT NULL;

ALTER TABLE order_line_refund DROP CONSTRAINT IF EXISTS order_line_refund_quantity_positive;
ALTER TABLE order_line_refund ADD CONSTRAINT order_line_refund_quantity_check
    CHECK (quantity IS NULL OR quantity > 0);
