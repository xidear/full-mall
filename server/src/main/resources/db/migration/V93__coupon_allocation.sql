-- 券额按订单行金额比例分摊：部分退货时才知道"这一行用掉多少券"
CREATE TABLE order_coupon_allocation (
    order_id     UUID NOT NULL REFERENCES orders(id),
    order_line_id UUID NOT NULL REFERENCES order_line(id),
    coupon_id    UUID NOT NULL,
    amount       BIGINT NOT NULL,
    PRIMARY KEY (order_line_id),
    CONSTRAINT order_coupon_allocation_amount_check CHECK (amount >= 0)
);

CREATE INDEX order_coupon_allocation_order_idx ON order_coupon_allocation (order_id);
