-- 支付配置：可变配置 + 不可变版本（下单引版本，不引可编辑行）
CREATE TABLE payment_provider_config (
    id             UUID PRIMARY KEY,
    channel        VARCHAR(32) NOT NULL,           -- WECHAT / ALIPAY / UNIONPAY / AGGREGATOR
    mode           VARCHAR(16) NOT NULL,           -- DIRECT / SUB_MERCHANT
    market_code    VARCHAR(16) NOT NULL,
    display_name   VARCHAR(128) NOT NULL,
    active         BOOLEAN     NOT NULL DEFAULT TRUE,
    current_version INT        NOT NULL DEFAULT 1,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT payment_provider_config_mode_check CHECK (mode IN ('DIRECT', 'SUB_MERCHANT'))
);

-- 同一渠道同一市场只能有一条生效配置（切换服务商 = 停旧建新）
CREATE UNIQUE INDEX payment_provider_config_active_idx
    ON payment_provider_config (channel, market_code) WHERE active;

-- 版本表只追加：编辑配置 = 生成新版本，历史支付永远指向当时的版本
CREATE TABLE payment_provider_config_version (
    id             UUID PRIMARY KEY,
    config_id      UUID NOT NULL REFERENCES payment_provider_config(id),
    version_no     INT  NOT NULL,
    channel        VARCHAR(32) NOT NULL,
    mode           VARCHAR(16) NOT NULL,
    sp_name        VARCHAR(128),
    sp_mch_id      VARCHAR(64),
    sp_app_id      VARCHAR(128),
    sub_mch_id     VARCHAR(64),
    sub_app_id     VARCHAR(128),
    mch_id         VARCHAR(64),
    app_id         VARCHAR(128),
    callback_url   VARCHAR(512),
    created_by     VARCHAR(128) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (config_id, version_no),
    CONSTRAINT payment_provider_config_version_mode_check CHECK (mode IN ('DIRECT', 'SUB_MERCHANT'))
);

ALTER TABLE payment
    ADD COLUMN config_version_id UUID REFERENCES payment_provider_config_version(id),
    ADD COLUMN channel       VARCHAR(32),
    ADD COLUMN mode          VARCHAR(16),
    ADD COLUMN sp_mch_id     VARCHAR(64),
    ADD COLUMN sub_mch_id    VARCHAR(64);

-- 订单行商品快照：下单即冻结，不随商品后续修改变化（类似淘宝商品快照）
ALTER TABLE order_line
    ADD COLUMN image_snapshot      VARCHAR(1024),
    ADD COLUMN locale              VARCHAR(16),
    ADD COLUMN attributes_snapshot VARCHAR(1024),
    ADD COLUMN brand_snapshot      VARCHAR(128),
    ADD COLUMN category_snapshot   VARCHAR(255),
    ADD COLUMN list_price_amount   BIGINT,
    ADD COLUMN cost_amount         BIGINT;

ALTER TABLE orders ADD COLUMN locale VARCHAR(16);

COMMENT ON COLUMN order_line.cost_amount IS '出库时点成本快照（COGS），用于毛利，不随补货变化';
COMMENT ON COLUMN payment.config_version_id IS '下单时使用的支付配置版本；退款必须按此路由，不能按当前配置';