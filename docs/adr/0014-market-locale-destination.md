# ADR-0014: 市场（Market）/ 语言（Locale）/ 收货地（Destination）三轴分离

- 状态：Accepted（设计）
- 日期：2026-10-29
- 相关：ADR-0003（中国市场优先）、ADR-0004（金额与汇率）、ADR-0013（区划数据）

## 背景

需求原话：**「跨境用到的东西和多语言是两码事，和用户所在地区也不完全一样，三者不能统一」**。这不是风格问题，是三个正交维度被合并进了一个字段。

同为中国，大陆与香港在**每一个**维度上都不同：关税区（大陆/香港单独关税区）、法律主体、币种（CNY/HKD）、支付体系、物流与清关、地址体系（香港没有邮政编码、没有省级行政区）、税务（大陆增值税 / 香港无 VAT）、合规定价与文案。所以 **market 不能从 country 推导，必须独立实体**。

### 现状：三轴已被合并的证据

| 合并点 | 证据 |
|---|---|
| 市场 = 币种 + **默认语言**，1:1 硬绑；无 `market_locale` 关联表 | `market(code, currency, default_locale)`（[V1__core.sql:1-8](../server/src/main/resources/db/migration/V1__core.sql#L1-L8)）；全仓检索 `market_locale` 0 命中 |
| **全站只能有一个启用的支付配置**，香港站与大陆站无法各有各的 | `integration_config` 主键 `(capability, provider)` + `UNIQUE (capability) WHERE enabled`（[V91:2-15](../server/src/main/resources/db/migration/V91__integration_config.sql#L2-L15)），**无 market 维度** |
| 全站单一"市场"设置 | `system_setting.default_market` 单值 + `MARKET_VALUES = Set.of("CN","HK","MO","TW")` 白名单（SystemSettingService.java:92） |
| 语言是全局白名单，与市场无关 | `SUPPORTED = Set.of("zh-CN","en")`（UiTranslationService.java:27） |
| 订单只有 `locale`，无 `market_code`、无目的地国家 | `orders` 的所有 ALTER 中只有 `ADD COLUMN locale`（V35:57） |
| 又是唯一做对的一处 | `legal_document` 有 `market_code`（V1:279） |

结论：**同一个仓库里 `legal_document` 按市场分列、`system_setting` 不按市场分列**——两套口径并存，说明不是设计选择，是遗漏。

## 决策

### 1. 三轴定义（谁回答什么问题）

| 轴 | 回答的问题 | 由谁决定 | 可变性 | 存哪 |
|---|---|---|---|---|
| **Locale（语言）** | 界面用什么语言、日期/数字/货币怎么格式化 | 用户偏好 | 随时可切，**不影响价格与税率** | cookie + member 资料 |
| **Market（市场 / 开通地区）** | 谁在卖、用什么币种、哪个法律主体、默认税务辖区、可用支付与物流、合规文案、域名入口 | 运营配置 | 由入口（域名/路径）决定，用户**不能随心切** | 配置表 |
| **Destination（收货地）** | 寄到哪、运费多少、目的地税/进口税、能否配送、报关要素、地址字段集 | 每单地址 | **每单可变** | 地址行/订单快照 |

### 2. 决定矩阵（哪个轴驱动什么）—— 实现时按这张表查，不要凭直觉

| 需求 | 驱动轴 | 说明 |
|---|---|---|
| 界面文案、日期/数字格式 | **Locale** | |
| 地址输入框的 label 语言（"都道府县"/"State"） | **Locale** | 同一个国家可有多语言 label |
| 地址字段集、邮编正则、层级深度、是否必填 | **Destination 国家** | **不是 locale**：日本人用英文界面，地址仍是都道府县 |
| 商品价格与币种、价格表 | **Market** | 同 SKU 在 CN/HK 两个价 |
| 结算主体、发票主体、ICP/营业执照展示 | **Market** | |
| 可用支付方式、可用物流承运商 | **Market** | |
| 客服电话/邮箱 | **Market** | |
| **运费** | **Market × Destination** | 大陆站寄香港与寄新疆运费不同 |
| **税（VAT/GST/销售税/进口税）** | **Destination 计税，Market 定申报主体与税率表** | 最容易做错的一点：税额按收货地算（EU OSS/IOSS、US ship-to nexus），但由哪个主体申报、适用哪张税率表由市场决定 |
| 报关要素（HS/原产地/申报价值） | SKU + Destination | |
| 允许的语言候选 | **Market** | `market_locale` = 允许集；用户实际选择 = 实际值 |
| 配送可达性（能否寄到该国） | **Market × Destination** | |

### 3. 数据模型

```text
-- 市场：独立实体，不从 country 推导（HK 与 CN 是不同关税区/币种/法律主体）
market (
  code              VARCHAR(8)  PRIMARY KEY,     -- CN / HK / MO / TW / JP / SG ...
  name              VARCHAR(64) NOT NULL,
  country_code      CHAR(2),                    -- 主要国家；允许 NULL（跨国市场如 EU）
  currency          CHAR(3)     NOT NULL,
  tax_jurisdiction  VARCHAR(16) NOT NULL,       -- 可 ≠ country_code（如 EU-OSS）
  price_includes_tax BOOLEAN    NOT NULL,
  timezone          VARCHAR(64) NOT NULL,       -- 市场本地时区，定时任务/报表用
  legal_entity_name VARCHAR(128),
  active            BOOLEAN     NOT NULL DEFAULT TRUE,
  sort              INT         NOT NULL DEFAULT 0
);

-- 一个市场多种语言（取代 market.default_locale 的 1:1 绑定）
market_locale (
  market_code VARCHAR(8) REFERENCES market(code),
  locale      VARCHAR(16) NOT NULL,
  is_primary  BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (market_code, locale)
);
-- 每市场最多一个 primary
CREATE UNIQUE INDEX market_locale_primary_idx ON market_locale (market_code) WHERE is_primary;

-- 按市场分列的站点设置（全局 system_setting 只保留纯站点级：site_status、site_domain）
market_setting (
  market_code  VARCHAR(8) REFERENCES market(code),
  setting_key  VARCHAR(64) NOT NULL,
  setting_value TEXT NOT NULL DEFAULT '',
  PRIMARY KEY (market_code, setting_key)
);

-- 第三方通道按市场分列（解决"全站只能一个启用支付"）
market_integration (
  market_code VARCHAR(8) REFERENCES market(code),
  capability  VARCHAR(32) NOT NULL,   -- payment / sms / logistics / storage / address / moderation
  provider    VARCHAR(32) NOT NULL,
  enabled     BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (market_code, capability, provider)
);
CREATE UNIQUE INDEX market_integration_one_enabled ON market_integration (market_code, capability) WHERE enabled;

-- 配送可达性（市场 × 目的地）
market_destination (
  market_code        VARCHAR(8) REFERENCES market(code),
  destination_country CHAR(2)   NOT NULL,
  deliverable        BOOLEAN    NOT NULL DEFAULT TRUE,
  PRIMARY KEY (market_code, destination_country)
);
```

### 4. 为什么不能合并（用反例锁死结论）

| 若合并 | 立刻出错的场景 |
|---|---|
| Locale = Market | 香港站必须同时支持 `zh-HK` / `zh-TW` / `en`；1:1 绑定后只能一种。且海外用户切英文界面会把币种一起切掉 |
| Market = Destination | 大陆站直邮新加坡：市场是 CN、收货地是 SG。用 market 算运费/税必错；反过来香港站寄大陆同理 |
| Locale = Destination | 日本用户用英文界面（`locale=en`）；海外华人用中文买美国货（`locale=zh-CN, destination=US`） |
| Country = Market | 中国大陆与香港：不同关税区、不同币种、香港无邮编无省级行政区、无 VAT。按 `country_code` 一刀切会把两者等同 |

### 5. 迁移路径（兼容现有 4 个市场）

| 阶段 | 动作 | 兼容处理 |
|---|---|---|
| 1 | `market` 加字段；建 `market_locale`、`market_setting`、`market_destination` | 现有 4 行按 1:1 回填 `market_locale(CN,zh-CN,primary)` 等 |
| 2 | `integration_config` → `market_integration` | 现有行回填到 `default_market` 对应市场；保留旧表一版兼容读取 |
| 3 | `orders` 加 `market_code`、`member_address` 加 `country_code` | 历史数据回填 `CN`，并在代码注释写明"回填假设" |
| 4 | 价格表按市场（[`sku_price`](0013-address-region-data.md) 相关，见审计 P0-6） | 现有单价回填为该市场默认价 |

## 实现状态（2026-10-29）**已实施并验证**

| 项 | 状态 |
|---|---|
| V142 迁移：market 扩表 + market_locale/market_setting/market_integration/market_destination + orders.market_code + member_address.country_code + 回填 | **已实施**，全新库 141 个迁移重放 + 种子通过 |
| market 模块（MarketService/MarketController/Repository）、`GET/PUT /admin/v1/markets`、`GET /api/v1/markets` | **已实施** |
| 去掉 `MARKET_VALUES` 写死白名单，改为查库校验 `default_market` | **已实施**；单测 13 项通过 |
| `MarketContext` 下单市场快照写入 `orders.market_code`（解析不到明确失败，不默认 CN） | **已实施**；结算相关测试 11 项通过 |
| `market.default_locale` 由 `market_locale.is_primary` 取代（旧列删除） | **已实施** |
| admin「市场配置」页 + 帮助条目 | **已实施**；`check-help-coverage.py` 38 页通过 |
| Playwright 真机测试（后台编辑市场 + 公开接口三轴） | **已通过**（2/2） |
| 价格表按市场、运费模板、税表 | **已实施**（批次 D/E，见下表） |

### 后续批次实现状态（2026-10-29）

| 批次 | 内容 | 状态与证据 |
|---|---|---|
| **D** | `sku_price` 按市场覆盖价（基础价兜底需币种一致）、`shipping_rate` 市场×目的地×重量阶梯、SKU 补 `hs_code`/`origin_country`/`net_weight_grams`/`gross_weight_grams`、`orders.total_weight_grams` 快照 | **已实施**（V147）；`MultiMarketPricingTest` **7/7**；跨币种无覆盖价 → `SKU_PRICE_MISSING`；跨国无运费规则 → `SHIPPING_RULE_MISSING` |
| **E** | `tax_rule`（基点制税率 + 含税/净价口径）、`order_tax_line` 快照、报关单导出 `GET /admin/v1/orders/{id}/customs-invoice` | **已实施**（V148）；`TaxAndCustomsTest` **5/5**；价内税反算、价外税另加、缺 HS/原产地/净重逐行告警 |
| **F1** | 邮箱登录：`PUT /api/v1/member/auth/email` 绑定 + `POST /email/otp` / `/email/verify` | **已实施**（V149）；`EmailLoginAndCustomsTest` 覆盖"未绑定不得登录""绑定后可登录""邮箱不可重复" |
| **F2** | 跨境清关状态：`FulfillmentStatus` 增 `CUSTOMS_CLEARING`/`CUSTOMS_CLEARED` + `POST /admin/v1/order-fulfillments/{id}/customs` | **已实施**；未发货就登记清关会报 `FULFILLMENT_NOT_SHIPPED` |
| **F3** | 国际支付/物流供应商真实接入 | **未做**：`payment_provider_config` 已按市场分列、`market_integration` 已落库；真实通道需要商务/资质，不在代码范围 |
| **G** | **能力门禁**：`MarketCapabilityService`（市场 × 目的地）+ `GET /api/v1/markets/{code}/capabilities`，下单入口前置拦截 | **已实施**；`MarketCapabilityTest` **6/6**；Playwright 断言前台当场提示"暂不支持配送到该国家/地区" |
| **H（收口）** | ①`market_setting` 全量接入（`publicSettings(market)` / `marketValue` + `X-Market`）②`MarketIntegrationService`（启用唯一性 + 同能力互斥）接入能力视图与物流服务商，并补 admin「通道启用」界面 ③邮箱验证码**真实走邮件通道**（`sendToAddress` + 失败即 `EMAIL_SEND_FAILED`） | **已实施**；`MarketScopedConsolidationTest` **5/5**、`EmailLoginAndCustomsTest` **5/5**（含"邮件落库为 SENT"断言）、Playwright **10/10** |
| **I（区划补深）** | `V151`：中国区划从市级补到**区县级**（3117 个 DEPENDENT 节点）；抓取脚本支持 `--cn-depth 3`（并发 8，写临时文件后原子替换） | **已实施**；全新库重放通过；`GET /api/v1/regions?country=CN&parent=CN-03` 返回 16 个区；前端级联到区县（无数据时退文本输入） |

### 能力门禁规则（「这个地区到底能不能做生意」）

模块能力原本散在各处（地址格式在 region、运费在 shipping、税在 tax、支付/物流开关在 market_integration），
每个模块只知道自己那一半，于是会出现"市场配了、运费配了，但目的地根本没开通"这种组合错误。
因此统一到一处按 **市场 × 目的地** 求值：

| 阻断项 | 依据 | 错误码 | 是否硬门禁 |
|---|---|---|---|
| 市场未启用 | `market.active` | `MARKET_INACTIVE` | 是 |
| 缺目的地国家 | 地址 `countryCode` | `DESTINATION_REQUIRED` | 是（配送单） |
| 目的地未开通 | `market_destination.deliverable`；**无行时只认本国**（"没配"≠"全球可达"） | `DESTINATION_NOT_DELIVERABLE` | 是 |
| 地址格式缺失 | `address_format` 是否存在 | `ADDRESS_FORMAT_MISSING` | 是 |
| 运费规则缺失 | `shipping_rate` 是否有档位（或本国默认免运费） | `SHIPPING_RULE_MISSING` | 是 |
| 税率缺失 | `tax_rule` | 无（`taxRule=false` 仅展示） | **否**：打 WARN + 报关单列出 |
| 支付/物流供应商 | `market_integration(capability, enabled)` | 无（仅展示） | **否**：在支付/发货环节处理 |

自提单**不校验目的地**：自提不寄，目的地国家与运费规则都无关。

> 「税率不阻断」是有意的：不少辖区确实没有可计税种或落在低值免税额内，做成硬门禁会让正常订单下不了；
> 缺税率必须**可见**（日志 WARN + 报关单"没有税行"），而不是静默当作 0。

### 实施中修掉的数量级 bug（值得记住）

税率基点分母写成了 100000（应为 10000，1300 bps = 13%），被测试直接抓出（113.00 含税算成税基 111.55）。现在分母是常量 `BPS_SCALE`，DB 上还有 `rate_bps <= 10000` 的 CHECK 兜底。

## 后果

- 所有"按市场"的读取点必须显式传 market（从请求入口解析：域名/路径 → market），**不允许默认 `CN` 兜底**（现状 `ThemeConfigService.normalizeMarket` 就是 `market == null ? "CN"`，属静默兜底）。
- `system_setting` 需要拆分：站点级（域名、状态）留在全局，市场级（ICP、币种、客服、语言）下沉到 `market_setting`。
- 语言回退必须按 `market_locale` 的允许集判断，不能再硬编码 `zh-CN` 兜底。
- 前端仍只做展示格式化，但**格式化用的 locale 与 currency 分别来自不同轴**（前者用户偏好、后者市场）。

## 替代方案

- **继续用 `market` 表代表一切（现状）**：币种/语言/主体/通道全绑在一起，加第二个市场必然返工；否决。
- **用 `country_code` 代替 market**：大陆与香港证明不可行；否决。
- **用 locale 代替 market**（"英文站=海外站"）：海外用户也可以看中文，且香港/台湾同为中文但币种与税制不同；否决。
- **只加一个 `region` 表把三者都塞进去**：等于把现在的耦合换个名字，仍然回答不了"寄到哪"与"用哪种语言"；否决。

## 验证状态

| 项 | 状态 |
|---|---|
| 现状三轴合并的证据（market 1:1 绑 locale、integration_config 无 market、orders 无 market/destination、system_setting 全局、legal_document 有 market） | **已做并验证**（文件:行号见背景表） |
| 三轴定义与决定矩阵 | **设计决策**，待评审 |
| 大陆/香港在各维度的具体差异（关税区、税制、邮编、支付） | **已做部分验证**：币种/无邮编/address 层级有据；**关税区与税制细节需法务/税务确认**，代码不猜 |
| 迁移脚本与回填 | **未做** |
