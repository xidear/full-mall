# ERP 成本、角色化导出与性能设计

> 日期：2026-10-19
> 触发：用户指出「CSV 导出数据量大」「不同角色看到不同列（进货价/毛利）」「进货价不是单一字段而是长期均价」「admin 端是否考虑财务与运营概念」「系统性性能调优」
> 结论先行：**当前实现三项都没做**，只有同步导出 + 行数上限、行级范围、批次成本字段。本文给出成熟方案与实施顺序。
> 关联需求：SEC-002（导出脱敏）、SEC-001（字段级）、ERP-014、NFR-002、CR-021/CR-022（禁止自建字段矩阵）

## 0. 现状盘点（诚实）

| 事项 | 现状 | 差距 |
|---|---|---|
| 导出 | **同步**生成，`MAX_ROWS=2000`/`MAX_LIMIT=5000` 封顶 | 大导出会超时/占满请求线程；上限是"把问题挡住"，不是解决 |
| 列可见性 | 只有 `Scope` 行级范围 | **没有列级**；成本/毛利字段根本还没建模 |
| 进货价 | 仅 `inventory_batch.unit_cost_amount`（批次实际成本） | 无成本流水、无移动加权、无 COGS 快照，因此**算不出正确毛利** |
| 报表/性能 | 77 个索引；无分区、无物化视图 | 大表（stock_move/audit_event/pos_sale/orders）无分区；列表存在 N+1 |
| 财务概念 | 只有金额整数 + 幂等记录 | **期间关账、对账、毛利、成本差异均未建模** |

## 1. 大导出：异步任务 + 对象存储 + 签名 URL

成熟做法（Spring Batch / 任务表 + worker / 对象存储签名下载）：

```
export_job(id, requester, resource, params_json, scope_snapshot, projection,
           status, row_count, file_key, expires_at, error, created_at, started_at, finished_at)
status: PENDING → RUNNING → SUCCEEDED | FAILED | CANCELLED | EXPIRED
```

要点：

1. **抢占式执行**：`UPDATE export_job SET status='RUNNING', started_at=now() WHERE id = (SELECT id FROM export_job WHERE status='PENDING' ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 1)`，
   多副本不会重复跑同一任务。
2. **流式写**：`CSVPrinter` 直接写 `OutputStream`，边查边写，不进内存；chunk 500–1000。
3. **深分页用 keyset**：`WHERE (created_at, id) > (?, ?) ORDER BY created_at, id LIMIT ?`，禁用 `OFFSET` 大偏移。
4. **存储与下载**：落 SeaweedFS/S3，返回**短期签名 URL**（默认 7 天过期）；下载要再鉴权一次，且记录下载审计。
5. **配额与限流**：每用户并发任务 1–2 个、每日 N 次；超限 429。
6. **状态查询**：`GET /admin/v1/export-jobs/{id}` 返回 PENDING/RUNNING/SUCCEEDED + 进度；完成发站内通知。
7. **审计**：`export_job` 即任务审计；下载另记 `EXPORT_DOWNLOADED`。

替换对象：`SupplierSettlementCsvService`、`MemberExportService` 改为"创建任务"，HTTP 立即返回 202 + jobId。

## 2. 角色级列可见性：角色化 DTO 投影（不自建字段矩阵）

原则与项目规则一致（CR-021/CR-022）：**字段可见性用按角色的 DTO 投影实现，不建"字段-角色"矩阵**。

```java
record OrderExportRowOps(...)      // 无成本、无毛利
record OrderExportRowFinance(..., long costAmount, long grossProfit, int grossMarginBps)
```

- 选择投影：按权限点 `finance.cost.view`（Spring Security authority）决定，Controller 只做映射；
- 导出任务表记录 `projection` 与列集合，事后可追；下载文件里不含未授权列（不是前端隐藏）；
- 负向测试必做：普通运营导出文件**不含** `costAmount/grossProfit`；财务角色包含；
- 触发升级的条件：若投影组合 > 3–4 种或需要按客户/供应商动态字段，再引入 **Casbin/OPA** 返回允许字段，应用仍用 DTO 执行。

## 3. 「进货价」：ERP 计价模型（不是单一字段）

四种成熟计价法：标准成本（Standard）、移动加权平均（Moving Average / WAC）、先进先出（FIFO）、批次实际成本（Specific/Batch）。

本项目已有批次/效期与 FEFO，采用**混合口径**：

| 层 | 用途 | 数据来源 |
|---|---|---|
| 批次实际成本 `inventory_batch.unit_cost_amount` | 采购/收货真实成本、退货、供应商结算 | 收货单录入 |
| SKU+仓库 移动加权平均 `sku_cost.avg_cost` | 财务报表口径（库存估值） | 每次收货重算 |
| 销售 COGS 快照 `pos_sale_line/order_line.cost_amount` | **历史毛利不漂移** | 出库时按当时 avg 写入 |

关键：**毛利必须用出库时点成本快照**，用"当前平均价"倒算会让历史毛利随补货变动，财务无法对账。

新增不可变账本：

```
cost_ledger(id, sku_id, warehouse_id, occurred_at, qty_delta, unit_cost, value_delta,
            source_type RECEIPT|SALE|RETURN|ADJUST|TRANSFER, source_id,
            balance_qty, balance_value, avg_cost)
```

- 收货：`qty+`，`value += qty*unit_cost`，`avg = value/qty`；
- 出库：`value -= qty*avg`（快照写入业务行），`avg` 不变；
- 退货回冲：按**原单成本**回冲，不按当前 avg；
- 并发：`SELECT ... FOR UPDATE` 锁 `sku_cost` 行；账本只追加；
- 对账：`SUM(value_delta)` 必须等于 `sku_cost.value`，差异即报警；
- 期间关账（period close）：月度冻结，禁止倒补历史，需红冲单据。

毛利口径：`毛利 = 净销售额（实付 − 退款） − COGS`；`毛利率 = 毛利 / 净销售额`（bps 存储，避免浮点）；运费与税单列，可选计入。

## 4. 性能优化（按收益排序）

| 项 | 现状 | 方案 |
|---|---|---|
| 导出 | 同步、全量入内存 | 异步 + 流式 + keyset（见 §1） |
| 深分页 | `LIMIT/OFFSET` | keyset 分页 + 覆盖索引 |
| N+1 | `listOrders` 每单再查行 | 单条 JOIN 或按 id 批量 `IN` 查询 |
| 大表增长 | 无分区 | `stock_move`、`audit_event`、`pos_sale`、`orders` 按 `created_at` 月分区 + 保留期归档 |
| 报表查询 | 实时聚合 | 物化视图（日销/SKU 毛利）+ `REFRESH MATERIALIZED VIEW CONCURRENTLY`，或读副本 |
| 缓存 | 本地 30s 缓存 | Valkey 集中缓存（开关/设置/类目树），写明失效点 |
| 批量写 | 单条 SQL | `COPY` 或 `executeBatch`，500–1000/批 |
| 连接池 | Hikari 20 | 导出 worker 独立小池，避免挤占 API |
| 可观测 | 无 | `pg_stat_statements`、慢查询日志、P95 指标、EXPLAIN 回归 |

## 5. 实施顺序（并入 v3 计划）

1. **A5 角色化导出投影**：先做"有/无成本列"两套 DTO + 负向测试（不依赖成本账本，可先用批次成本做毛利近似）。
2. **A6 异步导出**：`export_job` + worker + 对象存储 + 签名 URL + 配额；替换现有两个同步导出。
3. **F7 成本账本与移动加权**：`sku_cost` + `cost_ledger` + COGS 快照；回填历史批次成本。
4. **A7 对账与期间关账**：`SUM(value_delta)=value` 校验任务、月度冻结、差异告警。
5. **A8 报表与索引治理**：物化视图 + 分区 + keyset 分页规范 + EXPLAIN 回归。

> 每项完成需：迁移在真实 PostgreSQL 冒烟、金额用整数分、负向测试（无权限看不到成本）、文档同步。
## 6. 商品快照与 PII（2026-10-19 补充）

### 6.1 订单行必须存快照，不存关联键

订单一旦创建，**商品信息冻结**（类似淘宝商品快照），后续改标题、改价、下架、删商品都不能影响历史订单：

| 已冻结 | 快照内容（V36：单 `snapshot JSONB` 字段） |
|---|---|
| `title_snapshot`、`sku_code_snapshot`、`unit_price_amount` | `image`、`attributes`、`brand`、`category`、`listPriceAmount`、`costAmount`（COGS）、`locale` 等 |

> **设计修正（2026-10-19，用户指出）**：曾把快照拆成多个列，每加一项就要改表、改 INSERT、改映射；
> 改为**一个 JSONB 快照字段**，商品字段继续演进也不用动订单表。这与"后台禁止手填 JSON"不冲突——
> 该规则约束输入层，快照是服务端生成的历史事实。

要点：

- `sku_id` 仍保留，但**只作内部追溯**，展示与对账一律用快照字段；
- `locale` 记录下单时的展示语言，法律/客服争议时能还原"用户当时看到什么"；
- `cost_amount` 是**出库时点成本**，用于毛利，不能回查当前平均价。

### 6.2 支付配置版本化（对应 V35）

配置可编辑（换密钥、改回调、换服务商），因此支付记录引用 **不可变版本**（`payment_provider_config_version`），
并冗余 `channel/mode/sp_mch_id/sub_mch_id`；退款按版本路由，改配置不影响历史订单。

### 6.3 PII 与合规（中国大陆优先，预留其它地区）

| 要求 | 落地 |
|---|---|
| 手机号/身份证脱敏 | 输出侧 `FieldMasker`（已有）；后台列表默认掩码，仅授权角色可见明文并记审计 |
| 身份证号加密存储 | 需要时按字段级加密（AES-GCM/KMS），不落明文；`consent_ref` 记录授权来源 |
| 日志禁止明文 PII | 统一日志脱敏过滤器；异常信息里不拼接手机号/证件号 |
| 数据最小化与保留期 | 只存必要字段；订单/会话按法定期限保留，到期归档或删除（可配置） |
| 用户权利（查询/删除） | 提供导出与删除流程，删除走软删 + 关联脱敏 |
| 地区差异 | `pii_policy(region, field, action)` 配置化（MASK/ENCRYPT/DENY），大陆先落 MASK+ENCRYPT，其它地区按需扩展 |

## 7. 字段收敛审计（2026-10-19，用户要求"都合并"）

原则：**按类型会持续增加的字段，一律收敛为 JSONB**；只有用于过滤/排序/关联/对账的字段保留普通列。
前端用组合表单生成结构化数据，后台存 JSON；这与"禁止手填 JSON"不冲突（约束输入层）。

| 表 | 现状 | 处理 |
|---|---|---|
| `order_line` | 曾经拆成 image/attributes/brand/... 多列 | ✅ V36 收敛为 `snapshot JSONB` |
| `payment_provider_config_version` | 8 个渠道专属列 | ✅ V37 收敛为 `settings JSONB`（`payment` 表仍保留 4 个路由列） |
| `promotion` | **37 列**（类型专属：秒杀价、赠品、拼团人数…） | ⬜ 待做：`rules JSONB` + 保留 type/status/时间窗列；需同步重构 `PromotionService` 与后台表单 |
| `legal_document` | 32 列（多市场多语言正文） | ⬜ 待做：正文与元数据入 `content JSONB` 或独立 translation 表 |
| `theme_config` | 已是 `config_json` | ✅ 无需改 |
| `system_setting` | 已是 key-value | ✅ 无需改 |
| `sku.options` / `orders.address` | 已是 JSONB | ✅ 无需改 |
| `audit_event.detail` | VARCHAR(1024) | ⬜ 改为 `detail JSONB`（事件类型不同，字段差异大） |

保留普通列的判断标准（不要全塞 JSON）：

1. 需要索引/WHERE/ORDER BY（如 `type`、`status`、`created_at`、`channel`）
2. 参与外键关联
3. 金额汇总与对账必需（`gross_amount`/`payable_amount` 等）


## 8. 图片地址：快照存绝对 URL，不存相对路径（2026-10-19）

原因（用户明确）：

1. **域名会变**；
2. **小程序/客户端要求 `http(s)://` 绝对地址**；
3. **存储商会变、存储方案会整体迁移**（测试期本地 → 上线云存储）。

实现 `platform/media/MediaUrlResolver`：

| 输入 | 输出 |
|---|---|
| `https://www.aaa.com/bbb.jpg` | 原样保留（历史快照指向当时的存储/CDN） |
| `images/aaa.jpg` 或 `/images/aaa.jpg` | `{app.media.public-base-url}/images/aaa.jpg` |
| `//cdn.example.com/a.jpg` | `https://cdn.example.com/a.jpg` |
| `../images/aaa.jpg` | **拒绝**（路径穿越） |
| 相对路径但未配置基址 | **拒绝**（不允许把相对路径写进快照） |

配置项：`app.media.public-base-url`（环境变量 `MEDIA_PUBLIC_BASE_URL`），
测试期 `http://localhost:8080/media`，上线改 CDN/云存储域名，**代码不动**。

写入点：`OrderService` 生成订单行快照时调用 `mediaUrls.absolute(sku.imageUrl())`，
因此快照里永远是绝对地址。
