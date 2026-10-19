-- 功能开关（FLAG-001..005）
-- 只有布尔类模块开关入库；site_status 仍由 system_setting 单一存放，避免双份真相。
CREATE TABLE feature_flag (
    code VARCHAR(64) PRIMARY KEY,
    enabled BOOLEAN NOT NULL,
    updated_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reason VARCHAR(512) NOT NULL
);

INSERT INTO feature_flag (code, enabled, updated_by, reason) VALUES
('supplier.enabled', TRUE, 'migration:V22', '初始值：供应商模块开启'),
('pos.sale.enabled', TRUE, 'migration:V22', '初始值：POS 卖货开启'),
('pos.pickup.enabled', TRUE, 'migration:V22', '初始值：POS 自提核销开启');