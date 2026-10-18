INSERT INTO staff_user (id, username, password_hash, display_name, role, status)
VALUES
('aaaaaaaa-0000-0000-0000-000000000001', 'admin',
 '120000:O38NLTbPOapj1CKFL0mKYg==:p/tobrUa1MIgYiuWaSvvIZ61JmRyOhmXVKTk6cSSZDw=',
 '超级管理员', 'ADMIN', 'ACTIVE'),
('aaaaaaaa-0000-0000-0000-000000000002', 'operator',
 '120000:/+8ufsCwCHqHwDQPPj/Tgw==:7Q8Af6rcdBISt2q54EI/FYunwYlXI/6GXV/gu1utTEA=',
 '运营专员', 'OPERATOR', 'ACTIVE'),
('aaaaaaaa-0000-0000-0000-000000000003', 'store',
 '120000:yo30lbPMBKbXMrVtUVvDrA==:DdAsYBM6797+bWqJTlc00i2a9XKYe7Qlhgu/UQmS/Uc=',
 '门店店员', 'STORE', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;