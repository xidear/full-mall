-- 提现审核独立权限：默认只有 ADMIN 拥有，可在权限矩阵中授予其他角色
INSERT INTO permission (code, label, group_name, sort_order)
VALUES ('withdrawal.review', '提现审核与打款', '财务', 70)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role, permission_code)
VALUES ('ADMIN', 'withdrawal.review')
ON CONFLICT (role, permission_code) DO NOTHING;
