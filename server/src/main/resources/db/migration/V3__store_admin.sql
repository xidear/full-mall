-- 门店管理（admin）需要的两个字段：
--   code       对外/对账用的稳定编码（自增 id 不适合人工抄写）
--   updated_at 列表展示"最后修改"，便于运营确认改动是否生效
-- 旧库升级路径：本迁移可重复执行（IF NOT EXISTS + backfill 仅补空值），
-- 但 V1 baseline 已经重建过库的环境仍需按部署文档执行一次 flyway migrate。
ALTER TABLE store ADD COLUMN IF NOT EXISTS code varchar(64);
ALTER TABLE store ADD COLUMN IF NOT EXISTS updated_at timestamp with time zone NOT NULL DEFAULT now();

-- 已有门店按 id 生成稳定编码；只补空值，不覆盖运营手工设置的编码。
UPDATE store
   SET code = 'STORE-' || upper(substr(replace(id::text, '-', ''), 1, 8))
 WHERE code IS NULL OR btrim(code) = '';

ALTER TABLE store ALTER COLUMN code SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'store_code_key') THEN
        ALTER TABLE store ADD CONSTRAINT store_code_key UNIQUE (code);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'store_code_format_check') THEN
        ALTER TABLE store ADD CONSTRAINT store_code_format_check
            CHECK (code ~ '^[A-Z0-9][A-Z0-9_-]{1,63}$');
    END IF;
END $$;