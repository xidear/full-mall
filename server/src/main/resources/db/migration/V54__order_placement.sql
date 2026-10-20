-- 下单来源：区分顾客自助与员工代下单（门店预定/预购等），便于追溯
ALTER TABLE orders
    ADD COLUMN placed_by VARCHAR(16) NOT NULL DEFAULT 'CUSTOMER',
    ADD COLUMN placed_by_operator VARCHAR(128);

ALTER TABLE orders ADD CONSTRAINT orders_placed_by_check
    CHECK (placed_by IN ('CUSTOMER', 'CASHIER', 'ADMIN'));

-- 员工代下单必须记录操作员
ALTER TABLE orders ADD CONSTRAINT orders_operator_required_check
    CHECK (placed_by = 'CUSTOMER' OR placed_by_operator IS NOT NULL);
