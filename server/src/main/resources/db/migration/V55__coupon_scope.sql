-- 优惠券适用范围：与促销规则一致（ALL / SKU / CATEGORY / BRAND）
ALTER TABLE coupon_template
    ADD COLUMN scope VARCHAR(16) NOT NULL DEFAULT 'ALL',
    ADD COLUMN scope_ids JSONB NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE coupon_template ADD CONSTRAINT coupon_template_scope_check
    CHECK (scope IN ('ALL', 'SKU', 'CATEGORY', 'BRAND'));

-- 非全场必须至少一个范围 ID
ALTER TABLE coupon_template ADD CONSTRAINT coupon_template_scope_ids_check
    CHECK (scope = 'ALL' OR jsonb_array_length(scope_ids) > 0);
