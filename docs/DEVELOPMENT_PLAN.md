# 源码开放跨境商城 — 详细开发计划 v1.2

## 0. 计划前提

- **市场节奏**：第一阶段中国大陆；第二阶段香港、澳门、台湾地区。多语言基础设施先上，首发 zh-CN，随后 zh-HK/zh-TW，英文后置。
- **币种**：CNY 首发，预留 HKD/MOP/TWD；价格表按市场/币种配置。
- **经营模式**：平台自营 + 供应商代发；支持仓库发货、门店发货、门店自提、供应商直发；订单可按履约源拆单。
- **仓库**：中央仓、区域仓、本地化仓（港澳台）、门店前置仓、供应商虚拟仓；预留保税仓。
- **团队**：4～6 人，32 周；2～3 人按 1.8～2.5 倍估算。
- **技术栈**：Java 21 + Spring Boot 3 + Spring Modulith；PostgreSQL；Valkey；RabbitMQ；OpenSearch；SeaweedFS；Vue 3 + Nuxt 3 + uni-app；Docker Compose + Dev Container。
- **开源版**：全套演示流程可走通；默认 Mock 支付/物流/短信/邮件；真实适配器可配置、可编译，但仓库不提供任何商户号、密钥、证书。
- **演示站**：只读 + GET Mock，禁止 POST/PUT/DELETE；独立编译开关；无外网副作用。
- **企业版**：预留 edition/license/entitlement 和签名更新机制；开源核心不依赖企业模块。
- **参考策略**：不 fork 大商城；clean-room 借鉴 Medusa/Saleor/OFBiz/Chatwoot/Hyperswitch/Documenso 等。
- **v1 不做**：完整 POS（可选加 3～4 周）、marketplace 分账、完整总账、MRP、复杂 B2B。

---

## 1. 业务范围与关键规则

### 1.1 市场与本地化

- **中国大陆**：zh-CN、CNY、微信支付/支付宝/银联（默认 Mock）、增值税发票、国内快递、7 天无理由退货（按法规/品类）。
- **香港/澳门/台湾地区**：zh-HK/zh-TW、HKD/MOP/TWD、当地支付/物流/自提点、当地法律主体与隐私政策；跨境物流、报关/清关、税费展示。
- 多语言先打通中文简繁；英文和其他语言后置。法律文档必须人工审校，不能只靠机器翻译。

### 1.2 履约模式

- **自营仓发货**：拣货、打包、电子面单、交接、轨迹。
- **门店发货**：门店库存拣货、打包、交运。
- **供应商代发**：订单推送/拉取、供应商发货、轨迹回传、超时/拒单/缺货处理。
- **门店自提**：就近门店、库存可见、自提时段、提货码、身份核验、超时释放/退款。
- **订单拆单**：按仓库/门店/供应商/时效/温层/保质期拆成多个履约单；运费按包裹分摊；支持部分发货、部分退款。

### 1.3 库存粒度与保质期

- 默认按 SKU（规格）管理；商品可配置 `inventory_tracking_level = VARIANT | PRODUCT`，部分商品按 SPU 管理，不细分规格。
- 保质期商品原子库存：`商品/SKU + 仓库/门店 + 批次 + 效期`；非保质期商品可只到 SKU + 地点。
- 商品/类目配置 `shelf_life_policy`：是否批次管理、最小剩余保质期、临期阈值、拣货策略 `FEFO/FIFO/NONE`、临期动作（正常/折扣/下架/报损）、自动阻断。
- 拣货：FEFO 类目优先早效期；非保质期类目按 FIFO 或无需批次；系统给出建议批次，扫码校验。
- 收货/调拨/盘点/退货/报损全部支持批次与效期；过期/隔离库存不可售。
- 库存流水不可变；`stock_level` 为投影；预占有 TTL；关键扣减原子更新，防超卖。

### 1.4 门店

- 门店主数据：地址、经纬度、服务半径、营业时间、节假日、自提时段、容量、员工与权限。
- 门店库存：收货、上架、盘点、调拨、临期、报损、自提预占、门店发货。
- 就近自提：按地址/定位 + 库存 + 距离 + 营业时间 + 时段排序；地图选店；无库存时回退快递。
- 自提流程：下单预占 → 拣货 → 待提货通知 → 提货码/身份证核验 → 核销 → 完成；超时自动取消/退款/释放库存。
- v1 门店以“前置仓 + 自提 + 发货”为主；完整 POS/离线收银作为可选模块，若需要另加 3～4 周。

### 1.5 支付与发票

- 开源默认 `MockPaymentAdapter`：下单 → 模拟支付 → 回调 → 订单已支付，全流程可走通。
- 真实适配器：微信支付、支付宝、银联、港澳台本地支付；代码保留 SPI，配置后才启用。
- 仓库不含真实商户号、密钥、证书；客户自行配置并编译。
- Webhook 幂等 inbox、重复/乱序处理、退款/部分退款、对账；发票：增值税电子发票适配器、港澳台票据模板。

---

## 2. 技术架构

```text
PC/移动 H5/小程序/APP   Admin   Store Console   Supplier Portal   Support Console
          │                │          │               │                │
          └──────── HTTPS/OpenAPI ────┴──── WSS ──────┴────────────────┘
                                   │
                          Nginx / CDN / WAF
                                   │
        ┌────────────────────────────────────────────────────┐
        │          Spring Boot 3 + Spring Modulith           │
        │          api / worker / ws 三进程同代码             │
        │ identity catalog pricing cart order payment        │
        │ inventory batch expiry fulfillment store supplier  │
        │ promotion legal support notification analytics     │
        └────────────────────────────────────────────────────┘
             │          │           │            │
        PostgreSQL    Valkey    RabbitMQ     OpenSearch
             │
        SeaweedFS / S3   + 可选 ClickHouse（分析）
```

- 模块边界由 Spring Modulith 校验；模块间领域事件 + 公开接口。
- `api`/`worker`/`ws` 同 jar 不同 profile；Docker 独立扩缩容。
- 金额 `bigint` 最小单位 + 币种；订单/支付/库存保存快照。
- Transactional Outbox；支付 inbox；库存流水；审计日志；业务事件日志。
- API：`/api/v1` 顾客、`/admin/v1` 运营、`/store/v1` 门店、`/supplier/v1` 供应商后台、`/open/v1` 供应商 API、webhooks。
- 认证：顾客 JWT/OTP/微信/支付宝登录；员工 RBAC；门店角色；供应商 API Key/HMAC；客服 WS ticket。
- 可选 ClickHouse 仅跑分析，不进入交易主链路；v1 可先用 Postgres 分区表 + 物化视图。

### 2.5 成熟方案借鉴与 clean-room 策略

**重点参考 6 个项目**：Medusa v2、Saleor、Apache OFBiz、Chatwoot、Hyperswitch、Documenso。其余按需补充：Vendure（插件/渠道）、Shopware/Magento MSI（多店/多源库存）、Odoo/ERPNext（ERP 流程）、Dify/Rasa（AI）、Lilishop/CRMEB/mall4j（国内多端工程经验）。许可证以仓库当前版本为准，Phase 0 做 Legal 复核。

| 层 | 参考项目 | 借鉴 | 不借鉴 |
|---|---|---|---|
| 交易内核 | Medusa v2 | 模块边界、Region/Currency、Price List、库存预占、Payment/Fulfillment Provider SPI | 代码、Admin UI、企业版功能 |
| 多渠道/多仓 | Saleor | Channel、Warehouse、Stock Allocation、Apps/Webhooks | GraphQL schema 原样照搬 |
| Java/ERP 领域 | Apache OFBiz | Order、Inventory、Facility、Shipment、Party、Invoice 的领域词汇和边界 | 老式架构和代码 |
| 客服 | Chatwoot | Inbox、Conversation、Agent、Team、Label、Canned Response、SLA、实时会话模型 | UI、品牌、代码 |
| 支付编排 | Hyperswitch | Connector 抽象、路由/重试、Webhook 归一化、对账、幂等 | v1 不需要全套编排 |
| 电子签名 | Documenso | Envelope、Template、Field、签署顺序、审计轨迹 | 不把 AGPL 代码链接进核心，独立部署或只学流程 |
| 多店/多源库存 | Shopware / Magento MSI | Sales Channel、Store View、Multi-Source Inventory、Tax Zone | 代码和复杂配置体系 |
| ERP | Odoo / ERPNext | 采购单、收货、调拨、盘点、Landed Cost、库存估值 | 不整体引入，只读文档和公开行为 |
| AI 客服 | Dify / Rasa | RAG、工具调用、意图、人工接管、评测集 | 有附加限制的平台不做核心 |
| 国内多端 | Lilishop / CRMEB / mall4j | uni-app 工程组织、微信/支付宝接入、后台交互 | 代码、UI、文案；它们不是跨境参考 |
| 跨境体验 | Shopify Markets（闭源） | Market、币种、关税/税费展示、语言/域名、结账本地化 | 任何代码，只做行为与 UX 参考 |

**Clean-room 规则**：

1. 先写自己的 ADR、ERD、OpenAPI、功能对照表，再读参考项目。
2. GPL/AGPL 项目只读官方文档和公开行为，不读源码、不复制；需要能力时独立部署、API 集成。
3. MIT/BSD 项目可读源码学习，但默认重写；复用代码必须保留版权和许可证声明。
4. 不复制 UI、品牌、文案、图标、数据库 schema 原文；领域概念可借鉴，表达和实现自己写。
5. 建 `docs/references/` 登记：项目、版本、许可证、借鉴点、未复用内容、对应 ADR。
6. CI 做 License/SBOM 扫描；AGPL 服务隔离；代码评审检查来源。
7. 用行为对照测试验收，不做逐行代码对照。

**对模块的映射**：

- 支付：自研 `PaymentConnector`，统一 authorize/capture/refund/webhook/reconcile；借鉴 Hyperswitch，v1 只做 Stripe/PayPal 以及国内 Mock。
- 库存：借鉴 Medusa/Saleor 的 location + reservation + allocation，Magento MSI 的多源库存，OFBiz 的 facility/shipment 词汇。
- 供应商/ERP：借鉴 OFBiz/Odoo/ERPNext 的 PO、收货、调拨、盘点、Landed Cost；只做履约所需简易 ERP。
- 客服：借鉴 Chatwoot 的会话/坐席/团队/快捷回复/SLA；增加订单、物流、商品上下文和多语言翻译。
- AI：借鉴 Dify/Rasa 的 RAG、工具调用、人工接管和评测；Spring AI + pgvector 自研，Dify 只作参考或独立 sidecar。
- 法律文档：借鉴 Documenso 的模板/字段/签署顺序/审计；顾客协议 clickwrap 自研，供应商合同可独立部署 Documenso。
- 多端：参考国内商城的 uni-app 组织方式，UI、业务、跨境逻辑全部自己写。

---

## 3. 多语言顾客协议与文档中心

这不是普通 i18n 文案，而是**有法律效力、可追溯、可版本化的内容系统**。

### 3.1 文档范围

- 顾客端：服务条款、隐私政策、Cookie 政策、退款/退货政策、配送政策、自提政策、支付条款、可接受使用政策、知识产权、保修说明。
- 供应商端：供应商合作协议、代发 SLA、数据处理协议 DPA、佣金/结算条款。
- 交易文档：订单确认、发票、贷项通知、装箱单、商业发票、报关资料。

### 3.2 数据模型

- `legal_document`：文档编码、类型、市场、法律主体。
- `legal_document_version`：版本号、状态（draft/review/published/retired）、生效/失效时间、HTML/Markdown 源、内容哈希、审核人、发布时间。
- `legal_document_localization`：版本 + locale + 标题/正文/哈希 + 翻译状态 + 法律审校人。
- `legal_acceptance`：用户/游客、文档版本、locale、接受时间、IP、User-Agent、方式（clickwrap/e-sign）、证据哈希、订单/会话 ID。
- `legal_consent`：Cookie 分类、营销同意、撤回记录。
- 交易文档：`invoice`、`credit_note`、`packing_list`、`customs_document`，保存不可变快照和 PDF 哈希。

### 3.3 生命周期

1. 运营起草 → 2. 法务审核 → 3. 翻译 → 4. 本地法务/语言审校 → 5. 定时发布 → 6. 旧版本自动 retired。
2. 已发布版本**不可修改**，任何变更生成新版本；订单和接受记录永远指向具体版本。
3. 实质性条款变更触发重新同意；非实质性变更只提示。
4. locale 缺失时按市场 fallback，但法律文档必须人工审校，不能只靠机器翻译。
5. 每个市场绑定法律主体、适用法律、争议解决地、VAT 号、地址和联系信息。

### 3.4 渲染与签署

- HTML/Markdown 模板 → PDF（Typst 或 WeasyPrint），保存源文件、PDF、哈希和渲染时间。
- 顾客协议用 **clickwrap**：勾选 + 版本 + 时间 + IP/UA + 哈希，满足大多数电商场景。
- 供应商合同可接 **Documenso**（AGPL，自托管）做电子签名；独立服务，不链接进核心代码。
- 发票/报关单支持多语言模板、币种、税号、HS Code、原产地、申报价值。
- 提供 PDF/A 选项和 PAdES 签名扩展点。

### 3.5 管理端与 API

- 管理端：文档列表、版本对比、Markdown 编辑、变量占位符、预览、翻译任务、审校、定时发布、接受记录查询。
- 顾客端：法律中心、页脚链接、注册/结账同意、重新同意弹窗、账号内接受历史、PDF 下载。
- API：`GET /api/v1/legal/documents?type=&locale=&market=`、`POST /api/v1/legal/acceptances`、管理端 CRUD/发布、供应商合同签署回调。
- 合规：GDPR 同意记录、DSAR 导出/删除、数据保留策略、Cookie 分类、子处理者清单。

### 3.6 测试

- 每个 locale 的 key/章节完整性检查。
- 链接检查、渲染快照、版本不可变测试、接受证据审计。
- PDF 字体/RTL/日期/金额格式测试。
- 法律文案变更 diff 和发布审批测试。

---

## 4. 客服系统与 AI 机器人演进

### 4.1 v1 人工客服

- WebSocket 网关：短时 ticket 鉴权、会话房间、消息序号、ack、断线重连补拉、已读回执、typing、附件。
- 会话模型：游客/登录用户、订单上下文、商品卡片、订单卡片、系统消息、内部备注、标签、快捷回复、转接、坐席分配、最大并发、SLA。
- 客服工作台：会话列表、顾客信息、订单/物流侧栏、翻译、快捷回复、历史搜索、质检。
- 推送：站内通知、邮件、Web Push、FCM/APNs；微信订阅消息后续接入。离线消息进队列，重连补拉。
- 存储：消息 Postgres，附件 S3，按需分区；敏感字段脱敏；审计日志。

### 4.2 AI 机器人架构（现在留接口，不提前堆功能）

- `BotEngine` 接口：`RuleBasedBot`（FAQ/订单查询）→ `AiBot`（LLM + RAG + tools）。
- 消息流水线：语言识别 → PII 脱敏 → 意图/检索 → 工具调用 → 安全护栏 → 回复 → 置信度/人工兜底。
- 知识库：FAQ、政策、商品资料、售后规则；pgvector 向量检索；回答带引用；知识按 locale/市场/版本隔离。
- 工具：订单查询、物流轨迹、退款资格、商品问答、账户信息；严格按用户权限授权，所有调用留审计。
- 模型抽象：OpenAI-compatible、Anthropic、本地 Ollama/vLLM；超时/限流/降级/成本预算；默认不拿顾客数据训练。
- 安全：Prompt Injection 防护、输出审核、PII 脱敏、敏感操作二次确认、人工升级、对话留存策略。
- 坐席辅助：建议回复、实时翻译、会话摘要、情绪识别。
- 评估：离线评测集、引用准确率、幻觉率、人工接管率、CSAT、单会话成本；灰度发布 + A/B。
- 部署：v1 用 Spring AI + pgvector，跑在 `ai` profile；如果 AI 团队更偏 Python，再拆独立 FastAPI `ai-service`。
- 指标：Bot 解决率、转人工率、首响时间、平均会话时长、CSAT、投诉率。

---

## 5. 分阶段开发计划（32 周基线）

| 阶段 | 周次 | 主题 | 关键交付 | 验收标准 |
|---|---|---|---|---|
| Phase 0 | W1–3 | 地基 | ADR、ERD、OpenAPI、参考登记/对照表、仓库骨架、Compose/Dev Container、CI、认证、i18n、可观测、seed | 一键启动；热重载；登录；简繁切换；全链路 trace；CI 绿 |
| Phase 1 | W4–7 | 商品 | SPU/SKU/多规格、属性、媒体、分类、翻译、价格表/多币种、搜索、后台、PC 列表/详情 | 3 种语言创建多规格商品并上架；搜索/筛选/币种切换可用 |
| Phase 2 | W8–11 | 交易 | 购物车、结账、订单状态机、Mock 支付、库存预占/流水、基础运费/税费、邮件、法律 clickwrap | 下单支付；库存正确预占/释放；重复回调安全；接受记录可审计 |
| Phase 3 | W12–16 | 库存与履约 | 多仓、门店、批次/效期、FEFO、门店发货、自营发货、供应商代发、自提、拆单 | 多批次/临期拣货；就近自提；供应商推单；拆单发货 |
| Phase 4 | W17–20 | 供应商/ERP/分析 | 供应商 API/后台、CSV、库存价格同步、订单推送、轨迹；PO/收货/调拨/盘点/成本；分析事件与看板 | 供应商同步；采购入库；临期看板；经营看板 |
| Phase 5 | W21–24 | 客服/AI/移动/增长 | WS、客服工作台、推送、规则 Bot、RAG/tools 接口；uni-app H5/小程序/APP；促销/评价/收藏 | 断线补拉、转接、离线推送；Bot 引用政策；小程序下单 |
| Phase 6 | W25–28 | 港澳台本地化 | zh-HK/zh-TW、HKD/MOP/TWD、当地支付/物流/自提、本地化仓、报关/税费、法律文档 | 三地市场配置；本地支付/物流 Mock；多币种价格 |
| Phase 7 | W29–32 | 演示/企业版/上线 | Demo 站（GET+mock）、企业更新骨架、安全/性能、UAT、Runbook、灰度/回滚 | 演示站禁写；生产检查单；备份恢复；Go-live |

- 4～6 人团队；2～3 人 12～18 个月；完整 POS 另加 3～4 周；更多支付/物流适配器按市场另估。
- 每个阶段有 Gate：功能、测试、安全、文档、演示数据、回滚方案。

---

## 6. 测试、安全与运维

### 6.1 测试

- 单元测试：领域规则、金额、库存、批次/效期、FEFO、拆单、自提、状态机、权限。
- 集成测试：Testcontainers 起 Postgres/Valkey/RabbitMQ；支付 webhook、Outbox、库存并发。
- 契约测试：OpenAPI 生成 client；供应商 API 兼容性。
- E2E：Playwright 覆盖注册 → 商品 → 购物车 → 支付 → 拆单 → 发货/自提 → 售后 → 客服。
- 压测：k6，重点下单、库存扣减、搜索、WS 连接、看板查询。
- 安全：OWASP ASVS、SAST/DAST、依赖扫描、密钥扫描、权限绕过、演示站防滥用。
- 本地化 QA：多语言、简繁、日期/金额/地址格式、PDF 渲染。

### 6.2 安全与合规

- PCI：支付用 hosted fields/redirect，做 SAQ A，不落卡数据。
- 合规：PIPL（中国大陆）、PDPO（香港）、PDPA（澳门）、台湾地区个人资料保护相关法规；数据出境评估。
- 防欺诈：3DS、地址校验、velocity limit、黑名单、chargeback 证据。
- 审计：管理端/供应商/门店/客服所有敏感操作留痕。
- 权限：最小权限、供应商数据隔离、门店数据隔离、客服会话授权。

### 6.3 CI/CD

- GitHub Actions：后端测试、前端 lint/test/build、E2E、Trivy/依赖/密钥扫描、镜像构建、SBOM。
- Flyway 迁移作为独立 Job；禁止应用启动时自动改生产库。
- staging 自动部署；生产灰度/蓝绿；一键回滚。
- 分支：trunk-based + 短分支 + feature flags；语义化版本 + CHANGELOG。

### 6.4 可观测与灾备

- OpenTelemetry + Prometheus + Grafana + Loki + Tempo。
- 核心 SLO：下单成功率、支付成功率、API p95、WS 在线率、库存一致性、客服首响、自提核销成功率。
- Postgres PITR、S3 版本化、备份恢复演练；RabbitMQ DLQ 告警；OpenSearch 快照。

---

## 7. 团队分工与协作

| 角色 | 职责 |
|---|---|
| Tech Lead / 后端 | 架构、ADR、订单/库存/支付、代码评审 |
| 后端 / ERP | 供应商、采购、批次/效期、库存、履约、报表 |
| 前端 / 商城 | Nuxt 3、SEO、多语言、性能 |
| 前端 / 后台与移动 | Vue 管理端、门店端、客服工作台、uni-app |
| QA / DevOps | Compose/CI、E2E、压测、安全、发布 |
| 产品 / 设计 | 范围、原型、验收、文案 |
| 法律 / 翻译（外部） | 条款审校、隐私合规、翻译质量 |

节奏：每周 Demo；ADR 评审；每阶段 Gate；发布前安全/法务/运维三方签字。

---

## 8. 主要风险与应对

| 风险 | 应对 |
|---|---|
| 支付/物流开户慢 | Phase 0 启动微信/支付宝/银联申请；Mock 先行；备用服务商 |
| 税务/报关复杂 | 市场规则配置；港澳台本地仓/报关服务；法务确认税费展示 |
| 供应商履约不稳定 | SLA、超时自动取消、备用供应商、库存水位、日对账 |
| 超卖/库存不一致 | 流水 + 预占 + 原子扣减 + 并发压测 + 对账任务 |
| 批次/效期数据错误 | 扫码优先、日期选择、收货校验、盘点、FEFO 合规看板 |
| 门店账实不符 | 收货/盘点/调拨/自提/发货全链路；POS 未接入时明确前置仓假设 |
| 范围蔓延 | v1 明确单商户 + 代发 + 简易 ERP；POS/marketplace/总账后置 |
| AI 幻觉/合规 | RAG 引用、护栏、人工兜底、评测集、灰度、数据保留策略 |
| 法律翻译错误 | 人工法律审校；版本不可变；市场 fallback；发布审批 |
| 演示站滥用 | 只读 + GET mock + 限流 + 无外网 + 独立构建 |
| Docker 资源占用 | `lite` profile、资源限制、按需启用 obs/ai profile |
| License/参考污染 | PolyForm-Noncommercial-1.0.0（仅限非商业用途）+ SBOM + CI 扫描；AGPL 服务隔离；clean-room 登记 |

---

## 9. 开工前需要确认的决策

1. 首发是否只做中国大陆，港澳台放 Phase 6？（默认：是）
2. 门店是否同时零售，需要完整 POS？（默认：v1 只做前置仓/自提/发货；POS 可选）
3. 真实支付首发：微信/支付宝/银联；港澳台本地支付优先级？
4. 本地化仓：香港/澳门/台湾是否自建/第三方仓？
5. 演示站是否严格 GET-only，还是允许独立 sandbox 交互？（默认：公开站严格 GET+mock）
6. 企业版边界：哪些功能收费、更新通道、离线包？
7. 团队人数/上线日期。

---

## 10. 批准后的第一批产出

- `docs/DEVELOPMENT_PLAN.md`：本计划完整版。
- `docs/adr/`：技术栈、模块化单体、金额与币种、法律文档、供应商 API、AI 边界、clean-room 政策。
- `docs/references/`：参考项目登记、许可证、借鉴点、未复用内容。
- `docs/parity-matrix.md`：功能对照表（参考项目 vs 我们的 v1/v2）。
- `docs/erd.md`：核心 ERD（Mermaid）。
- `docs/openapi.yaml`：顾客/管理/门店/供应商 API v1 契约。
- `compose.yaml`、`.devcontainer/`、`Makefile`、`.env.example`。
- 仓库骨架：`server`（api/worker/ws profiles）、`web/apps/storefront`、`admin`、`store-console`、`supplier-portal`、`support-console`、`mobile`。
- Seed 数据 + 冒烟测试 + CI 流水线。
- 第一个垂直切片：多规格商品 → 购物车 → Mock 支付 → 拆单（仓库/门店/供应商）→ 批次 FEFO 拣货 → 自提/发货 → 客服。

---

## 11. Definition of Done

每个功能必须满足：

- 代码评审通过，模块边界未被破坏。
- 单元/集成/E2E 测试覆盖关键路径。
- 多语言 key 完整，法律文案有版本和审校记录。
- 有 trace、日志、指标、告警。
- 有权限校验、审计日志、幂等/重试策略。
- 有 feature flag 和回滚方案。
- API 契约更新，文档同步。
- 参考实现有 clean-room 登记，未复制受限代码。
- 验收标准由产品/QA 签字。

**成功指标**：下单成功率 ≥ 99.5%，支付成功率 ≥ 97%，API p95 < 300ms，库存对账差异为 0，客服首响 < 2 分钟，Bot 解决率目标 ≥ 40%（上线后逐步提升）。

---

## 12. 补充需求（2026-10-18）

### 12.1 多主题与模板
- PC 商城提供多份模板：`mall`（通用零售）、`industrial`（机械设备/工业品）、`fresh`、`minimal`。
- 通过 `THEME` / `NUXT_PUBLIC_THEME` 或 URL `?theme=industrial` 切换；运行时只替换主题 token 和少量结构样式，不复制整页代码。
- 新增 `@fullmall/theme` 共享包，PC、管理后台、小程序共用 `ThemeConfig`、预设和 CSS 变量输出。
- 小程序提供多风格；运营后台「主题配置」用颜色选择器、滑块、下拉框、开关微调品牌色、圆角、字号、卡片风格、商品布局、Banner 文案，并实时预览。
- 参考 CRMEB 的“预设 + 可视化配置 + 预览 + DIY 页面”产品思路，只做 clean-room 借鉴，不复制代码/UI/文案/数据结构。
- 后续：主题配置服务端持久化、按市场/门店覆盖、版本发布/回滚、页面级 DIY（Banner、楼层、商品分组、活动位）。

### 12.2 安全、并发与库存正确性
- 结算支持 `Idempotency-Key`：同 key + 同请求体返回同一订单；不同请求体返回 409。
- 库存预占使用数据库原子更新；并发抢单必须保证不超卖、不出现负库存。
- 写接口加入单节点限流（后续替换为 Valkey 分布式限流）。
- 增加安全响应头、输入边界、生产敏感接口 fail-closed、demo 只读。
- 测试要求：
  - 20/30 库存下 50 并发下单，成功数必须等于库存，预占不得超库存。
  - 10 并发同幂等键只能产生 1 个订单、1 次预占。
  - 真实 PostgreSQL + Flyway + seed + 自提闭环。
  - Playwright 覆盖商城下单、主题切换、运营后台、门店拣货/核销。
  - 依赖审计：生产依赖 critical = 0；剩余 high 必须记录并跟踪升级。