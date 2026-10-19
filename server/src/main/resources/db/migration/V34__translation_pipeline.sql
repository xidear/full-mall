-- 翻译流水线（I18N-003/004）：状态机 + 翻译记忆 + 术语表
ALTER TABLE product_translation
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED',
    ADD COLUMN source_hash VARCHAR(64),
    ADD COLUMN reviewed_by VARCHAR(128),
    ADD COLUMN reviewed_at TIMESTAMPTZ,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE product_translation
    ADD CONSTRAINT product_translation_status_check
    CHECK (status IN ('DRAFT', 'MACHINE', 'REVIEWED', 'PUBLISHED', 'OUTDATED'));

-- 翻译记忆：同一源文 + 语言 + 术语表版本只计费一次
CREATE TABLE translation_memory (
    source_hash      VARCHAR(64)  NOT NULL,
    locale           VARCHAR(16)  NOT NULL,
    glossary_version INT          NOT NULL,
    translated_text  TEXT         NOT NULL,
    provider         VARCHAR(32)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (source_hash, locale, glossary_version)
);

-- 术语表：品牌词/品类词/禁译词，优先于机翻
CREATE TABLE translation_glossary (
    id         UUID PRIMARY KEY,
    locale     VARCHAR(16)  NOT NULL,
    source_term VARCHAR(200) NOT NULL,
    target_term VARCHAR(200) NOT NULL,
    version    INT          NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (locale, source_term)
);