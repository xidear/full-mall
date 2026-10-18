# 功能对照矩阵

用于控制范围：参考项目有什么，不代表我们 v1 要做。`v1` 是首发中国大陆闭环，`v2` 是港澳台与增强能力。

| 能力 | 参考项目 | v1 决策 | v2/后续 | 备注 |
|---|---|---|---|---|
| 多规格 SPU/SKU | Medusa、Saleor、Shopware | 做 | — | 变体矩阵、属性库、媒体 |
| 多语言 | Saleor、Shopware | 做 zh-CN，预留 zh-HK/zh-TW | 英文及其他 | 独立翻译表 |
| 多币种/价格表 | Medusa、Shopware | 做 CNY，预留 HKD/MOP/TWD | 多市场汇率 | 金额整数分 |
| 商品搜索/筛选 | Saleor、Shopware | 做 | 多语言分词增强 | OpenSearch/Meilisearch |
| 库存流水 | Medusa、Saleor、OFBiz | 做 | — | 不可变 stock_move |
| 库存预占/分配 | Medusa、Saleor | 做 | 智能分配/跨仓调拨优化 | 防超卖 |
| 多仓库 | Saleor、Magento MSI | 做 | 保税仓、本地化仓 | 仓库-市场映射 |
| 批次/效期 | Odoo、ERPNext、Magento MSI | 做 | 序列号/温层 | 门店重点 |
| FEFO/FIFO/无策略 | Odoo、ERPNext | 做 | 按类目/商品可视化配置 | 临期优先 |
| 门店库存/盘点 | Odoo、OFBiz | 做 | 完整 POS | v1 前置仓/自提/发货 |
| 就近自提 | Shopify Pickup、Magento MSI | 做 | 跨店调拨、自提柜 | 库存+距离+时段 |
| 自营仓发货 | Medusa、OFBiz | 做 | 波次拣货、自动化设备 | 面单/轨迹 |
| 门店发货 | Shopify、Magento MSI | 做 | 门店波次 | 门店库存 |
| 供应商代发 | Medusa、Saleor | 做 | 多供应商分单、结算 | API + CSV |
| 订单拆单 | Saleor、OFBiz | 做 | 智能拆单优化 | 按履约源 |
| 部分发货/退款 | Medusa、Saleor | 做 | 复杂售后 | 履约单级别 |
| 支付适配器 | Hyperswitch、Medusa | 做 Mock + SPI | 微信/支付宝/银联/港澳台 | 开源默认不配置真实支付 |
| 支付对账 | Hyperswitch | 做基础 | 自动对账/差异工作流 | 日结 |
| 发票 | 国内商城 | 做基础 | 增值税电子发票深度对接 | 港澳台票据模板 |
| 供应商 API | Saleor Apps、Medusa | 做 `/open/v1` | 更多协议 | HMAC/幂等/Webhook |
| 简易 ERP | OFBiz、Odoo、ERPNext | 做采购/收货/调拨/盘点/成本 | 总账/MRP/结算 | 不整体引入 |
| 客服 WS | Chatwoot | 做 | 全渠道（微信/邮件/APP） | 原生 WS + JSON |
| 客服工作台 | Chatwoot | 做基础 | 质检、排班、SLA 升级 | 订单/物流上下文 |
| AI 客服 | Dify、Rasa | 做接口 + 规则 Bot | RAG/工具/评测/坐席辅助 | 不提前堆功能 |
| 法律文档版本化 | Documenso | 做 | 供应商电子签名 | Clickwrap + PDF |
| 运营分析 | 国内商城、Shopware | 做基础看板 | ClickHouse/自助 BI | 业务事件日志 |
| 审计日志 | OFBiz、Magento | 做 | 合规报表 | 敏感操作全留痕 |
| 演示站只读 | 无统一参考 | 做 | 独立 sandbox | GET + mock |
| 企业版更新 | 商业产品 | 预留骨架 | 在线/离线更新 | 签名 manifest |
| 后台无 JSON/逗号输入 | 无统一参考 | 做 | 规则构建器增强 | 默认结构化控件 |
| 多套 PC 模板 | CRMEB、Shopware | 做 mall/industrial/fresh/minimal 预设 | 页面级 DIY、按市场覆盖 | 主题 token + 配置选择 |
| 小程序多风格/图形化配置 | CRMEB | 做预设 + 主题工作室 | 持久化、版本发布、页面级 DIY | 只学产品思路，不复制 UI/代码 |
| 完整 POS | Odoo、Shopify POS | 不做 | 可选模块 | 需另加 3–4 周 |
| Marketplace 分账 | Saleor、Medusa | 不做 | v2+ | 预留 vendor_id |
| 完整总账/MRP | Odoo、ERPNext | 不做 | 集成/自研评估 | v1 只做履约 ERP |