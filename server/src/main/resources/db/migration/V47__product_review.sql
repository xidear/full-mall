-- 商品评价：只能对"自己的、已支付"的订单中购买过的 SKU 评价，且每单每 SKU 只能评一次
CREATE TABLE product_review (
    id         UUID PRIMARY KEY,
    sku_id     UUID NOT NULL REFERENCES sku(id),
    member_id  UUID NOT NULL REFERENCES member(id),
    order_id   UUID NOT NULL REFERENCES orders(id),
    rating     INT  NOT NULL,
    content    VARCHAR(1000),
    status     VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT product_review_rating_check CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT product_review_status_check CHECK (status IN ('PUBLISHED', 'HIDDEN')),
    UNIQUE (order_id, sku_id, member_id)
);

CREATE INDEX product_review_sku_idx ON product_review (sku_id, created_at DESC);
