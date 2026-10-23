-- coupon_template.scope_ids（JSONB 数组）改为关联表：可加外键、可索引、可校验
CREATE TABLE IF NOT EXISTS coupon_template_scope (
    template_id UUID NOT NULL REFERENCES coupon_template(id) ON DELETE CASCADE,
    scope_type  VARCHAR(16) NOT NULL,
    scope_id    UUID NOT NULL,
    PRIMARY KEY (template_id, scope_type, scope_id),
    CONSTRAINT coupon_template_scope_type_check
        CHECK (scope_type IN ('SKU', 'CATEGORY', 'BRAND'))
);

CREATE INDEX IF NOT EXISTS coupon_template_scope_target_idx
    ON coupon_template_scope (scope_type, scope_id);

-- 回填既有数据
INSERT INTO coupon_template_scope (template_id, scope_type, scope_id)
SELECT t.id, t.scope, ids.value::uuid
  FROM coupon_template t
  CROSS JOIN LATERAL jsonb_array_elements_text(t.scope_ids) AS ids(value)
 WHERE t.scope IN ('SKU', 'CATEGORY', 'BRAND')
   AND ids.value ~ '^[0-9a-fA-F-]{36}$'
ON CONFLICT DO NOTHING;

-- 写入时由触发器维护关联表：scope_ids 仍是写入入口，关联表是读取真相
CREATE OR REPLACE FUNCTION coupon_template_sync_scope() RETURNS trigger AS $$
BEGIN
    DELETE FROM coupon_template_scope WHERE template_id = NEW.id;
    IF NEW.scope IN ('SKU', 'CATEGORY', 'BRAND') THEN
        INSERT INTO coupon_template_scope (template_id, scope_type, scope_id)
        SELECT NEW.id, NEW.scope, ids.value::uuid
          FROM jsonb_array_elements_text(NEW.scope_ids) AS ids(value)
         WHERE ids.value ~ '^[0-9a-fA-F-]{36}$'
        ON CONFLICT DO NOTHING;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS coupon_template_sync_scope_trg ON coupon_template;
CREATE TRIGGER coupon_template_sync_scope_trg
    AFTER INSERT OR UPDATE OF scope, scope_ids ON coupon_template
    FOR EACH ROW EXECUTE FUNCTION coupon_template_sync_scope();
