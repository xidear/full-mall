# ADR-0001: 技术栈

- 状态：Accepted
- 日期：2026-10-18

## 背景

项目需要覆盖 PC 商城、H5、微信/支付宝小程序、APP、管理后台、门店端、供应商端和客服工作台；核心域包含订单、库存、批次效期、支付、履约和简易 ERP。首发中国大陆，后续港澳台。

## 决策

- 后端：Java 21、Spring Boot 3、Spring Modulith、Spring Security、Flyway。
- 数据访问：MyBatis-Plus 或 jOOQ；库存、对账、报表等关键 SQL 手写。
- 数据库：PostgreSQL 17。
- 缓存/在线状态：Valkey。
- 消息：RabbitMQ + Transactional Outbox。
- 搜索：OpenSearch（MVP 可用 Meilisearch 降低运维）。
- 对象存储：SeaweedFS（S3 兼容）；MinIO 作为可选，但注意 AGPL。
- PC：Nuxt 3 + Vue 3。
- 后台/门店/供应商/客服：Vue 3 + Element Plus 或 Vben Admin。
- 移动端：uni-app（Vue 3）；APP 体验成为瓶颈时可迁移 Flutter，API 不变。
- 实时：原生 WebSocket + JSON 协议，不使用 Socket.IO。
- 部署：Docker Compose（开发/小规模）；Kubernetes（生产扩展）。

## 后果

- 前后端语言不同，通过 OpenAPI 生成 TypeScript client 保持契约一致。
- Java 的强事务能力适合订单、库存和 ERP；Vue/uni-app 最大化多端复用。
- 需要维护 Java 与 Node 两套工具链，但 Docker/Dev Container 统一环境。

## 替代方案

- NestJS 全栈：开发更快、单语言，但库存/ERP 长期复杂度和国内招聘不如 Java。
- Go + React：网关性能好，但复杂 ERP 领域建模和生态不占优。
- 直接 fork Medusa/Saleor：会继承其架构、升级和许可证约束，不适合作为长期自有产品核心。