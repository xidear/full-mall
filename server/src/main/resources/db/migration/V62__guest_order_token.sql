-- 游客订单访问令牌：仅凭订单号不应能看到地址/电话/自提码
ALTER TABLE orders ADD COLUMN access_token_hash VARCHAR(64);

CREATE INDEX orders_access_token_idx ON orders (access_token_hash)
    WHERE access_token_hash IS NOT NULL;
