-- C2：券与促销叠加口径统一——单券可覆盖全局默认（NULL = 跟随全局）
ALTER TABLE coupon_template ADD COLUMN IF NOT EXISTS stackable BOOLEAN;
COMMENT ON COLUMN coupon_template.stackable IS
    'NULL 跟随 app.promotion.coupon.mode；TRUE 与促销叠加；FALSE 与促销互斥（取更优）';
