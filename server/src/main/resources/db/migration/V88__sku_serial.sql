-- 单件序列号（IMEI/唯一码）：手机等 3C 商品需要定位"具体个体"
CREATE TABLE sku_serial (
    id          UUID PRIMARY KEY,
    sku_id      UUID NOT NULL REFERENCES sku(id),
    batch_id    UUID NOT NULL REFERENCES inventory_batch(id),
    serial_no   VARCHAR(64) NOT NULL UNIQUE,
    status      VARCHAR(16) NOT NULL DEFAULT 'IN_STOCK',
    order_line_id UUID REFERENCES order_line(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT sku_serial_status_check
        CHECK (status IN ('IN_STOCK', 'RESERVED', 'SOLD', 'RETURNED', 'SCRAPPED'))
);

CREATE INDEX sku_serial_batch_idx ON sku_serial (batch_id);
CREATE INDEX sku_serial_sku_idx ON sku_serial (sku_id, status);
