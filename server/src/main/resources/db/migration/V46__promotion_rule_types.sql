-- 促销引擎扩展：支持满减 / 满折（规则参数仍放 rules JSONB，不改表结构）
ALTER TABLE promotion DROP CONSTRAINT promotion_type_check;
ALTER TABLE promotion ADD CONSTRAINT promotion_type_check
    CHECK (type IN ('FLASH_SALE', 'BUY_GIFT', 'GROUP_BUY', 'FULL_REDUCTION', 'FULL_DISCOUNT'));
