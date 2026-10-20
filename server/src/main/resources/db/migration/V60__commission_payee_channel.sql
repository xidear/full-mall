-- 出款通道与身份解耦：员工佣金走工资/绩效（PAYROLL），会员佣金走钱包（WALLET）
ALTER TABLE distribution_commission
    ADD COLUMN payee_channel VARCHAR(16) NOT NULL DEFAULT 'WALLET';

ALTER TABLE distribution_commission ADD CONSTRAINT distribution_commission_payee_channel_check
    CHECK (payee_channel IN ('WALLET', 'PAYROLL'));

-- 存量：员工推广的一律进工资通道，不进可提现余额
UPDATE distribution_commission SET payee_channel = 'PAYROLL' WHERE promoter_type = 'STAFF';

CREATE INDEX distribution_commission_payroll_idx
    ON distribution_commission (payee_channel, created_at DESC);
