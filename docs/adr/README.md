# Architecture Decision Records

本目录记录不可轻易推翻的架构决策。每条 ADR 包含状态、背景、决策、后果和替代方案。

| ADR | 标题 | 状态 |
|---|---|---|
| 0001 | 技术栈：Java 21 + Spring Boot 3 + Vue 3 | Accepted |
| 0002 | 模块化单体 + api/worker/ws 三进程 | Accepted |
| 0003 | 中国市场优先与本地化策略 | Accepted |
| 0004 | 金额、币种与汇率快照 | Accepted |
| 0005 | 库存粒度、批次与保质期 | Accepted |
| 0006 | 多模式履约与订单拆单 | Accepted |
| 0007 | Demo / Prod 双构建模式 | Accepted |
| 0008 | 企业版与更新边界 | Accepted |
| 0009 | Clean-room 参考策略 | Accepted |
| 0010 | 法律文档版本化与接受证据 | Accepted |
| 0011 | 多主题模板与小程序图形化配置 | Accepted |
| 0012 | POS 收银、组合支付与现金财务 | Accepted（设计） |

新增 ADR 使用 `NNNN-short-title.md`，不要修改已接受 ADR 的决策内容；如需变更，新增一条 superseding ADR。