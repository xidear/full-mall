-- 累计退款额：单次校验不够，退两次会超过支付额（超退风险）
ALTER TABLE payment ADD COLUMN refunded_amount BIGINT NOT NULL DEFAULT 0;

-- 存量回填：按历史退款事件累加（事件 detail 形如 {"amount":4000}）
UPDATE payment p SET refunded_amount = COALESCE((
    SELECT SUM((e.payload_json ->> 'amount')::bigint)
      FROM payment_event e
     WHERE e.payment_id = p.id AND e.event_type = 'PAYMENT_REFUNDED'
), 0);

ALTER TABLE payment ADD CONSTRAINT payment_refunded_check
    CHECK (refunded_amount >= 0 AND refunded_amount <= amount_amount);
