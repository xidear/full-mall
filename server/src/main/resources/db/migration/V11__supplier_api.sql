CREATE TABLE supplier_credential (
    id UUID PRIMARY KEY,
    supplier_id UUID NOT NULL UNIQUE REFERENCES supplier(id),
    access_key VARCHAR(64) NOT NULL UNIQUE,
    secret_encrypted TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    rotated_at TIMESTAMPTZ
);

ALTER TABLE fulfillment_order
    ADD COLUMN supplier_accepted_at TIMESTAMPTZ,
    ADD COLUMN supplier_note TEXT;

CREATE TABLE supplier_stock_feed (
    id UUID PRIMARY KEY,
    supplier_id UUID NOT NULL REFERENCES supplier(id),
    sku_id UUID NOT NULL REFERENCES sku(id),
    supplier_sku_code VARCHAR(64) NOT NULL,
    available_quantity INT NOT NULL,
    price_amount BIGINT,
    price_currency CHAR(3),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (supplier_id, sku_id),
    CONSTRAINT supplier_stock_feed_quantity_non_negative CHECK (available_quantity >= 0)
);

CREATE INDEX idx_supplier_stock_feed_supplier ON supplier_stock_feed (supplier_id, updated_at DESC);

-- Demo product that can only be fulfilled by the supplier, so the supplier API
-- flow can be verified end-to-end without touching warehouse/store stock.
INSERT INTO product (id, title, description, category_id, status, inventory_tracking_level, shelf_life_managed)
VALUES ('aaaa0000-0000-0000-0000-0000000000c1', '供应商直发测试商品',
        '仅由演示供应商供货，用于 /open/v1 代发履约联调。', '99999999-9999-9999-9999-999999999998',
        'ACTIVE', 'VARIANT', FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO product_translation (product_id, locale, title, description, slug, seo_title, seo_description)
VALUES ('aaaa0000-0000-0000-0000-0000000000c1', 'zh-CN', '供应商直发测试商品',
        '仅由演示供应商供货，用于 /open/v1 代发履约联调。', 'supplier-dropship-demo',
        '供应商直发测试商品', '供应商代发联调商品')
ON CONFLICT (product_id, locale) DO NOTHING;

INSERT INTO sku (id, product_id, sku_code, barcode, price_amount, price_currency,
                 compare_at_amount, compare_at_currency, options_json, image_url, shelf_life_policy_json)
VALUES ('bbbb0000-0000-0000-0000-0000000000c1', 'aaaa0000-0000-0000-0000-0000000000c1',
        'SUPPLIER-ONLY', '6900000000c1', 5000, 'CNY', NULL, NULL, '{"规格":"供应商直发"}'::jsonb,
        NULL, '{}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO supplier_sku (supplier_id, sku_id, supplier_sku_code, price_amount, price_currency)
VALUES ('33333333-3333-3333-3333-333333333333', 'bbbb0000-0000-0000-0000-0000000000c1',
        'SUP-DROPSHIP-001', 3500, 'CNY')
ON CONFLICT (supplier_id, sku_id) DO NOTHING;

INSERT INTO inventory_batch (id, sku_id, location_type, location_id, batch_no, production_date, expiry_date,
                             quantity, reserved_quantity, status, unit_cost_amount, unit_cost_currency, received_at)
VALUES ('b3000000-0000-0000-0000-0000000000c1', 'bbbb0000-0000-0000-0000-0000000000c1',
        'SUPPLIER', '33333333-3333-3333-3333-333333333333', 'SUPPLIER-FEED', NULL, NULL,
        100, 0, 'AVAILABLE', 3500, 'CNY', now())
ON CONFLICT (id) DO NOTHING;