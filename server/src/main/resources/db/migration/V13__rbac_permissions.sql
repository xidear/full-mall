CREATE TABLE permission (
    code VARCHAR(64) PRIMARY KEY,
    label VARCHAR(128) NOT NULL,
    group_name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE role_permission (
    role VARCHAR(32) NOT NULL,
    permission_code VARCHAR(64) NOT NULL REFERENCES permission(code),
    PRIMARY KEY (role, permission_code)
);

INSERT INTO permission (code, label, group_name, sort_order) VALUES
('dashboard.view', '查看经营总览', '工作台', 10),
('product.manage', '商品管理', '商品', 20),
('order.manage', '订单管理', '交易', 30),
('customer.view', '客户与会员', '客户', 40),
('inventory.view', '库存与批次', '库存', 50),
('warehouse.manage', '仓储作业', '库存', 60),
('pos.manage', '门店收银', '门店', 70),
('supplier.manage', '供应商与凭证', '供应商', 80),
('notification.view', '通知中心', '运营', 90),
('theme.manage', '主题配置', '运营', 100),
('staff.manage', '员工与权限', '系统', 110),
('settings.manage', '系统设置', '系统', 120),
('permission.manage', '权限矩阵', '系统', 130)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role, permission_code)
SELECT 'OPERATOR', code FROM permission
WHERE code IN ('dashboard.view', 'product.manage', 'order.manage', 'customer.view',
               'inventory.view', 'warehouse.manage', 'pos.manage', 'supplier.manage',
               'notification.view', 'theme.manage')
ON CONFLICT (role, permission_code) DO NOTHING;