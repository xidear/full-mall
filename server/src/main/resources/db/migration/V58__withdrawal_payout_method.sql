-- 提现打款方式：由申请人在申请时选择
ALTER TABLE withdrawal
    ADD COLUMN payout_method VARCHAR(16),
    ADD COLUMN payout_account VARCHAR(128);

ALTER TABLE withdrawal ADD CONSTRAINT withdrawal_payout_method_check
    CHECK (payout_method IS NULL OR payout_method IN ('ALIPAY', 'BANK', 'WECHAT'));
