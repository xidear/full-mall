-- 供应商结算单（SUP-020）。佣金/退货冲减字段先留出，规则未定前默认 0（不臆造费率）。
CREATE TABLE supplier_settlement (
    id               UUID PRIMARY KEY,
    supplier_id      UUID         NOT NULL REFERENCES supplier(id),
    period_start     TIMESTAMPTZ  NOT NULL,
    period_end       TIMESTAMPTZ  NOT NULL,
    order_count      INT          NOT NULL,
    gross_amount     BIGINT       NOT NULL,
    refund_amount    BIGINT       NOT NULL DEFAULT 0,
    commission_amount BIGINT      NOT NULL DEFAULT 0,
    payable_amount   BIGINT       NOT NULL,
    currency         CHAR(3)      NOT NULL,
    status           VARCHAR(16)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT supplier_settlement_amounts_non_negative
        CHECK (gross_amount >= 0 AND refund_amount >= 0 AND commission_amount >= 0),
    CONSTRAINT supplier_settlement_period_check CHECK (period_end > period_start),
    CONSTRAINT supplier_settlement_status_check
        CHECK (status IN ('DRAFT', 'CONFIRMED', 'PAID', 'DISPUTED')),
    CONSTRAINT supplier_settlement_payable_check
        CHECK (payable_amount = gross_amount - refund_amount - commission_amount)
);

CREATE INDEX supplier_settlement_supplier_idx ON supplier_settlement (supplier_id, period_end DESC);