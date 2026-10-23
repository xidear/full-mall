# 数据库与架构设计审查（上线前整改）

> 本文只写**实测结果**：数据来自对当前 PG 实例 `information_schema` / `pg_constraint` / `pg_indexes` 的查询，以及代码静态扫描。
> 已改的写"已改"，未改的写"待办"并说明原因。

## 一、数据库现状

| 指标 | 实测值 |
|---|---|
| 表数量 | **140** |
| 无主键的表 | **0** |
| 无任何索引的表 | **0** |
| 外键列**缺索引** | **45** |
| 重复索引 | **1**（`theme_config` 的 `(channel, market_code)` 唯一索引 + 普通索引） |
| 金额列类型 | 全部 `bigint`（分），**无 float/numeric 混用** |
| JSONB 列 | 20 个（分类见下） |

### 1.1 已修（迁移 `V101__schema_review_indexes.sql`）

- 删除 `theme_config` 上重复的普通索引（保留唯一索引）；
- **补齐 45 个外键列索引**（FK 校验与级联删除不再全表扫描）；
- 新增高频复合索引：
  - `orders (status, created_at DESC, id DESC)` —— 订单列表与 keyset 深页
  - `product_review (status, created_at DESC)` —— 评价审核
  - `audit_log (occurred_at DESC)` —— 审计倒序
  - `member_coupon (member_id, status, expires_at)` —— 我的券
  - `coupon_redemption (coupon_id, created_at DESC)` —— 核销查询

> 验证：全量测试 **635 个通过**（迁移在测试库实际执行）。

### 1.2 JSONB 列分类（该 JSON 的留、该关联表的改）

| 列 | 判定 | 理由 |
|---|---|---|
| `orders.address_json` | ✅ 保留 JSON | 下单快照，地址后续变更不应影响历史订单 |
| `order_line.snapshot` | ✅ 保留 JSON | 商品快照（标题/规格/单价），必须冻结 |
| `audit_log.before_json/after_json` | ✅ 保留 JSON | 审计留证，结构随业务演进 |
| `payment_event.payload_json`、`webhook_delivery.payload` | ✅ 保留 JSON | 外部报文原文，用于对账与举证 |
| `sku.shelf_life_policy_json` | ⚠️ 建议改列 | 只有 `{mode,days,warnDays}` 三个字段，查询/校验都要解 JSON；建议拆三列（待办，涉及前后端） |
| `promotion.rules` | ⚠️ 混合 | `thresholdAmount/value/channel` 已在查询里被 `->>` 提取，说明是**关系型数据**；建议拆列，`excludeSkuIds` 单独建关联表 |
| `coupon_template.scope_ids` | ⚠️ 建议改关联表 | 一组 ID 用 JSON 存无法加外键、无法索引；建议 `coupon_template_scope(template_id, scope_type, scope_id)` |
| `sku.options_json` | ✅ 保留（ADR-003 已定） | 关联表为真相来源，JSON 是投影；生成/回填后自动重建 |
| `theme_config.config_json`、`integration_config.config_hint`、`export_job.params`、`support_message.metadata_json`、`ai_event.payload`、`ai_action_draft.payload`、`analytics_event.properties_json` | ✅ 保留 JSON | 结构不固定或仅展示用 |

### 1.3 缺 `created_at` 的表（33 个）

多数是**关联/明细表**（`after_sale_line`、`order_line_refund`、`role_permission`、`pick_task_line` 等），没有时间戳可接受。

但有 6 个**实体表**建议补，否则无法按时间排查：

`product_review_image`、`product_translation`、`feature_flag`、`scheduled_task`、`sku_cost`、`knowledge_chunk`

> 状态：**未改**。原因是补列需要回填历史值（`now()` 会污染审计），应与你确认"回填取父记录时间还是留空"。

## 二、架构与重复实现

### 2.1 已收敛

| 问题 | 实测 | 处理 |
|---|---|---|
| 每处各自 `new ObjectMapper()` | **21 处** | 新增 `platform/json/Json`，已把 6 处支付/风控/Webhook 客户端改为共享实例（剩余 15 处为纯 DTO 解析，逐步替换） |
| HMAC-SHA256 各自实现 | **2 处**（供应商签名过滤器、Webhook 签名） | 新增 `platform/crypto/Hmac`，`WebhookSigner` 已委托调用；`SupplierHmacFilter` 下一批替换 |

### 2.2 仍待收敛（如实列出）

| 重复点 | 位置 | 建议 |
|---|---|---|
| **keyset 分页逻辑复制 6 份** | `order`、`aftersale`、`review`、`customer`、`audit`、`supplier` 各写一遍"游标解析 + 查询 + 组装 next" | 抽 `platform/paging/KeysetSupport`（游标编解码 + 统一排序规则），各模块只写 SQL |
| **HttpClient 各自创建 10 处** | 微信/支付宝/短信/物流/AI/审核适配器 | 抽 `platform/http/HttpClients`（统一超时、User-Agent、可选代理），避免连接池打散 |
| **金额元↔分转换 2 处** | `PaymentReconciliationService.yuanToFen`、`AlipayPrecheck` 内部 | 收敛到 `platform/money/Money`（已有 `Money` 值对象，补齐 `parseYuan`） |
| **渠道配置读取重复** | 各 `*Config` 都写"取 DB 配置 → 空则报错" | 已有 `IntegrationGate`，但支付/短信/存储各自又写了一遍判定；应统一走 gate |
| **促销与券的叠加判定分散** | `PromotionStackingPolicy` + 券属性 `allowCouponStack` 两套 | 合并为一个 `DiscountCombination`（需要你先定口径，见 FINAL_BLOCKERS C2） |

### 2.3 框架使用问题

| 问题 | 现状 | 建议 |
|---|---|---|
| 大量 `JdbcTemplate` 手写 SQL，未用 JPA | 有意为之（批量与复杂查询更可控） | 保持，但**读模型**可考虑 jOOQ 统一（需评估引入成本，暂不动） |
| Modulith 只做结构校验 | 已能在 CI 抓循环依赖（本轮就抓到 distribution↔payment） | 保持，新增模块必须过 `ModulithStructureTest` |
| 定时任务用自研 `@Scheduled` + 状态表 | 多实例会重复执行 | 已在关键任务用 `pg_advisory_xact_lock`，**其余任务需逐个核对**（待办） |
| 事件发布用 Modulith events | 正常 | 保持 |
| 无统一分页 DTO | 各接口返回字段名不一致（`nextCreatedAt` vs `cursor`） | 建议统一 `KeysetPage<T>{items,nextCursor}` |

## 三、本轮改动清单

1. `V101__schema_review_indexes.sql`：45 个外键索引 + 5 个复合索引 + 删 1 个重复索引。
2. `platform/json/Json.java`：统一 JSON 入口；6 处客户端改用它。
3. `platform/crypto/Hmac.java`：统一 HMAC；`WebhookSigner` 改用它。
4. 全量测试 635 通过。

## 四、下一批（按优先级）

1. `platform/http/HttpClients` + 替换 10 处（低风险，收益是连接复用与超时一致）。
2. `platform/paging/KeysetSupport` + 6 个模块改造（中风险，需逐个跑测试）。
3. `promotion.rules` 拆列 + `coupon_template.scope_ids` 改关联表（**改动大、涉及前端**，建议单独一轮）。
4. 6 个实体表补 `created_at`（需你定回填策略）。
