-- 批次 A（ADR-0014）：市场/语言/收货地三轴分离
--
-- 为什么必须分开：大陆与香港同属"中国"，但关税区/币种/法律主体/支付体系/地址体系全不同，
-- 所以 market 不能由 country 推导；而"界面语言"是用户偏好，"收货地"是每单地址，
-- 三者一旦合并（现状 market(code,currency,default_locale) 1:1 绑死），加第二个市场必然返工。

-- 1) market 扩表：市场要能表达"谁在卖、按哪个税辖区、含不含税、什么时区"
ALTER TABLE market
    ADD COLUMN IF NOT EXISTS country_code      CHAR(2),
    ADD COLUMN IF NOT EXISTS tax_jurisdiction  VARCHAR(32),
    ADD COLUMN IF NOT EXISTS price_includes_tax BOOLEAN,
    ADD COLUMN IF NOT EXISTS timezone          VARCHAR(64),
    ADD COLUMN IF NOT EXISTS legal_entity_name VARCHAR(128),
    ADD COLUMN IF NOT EXISTS sort              INT NOT NULL DEFAULT 0;

-- 2) 语言改为"一个市场多语言"：market_locale 取代 market.default_locale 的 1:1 绑定
CREATE TABLE IF NOT EXISTS market_locale (
    market_code VARCHAR(8)  NOT NULL REFERENCES market (code) ON DELETE CASCADE,
    locale      VARCHAR(16) NOT NULL,
    is_primary  BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (market_code, locale)
);

-- 每个市场最多一个主语言（部分唯一索引兜底，避免"两个主语言"的不确定行为）
CREATE UNIQUE INDEX IF NOT EXISTS market_locale_primary_idx
    ON market_locale (market_code) WHERE is_primary;

-- 先把历史数据搬过来，再删旧列：顺序反了会丢数据
INSERT INTO market_locale (market_code, locale, is_primary)
SELECT code, default_locale, TRUE FROM market
ON CONFLICT (market_code, locale) DO NOTHING;

ALTER TABLE market DROP COLUMN IF EXISTS default_locale;

-- 港澳台市场补上常见语言（港/澳用繁体中文与英文，台湾用繁体中文）。
-- 必须 JOIN market：全新库在这个时点还没有市场行（种子在迁移之后才跑），
-- 直接 VALUES 插入会撞 market_locale 的外键。
INSERT INTO market_locale (market_code, locale, is_primary)
SELECT m.code, v.locale, v.is_primary
  FROM market m
  JOIN (VALUES ('HK', 'zh-HK', FALSE),
               ('HK', 'en',    FALSE),
               ('MO', 'zh-HK', FALSE),
               ('TW', 'zh-TW', FALSE)) AS v(code, locale, is_primary)
    ON m.code = v.code
ON CONFLICT (market_code, locale) DO NOTHING;

-- 2.1) 保证「站点默认市场」一定有对应的市场行。
-- 不在这里编造时区/含税口径（那属于运营决策）：timezone / price_includes_tax 保持 NULL 表示「未配置」，
-- 由 MarketService 在后台保存该市场时强制补齐；缺行会让结算在 MarketContext 处直接失败，所以必须存在。
INSERT INTO market (code, name, currency, country_code, tax_jurisdiction, active, sort)
SELECT s.setting_value,
       s.setting_value,
       COALESCE((SELECT setting_value FROM system_setting WHERE setting_key = 'default_currency'), 'CNY'),
       CASE WHEN s.setting_value ~ '^[A-Z]{2}$' THEN s.setting_value END,
       s.setting_value,
       TRUE,
       0
  FROM system_setting s
 WHERE s.setting_key = 'default_market'
   AND s.setting_value <> ''
   AND NOT EXISTS (SELECT 1 FROM market m WHERE m.code = s.setting_value)
ON CONFLICT (code) DO NOTHING;

-- 3) 站点设置按市场分列：ICP/营业执照/客服电话/默认币种只能一套是缺陷（香港站会显示京ICP备）
CREATE TABLE IF NOT EXISTS market_setting (
    market_code   VARCHAR(8)  NOT NULL REFERENCES market (code) ON DELETE CASCADE,
    setting_key   VARCHAR(64) NOT NULL,
    setting_value TEXT        NOT NULL DEFAULT '',
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (market_code, setting_key)
);

-- 把现有"其实是市场级"的全局设置搬到默认市场名下；全局表保留纯站点级键（site_status、site_domain 等）
INSERT INTO market_setting (market_code, setting_key, setting_value)
SELECT m.code, s.setting_key, s.setting_value
  FROM system_setting s
  JOIN market m
    ON m.code = COALESCE((SELECT setting_value FROM system_setting WHERE setting_key = 'default_market'), 'CN')
 WHERE s.setting_key IN (
        'company_name', 'icp_number', 'public_security_record', 'business_license_no',
        'service_phone', 'service_email', 'default_currency', 'enabled_locales',
        'sms_provider', 'payment_provider', 'logistics_provider', 'storage_provider'
       )
ON CONFLICT (market_code, setting_key) DO NOTHING;

-- 4) 第三方通道按市场分列：原来 UNIQUE(capability) WHERE enabled 意味着全站只能一个启用支付，
--    香港站与大陆站无法各有各的配置
CREATE TABLE IF NOT EXISTS market_integration (
    market_code VARCHAR(8)  NOT NULL REFERENCES market (code) ON DELETE CASCADE,
    capability  VARCHAR(32) NOT NULL,
    provider    VARCHAR(32) NOT NULL,
    enabled     BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (market_code, capability, provider)
);

-- 同市场同能力只允许一个启用（比原全局唯一约束更合理：市场内唯一，市场间可不同）
CREATE UNIQUE INDEX IF NOT EXISTS market_integration_one_enabled
    ON market_integration (market_code, capability) WHERE enabled;

INSERT INTO market_integration (market_code, capability, provider, enabled)
SELECT m.code, i.capability, i.provider, i.enabled
  FROM integration_config i
  JOIN market m
    ON m.code = COALESCE((SELECT setting_value FROM system_setting WHERE setting_key = 'default_market'), 'CN')
ON CONFLICT (market_code, capability, provider) DO UPDATE SET enabled = EXCLUDED.enabled;

-- 5) 配送可达性（市场 × 目的地）：运费/可达性由两轴共同决定
CREATE TABLE IF NOT EXISTS market_destination (
    market_code         VARCHAR(8) NOT NULL REFERENCES market (code) ON DELETE CASCADE,
    destination_country CHAR(2)    NOT NULL,
    deliverable         BOOLEAN    NOT NULL DEFAULT TRUE,
    PRIMARY KEY (market_code, destination_country)
);

INSERT INTO market_destination (market_code, destination_country, deliverable)
SELECT code, code, TRUE FROM market
ON CONFLICT DO NOTHING;

-- 6) 订单记录所属市场：否则无法按市场做报表/税务归档（原来只有 locale）
ALTER TABLE orders ADD COLUMN IF NOT EXISTS market_code VARCHAR(8) REFERENCES market (code);
-- 回填假设：历史订单都产生于单市场时期（default_market 当时只有 CN 生效）。
-- 这是一次性迁移假设，不是运行时兜底；新订单必须由结算流程显式写入。
UPDATE orders SET market_code = 'CN' WHERE market_code IS NULL;
CREATE INDEX IF NOT EXISTS orders_market_created_idx ON orders (market_code, created_at);

-- 7) 收货地址记录国家：没有国家，地址格式/运费/税/报关全部无从生效
ALTER TABLE member_address ADD COLUMN IF NOT EXISTS country_code CHAR(2);
-- 同上：历史地址都录于单市场时期，回填 CN；新写入由 AddressService 校验必填（见 address_format）。
UPDATE member_address SET country_code = 'CN' WHERE country_code IS NULL;

COMMENT ON COLUMN market.tax_jurisdiction IS '税务辖区代码，可≠country_code（如 EU-OSS 跨国申报）；税率表按此挂载';
COMMENT ON COLUMN market.price_includes_tax IS 'TRUE=标价含税（大陆习惯）；FALSE=净价另计（美加习惯）';
COMMENT ON COLUMN market_locale.is_primary IS '该市场的默认语言；用户实际界面语言是独立维度（ADR-0014）';
COMMENT ON COLUMN orders.market_code IS '下单时的市场快照；历史行回填 CN 是一次性迁移假设';
COMMENT ON COLUMN member_address.country_code IS '收货地国家（ISO 3166-1）；决定地址格式/运费/税/报关，不是市场';

