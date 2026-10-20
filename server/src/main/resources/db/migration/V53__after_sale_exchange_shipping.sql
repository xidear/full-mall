-- 换货发货：记录换货商品的承运商与运单号
ALTER TABLE after_sale
    ADD COLUMN exchange_carrier_code VARCHAR(32),
    ADD COLUMN exchange_tracking_no VARCHAR(128),
    ADD COLUMN exchange_shipped_at TIMESTAMPTZ;

-- 换货并发货后状态推进（状态列无 CHECK 约束，此处仅注释避免误改：REQUESTED/APPROVED/RECEIVED/REFUNDED/REJECTED/EXCHANGE_SHIPPED）
COMMENT ON COLUMN after_sale.status IS
    'REQUESTED/APPROVED/RECEIVED/REFUNDED/REJECTED/EXCHANGE_SHIPPED';
