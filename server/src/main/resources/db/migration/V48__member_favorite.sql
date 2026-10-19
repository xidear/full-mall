-- 商品收藏：一个会员对同一 SKU 只收藏一次
CREATE TABLE member_favorite (
    member_id  UUID NOT NULL REFERENCES member(id),
    sku_id     UUID NOT NULL REFERENCES sku(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (member_id, sku_id)
);

CREATE INDEX member_favorite_created_idx ON member_favorite (member_id, created_at DESC);
