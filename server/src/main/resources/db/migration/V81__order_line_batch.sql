-- 订单行记录实际出库批次：售后退货必须按**原批次成本**回冲，
-- 没有这一列就只能猜（会虚增/虚减利润）。
ALTER TABLE order_line ADD COLUMN batch_id UUID REFERENCES inventory_batch(id);
CREATE INDEX order_line_batch_idx ON order_line (batch_id) WHERE batch_id IS NOT NULL;
