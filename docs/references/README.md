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