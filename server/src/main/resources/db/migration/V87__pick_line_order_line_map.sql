-- 拣货行与订单行的多对多映射：合并行（同 SKU+批次，付费行+赠品行）
-- 也需要能回填批次，否则售后退货无法按原批次回冲成本。
CREATE TABLE pick_line_order_line (
    pick_line_id UUID NOT NULL REFERENCES pick_task_line(id),
    order_line_id UUID NOT NULL REFERENCES order_line(id),
    quantity INT NOT NULL,
    PRIMARY KEY (pick_line_id, order_line_id),
    CONSTRAINT pick_line_order_line_quantity_check CHECK (quantity > 0)
);

CREATE INDEX pick_line_order_line_order_idx ON pick_line_order_line (order_line_id);
