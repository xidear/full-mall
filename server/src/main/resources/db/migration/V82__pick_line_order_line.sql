-- 拣货行到订单行的引用：用于把实际批次回填到 order_line.batch_id（退货按原批次回冲）。
-- 可空：同一 SKU+批次可能来自多条订单行（付费行+赠品行）而合并为一条拣货行，
-- 此时无法唯一定位，必须先拆分辨率（见 OPEN_ITEMS H21）。
ALTER TABLE pick_task_line ADD COLUMN order_line_id UUID REFERENCES order_line(id);
