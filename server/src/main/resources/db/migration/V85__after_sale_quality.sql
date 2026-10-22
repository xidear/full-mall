-- 退货入库质检：隔离批次需人工判定合格/不合格后才能处置
ALTER TABLE after_sale ADD COLUMN quality_result VARCHAR(16);
ALTER TABLE after_sale ADD COLUMN quality_note VARCHAR(500);
ALTER TABLE after_sale ADD COLUMN quality_checked_at TIMESTAMPTZ;
ALTER TABLE after_sale ADD CONSTRAINT after_sale_quality_check
    CHECK (quality_result IS NULL OR quality_result IN ('PASS', 'FAIL'));
