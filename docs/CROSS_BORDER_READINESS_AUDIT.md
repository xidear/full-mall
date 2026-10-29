# 跨境商城适配性审计（全库扫描）

- 日期：2026-10-29
- 范围：`server/src`（Java + Flyway 迁移）、`web/apps`（admin / storefront / mobile / store-console / supplier-portal / support-console）、`docs/validation.schema.json`
- 方法：按 9 个维度做关键词全量扫描，**每条结论都能定位到文件:行号**；未验证项单独标注，不混进结论。
- 关联：起点是「省市区」问题（[ADR-0013](adr/0013-address-region-data.md)）；市场/语言/收货地的结构性问题见 [ADR-0014](adr/0014-market-locale-destination.md)。

## 0. 结论摘要

| 级别 | 含义 | 条数 |
|---|---|---|
| **P0** | 阻断跨境：开出第二个市场必然出错，或收了钱会算错 | 8 |
| **P1** | 功能缺失：跨境体验断裂，单市场仍可运行 | 10 |
| **P2** | 一致性缺陷 / 需按国配置 | 4 |

根因不是"漏做几项功能"，而是一个模式：**"中国单市场 + 人民币 + 两级地址 + 手机号 + 单一语言"的假设被写进了数据模型和常量，而不是收敛到配置层**。省市区只是这个模式最显眼的一处。

---

## 1. P0：阻断跨境

### P0-1 全栈没有「国家」概念

- 证据：`member_address`（[V42:2-18](../server/src/main/resources/db/migration/V42__member_address.sql#L2-L18)）与 `orders`（[V1:156-172](../server/src/main/resources/db/migration/V1__core.sql#L156-L172)）都没有 `country_code`；对 `server/src/main/java` 检索 `country` **0 命中**。
- 影响：无法回答"这个地址在哪个国家" ⇒ 地址格式、运费、税、报关全部无从生效。
- 要求：地址与订单必须有 `country_code NOT NULL`，下单快照。

### P0-2 订单没有任何税字段；唯一的"税"是中国个税

- 证据：税相关全量检索 35 处，全是 `LaborIncomeTax`（劳务报酬 20%/30%/40% + 速算扣除数，[LaborIncomeTax.java:10-45](../server/src/main/java/com/fullmall/distribution/app/LaborIncomeTax.java#L10-L45)）、违禁词"代开发票"、`PROMOTION_NOT_ACTIVATABLE`。`orders`/`sku` 无 `tax_*` 字段。
- 影响：VAT/GST/销售税/关税无处落账。**ADR-0004 明确承诺"保存税费快照"，未实现。**
- 要求：`order_tax_line`（税种/税率/税基/金额/是否含税价）+ 按市场配置税率规则。

### P0-3 无报关要素：HS 编码 / 原产地 / 重量 全缺

- 证据：`hs_code`/`origin_country`/`weight`/`net_weight`/`gross_weight` 检索**仅 1 处误命中**（SVG 字体属性）；`sku` 表（[V1:42-56](../server/src/main/resources/db/migration/V1__core.sql#L42-L56)）只有 `price_amount/price_currency/options_json/barcode`。
- 影响：出不了商业发票与报关单；也无法按重量计运费。
- 要求：SKU 补 `hs_code`、`origin_country`、`net_weight_grams`、`gross_weight_grams`、`dimensions`；订单行快照。

### P0-4 金额小数位写死 2 位 —— 日元/韩元差 100 倍

- 证据：`Money.toYuan()`/`parseYuan()` 用 `movePointLeft(2)`/`movePointRight(2)`（[Money.java:63-76](../server/src/main/java/com/fullmall/platform/money/Money.java#L63-L76)）；前端 `formatMoney` 同样 `amount / 100`（[utils/money.ts:13](../web/apps/storefront/utils/money.ts#L13)）；另有 **93 处** `¥`/`元`/`'zh-CN'` 硬编码散落于 `web/apps`（admin 最多，其次 store-console / storefront）。
- 影响：JPY/KRW 是 0 位小数、KWD/BHD/TND 是 3 位。按 2 位换算会把 1000 日元当 10 日元。**ADR-0004 承诺"每个市场配置舍入规则和最小价格单位"，未实现。**
- 要求：小数位从币种推导（`Currency.getDefaultFractionDigits()` / `Intl.NumberFormat().resolvedOptions()`），清除所有 `/100` 字面量；"元/分"命名改为"最小货币单位"。

### P0-5 无汇率：ADR-0004 承诺的 ExchangeRateProvider 不存在

- 证据：`ExchangeRateProvider`/`exchangeRate`/`fx_rate` **0 命中**；[SupplierSettlementRepository.java:31](../server/src/main/java/com/fullmall/supplier/infra/SupplierSettlementRepository.java#L31) 注释写明「多币种会返回多行，由服务层显式拒绝（**不做汇率换算的臆造**）」——拒绝是对的，但等于承认没有换算能力。
- 要求：汇率表 + 下单快照；跨币种显式记录而非静默换算。

### P0-6 同一商品无法按市场定价（无价格表）

- 证据：`sku` 只有单一 `price_amount + price_currency`（[V1:47-50](../server/src/main/resources/db/migration/V1__core.sql#L47-L50)）；`price_list`/`market_price`/`region_price` **0 命中**；`OrderService.java:184-191` 取首个 SKU 币种，混币种直接拒绝。
- 影响：CN 卖 CNY、HK 卖 HKD 需要两套价；现在只能"一个 SKU 一个价一个币种"。
- 要求：`sku_price(sku_id, market_code, currency, amount, starts_at, ends_at)`，下单按市场取价并快照。

### P0-7 订单不记录所属市场

- 证据：`orders` 的所有 ALTER（V17/V35/V45/V54/V59/V62）中只有 `ADD COLUMN locale`；`market_code` 只出现在 `warehouse`(V1:60)、`store`(V1:69)、`legal_document`(V1:279)。
- 要求：`orders.market_code NOT NULL`（下单快照），历史回填 `CN`。

### P0-8 市场 / 语言 / 收货地三轴被合并（结构性问题，优先修）

- 证据：`market(code, currency, default_locale)` 把**币种与语言 1:1 硬绑**且无 `market_locale` 关联表（[V1:1-8](../server/src/main/resources/db/migration/V1__core.sql#L1-L8)）；`integration_config` 主键 `(capability, provider)` + `UNIQUE (capability) WHERE enabled` ⇒ **全站只能一个启用支付**，香港站与大陆站无法各有各的（[V91:2-15](../server/src/main/resources/db/migration/V91__integration_config.sql#L2-L15)）；`system_setting` 全局 KV 无市场维度，而 `legal_document` 却有 `market_code` —— 两套口径并存。
- 影响：这是 P0-1/P0-7 以及 P1-6/P1-8/P2-3 的**共同上游**。不先分离三轴，其余修复都会返工。
- 要求：见 [ADR-0014](adr/0014-market-locale-destination.md)：Market（币种/主体/通道/合规）与 Locale（语言/格式）与 Destination（运费/税/报关/地址格式）分别建模，且 **market 不能由 country 推导**（大陆与香港即反例）。

---

## 2. P1：功能缺失

| # | 问题 | 证据 | 跨境要求 |
|---|---|---|---|
| P1-1 | 运费恒为 0，无区域/重量计费 | `FLAT_SHIPPING_FEE = 0L`（[OrderService.java:67](../server/src/main/java/com/fullmall/order/app/OrderService.java#L67)），非自提即取该常量（:234-236） | 运费模板：按 Market × Destination × 重量/件数阶梯 |
| P1-2 | 支付只有微信/支付宝/银联/聚合 | `CHANNELS`（[PaymentConfigService.java:30](../server/src/main/java/com/fullmall/payment/app/PaymentConfigService.java#L30)） | 国际卡（Stripe/PayPal）+ 本地支付方式 |
| P1-3 | 物流只有快递100（国内聚合） | `Kuaidi100Client`:24、`LogisticsTrackingService:92` `case "kuaidi100"` | 国际承运商、面单、清关状态、多段轨迹 |
| P1-4 | 无发票能力 | `invoice_type`/`invoice_title`/`开票`/`taxNo` **0 命中** | 增值税发票 / commercial invoice + packing list |
| P1-5 | 登录只有手机 OTP | `MemberAuthService`:20；无 email/password/social | 邮箱登录与验证（很多国家短信不可达或成本高） |
| P1-6 | 语言白名单只有 zh-CN/en，别处却在用 zh-TW/ja | `SUPPORTED = Set.of("zh-CN","en")`、`FALLBACK="zh-CN"`（[UiTranslationService.java:27-28](../server/src/main/java/com/fullmall/i18n/UiTranslationService.java#L27-L28)）；但 `enabled_locales` 种子是 `en,ja`（V123:46）、`seed.sql` 有 `zh-TW` | 设置里能选的语言必须真生效，否则选了 ja 得到中文（静默降级） |
| P1-7 | SQL 写死 `locale='zh-CN'` | [BrowseHistoryService.java:59](../server/src/main/java/com/fullmall/history/app/BrowseHistoryService.java#L59)、[FavoriteService.java:64](../server/src/main/java/com/fullmall/favorite/app/FavoriteService.java#L64)、[PosRepository.java:806](../server/src/main/java/com/fullmall/pos/infra/PosRepository.java#L806) | 多语言下商品名固定显示中文 |
| P1-8 | `system_setting` 全局 KV | [V7:1-5](../server/src/main/resources/db/migration/V7__system_setting.sql#L1-L5) | ICP/营业执照/客服电话/默认币种只能一套 ⇒ 香港站显示"京ICP备" |
| P1-9 | 审核词库中文内置 | `DEFAULT_BLOCKED`（[ReviewModerationPolicy.java:26-27](../server/src/main/java/com/fullmall/review/app/ReviewModerationPolicy.java#L26-L27)）；适配器为通用 HTTP | 按 locale/国家配置词库与供应商 |
| P1-10 | 短信只接阿里云；EMAIL 通道复用短信 provider 配置 | [NotificationChannelConfig.java:36-54](../server/src/main/java/com/fullmall/notification/config/NotificationChannelConfig.java#L36-L54)：`emailSender` 也读 `app.sms.provider` | 国际短信与邮件服务商应独立配置 |

---

## 3. P2：一致性缺陷

| # | 问题 | 证据 | 说明 |
|---|---|---|---|
| P2-1 | 时区来源不一致 | `Ids.java:21` 用可配 `zone`，但 [ScheduledTaskService.java:138](../server/src/main/java/com/fullmall/platform/schedule/ScheduledTaskService.java#L138) 用 `ZoneId.systemDefault()` | 服务器在 UTC、市场在 UTC+8 时定时任务会在错误时区跑 |
| P2-2 | 地址/邮编未纳入单一来源 | [validation.schema.json](../docs/validation.schema.json) 只有 `phone` 与 `contactName` | 违反 §9.4；解决见 ADR-0013 §8.1 `address_format.zip` |
| P2-3 | 市场写死白名单 | `MARKET_VALUES`（[SystemSettingService.java:92](../server/src/main/java/com/fullmall/setting/app/SystemSettingService.java#L92)） | 加国家要改代码发版 |
| P2-4 | 履约/物流无清关状态 | 状态机无 CUSTOMS/CLEARED | 跨境"清关中/已清关"无法表达 |

---

## 4. 已经做对的部分（不要回退）

| 项 | 证据 |
|---|---|
| 金额用整数最小单位、禁 double、精确分摊 | [Money.java](../server/src/main/java/com/fullmall/platform/money/Money.java)（`mulDiv` 用 BigDecimal HALF_UP） |
| 币种强制 ISO-4217 校验 | `Money` 构造器 `Currency.getInstance()`（:19-23） |
| 多币种订单**显式拒绝**而非臆造汇率 | `OrderService.java:187-189`；`SupplierSettlementRepository.java:31` |
| 日期显示按 `currentLocale()` + `timeZone` | [utils/datetime.ts:22,31](../web/apps/storefront/utils/datetime.ts#L22) |
| 法律文档按 market + locale 版本化，缺失时**明示回退** | `legal_document`(V1:279)；storefront `pages/legal/[type].vue` |
| 仓库/门店挂 `market_code` | `warehouse`(V1:60)、`store`(V1:69) |
| 手机号支持国际格式（不是写死 11 位） | `validation.schema.json` `phone`: 6–20 位可选 `+` 前缀 |
| PII 掩码集中 | `FieldMasker` |

---

## 5. 根因模式

1. **常量承载业务规则**：`FLAT_SHIPPING_FEE=0`、`MARKET_VALUES`、`SUPPORTED={"zh-CN","en"}`、`DEFAULT_BLOCKED`、`/100`。跨第一步就是把这些常量变成按市场/国家/币种查询的数据。
2. **全局单一值占据按市场分列的位置**：`system_setting` 无市场列而 `legal_document` 有 —— 不是设计选择，是遗漏。
3. **ADR 承诺未落地**：ADR-0004 的三项（税费快照/汇率 provider/每市场舍入）全空，但状态是 Accepted。**建议给 ADR 增加"实现状态"字段**，否则文档会持续承诺不存在的能力。
4. **测试用中国数据自证**：e2e 地址全是"上海市/静安区"，没有一个非中国用例 ⇒ 上述问题不会被测试发现。加一组日本/德国/US 地址用例是最便宜的防线。

---

## 6. 建议修复顺序（按依赖，不按编号）

| 批次 | 内容 | 依赖 | 验收 |
|---|---|---|---|
| **A** | 三轴分离（ADR-0014）：market 扩表 + market_locale/market_setting/market_integration + `orders.market_code` + `member_address.country_code` | 无 | 后台按市场配 ICP/币种/客服/支付能生效；大陆与香港各配一套 |
| **B** | 金额正确性：小数位按币种、清 `/100`、`¥`/`元` 改 locale 驱动、汇率表 + 快照 | A | JPY/KWD 单测；93 处硬编码清零 |
| **C** | 地址表单国别驱动（ADR-0013 §8）：address_format + region + 三端动态表单 | A | 日本/德国/US 三种形状 e2e |
| **D** | 计价：价格表按市场、运费模板、SKU 补 HS/原产地/重量 | A、B | 同 SKU 双市场双价；重量阶梯运费 |
| **E** | 税与报关：税率规则、`order_tax_line`、商业发票/报关单导出 | D | 含税/不含税两套；报关字段齐全 |
| **F** | 通道：国际支付、国际物流与清关态、邮箱登录、SMS/邮件解耦 | A | 至少一条非中国支付 + 一条非中国物流端到端 |

> 顺序理由：A 是地基（没有三轴，后面每批都要返工）；B 不解决会**算错钱**，优先级高于功能；E/F 涉及外部资质与合同，周期最长，可并行准备。

---

## 7. 验证状态

| 项 | 状态 |
|---|---|
| 9 维度关键词全量扫描 + 关键文件阅读 | **已做并验证**（每条附文件:行号） |
| 计数类结论（前端 ¥/元/zh-CN = 93；税相关 35 处；HS/重量 1 处误命中） | **已做并验证**（grep 实测） |
| 大陆/香港在各轴的具体差异（关税区/税制） | **部分验证**：币种、无邮编、地址层级有据；关税区与税制细节需法务/税务确认 |
| `integration_config` 是否已有其他按市场的变体 | **已做并验证**：无 market 维度 |
| 各国地址格式（require/fmt/zip/层级/sub_keys） | **已做并验证**：2026-10-29 经代理直连 Google Address Data Service 实测 23 个 key，数据表见 ADR-0013 §8；同时纠正了先前 3 处凭常识的误判（SG 邮编 6 位非 4 位、HK 有 3 个 area 而非「无行政区」、MO 只需一条地址行） |
| 任何代码/迁移实现 | **已实施批次 A + C**，见 [ADR-0013](adr/0013-address-region-data.md#L211) 与 [ADR-0014](adr/0014-market-locale-destination.md#L125) 的实现状态表 |

## 8. 建成但没接的"死表"（自查发现，已修）

实现过程中自查发现一个典型问题：`V142` 建了 `market_destination` 与 `market_integration`，
但**没有任何代码读它们**——"表建好了"和"能力真的生效了"是两件事。

| 表 | 原状态 | 现在 |
|---|---|---|
| `market_destination`（市场 × 目的地可达性） | **死表**：建了没读 | **已接入**：`MarketCapabilityService` 下单硬门禁（无行时**只认本国**） |
| `market_integration`（按市场的通道开关） | **死表**：建了没读 | **已接入**：`MarketIntegrationService`（启用唯一性 + 同能力互斥）→ 能力视图 + 物流服务商解析（有启用项优先，无则回退全局设置） |
| `market_setting`（市场级设置） | 只有后台 API 读写 | **已接入**：`SystemSettingService.publicSettings(market)` / `marketValue(market,key)`，`/api/v1/site/settings` 按 `X-Market` 返回；香港站不再显示大陆 ICP |

> 教训：建表时就要同时写出"谁读它"，否则下次审计还会把它当成已完成。

## 9. 全量测试的已知失败（**均非本批改动引入**，2026-10-29 复核）

| 失败类 | 现象 | 归因 | 复核方式 |
|---|---|---|---|
| `ai.media.AiProviderPayloadTest` / `AiMediaPipelineTest` / `AiMultiReferenceTest` / `AiStoryboardTest`（批次 D/E/F 全量跑时共 8 个 error） | `IllegalStateException: embedding 请求失败` ← `ConnectException`，目标为本地 Ollama embedding（`nomic-embed-text`） | **环境依赖**：本机没有可用的 embedding 服务 | 单独跑同样失败，与市场/地址/税/计价改动无关 |
| `ModulithStructureTest` | `Module 'asset' depends on non-exposed type com.fullmall.ai.media.AiMediaService$AssetGenerated` / `ai.clip.ClipEmbeddingClient` | **既有未提交的 AI 工作**（`ai/clip/` 在 git 中仍是 untracked） | 本批新增的 `region`/`market` 模块违规已修（`region.domain` 加 `@NamedInterface("region-model")`），当前不再出现在违规列表里 |
| `aftersale.ExchangeShippingTest`（全量跑 7 error） | `NoSuchBeanDefinitionException: AfterSaleService` | **全量跑的资源竞争抖动**（多个嵌入式 Postgres 并发） | 单独跑 **7/7 通过** |

> 本批自己新增/改动相关的 9 个测试类（`RegionHttpTest`、`AddressServiceTest`、`CartAndAddressHttpTest`、`ValidationRulesContractTest`、`SystemSettingServiceTest`、`CheckoutRealPersonTest`、`CouponCheckoutTest`、`PickLineMappingTest`、`OrderNotificationFlowTest`）**53 项全绿**；Playwright **5/5 通过**。
