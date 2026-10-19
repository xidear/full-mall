-- 佣金账本（概念来自成熟商城的分销模型，非照搬表结构）
-- 目标：佣金不再"支付即得"，而是 冻结 → 可提现 → 已提现/已回冲，且每次变更都有不可变流水。

ALTER TABLE distribution_commission
    ADD COLUMN frozen_until TIMESTAMPTZ,
    ADD COLUMN available_at TIMESTAMPTZ,
    ADD COLUMN reversed_at  TIMESTAMPTZ,
    ADD COLUMN reverse_reason VARCHAR(255);

-- 存量映射：已结算 → AVAILABLE；未结算 → FROZEN（冻结期按创建时间 +7 天）
UPDATE distribution_commission
   SET status = CASE WHEN status = 'SETTLED' THEN 'AVAILABLE' ELSE 'FROZEN' END,
       frozen_until = COALESCE(created_at + interval '7 days', created_at),
       available_at = CASE WHEN status = 'SETTLED' THEN settled_at ELSE NULL END;

ALTER TABLE distribution_commission
    ADD CONSTRAINT distribution_commission_status_new_check
    CHECK (status IN ('FROZEN', 'AVAILABLE', 'WITHDRAWN', 'REVERSED'));

CREATE INDEX distribution_commission_due_idx
    ON distribution_commission (status, frozen_until);

-- 状态变更流水：只追加
CREATE TABLE commission_ledger (
    id            UUID PRIMARY KEY,
    commission_id UUID NOT NULL REFERENCES distribution_commission(id),
    from_status   VARCHAR(16) NOT NULL,
    to_status     VARCHAR(16) NOT NULL,
    amount_amount BIGINT NOT NULL,
    currency      CHAR(3) NOT NULL,
    reason        VARCHAR(255),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX commission_ledger_commission_idx ON commission_ledger (commission_id, created_at);
