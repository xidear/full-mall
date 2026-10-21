-- 订单行补创建时间：此前只能按 id 排序（UUID 无序，语义不精确）
ALTER TABLE order_line ADD COLUMN created_at TIMESTAMPTZ;

-- 回填：用所属订单的创建时间（订单行与订单同批写入，误差可忽略且可解释）
UPDATE order_line l SET created_at = o.created_at FROM orders o WHERE o.id = l.order_id;

UPDATE order_line SET created_at = now() WHERE created_at IS NULL;
ALTER TABLE order_line ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE order_line ALTER COLUMN created_at SET DEFAULT now();

CREATE INDEX order_line_created_idx ON order_line (order_id, created_at);
