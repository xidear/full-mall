-- 1) 支付配置版本：渠道专属字段收敛为 settings JSONB
--    版本表尚无业务代码读取，可安全收敛；payment 表保留 channel/mode/sp_mch_id/sub_mch_id 用于路由与对账。
ALTER TABLE payment_provider_config_version ADD COLUMN settings JSONB;

UPDATE payment_provider_config_version SET settings = jsonb_strip_nulls(jsonb_build_object(
    'spName', sp_name,
    'spMchId', sp_mch_id,
    'spAppId', sp_app_id,
    'subMchId', sub_mch_id,
    'subAppId', sub_app_id,
    'mchId', mch_id,
    'appId', app_id,
    'callbackUrl', callback_url
)) WHERE settings IS NULL;

ALTER TABLE payment_provider_config_version
    DROP COLUMN sp_name,
    DROP COLUMN sp_mch_id,
    DROP COLUMN sp_app_id,
    DROP COLUMN sub_mch_id,
    DROP COLUMN sub_app_id,
    DROP COLUMN mch_id,
    DROP COLUMN app_id,
    DROP COLUMN callback_url;

COMMENT ON COLUMN payment_provider_config_version.settings IS
    '渠道配置（服务商/商户号/AppID/回调等在 JSON 中；新增渠道参数不改表）';

-- 2) 文章系统：公告 / 帮助文档 / 单页（含多语言）
CREATE TABLE article_category (
    id         UUID PRIMARY KEY,
    code       VARCHAR(64) NOT NULL UNIQUE,
    name       VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE article (
    id           UUID PRIMARY KEY,
    category_id  UUID REFERENCES article_category(id),
    slug         VARCHAR(128) NOT NULL UNIQUE,
    type         VARCHAR(24) NOT NULL,           -- ANNOUNCEMENT / HELP / PAGE
    status       VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT article_type_check CHECK (type IN ('ANNOUNCEMENT', 'HELP', 'PAGE')),
    CONSTRAINT article_status_check CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE INDEX article_published_idx ON article (type, status, published_at DESC);

CREATE TABLE article_translation (
    article_id      UUID NOT NULL REFERENCES article(id),
    locale          VARCHAR(16) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    summary         VARCHAR(512),
    content         TEXT NOT NULL,
    seo_title       VARCHAR(255),
    seo_description VARCHAR(512),
    status          VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (article_id, locale),
    CONSTRAINT article_translation_status_check
        CHECK (status IN ('DRAFT', 'MACHINE', 'REVIEWED', 'PUBLISHED', 'OUTDATED'))
);

CREATE INDEX article_translation_locale_idx ON article_translation (locale, status);
