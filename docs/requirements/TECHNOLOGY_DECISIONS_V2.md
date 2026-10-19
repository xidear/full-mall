# 技术选型与成熟方案 v2

> 版本：2.0
> 日期：2026-10-19
> 目的：所有需求先调研成熟 Java/开源方案，禁止按“土方法”默认自研。
> 结论会随 spike 更新；选型变更必须写 ADR。

## 0. 选型原则

1. 先标准库/官方 SDK，再成熟开源，最后才自研。
2. 评估：维护活跃度、许可证与 PolyForm Noncommercial 的兼容性、安全记录、社区、与现有 Spring/Java 栈契合度。
3. 不因为“ThinkPHP 这样做”就在 Java 里复刻；Java 生态有更成熟的组合。
4. **字段-角色可见性不是硬性要求**；只有 ERP 等确有业务需要时才启用，且用成熟策略引擎 + DTO 投影，不自建字段权限矩阵。
5. 每个选择记录：候选、结论、原因、许可证、官方文档、待验证项。

## 1. 核心结论

### 1.1 权限与可见性

| 层次 | 成熟方案 | 结论 |
|---|---|---|
| 登录/会话/角色 | Spring Security 6 + 可选 Keycloak | 采用 Spring Security；需要 SSO/集中身份时接 Keycloak |
| 操作级权限 | Spring Security `@PreAuthorize` / `AuthorizationManager` | 采用；官方支持方法级授权 |
| 行级范围（供应商/门店/客服） | Spring Data JPA `Specification`、Hibernate `@Filter`/`@TenantId`；JdbcTemplate 统一 scope SQL | 采用；不要在每个 Controller 手写 where |
| 字段级可见性 | **没有单一成熟库可自动完成 ORM+JSON+UI 全链路**；成熟组合是 DTO 投影 + 策略引擎（OPA/Casbin） | 字段可见性非硬性要求；需要时用 OPA 或 Casbin，不自建字段矩阵 |
| 对象级权限 | Spring Security ACL / `PermissionEvaluator` | 需要按单条记录授权时采用 |
| 数据平台列脱敏 | Apache Ranger | 仅数据平台/Hive 场景；不用于应用 CRUD |

**明确结论：**

- Spring Security 官方只提供请求级/方法级/对象级授权，**不原生支持字段级 ABAC**。
- Keycloak Authorization Services 提供资源/作用域/策略，**不提供自动字段脱敏**。
- OPA 是 CNCF 毕业项目，Rego 策略 + REST 决策 API；可返回“允许字段/脱敏值”等任意决策对象，但需要应用执行。
- Casbin/jCasbin 是成熟嵌入式授权库，支持 ACL/RBAC/ABAC；字段级需要自定义 ABAC 模型，仍比自建权限表成熟。
- 因此采用“**Spring Security + JPA 行级范围 + DTO 投影 + 可选 OPA/Casbin**”，而不是自研一套字段-角色矩阵。

### 1.2 ERP 角色可见性

参考成熟 ERP 的产品做法，而不是复制代码：

- **Odoo**：字段可用 `groups=` 限制可见/可写；记录规则（record rules）做行级；界面按组隐藏字段。
- **ERPNext/Frappe**：角色权限 + permlevel（字段级权限级别）+ 用户权限（行级）。
- **Apache OFBiz**：Java ERP，安全组/权限/屏幕组件；可作为 Java 领域参考，但不整体引入。

Full Mall 的落地：

- 默认只做**页面/操作/行级**权限；
- 仅当 ERP 明确需要（如成本、毛利、薪资、供应商底价）时，启用字段级可见性；
- 字段级通过 **OPA 或 Casbin 策略 + DTO 投影**实现；
- 不做“每张表一套字段-角色配置”的自研后台，除非策略引擎无法满足并有 ADR。

### 1.3 模型与 ORM

| 方案 | 结论 |
|---|---|
| Spring Data JPA + Hibernate 6 | 新 CRUD/ERP 模块采用；`@Version`、`@Filter`、`@TenantId`、审计 |
| MyBatis-Plus | SQL 可控、租户插件、逻辑删除；若团队偏好 SQL 可选，但不再混第三套 |
| 现有 JdbcTemplate | 库存/POS/促销等热路径保留；统一 scope/审计组件 |
| Flyway | 生产 schema 唯一迁移源；Hibernate `ddl-auto=validate` |
| MapStruct | DTO 映射；禁止 `BeanUtils.copyProperties` 全量覆盖 |
| JaVers | 审计/版本对比（Apache-2.0）；替代自研 diff |
| Hibernate Envers | 可选；LGPL，需评估许可证兼容 |

**不采用**：Hibernate `ddl-auto=update` 上生产、全量 ORM 重写、为字段权限自建元数据框架。

### 1.4 规则/流程/开关

| 领域 | 候选 | 建议 |
|---|---|---|
| 促销/风控规则 | Drools（Apache-2.0）、Easy Rules（MIT） | 复杂叠加/最优价用 Drools；简单规则用 Easy Rules；先 spike，不自研表达式解析器 |
| 盘点/报损/交接审批 | Flowable（Apache-2.0） | 采用 BPMN 流程；采购审批不做 |
| 定时任务集群 | ShedLock + Spring Scheduling | 采用；避免多实例重复执行 |
| 功能开关 | Unleash（Apache-2.0）、FF4j（Apache-2.0） | 需要集中 UI/灰度用 Unleash；嵌入式轻量用 FF4j；不自研开关表 |
| 风险预警 | OPA/Casbin + 规则引擎 | 规则与授权共用策略层；阈值告警走通知 |

### 1.5 客服 / AI

| 领域 | 候选 | 建议 |
|---|---|---|
| WebSocket | Spring WebSocket/Spring Messaging | 采用；seq/ack/补拉自行定义协议 |
| AI 编排 | Spring AI（Apache-2.0）、LangChain4j（Apache-2.0） | 采用其一；Spring AI 与 Spring Boot 集成更直接 |
| 向量库 | pgvector、OpenSearch k-NN | 已有 Postgres/OpenSearch，优先 pgvector；规模大再 OpenSearch |
| 文档解析 | Apache Tika | 采用；不自写 PDF/Office 解析 |
| 附件安全 | ClamAV、Apache Tika MIME | 采用；病毒扫描 + MIME 校验 |
| 客服产品参考 | Chatwoot、Zammad | 只借鉴会话/分组/SLA，不复制代码 |

### 1.6 多语言 / 翻译

| 领域 | 候选 | 建议 |
|---|---|---|
| 前端 i18n | Nuxt I18n / Vue I18n（MIT）、ICU MessageFormat | 采用 |
| RTL | CSS logical properties + `dir=rtl` | 采用；不自写 RTL 引擎 |
| 翻译管理 | Tolgee（Apache-2.0）、Weblate（GPL，独立服务） | 优先 Tolgee；Weblate 作为可选独立服务，避免许可证污染 |
| 机器翻译 | Tolgee/Weblate 内置 provider 或 AI provider | AI 翻译 + 人工校对；法律文案强制人工 |

### 1.7 存储 / 素材库

| 领域 | 候选 | 建议 |
|---|---|---|
| 对象存储 | AWS SDK v2 S3（兼容 MinIO/SeaweedFS）、七牛官方 SDK、阿里云 OSS SDK | 采用官方 SDK；不手写签名 |
| 图片处理 | imgproxy / Thumbor | 采用；不自写缩略图/裁剪 |
| 文件类型 | Apache Tika | 采用 |
| 病毒扫描 | ClamAV | 采用 |
| 素材库产品参考 | ResourceSpace、Pimcore | 借鉴公共/个人库、引用、软删除；不整体引入 |

### 1.8 API / 安全 / 文档

| 领域 | 候选 | 建议 |
|---|---|---|
| API 文档 | springdoc-openapi + OpenAPI Generator + Redoc | 采用；版本化生成 |
| 契约测试 | Spring Cloud Contract / OpenAPI diff | 采用；CI 校验 |
| 限流 | Bucket4j + Valkey | 采用 |
| 幂等/重试 | Spring Retry / Resilience4j + RabbitMQ 延迟队列 | 采用；不自写重试循环 |
| 审计 | JaVers | 采用 |
| 密码/密钥 | Spring Security Crypto、AES-GCM、KMS/secret manager | 采用；不自写加密 |
| 供应商签名 | JDK `Mac` HMAC-SHA256 + constant-time compare | 标准库即可 |
| 风险预警 | OPA/Casbin + 规则引擎 + 通知 | 策略复用 |
| CSV | Apache Commons CSV / OpenCSV | 采用；配 Hibernate Validator |
| 金额/币种 | 现有 `Money`（分）；复杂多币种评估 JSR 354 Moneta | 先保留；不自写汇率/舍入表 |

### 1.9 支付

| 领域 | 候选 | 建议 |
|---|---|---|
| 微信支付 | 微信支付官方 Java SDK | 适配器内使用 |
| 支付宝 | 支付宝官方 Java SDK | 适配器内使用 |
| 银联 | 银联官方 SDK/签名规范 | 适配器内使用 |
| 聚合收单 | 服务商官方 SDK | 适配器内使用 |
| 对账 | Apache POI/Commons CSV | 采用；不自写 Excel 解析 |

### 1.10 前端/测试

| 领域 | 候选 | 建议 |
|---|---|---|
| 前端框架 | Vue 3/Nuxt 3 | 已采用 |
| 组件库 | Element Plus | 已采用 |
| E2E | Playwright | 已采用 |
| 前端状态 | Pinia/Nuxt composables | 已采用 |
| 主题 | 现有 CSS variables | 保留 |

## 2. 研究记录与官方来源

| 主题 | 官方来源 | 结论 |
|---|---|---|
| Spring Security 方法级授权 | https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html | 支持方法/对象级；**不原生支持字段级 ABAC** |
| Spring Data JPA Specifications | https://docs.spring.io/spring-data/jpa/reference/jpa/specifications.html | 行级范围/动态查询 |
| Hibernate Filters | https://docs.hibernate.org/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#filters | 行级过滤；需启用 filter |
| Hibernate @TenantId | https://docs.hibernate.org/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#multi-tenancy-discriminator | 多租户判别列 |
| Keycloak Authorization Services | https://www.keycloak.org/docs/latest/authorization_services/index.html | 资源/作用域/策略；**不提供自动字段脱敏** |
| OPA | https://www.openpolicyagent.org/ | CNCF 毕业；Rego + 决策 API；可返回允许字段 |
| Casbin / jCasbin | https://casbin.apache.org/ 、https://github.com/casbin/jcasbin | 嵌入式 RBAC/ABAC；字段级需自定义模型 |
| Cerbos | https://docs.cerbos.dev/ | 策略引擎/Java SDK 候选；需 spike 字段输出能力 |
| Apache Ranger | https://ranger.apache.org/ | 数据平台列脱敏/行过滤；不适合应用 CRUD |
| Odoo Security | https://www.odoo.com/documentation/17.0/developer/reference/backend/security.html | ERP 字段 groups/记录规则参考 |
| ERPNext Permissions | https://docs.erpnext.com/ | 角色 + permlevel 参考 |
| Apache OFBiz | https://ofbiz.apache.org/ | Java ERP 安全组参考 |
| Flowable | https://www.flowable.org/ | BPMN 审批 |
| Drools | https://www.drools.org/ | 规则引擎 |
| Easy Rules | https://github.com/j-easy/easy-rules | 轻量规则 |
| Unleash | https://www.getunleash.io/ | 功能开关/灰度 |
| FF4j | https://ff4j.github.io/ | Java 嵌入式开关 |
| Spring AI | https://docs.spring.io/spring-ai/reference/ | AI 编排 |
| LangChain4j | https://docs.langchain4j.dev/ | AI 编排候选 |
| Apache Tika | https://tika.apache.org/ | 文档/MIME |
| ClamAV | https://www.clamav.net/ | 病毒扫描 |
| imgproxy | https://imgproxy.net/ | 图片处理 |
| Tolgee | https://tolgee.io/ | 翻译管理 |
| Weblate | https://weblate.org/ | 翻译管理（GPL，独立服务） |
| springdoc-openapi | https://springdoc.org/ | OpenAPI 生成 |
| OpenAPI Generator | https://openapi-generator.tech/ | 客户端/文档生成 |
| Resilience4j | https://resilience4j.readme.io/ | 重试/熔断 |
| JaVers | https://javers.org/ | 审计/版本 |
| Apache Commons CSV | https://commons.apache.org/proper/commons-csv/ | CSV |
| Bucket4j | https://bucket4j.com/ | 限流 |
| AWS SDK for Java v2 | https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html | S3 兼容存储 |
| 七牛 Java SDK | https://developer.qiniu.com/kodo/sdk/java | 七牛 |
| 阿里云 OSS Java SDK | https://help.aliyun.com/zh/oss/developer-reference/java/ | 阿里云 |

## 3. 待 spike（P0/P1）

1. **OPA vs Casbin**：字段级/ERP 角色可见性；比较部署成本、Java 集成、策略可测试性、返回字段能力。默认 OPA（集中策略），嵌入式场景 Casbin。
2. **Drools vs Easy Rules**：促销叠加/最优价；以可维护性和性能为准，不自研表达式解析器。
3. **Unleash vs FF4j**：集中 UI/灰度 vs 嵌入式；默认 Unleash，单机轻量用 FF4j。
4. **Tolgee vs Weblate**：许可证/部署/翻译流程；优先 Tolgee。
5. **Spring AI vs LangChain4j**：模型供应商适配、RAG、工具调用；默认 Spring AI。
6. **JPA 迁移范围**：先素材库/供应商结算/采购/排班试点；库存/POS 不迁移。
7. **Ranger 是否需要**：只有数据平台列脱敏需求时才引入，应用层不用。
8. **Moneta**：多币种舍入复杂时评估 JSR 354；当前 `Money` 保留。

## 3.1 已结论：功能开关（P0，2026-10-19）

**结论**：P0 采用「平台自有 `feature_flag` 表 + 30s 缓存 + 服务端强制过滤器」的薄封装，不引入开关中间件。
理由与后续升级路径记录在案，避免被当作“自研框架”。

| 候选 | 许可证 | 实测（Maven Central，2026-10-19） | 采纳情况 |
|---|---|---|---|
| FF4j | Apache-2.0 | `ff4j-spring-boot-starter` 最新 1.9，lastUpdated **2022-12**，面向 Spring Boot 2.x | 不采纳：对 Boot 3.4 过旧 |
| Togglz | Apache-2.0 | `togglz-spring-boot-starter` 4.6.4，lastUpdated **2026-08** | 备选：维护活跃，若开关数量增长/需要灰度时切换 |
| Unleash | Apache-2.0 | `unleash-client-java` 12.3.0，lastUpdated **2026-08** | 不采纳（P0）：需额外部署 Unleash Server，4 个开关不划算 |
| Spring Boot `@ConditionalOnProperty` | Apache-2.0 | 内置 | 不采纳：变更需重启，不满足 FLAG-004 实时生效 |

**为什么现在不引入库**：

1. 架构文档（`TECHNICAL_ARCHITECTURE_V2.md` §6.1）已经规定了 `feature_flag(code, enabled, scope, payload, updated_by, updated_at, reason)` 表结构与审计字段，引入开关库会形成第二套存储与真相。
2. P0 只有 3 个布尔开关 + 1 个站点状态，无灰度/分群/A-B 需求；库带来的 UI、策略、灰度能力当前无对应需求。
3. `FeatureFlagService` 是薄接口（`isEnabled(code)` / `update(code, enabled, reason, actor)`），后续切 Togglz 只需替换实现，调用方与过滤器不变。

**升级触发条件**：开关数量 > 20、需要按用户/门店灰度、需要 A/B 实验或外部配置中心托管时，切 Togglz（嵌入式、Apache-2.0、维护活跃）或 Unleash（集中式）。

**站点状态单一真相**：`site.status` 仍存放在 `system_setting.site_status`（V7 已有，后台设置页已在用），`FeatureGuardFilter` 通过 `SystemSettingService.siteStatus()` 读取并缓存 30s；`FEATURE` 表只放布尔开关，避免同一语义两处存储。

## 3.2 已结论：审计（JaVers）与 JPA 基座（P0，2026-10-19）

| 候选 | 结论 |
|---|---|
| JaVers 7.11.9（`javers-core` + `javers-persistence-sql`） | **采纳**，手工装配 |
| JaVers `javers-spring-boot-starter-sql` 7.11.9 | **不采纳**：该版本面向 Spring Boot 4，依赖 `org.springframework.boot.jpa.autoconfigure.JpaProperties`，本项目 Boot 3.4 启动即 `ClassNotFoundException` |
| 自研审计表 | 不采纳：字段级旧值/新值、类型变更、查询 API 都要自己做 |

**实测发现（必须在升级前复查）**：

1. `javers-persistence-sql` **不关闭** `ConnectionProvider` 返回的连接。
   用计数数据源实测：5 次 `commit` 打开 37 个连接、关闭 0 个；接 Hikari(20) 会在 20 次提交后耗尽连接池。
2. 因此 `AuditConfig` **固定复用一条连接**，并由 `AuditTrail` 串行化所有 JaVers 调用（连接数恒为 1）。
   代价是审计写入串行；升级 JaVers 后应改回连接池并重跑 `AssetScopeAndAuditTest`。
3. JaVers 建表使用显式序列 `jv_*_pk_seq`，Flyway 迁移 V24 按此编写；不能用 `BIGSERIAL`（序列名不匹配）。
4. 审计提交与业务事务不在同一事务（JaVers 用自己的连接）。当前策略是“多记不可漏记”，
   强一致场景后续用 outbox。

**JPA 基座**：`spring-boot-starter-data-jpa` + `ddl-auto=validate`。存量 JdbcTemplate 模块不迁移；
新 CRUD 模块（首个试点 `asset`）继承 `BaseEntity` / `BaseScopedService` / `BaseCrudController`，
行级范围由 `ScopeResolver` 统一解析，越权返回 404。

## 3.3 待落地：成本计价与异步导出（2026-10-19）

用户明确指出「进货价不是单一字段，而是长期均价」，且 ERP 需要按角色查看成本/毛利。

- 计价模型：批次实际成本（已有 `inventory_batch.unit_cost_amount`）+ SKU/仓库移动加权平均 + **出库时点 COGS 快照**；
  成熟参照：Moving Average / FIFO / Standard Cost（SAP、Odoo、ERPNext 均支持，本项目取移动加权 + 批次实际成本混合）。
- 列可见性：按角色的 DTO 投影（CR-021 禁止字段矩阵）；组合爆炸时才上 Casbin/OPA。
- 异步导出：`export_job` + `FOR UPDATE SKIP LOCKED` 抢占 + 流式 CSV + 对象存储签名 URL。
- 详见 `docs/ERP_COST_EXPORT_AND_PERFORMANCE_DESIGN.md`。

## 3.4 待落地：多语言媒体与 AI 生成（2026-10-19）

- 语言边界：Admin/供应商固定中文，前台多语言；回退链只处理"未翻译"，不掩盖"翻错"。
- 文案：专业翻译 API（DeepL / Google Cloud Translation / Azure）+ 翻译记忆 + 术语表 + 人工审校状态机；
  审校工作流用 Tolgee/Weblate，不自研翻译后台。
- 媒体：`asset/asset_variant/product_media` 三层，按 locale 取图；**中文图不回退给外语用户**。
- AI 生成：`MediaGenerationProvider` 抽象（图片/视频），生成无文字视觉图 + 本地化叠字渲染，
  避免模型把外文文字画错；生成结果必须人工审核。
- 用量：`ai_usage` 记录 provider/model/单位/单价快照/幂等键，月度与供应商账单对账。
- 详见 `docs/I18N_MEDIA_AI_DESIGN.md`。

## 4. 禁止事项

- 禁止自研字段-角色权限矩阵作为默认方案。
- 禁止用 `@JsonView` 代替输入校验；输出裁剪不等于安全。
- 禁止在 Controller 手写行级 where；必须走统一 scope。
- 禁止全量 `BeanUtils.copyProperties` 覆盖实体。
- 禁止自研 PDF/Office 解析、图片处理、病毒扫描、汇率表、加密算法。
- 禁止生产 `ddl-auto=update`。
- 禁止生产缺配置回退 Mock。
- 禁止把“ThinkPHP 的做法”直接翻译成 Java 自研框架；先找成熟替代。
