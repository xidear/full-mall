-- 换货自动完成：发货后超过 N 天且顾客未提出"未收到"，视为确认收货。
-- 依据（用户确认 2026-10-19）：自营场景下快递签收即视为送达，争议走客服；
-- 大量顾客不会主动点确认，若一直停在 EXCHANGE_SHIPPED 会阻塞售后与佣金结算。
ALTER TABLE after_sale ADD COLUMN auto_complete_at TIMESTAMPTZ;
ALTER TABLE after_sale ADD COLUMN disputed_at TIMESTAMPTZ;
ALTER TABLE after_sale ADD COLUMN auto_completed BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX after_sale_auto_complete_idx ON after_sale (auto_complete_at)
    WHERE status = 'EXCHANGE_SHIPPED';
