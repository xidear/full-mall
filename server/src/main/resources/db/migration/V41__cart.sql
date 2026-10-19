-- 购物车（服务端，跨端一致）：登录用户与游客都可用，登录后合并
CREATE TABLE cart (
    id         UUID PRIMARY KEY,
    owner_key  VARCHAR(128) NOT NULL,       -- member:<id> 或 guest:<token>
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (owner_key)
);

CREATE TABLE cart_item (
    id         UUID PRIMARY KEY,
    cart_id    UUID NOT NULL REFERENCES cart(id) ON DELETE CASCADE,
    sku_id     UUID NOT NULL REFERENCES sku(id),
    quantity   INT  NOT NULL,
    added_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (cart_id, sku_id),
    CONSTRAINT cart_item_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX cart_item_cart_idx ON cart_item (cart_id, added_at);
