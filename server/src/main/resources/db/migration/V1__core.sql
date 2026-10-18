CREATE TABLE market (
    code VARCHAR(8) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    currency CHAR(3) NOT NULL,
    default_locale VARCHAR(16) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE category (
    id UUID PRIMARY KEY,
    parent_id UUID REFERENCES category(id),
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE product (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category_id UUID REFERENCES category(id),
    brand_id UUID,
    status VARCHAR(32) NOT NULL,
    inventory_tracking_level VARCHAR(16) NOT NULL DEFAULT 'VARIANT',
    shelf_life_managed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE product_translation (
    product_id UUID NOT NULL REFERENCES product(id),
    locale VARCHAR(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    slug VARCHAR(255),
    seo_title VARCHAR(255),
    seo_description TEXT,
    PRIMARY KEY (product_id, locale)
);

CREATE TABLE sku (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES product(id),
    sku_code VARCHAR(64) NOT NULL UNIQUE,
    barcode VARCHAR(64),
    price_amount BIGINT NOT NULL,
    price_currency CHAR(3) NOT NULL,
    compare_at_amount BIGINT,
    compare_at_currency CHAR(3),
    options_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    image_url TEXT,
    shelf_life_policy_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT sku_price_non_negative CHECK (price_amount >= 0)
);

CREATE TABLE warehouse (
    id UUID PRIMARY KEY,
    market_code VARCHAR(8) NOT NULL REFERENCES market(code),
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE store (
    id UUID PRIMARY KEY,
    market_code VARCHAR(8) NOT NULL REFERENCES market(code),
    name VARCHAR(128) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    pickup_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    phone VARCHAR(32),
    opening_hours VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE supplier (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    contact_name VARCHAR(64),
    contact_phone VARCHAR(32),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE supplier_sku (
    supplier_id UUID NOT NULL REFERENCES supplier(id),
    sku_id UUID NOT NULL REFERENCES sku(id),
    supplier_sku_code VARCHAR(64) NOT NULL,
    price_amount BIGINT,
    price_currency CHAR(3),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (supplier_id, sku_id)
);

CREATE TABLE inventory_batch (
    id UUID PRIMARY KEY,
    sku_id UUID NOT NULL REFERENCES sku(id),
    location_type VARCHAR(16) NOT NULL,
    location_id UUID NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    production_date DATE,
    expiry_date DATE,
    quantity INT NOT NULL,
    reserved_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
    unit_cost_amount BIGINT,
    unit_cost_currency CHAR(3),
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT inventory_batch_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT inventory_batch_reserved_non_negative CHECK (reserved_quantity >= 0),
    CONSTRAINT inventory_batch_reserved_not_above_quantity CHECK (reserved_quantity <= quantity),
    UNIQUE (sku_id, location_type, location_id, batch_no)
);

CREATE INDEX idx_inventory_batch_lookup
    ON inventory_batch (sku_id, location_type, location_id, expiry_date, status);

CREATE TABLE stock_move (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES inventory_batch(id),
    sku_id UUID,
    location_type VARCHAR(16),
    location_id UUID,
    move_type VARCHAR(32) NOT NULL,
    quantity INT NOT NULL,
    reference_type VARCHAR(32),
    reference_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_stock_move_batch ON stock_move (batch_id, occurred_at);
CREATE INDEX idx_stock_move_reference ON stock_move (reference_type, reference_id);

CREATE TABLE reservation (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    order_line_id UUID NOT NULL,
    batch_id UUID NOT NULL REFERENCES inventory_batch(id),
    quantity INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT reservation_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_reservation_order ON reservation (order_id);
CREATE INDEX idx_reservation_expiry ON reservation (status, expires_at);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    currency CHAR(3) NOT NULL,
    subtotal_amount BIGINT NOT NULL,
    shipping_fee_amount BIGINT NOT NULL DEFAULT 0,
    discount_amount BIGINT NOT NULL DEFAULT 0,
    total_amount BIGINT NOT NULL,
    contact_name VARCHAR(64) NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    contact_email VARCHAR(128),
    address_json JSONB,
    fulfillment_mode VARCHAR(16) NOT NULL,
    store_id UUID REFERENCES store(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_status_created ON orders (status, created_at);

CREATE TABLE order_line (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    sku_id UUID NOT NULL,
    title_snapshot VARCHAR(255) NOT NULL,
    sku_code_snapshot VARCHAR(64) NOT NULL,
    quantity INT NOT NULL,
    unit_price_amount BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    fulfillment_type VARCHAR(32) NOT NULL,
    CONSTRAINT order_line_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_order_line_order ON order_line (order_id);

CREATE TABLE fulfillment_order (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    source_id UUID,
    carrier_code VARCHAR(32),
    tracking_no VARCHAR(128),
    pickup_code VARCHAR(16),
    pickup_expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_fulfillment_order_order ON fulfillment_order (order_id);

CREATE TABLE fulfillment_line (
    id UUID PRIMARY KEY,
    fulfillment_order_id UUID NOT NULL REFERENCES fulfillment_order(id),
    order_line_id UUID NOT NULL,
    sku_id UUID NOT NULL,
    quantity INT NOT NULL
);

CREATE INDEX idx_fulfillment_line_order ON fulfillment_line (fulfillment_order_id);

CREATE TABLE pick_task (
    id UUID PRIMARY KEY,
    fulfillment_order_id UUID NOT NULL REFERENCES fulfillment_order(id),
    store_id UUID REFERENCES store(id),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_pick_task_store_status ON pick_task (store_id, status);

CREATE TABLE pick_task_line (
    id UUID PRIMARY KEY,
    pick_task_id UUID NOT NULL REFERENCES pick_task(id),
    sku_id UUID NOT NULL,
    quantity INT NOT NULL,
    suggested_batch_id UUID REFERENCES inventory_batch(id),
    suggested_expiry_date DATE,
    picked_quantity INT NOT NULL DEFAULT 0,
    CONSTRAINT pick_task_line_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_pick_task_line_task ON pick_task_line (pick_task_id);

CREATE TABLE pickup (
    id UUID PRIMARY KEY,
    fulfillment_order_id UUID NOT NULL REFERENCES fulfillment_order(id),
    store_id UUID NOT NULL REFERENCES store(id),
    pickup_code VARCHAR(16) NOT NULL UNIQUE,
    status VARCHAR(16) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    contact_phone VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_pickup_status ON pickup (status, expires_at);

CREATE TABLE payment (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    provider VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    amount_amount BIGINT NOT NULL,
    amount_currency CHAR(3) NOT NULL,
    provider_transaction_id VARCHAR(128),
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_order ON payment (order_id, created_at);

CREATE TABLE payment_event (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payment(id),
    event_type VARCHAR(64) NOT NULL,
    payload_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE legal_document (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    type VARCHAR(32) NOT NULL,
    market_code VARCHAR(8) NOT NULL REFERENCES market(code),
    legal_entity VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE legal_document_version (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES legal_document(id),
    version VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ,
    content_hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (document_id, version)
);

CREATE TABLE legal_document_localization (
    id UUID PRIMARY KEY,
    version_id UUID NOT NULL REFERENCES legal_document_version(id),
    locale VARCHAR(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    content_hash VARCHAR(128) NOT NULL,
    review_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (version_id, locale)
);

CREATE TABLE legal_acceptance (
    id UUID PRIMARY KEY,
    user_id UUID,
    document_version_id UUID NOT NULL REFERENCES legal_document_version(id),
    locale VARCHAR(16) NOT NULL,
    accepted_at TIMESTAMPTZ NOT NULL,
    ip VARCHAR(64),
    user_agent TEXT,
    method VARCHAR(16) NOT NULL,
    evidence_hash VARCHAR(128) NOT NULL,
    order_id UUID,
    session_id VARCHAR(64)
);

CREATE INDEX idx_legal_acceptance_version ON legal_acceptance (document_version_id, accepted_at);

CREATE TABLE analytics_event (
    id UUID PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    market_code VARCHAR(8),
    channel VARCHAR(32),
    store_id UUID,
    warehouse_id UUID,
    supplier_id UUID,
    order_id UUID,
    sku_id UUID,
    batch_id UUID,
    properties_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    schema_version INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_analytics_event_type_time ON analytics_event (event_type, occurred_at);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    actor_id UUID,
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id UUID,
    before_json JSONB,
    after_json JSONB,
    ip VARCHAR(64),
    user_agent TEXT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_resource ON audit_log (resource_type, resource_id, occurred_at);

CREATE TABLE support_conversation (
    id UUID PRIMARY KEY,
    customer_id UUID,
    agent_id UUID,
    order_id UUID,
    status VARCHAR(16) NOT NULL,
    locale VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_support_conversation_status ON support_conversation (status, created_at);

CREATE TABLE support_message (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES support_conversation(id),
    sender_type VARCHAR(16) NOT NULL,
    sender_id UUID,
    message_type VARCHAR(16) NOT NULL,
    content TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_support_message_conversation ON support_message (conversation_id, created_at);