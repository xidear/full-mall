-- 按行退款：记录"退了哪件商品、退了几件、退多少钱"，是部分退款分摊的基础。
CREATE TABLE order_line_refund (
    id            UUID PRIMARY KEY,
    order_id      UUID NOT NULL REFERENCES orders(id),
    order_line_id UUID NOT NULL REFERENCES order_line(id),
    quantity      INT NOT NULL,
    amount_amount BIGINT NOT NULL,
    currency      CHAR(3) NOT NULL,
    reason        VARCHAR(255),
    refunded_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT order_line_refund_quantity_positive CHECK (quantity > 0),
    CONSTRAINT order_line_refund_amount_positive CHECK (amount_amount > 0)
);

CREATE INDEX order_line_refund_order_idx ON order_line_refund (order_id, refunded_at);
CREATE INDEX order_line_refund_line_idx ON order_line_refund (order_line_id);
