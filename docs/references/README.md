# 参考项目登记（Clean-room Register）

本目录记录我们研究的成熟项目、许可证、借鉴点和明确未复用的内容。**许可证以各仓库当前版本为准，Phase 0 必须做一次 Legal 复核。**

## 重点参考

| 项目 | 类型/栈 | 许可证（待复核） | 借鉴 | 不复用 |
|---|---|---|---|---|
| [Medusa v2](https://github.com/medusajs/medusa) | TS 交易内核 | MIT 核心 + 企业功能商业授权 | 模块边界、Region/Currency、Price List、库存预占、Payment/Fulfillment Provider SPI | 代码、Admin UI、企业版功能 |
| [Saleor](https://github.com/saleor/saleor) | Python/GraphQL | BSD-3-Clause | Channel、Warehouse、Stock Allocation、Apps/Webhooks | GraphQL schema、代码 |
| [Vendure](https://github.com/vendure-ecommerce/vendure) | TS/NestJS | MIT | Channel、插件 SPI、促销/自定义字段 | 代码、UI |
| [Apache OFBiz](https://github.com/apache/ofbiz) | Java/ERP | Apache-2.0 | Order、Inventory、Facility、Shipment、Party、Invoice 领域词汇 | 老式架构、代码 |
| [Chatwoot](https://github.com/chatwoot/chatwoot) | Ruby/Vue | MIT + 企业功能单独授权 | Inbox、Conversation、Agent、Team、Label、Canned Response、SLA | 代码、UI、品牌 |
| [Hyperswitch](https://github.com/juspay/hyperswitch) | Rust 支付编排 | Apache-2.0 | Connector 抽象、路由/重试、Webhook 归一化、对账 | 全套编排、代码 |
| [Documenso](https://github.com/documenso/documenso) | TS 电子签名 | AGPL-3.0 | Envelope、Template、Field、签署顺序、审计轨迹 | 不链接进核心；独立部署或只学流程 |

## 按需参考

| 项目 | 类型/栈 | 许可证（待复核） | 借鉴 | 不复用 |
|---|---|---|---|---|
| [Shopware](https://github.com/shopware/shopware) | PHP 商城 | Community Edition MIT（待复核） | Sales Channel、Rule Builder、多语言 | 代码、复杂配置体系 |
| [Magento Open Source](https://github.com/magento/magento2) | PHP 商城 | OSL-3.0 | Store View、Multi-Source Inventory、Tax Zone | 代码 |
| [Odoo Community](https://github.com/odoo/odoo) | Python ERP | LGPL-3.0 | 采购、收货、调拨、盘点、Landed Cost | 代码 |
| [ERPNext](https://github.com/frappe/erpnext) | Python ERP | GPL-3.0 | 库存、采购、批次/效期、报表 | 代码；只读文档/行为 |
| [Dify](https://github.com/langgenius/dify) | AI 工作流 | Apache-2.0 + 附加条款（待复核） | RAG、Agent、工具调用、评测 | 不作为核心依赖 |
| [Rasa](https://github.com/rasa/rasa) | Python 对话 | Apache-2.0（Open Source） | 意图、对话策略、人工接管 | 代码 |
| [Lilishop](https://github.com/lilishop/lilishop) | Java/Vue/uni-app | 待复核 | 国内多端工程组织、后台交互 | 代码、UI、文案 |
| [CRMEB](https://github.com/crmeb/CRMEB) | PHP/uni-app | 待复核 | 多端接入经验、模板/主题可视化配置、DIY 页面产品思路 | 代码、UI、文案、数据结构 |
| [mall4j](https://github.com/gz-yami/mall4j) | Java/Vue/uni-app | 待复核 | uni-app 工程组织、后台交互 | 代码、UI、文案 |
| [Shopify Markets](https://www.shopify.com/markets) | 闭源商业产品 | 商业 | Market、币种、税费展示、本地化 UX | 任何代码；只做行为参考 |

## 数据集合规登记（非代码参考，ADR-0013）

区划/国家**数据**不是代码，但许可证同样受 ADR-0009 的 SBOM/License 扫描口径约束。

**传染性速查**（决定能否闭源交付，详版见 ADR-0013）：MIT / BSD / Apache-2.0 / WTFPL = **无传染**（WTFPL 比 MIT 还松）；GPL / AGPL = **copyleft 传染**；**ODbL = share-alike**（数据库版 copyleft，对外再分发衍生数据须署名 + 同许可）。
**fork 不改变许可证**：只有版权人能重新授权；fork 后自贴别的许可证 = 不实声明。冲突的正确解法是"不把它作为依赖分发，只落我们自己的快照"。

| 数据集 | 来源 | 许可证（实测） | 用途 | 风险与待复核项 |
|---|---|---|---|---|
| province-city-china | https://github.com/uiwjs/province-city-china | MIT（npm 与仓库一致，2026-10-11 pushed） | 中国省市区字典默认来源 | 数据本身源自 GB/T 2260 / 国家统计局发布，**事实数据不受版权保护**，但再分发需署名来源；刷新时记录 `source_version` |
| china-division（modood） | https://github.com/modood/Administrative-divisions-of-China | **实测 WTFPL**（上游 LICENSE 原文 + `npm pack` 解出的 tarball 内 LICENSE 原文一致）；同包 `package.json` 误写 MIT。发布者 `_npmUser` = 版权人本人 `modood <modood@qq.com>`，README 徽章亦自标 WTFPL | 备选五级数据（三级 pca-code.json 仅 137KB） | **MIT 与 WTFPL 都无传染性**，取保守口径 WTFPL 即零义务、可闭源商用。⚠️ **不得用 fork 把许可"改成" MIT**：fork 不改变版权归属，自贴 MIT = 不实授权声明。正解见 ADR-0013：不将该包作为依赖分发，只落我们自己的规范化快照并附 provenance。数据截止 2023-06-30（比默认来源老约 3 年） |
| province-city-china（uiwjs） | https://github.com/uiwjs/province-city-china | 仓库根 LICENSE 原文 **MIT**（Copyright (c) 2019 wabg）；经 GitHub Actions **OIDC 受信发布** | **中国省市区默认来源**（三级 area.csv 76KB，2,847 条区县） | ⚠️ **npm tarball 内不含 LICENSE 文件**（实测），而 MIT 要求许可证文本随副本分发。缺口由我们补齐：生成的快照内自附许可证文本 + 来源 + 抓取日期。数据更新至 2026-10-09，已改用国家地名信息库官方接口 |
| world-countries（mledoze/countries） | https://github.com/mledoze/countries | **ODbL-1.0**（npm 元数据；其 LICENSE 原文本次未取到，raw.githubusercontent 超时） | 备选（需要旗帜/多语言全量时） | ODbL = **share-alike（数据库版 copyleft）**，本议题里唯一真正"类传染"的许可：内部使用可以，**对外再分发衍生数据集**须署名 + 以 ODbL 开放。闭源商业交付默认避开；若启用必须先 Legal 复核，不复制其数据源结构原文 |
| i18n-iso-countries | https://github.com/michaelwittig/node-i18n-iso-countries | **MIT（已验证）** | **国家层默认来源**（ISO 3166-1 名称本地化） | 无 |
| GeoNames | https://download.geonames.org/export/dump/ | 据二手资料 CC-BY 4.0，**官方页未核实** | 市级以下/邮编/经纬度备选 | 采用前人工确认许可并保留署名条目 |
| 商业区划库（qqzeng 等） | https://www.qqzeng.com/ | 商业授权（三级 ¥68） | 未采用 | 不采购：三级用国标开源数据已够 |

## 登记模板

新增参考时，在 `docs/references/` 下建立 `REF-NNN-project.md`：

```markdown
# REF-NNN: 项目名

- 仓库/文档：
- 版本/commit：
- 许可证：
- 阅读方式：docs-only / source-allowed
- 借鉴的概念：
- 明确未复用：
- 对应 ADR：
- 复核人/日期：
```

## 使用规则

1. 先有本项目的 ADR、ERD、OpenAPI 和功能对照，再研究参考实现。
2. GPL/AGPL：只读官方文档和公开行为，不读源码、不复制；能力通过独立服务集成。
3. MIT/BSD：可读源码，但默认重写；复用必须保留版权和许可证声明。
4. 不复制 UI、品牌、文案、图标、数据库 schema 原文。
5. 代码评审检查来源；CI 执行 License/SBOM 扫描。