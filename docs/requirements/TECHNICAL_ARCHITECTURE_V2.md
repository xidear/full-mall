# Full Mall 技术架构 v2

> 版本：2.0
> 日期：2026-10-19
> 关联：`requirements/PRODUCT_REQUIREMENTS_V2.md`、`DEVELOPMENT_PLAN_V2.md`

## 1. 架构结论

### 1.1 不整体重写现有系统

现有后端是 Spring Boot + Spring Modulith + JdbcTemplate，已经承载库存、订单、POS、促销等强一致性逻辑。**不建议为了“模型继承”一次性改成全 ORM**，风险高、回归面大。

采用**混合架构**：

- **模型/CRUD 密集领域**（商品后台、供应商、素材库、CMS、结算单、采购单、排班、功能开关、客服知识库）：Spring Data JPA/Hibernate + MapStruct + 基础控制器。
- **高并发/强一致热路径**（库存预占、POS、促销名额、支付状态机）：保留 JdbcTemplate/显式 SQL，但复用同一字段权限、行级范围、审计组件。
- **迁移方式**：新模块直接使用新范式；存量模块按领域迁移，迁移前后用契约测试对比响应/副作用。

### 1.2 “一个模型定义”的落地含义

ThinkPHP 的模型并不负责建表，表结构仍来自 migration；Java 中同样建议：

- **Flyway = 生产 schema 的唯一迁移来源**；
- **JPA 实体/模型元数据 = 字段、类型、可读/可写、审计、DTO、OpenAPI 的唯一代码来源**；
- 启动时 `hibernate.ddl-auto=validate`，实体与 Flyway schema 不一致直接启动失败；
- 禁止 `ddl-auto=update` 上生产。

如果坚持“实体自动建表”作为单一定义，可用 Hibernate schema export 生成迁移候选，但生产仍要人工审查后落 Flyway；否则无法安全处理数据回填、索引、在线变更。

## 2. 模型层设计

### 2.1 基础模型

```java
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    private UUID id;

    @Version
    private long version;

    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant deletedAt;
}
```

约定：

- 所有业务表有 `id/version/created_at/updated_at/created_by/updated_by/deleted_at`。
- 软删除用 `deleted_at`；默认查询过滤 `deleted_at is null`。
- 乐观锁用 `@Version`；冲突返回 409，不覆盖。
- 金额用 `long` + `currency`，禁止 `BigDecimal` 浮点运算或 `double`。
- 枚举存字符串，状态列至少 `VARCHAR(32)`。

### 2.2 字段可见性/可写性

不要只用 `@JsonView`，它只控制序列化，不阻止写入。采用三层：

1. **输出 DTO 投影**：每个角色/域有独立 DTO；供应商 DTO 不包含成本、毛利、内部备注、风控字段。
2. **输入 allowlist**：`FieldAccessPolicy` 根据角色/域/记录状态返回可写字段集合；请求中出现不可写字段直接 400/403。
3. **持久化保护**：实体字段 `@Column(updatable=false)` 或 Service 层显式更新白名单；禁止 `BeanUtils.copyProperties` 全量覆盖。

```java
public interface FieldAccessPolicy<E> {
    Set<String> readable(Principal p, E entity);
    Set<String> writable(Principal p, E entity);
    default void assertWritable(Principal p, E entity, Set<String> requested) {
        Set<String> denied = new HashSet<>(requested);
        denied.removeAll(writable(p, entity));
        if (!denied.isEmpty()) {
            throw new ForbiddenFieldException(denied); // 明确报错，不静默丢弃
        }
    }
}
```

供应商策略示例：

| 字段 | 供应商可见 | 供应商可写 |
|---|---|---|
| 商品标题/图片/规格 | 是 | 否（由平台维护） |
| 供货价/库存/交期 | 是 | 是 |
| 建议零售价 | 是 | 否 |
| 成本/毛利/内部备注 | 否 | 否 |
| 订单收货人 PII | 脱敏 | 否 |
| 发货承运商/运单号 | 是 | 是 |
| 结算金额/状态 | 是 | 否 |

### 2.3 行级范围

- Hibernate `@FilterDef/@Filter` 或 `@TenantId` 做供应商/门店/客服范围；
- 复杂查询用 `Specification` 强制注入范围；
- 仓储层提供 `findScoped(principal, id)`，禁止裸 `findById` 用于外部接口；
- 越权返回 404 或 403，按安全策略统一；
- 所有外部接口必须有负向测试：供应商 A 读/写供应商 B。

### 2.4 基础仓储/服务

```java
public abstract class BaseScopedService<E extends BaseEntity, R extends JpaRepository<E, UUID>> {
    protected abstract Scope scope(Principal principal);
    protected abstract FieldAccessPolicy<E> fieldPolicy();

    public E getScoped(Principal p, UUID id) { ... }
    public E create(Principal p, CreateCommand cmd) { ... }
    public E update(Principal p, UUID id, UpdateCommand cmd) { ... }
    public void softDelete(Principal p, UUID id) { ... }
}
```

## 3. 基础控制器

```java
public abstract class BaseCrudController<E extends BaseEntity, DTO, CREATE, UPDATE> {

    protected abstract BaseScopedService<E, ?> service();
    protected abstract DTO toDto(E entity, Principal principal);
    protected abstract E fromCreate(CREATE request, Principal principal);
    protected abstract void applyUpdate(E entity, UPDATE request, Principal principal);

    @GetMapping
    public Page<DTO> list(@Valid Query query, Principal principal) { ... }

    @GetMapping("/{id}")
    public DTO get(@PathVariable UUID id, Principal principal) { ... }

    @PostMapping
    @ResponseStatus(CREATED)
    public DTO create(@Valid @RequestBody CREATE request, Principal principal) { ... }

    @PatchMapping("/{id}")
    public DTO update(@PathVariable UUID id, @Valid @RequestBody UPDATE request, Principal principal) { ... }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable UUID id, Principal principal) { ... }
}
```

供应商、Admin、门店可以继承同一基类，只实现：

- `scope()`：供应商 id / 门店 id / 客服组；
- `fieldPolicy()`：字段可见/可写；
- `toDto()`：角色 DTO；
- 专有动作（接单、发货、审批）作为子类方法。

**禁止**：Controller 直接拼 SQL；Controller 直接返回实体；Controller 自己实现分页/审计/权限。

## 4. 供应商 API 与门户复用

### 4.1 同一套 API

```text
供应商 Web / 供应商自建客户端 / 平台主动推送
                    │
              /open/v1（唯一业务入口）
                    │
      SupplierApiController -> SupplierScopedService -> Repository
```

- 供应商门户通过登录换取短期 token，仍调用 `/open/v1`；
- 门户 BFF 只做会话、CSRF、静态资源，不复制业务逻辑；
- 平台主动通知走同一套事件/待办 API：`GET /open/v1/todos` + `WSS /open/v1/stream`；
- 关闭供应商开关时，API/门户/登录/回调统一拒绝。

### 4.2 认证

生产建议同时启用：

- Bearer token（哈希存储、可轮换、可吊销、过期时间）；
- IP allowlist（CIDR）；
- HMAC 签名（服务端到服务端）；
- 限流 + nonce + 重放检测 + 审计。

风险提示：

- 只启用 token 或只启用 IP 时，后台显示“安全性未满配”；
- 连续失败、异常 IP、批量拉取、结算差异触发告警；
- token 轮换支持双 token 重叠期。

## 5. API 版本化与文档

### 5.1 版本策略

- 路径版本：`/open/v1`、`/open/v2`；
- 同一大版本内只做向后兼容新增；
- 破坏性变更升大版本，旧版本至少保留一个迁移周期；
- 响应头 `Deprecation`、`Sunset`、`Link: <docs>; rel="deprecation"`。

### 5.2 文档自动更新

- 代码注解/OpenAPI 生成 `docs/openapi/open-v1.yaml`；
- CI 校验生成文件与提交一致；
- 文档站点按版本发布，包含字段、错误码、签名示例、CSV 模板、Webhook 事件、Changelog；
- 契约测试：每个版本的关键请求/响应/错误码有测试；
- 供应商沙箱使用固定 Mock 凭证与可重放 Webhook。

## 6. 功能开关

### 6.1 开关模型

```text
feature_flag(code, enabled, scope, payload_json, updated_by, updated_at, reason)
```

开关：

- `supplier.enabled`
- `pos.sale.enabled`
- `pos.pickup.enabled`
- `site.status`（OPEN/MAINTENANCE/CLOSED）
- `payment.provider`、`ai.provider` 等配置型开关

### 6.2 执行点

- 服务端 Filter/Interceptor + Service 双保险；
- 供应商：`/open/v1/**`、`/supplier/**`、供应商登录；
- POS：卖货/自提分别校验；
- 站点关闭：只拦截顾客前台 API 和页面；`/admin/**`、支付/退款回调、健康检查、内部任务放行；
- 缓存 TTL ≤ 30s；变更审计；前端显示维护页/禁用提示。

## 7. 客服实时架构

- WebSocket 网关独立 profile；连接鉴权 ticket（短期、一次性）。
- 协议：`hello/ack/ping/message/read/typing/close/transfer`，每会话 seq，客户端 ack；断线按 seq 补拉。
- 消息持久化后推送；推送失败不影响落库。
- 路由：技能组/语言/班次/在线负载；离线自动转单；无人在线转 Bot/排队。
- Bot SPI：`MockBotEngine`（demo）、`AiBotEngine`（prod，缺配置启动失败）。
- RAG：文档分块、向量库（pgvector/OpenSearch）、引用、权限过滤。
- 附件：对象存储 + 签名 URL + 病毒扫描；消息只存元数据。
- SLA/质检：首响/解决时长、超时升级、抽检、评分、AI 摘要。

## 8. 促销引擎

### 8.1 规则模型

`promotion` + `promotion_rule` + `promotion_scope` + `promotion_channel` + `promotion_usage`。

- 条件：时间、渠道、市场、商品/类目/品牌、数量、金额、会员等级、首单、地区。
- 动作：减额、折扣、赠品、包邮、改价、积分倍率。
- 互斥组/叠加组/优先级/每人每单限制。
- 计算：候选规则 → 过滤 → 排序 → 组合（叠加/互斥）→ 最优价 → 明细。
- 所有计算服务端完成；前端只展示。

### 8.2 渠道隔离

- `channel = ONLINE | POS | PICKUP`；
- 线上活动不默认进 POS；
- POS 自提只核销已支付订单，不重算优惠；
- POS 卖货使用独立活动集合；
- 后台活动表单必须选择渠道，默认不跨渠道。

## 9. 支付架构

- `PaymentConnector` SPI：`create/confirm/capture/refund/query/webhook/reconcile`。
- 适配器模块：`payment-wechat`、`payment-alipay`、`payment-unionpay`、`payment-aggregator`；厂商 SDK 只在适配器内。
- 凭证加密：AES-GCM/KMS，后台掩码显示，轮换审计；生产缺配置 fail-fast。
- Webhook inbox 幂等；重复/乱序；退款/部分退款；对账文件解析与差异。
- 演示 Mock；生产禁止回退 Mock。

## 10. ERP / 门店

### 10.1 自营 ERP

- 采购单 → 到货预约 → 收货 → 质检 → 上架批次 → 库存可用。
- 盘点单 → 差异 → 审批 → 调整流水。
- 报损单 → 原因/批次/成本 → 审批 → 报损流水。
- 售后换货 → 收货/质检 → 补发履约单 → 库存预占/扣减。
- 不做多级采购审批；不做复杂 MRP。
- 供应商侧只做轻量协同，不要求供应商使用采购/质检。

### 10.2 门店

- 店长改价/作废授权：收银员发起，店长 PIN/账号审批，记录原因与审计。
- 交接班审核：现金差异、未结事项、店长确认。
- 排班：门店/岗位/班次/调班，关联收银开班。
- 线下扫码/碰一碰：`PosPaymentProvider` Mock（demo）；生产缺终端/商户配置明确报错。

## 11. 多语言、多币种

- Nuxt I18n + Vue I18n + ICU MessageFormat；语言包按应用拆分。
- RTL：`dir=rtl` + CSS logical properties（`margin-inline`、`padding-inline`、`inset-inline`），避免左右写死。
- 翻译流程：源文案 → AI 翻译 → 人工校对 → 发布；术语表/翻译记忆；审校状态。
- 法律/隐私/支付文案强制人工审校。
- 多币种：价格表按 market/currency；汇率快照；舍入规则；前台显示当地币种，后台仍以最小单位存储。
- 港澳台支付/物流适配器可先代码 + Mock；生产缺 AK/token 明确报错。

## 12. 素材库与云存储

### 12.1 元数据

`asset(id, owner_type, owner_id, folder_id, kind, mime, size, hash, storage_provider, object_key, url, status, deleted_at, created_by, ...)`

- 公共库/个人库；
- 文件夹/标签/搜索；
- 引用计数/引用表；
- 软删除/恢复/彻底删除（仅管理员，引用为 0）；
- 替换素材保留历史版本。

### 12.2 存储 SPI

```java
interface ObjectStorage {
    StoredObject put(InputStream in, ObjectMetadata meta);
    SignedUrl signGet(String key, Duration ttl);
    void delete(String key);
    boolean exists(String key);
}
```

实现：Local、SeaweedFS/S3、Qiniu、Aliyun OSS。

切换服务商：

1. 新建目标存储配置；
2. 迁移任务复制对象并校验 hash；
3. 元数据双写/双读；
4. 切换读路径；
5. 保留旧存储只读一个周期；
6. 回滚开关；
7. 旧资源不可用时明确告警，不返回空 URL。

## 13. 数据权限、审计、风控

- 统一 `Principal`：角色、用户、供应商、门店、客服组、语言、市场。
- `ScopeResolver`：供应商 id、门店 ids、客服组、运营权限。
- `FieldAccessPolicy`：字段级。
- 审计：谁、何时、对哪个对象、哪些字段、旧值/新值、请求 id、IP/UA。
- 导出：字段脱敏、范围、限流、文件过期、下载审计。
- 风险引擎：规则 + 阈值 + 告警渠道；不阻塞主流程的异步评估，关键风险可阻断。

## 14. 迁移路线

1. 先建公共层：`BaseEntity`、`FieldAccessPolicy`、`ScopeResolver`、`BaseCrudController`、审计、功能开关。
2. 新模块（供应商结算、素材库、采购/质检、排班、知识库）直接使用新范式。
3. 存量模块先接字段/范围策略，不急着换 ORM。
4. 每个领域迁移前写契约测试，迁移后对比响应、错误码、审计、副作用。
5. 禁止“大爆炸”重写；每次迁移可独立回滚。

## 15. 关键决策记录

| 决策 | 选择 | 理由 |
|---|---|---|
| ORM | JPA/Hibernate 用于 CRUD 密集领域，JdbcTemplate 保留热路径 | 满足模型复用，同时不牺牲库存/促销并发正确性 |
| 字段权限 | DTO 投影 + 输入 allowlist + 持久化保护 | `@JsonView` 只做输出，不能防写入 |
| schema | Flyway 为生产唯一迁移源，实体 validate | 避免 `ddl-auto=update` 的不可控变更 |
| 供应商门户 | 调 `/open/v1`，不建内部 API | 一套契约、一套权限、一套文档 |
| 渠道促销 | ONLINE / POS / PICKUP 分离 | 避免线上优惠在门店二次计算 |
| 站点关闭 | 只拦顾客前台，不拦支付回调 | 维护时仍能完成支付/退款 |
| 素材删除 | 软删除 + 引用检查 | 防止线上页面断图 |
| 存储切换 | 迁移 + 双读 + 回滚 | 多云/本地切换不断链 |
| 生产 Mock | 禁止静默回退，缺配置 fail-fast | 避免“演示成功、生产假成功” |
