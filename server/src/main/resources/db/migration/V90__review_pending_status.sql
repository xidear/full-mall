-- 评价审核：需要 PENDING（自动审核未通过/未配置供应商时待人工）
ALTER TABLE product_review DROP CONSTRAINT product_review_status_check;
ALTER TABLE product_review ADD CONSTRAINT product_review_status_check
    CHECK (status IN ('PUBLISHED', 'HIDDEN', 'PENDING'));
