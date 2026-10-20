-- 分销：二级分佣 + 可配置等级比例（比例来自后台设置，不再写死）
ALTER TABLE distribution_commission
    ADD COLUMN level INT NOT NULL DEFAULT 1,
    ADD COLUMN rate_mode VARCHAR(16) NOT NULL DEFAULT 'GROSS',
    ADD COLUMN rate_value BIGINT NOT NULL DEFAULT 5,
    ADD COLUMN promoter_type VARCHAR(16) NOT NULL DEFAULT 'STAFF';

ALTER TABLE distribution_commission DROP CONSTRAINT IF EXISTS distribution_commission_order_id_promoter_staff_id_key;
ALTER TABLE distribution_commission
    ADD CONSTRAINT distribution_commission_unique UNIQUE (order_id, promoter_staff_id, level);

ALTER TABLE distribution_commission ADD CONSTRAINT distribution_commission_level_check
    CHECK (level IN (1, 2));
ALTER TABLE distribution_commission ADD CONSTRAINT distribution_commission_rate_mode_check
    CHECK (rate_mode IN ('GROSS', 'FIXED'));
ALTER TABLE distribution_commission ADD CONSTRAINT distribution_commission_promoter_type_check
    CHECK (promoter_type IN ('STAFF', 'MEMBER'));

CREATE INDEX distribution_commission_order_idx ON distribution_commission (order_id, level);

-- 推广员的"上级"，用于二级分佣；为空表示该推广员没有上级
CREATE TABLE promoter_link (
    promoter_type VARCHAR(16) NOT NULL,
    promoter_id   UUID NOT NULL,
    upline_type   VARCHAR(16) NOT NULL,
    upline_id     UUID NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (promoter_type, promoter_id),
    CONSTRAINT promoter_link_type_check
        CHECK (promoter_type IN ('STAFF', 'MEMBER') AND upline_type IN ('STAFF', 'MEMBER'))
);
