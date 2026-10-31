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
| 附件安全 | Apache Tika MIME（已实施）、ClamAV（未实施） | Tika 嗅探已用于上传类型校验；**ClamAV 尚未接入**（代码与 compose 中都没有），接入前不得对外宣称有病毒扫描 |
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
| 病毒扫描 | ClamAV | **未实施**：compose 无 clamd、服务端无调用；上传目前只做类型/体积校验。要接入需加 clamd 容器 + 扫描适配器 + 超时/失败策略 |
| 素材库产品参考 | ResourceSpace、Pimcore | 借鉴公共/个人库、引用、软删除；不整体引入 |
| 上传类型判定 | Apache Tika `Tika.detect(byte[])`、手写魔术字节、ImageIO 解码探测 | 采用 Tika：已在依赖里（文档解析用同一套），只读文件头即可判型，避免为上传再引入一套 |
| 浏览器端 HEIC 转码 | heic2any（MIT，2.7MB wasm）、heic-to（LGPL-3.0，26MB）、libheif-js（LGPL-3.0）、后端 libheif JNI（imageio-heif） | 采用 heic2any：MIT 许可证干净、体积可控、浏览器端完成转码不增加容器原生依赖；heic-to 虽活跃但 LGPL + 26MB，后端 JNI 方案在容器里维护成本高 |

### 1.7.1 上传类型与体积校验（2026-10-29 落地）

结论与实现位置：

- **类型以 magic bytes 为准**：`MediaUploadValidator` 用 Tika 嗅探，客户端 Content-Type 只做一致性核对，落库扩展名取自嗅探结果。
- **白名单与默认上限的唯一来源**是 `docs/validation.schema.json` 的 `$defs.assetUpload`，由 `npm run generate:validation` 生成前端常量，Java 侧 `ValidationRules` 由 `ValidationRulesContractTest` 守护。
- **上限按上传者区分**：管理端（商品/素材）默认图片 5MB、视频 10MB；顾客端（评价）默认图片 2MB、视频 5MB。运行时值存 `system_setting`，后台「系统设置 → 素材上传」图形化修改，下一次上传即生效。
- **SVG 不在白名单**：SVG 是 XML 文档，直出同源会带来脚本执行风险；且无法栅格化做缩略图。HEIC/HEIF 可识别但必须先在浏览器转 JPEG（服务端返回 `ASSET_HEIC_NOT_CONVERTED`，前端自动转码）。
- **multipart 天花板**：Spring Boot 默认单文件 1MB，比业务上限小得多，必须显式配置并与 `ValidationRules.UPLOAD_MULTIPART_CEILING_BYTES` 一致，由 `UploadMultipartGuard` 启动自检。

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
| province-city-china（GB/T 2260 区划） | https://www.npmjs.com/package/province-city-china | 中国省市区字典**默认来源**（MIT，2026-10-11 有发布，活跃） |
| china-division（五级区划） | https://github.com/modood/Administrative-divisions-of-China | 备选；数据最全（五级）。package.json 写 MIT、**tarball 内 LICENSE 原文实测 WTFPL**，且发布者 = 版权人本人 ⇒ 属元数据写错，**取 WTFPL 口径即可合法使用，fork 无需也不能**（ADR-0013 §许可证性质）。数据截止 2023-06-30，比默认来源老约 3 年 |
| world-countries（mledoze/countries） | https://github.com/mledoze/countries | **降为备选**：ODbL-1.0 是本议题唯一带 share-alike（类传染）义务的许可，闭源交付避开；LICENSE 原文本次未取到 |
| i18n-iso-countries | https://github.com/michaelwittig/node-i18n-iso-countries | **国家层默认来源**（MIT 已验证，无 share-alike） |
| GeoNames dump | https://download.geonames.org/export/dump/ | 备选（市级以下/邮编/经纬度）；许可据二手资料为 CC-BY 4.0，**官方页未核实** |
| nv-i18n（Java） | https://search.maven.org/artifact/com.neovisionaries/nv-i18n | Maven Central 存在 1.29；国家/语言码枚举辅助，**不是区划数据集** |
| Maven Central 中国行政区划数据集 | https://search.maven.org | **检索 0 命中**：Java 生态无现成区划 artifact，故走「npm 数据包 → 规范化快照 → Flyway 灌库」（见 ADR-0013） |
| libaddressinput（google） | https://github.com/google/libaddressinput | **采用其模型与词汇**（Apache-2.0、633★、驱动 Android/Chromium）：地址表单由**国别元数据**驱动（require/fmt/zip/sub_keys），不是固定省市区三级。⚠️ 其元数据**不在仓库内**，须从 ADS 在线取 ⇒ 本项目必须转成自有快照，运行期不得访问 Google（ADR-0013 §8.5） |
| libpostal（openvenues） | https://github.com/openvenues/libpostal | 暂不采用：C 库 + 统计 NLP 模型（需额外原生依赖与模型下载），违反 §12「不得要求客户手工装东西」；地址解析需求现阶段用 address_format 正则 + 可选三方 provider 覆盖 |

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

## 3.5 已决策：框架版本、实时通道与审计连接（2026-10-19）

- **Spring Boot 升级到 4.1.1（GA）**：系统未上线，不做兼容；原 3.4.5 是沿用仓库现状而非评估结论。
  升级需同步 Spring Modulith、Spring Security 7、Hibernate 7，目标是可以直接用 `javers-spring-boot-starter-sql`。
- **轮询 vs WS**：低频（>30s）用轮询（导出进度、报表）；交互式用 WS（客服、待办增量）且**必须断线补拉**。
  异步导出定案为**轮询 + 指数退避**。
- **JaVers 连接问题根因**：`ExternalTransactionState.close()` 显式忽略关闭（"unmanaged ExternalTransaction"），
  `SqlRepositoryBuilder` 无 DataSource 入口 → 连接所有权在调用方，**不是版本 bug、不是配置错**；
  彻底方案是 **outbox + 单消费者审计写入器**（连接数恒为 1，业务线程不再串行）。
- 详见 `docs/SYNC_AND_TECH_STACK_DECISIONS.md`。

## 3.4 待落地：多语言媒体与 AI 生成（2026-10-19）

- 语言边界：Admin/供应商固定中文，前台多语言；回退链只处理"未翻译"，不掩盖"翻错"。
- 文案：专业翻译 API（DeepL / Google Cloud Translation / Azure）+ 翻译记忆 + 术语表 + 人工审校状态机；
  审校工作流用 Tolgee/Weblate，不自研翻译后台。
- 媒体：`asset/asset_variant/product_media` 三层，按 locale 取图；回退目标是
  **系统默认语言（单一配置，当前 zh-CN，出海改 en）**，不是写死英文。
  响应带 `contentSource=SELF|DEFAULT|PLACEHOLDER`，禁止把默认内容伪装成已本地化。
- AI 生成：`MediaGenerationProvider` 抽象（图片/视频），生成无文字视觉图 + 本地化叠字渲染，
  避免模型把外文文字画错；生成结果必须人工审核。
- 用量：`ai_usage` 记录 provider/model/单位/单价快照/幂等键，月度与供应商账单对账。
- 详见 `docs/I18N_MEDIA_AI_DESIGN.md`。

## 3.6 已确认：支付双模式与配置方式（2026-10-19）

- 微信/支付宝**同时支持直连商户模式与服务商模式**。
  **最终口径（2026-10-19 二次纠正）**：服务商是**我方选定的第三方**（可更换，也可不用、直接自己申请商户号）；
  服务商模式下**整个商城就是一个子商户**——我们不是服务商，下面没有子商户，也不是每个门店一个子商户（门店全自营）。
  资金进这个子商户账户，不做平台代收/分账。
- 因为服务商可更换：`payment` 必须记录下单时使用的 `provider_config_id`，**退款按原配置路由**，
  否则换服务商后旧订单退款会被拒或退错主体。
- 适配器上下文必须携带 `mode + configId + subMchId`；服务商模式缺 `sub_mch_id` 直接失败。
- 回调按 `sub_mch_id` 路由；对账按 `sp_mchid + sub_mchid` 拉子商户账单。
- **所有对外 Provider 配置走 Web 图形界面**：每 provider+mode 独立表单、证书文件上传、枚举下拉、
  禁止手写 JSON/粘贴 PEM、保存时强校验 + 连通性测试 + 审计。
- 详见 `docs/PAYMENT_PROVIDER_MODES_AND_CONFIG_UI.md`。

## 3.7 已决策：后台（admin）前端栈与组件复用（2026-10-28）

背景：admin 只有 Vue 3 + Element Plus + vue-router，菜单没有图标、不能折叠，页面各自实现请求状态；
商品页因此出现"裂图、切语言后表单与选择框不一致、编辑页 500 无人发现"等问题（见 docs/ADMIN_DESIGN_REVIEW_2026-10-28.md）。

候选（均为成熟开源方案，MIT）：

| 候选 | 许可证 | 结论 | 理由 |
|---|---|---|---|
| vben-admin / vue-vben-admin | MIT | 不整体引入 | 功能完整，但会引入 antd/主题/权限/多标签一整套约定；当前后台是 Element Plus 风格，迁移成本高 |
| vue-element-admin / vue-pure-admin | MIT | 不整体引入 | 模板成熟，但 Vue 2 血统（前者）或重路由/多页签约定，替换成本大于收益 |
| Element Plus（现用） | MIT | 继续使用 | 与现有页面一致，社区活跃，官方提供 icons 包与 el-sub-menu 折叠 |
| @element-plus/icons-vue | MIT | **采用** | 官方图标包，补齐菜单/操作图标，避免自绘 SVG |
| @vueuse/core | MIT | **采用** | useLocalStorage / useMediaQuery 等组合式工具，避免自写浏览器状态逻辑 |
| Pinia | MIT | 暂不引入 | 当前跨页共享状态只有登录态与设置，reactive 模块足够；出现跨页复杂状态时再引入 |
| @tanstack/vue-query | MIT | 暂不引入 | 请求状态已由 useAsyncState() 统一（含竞态保护）；若需要缓存/失效策略再引入 |

结论：

1. 保留 **Element Plus**，补齐 @element-plus/icons-vue（图标）与 @vueuse/core（组合式工具）；
2. 后台骨架：el-sub-menu 分组 + el-menu collapse + useLocalStorage 持久化折叠状态；
3. 请求状态统一走 composables/useAsyncState.ts（loading / error / data + 过期响应丢弃）；
4. 图片统一走 media.ts#resolveMediaUrl() + components/AdminThumb.vue（失败回落到占位块）；
5. 模块按"任务"而不是"表"划分：译文并入商品编辑页（按启用语言 tab），规格矩阵改为可复用模板（ADR-006）。

## 3.8 已决策：国家与行政区划数据来源（2026-10-29，ADR-0013）

背景：地址现状为纯手填（省市区三个自由文本，无字典表、无校验、无三方服务），地址不可治理。
详见 `docs/adr/0013-address-region-data.md`（含候选、许可证、活跃度、下载量实测与验证状态表）。

| 方案 | 结论 | 理由 |
|---|---|---|
| province-city-china（GB/T 2260） | **采用**：中国省市区默认来源 | MIT 一致、2026-10 仍在更新、直接对齐国标代码 |
| china-division | 备选，暂不采用 | 数据最全（五级）但快照截止 2023-06-30，比默认来源老约 3 年。package.json=MIT 与 tarball 内 LICENSE=WTFPL 的冲突**已查明是元数据写错**（发布者即版权人本人，README 自标 WTFPL），取保守口径 WTFPL 即可合法使用；**不靠 fork 解决**——fork 不改变版权归属，自贴 MIT 反而构成不实授权声明 |
| area-data / china-area-data | 淘汰 | 停更 8/6 年，area-data 仓库已 archived |
| i18n-iso-countries（国家层默认）/ world-countries（备选） | **采用前者** | i18n-iso-countries = MIT 已验证、无 share-alike；world-countries 多语言更全但 ODbL-1.0 属数据库版 copyleft（对外再分发衍生数据须署名+同许可），闭源交付避开 |
| GeoNames / 商业区划库（qqzeng） | 不引入 | 村级不是收货字典必要层级；许可/采购未核实 |
| 高德/Google/菜鸟地址 API | 仅作可选增强（capability='address'） | 三方不得成为唯一来源（§12 离线交付）；永不阻塞下单，不进 ProductionSafetyGuard |
| 「默认空表，用户填一个新增一个」 | **否决** | 权威字典沦为用户输入影子表，可被顾客端污染且全球追不完；改为 region_gap_report 上报 + 人工刷入快照 |
| **固定「省/市/区」三级表单铺到全球** | **否决** | libaddressinput 已证实形状因国而异（US 需州+邮编；根西岛**没有行政区级**；巴西 4 级）。改为 address_format 国别驱动：字段集/label/必填/邮编正则由后端下发（§8） |
| 每国灌完整区划树 | 分档，不全量 | 「格式 + 一级枚举」为开通前置；市镇级（德法日巴为万级、频繁改名）用文本 + 可选地址联想替代（§8.2） |

落地方式：npm 数据包只在「生成规范化快照」这一步需要（构建期/运维期）；运行期数据以 Flyway 迁移灌入 region 表，客户机器零外网依赖。未实施。

## 3.9 已决策：富文本详情编辑器与"素材必须来自素材库"闸门（2026-10-30）

背景：商品详情走富文本（`product.detail_html`）。现状编辑器为 TipTap 2.11.5，只能贴地址、不能上传；
服务端 `RichTextSanitizer` 只做 XSS 白名单，**不校验素材归属**——直接 PUT 一段 HTML 就能把外站图或
base64 内联大图写进详情，素材因此脱离 `asset` 表（不可检索、不可复用、无法统计），外站图还会因防盗链
裂图并把访客 IP 送给第三方。同期评估过"换成市场常用编辑器（UEditor/FCKEditor）"。

候选（活跃度/许可证均于 2026-10-30 实测）：

| 候选 | 许可证 | 结论 | 理由 |
|---|---|---|---|
| fex-team/ueditor | MIT | **淘汰** | GitHub API `archived=true`，最后推送 2023-01-06，open issues 1101；ES5 全局脚本 + 自带 PHP/JSP/ASP 上传端，素材进 `/ueditor/php/upload`，与 `asset` 表无关——正是"素材脱离素材库"的源头 |
| FCKEditor | — | 前提不成立 | 2009 年改名 CKEditor 3，作为独立产品已不存在 |
| CKEditor 5 | GPL2+/商业（GitHub 标 NOASSERTION） | 不采用 | 许可条款需法务确认、需自建 bundle；**未逐条核对商业条款，属未验证项** |
| @wangeditor/editor 5.1.23 | MIT | 淘汰 | npm 最后发布 2022-11-14（停更约 4 年），Vue3 封装停在 2022-06 |
| wangEditor-next | MIT | 备选不选 | 活跃（2026-10-27），但 850★ / 28 issues，体量换不来收益 |
| UEditorPlus | Apache-2.0 | 不选 | 仍是 UEditor 老架构（配置式上传端），只是社区打补丁 |
| **TipTap 3.32.1（保留并升级）** | MIT | **采用** | 38.9k★，2026-10-29 仍在提交；表格/链接/下划线/对齐/字数/占位扩展均 MIT；与现有 Vue3 栈零迁移成本 |
| Lexical / Quill | MIT / BSD-3 | 不采用 | 框架级重写，收益不匹配当前需求 |

结论：

1. **保留 TipTap 并升到 3.x**，补齐表格、链接、下划线、对齐、字数、占位；
   StarterKit 已内置 Link/Underline，二者不得再单独注册（否则 `Duplicate extension names`）。
2. **上传只有一个入口**：编辑器的按钮/拖拽/粘贴/截图全部走 `web/apps/admin/src/upload.ts#uploadMedia()`
   → `POST /admin/v1/media?purpose=PRODUCT` → 落对象存储并登记 `asset`。编辑器不拥有上传端，
   不写 base64，不直插外站图。插入的图片带 `data-asset-id`，服务端按 id 精确校验归属。
3. **服务端闸门**：新增 `RichTextMediaGuard`，`CatalogService.updateBasics` 在清洗之后调用。
   放行 `/images/`（随包内置演示图）、能在 `asset.url`/`asset_locale_variant.url` 命中的地址、
   以及 `data-asset-id` 有效的元素；其余（外站 http(s)、base64、库里没有的相对路径）拒绝保存，
   错误码 `RICH_TEXT_MEDIA_NOT_IN_LIBRARY`，消息列出具体地址（§10.1 管理员要能排障）。
4. `style` 白名单收窄成"只保留 `text-align`"：放行任意 CSS 等于放开 `position:fixed`（盖住购买按钮）
   与 `background:url()`（外站探针），因此清洗后统一归一化，其它声明整条丢弃。
5. 译文详情（`ProductTranslationWorkflowService`）由源详情逐节点翻译而来，src 与源一致，
   因此**不重复过闸门**；若将来允许 AI 往译文里插图，必须先补这一步。

拒绝的方案：

- **换 UEditor/FCKEditor**：停更 + 自带上传端，与"素材必须入库"直接冲突（详见上表）。
- **前端 only 校验**：请求可绕过编辑器，前端限制不是边界。
- **把外站图下载后直接内联**：会让详情 HTML 与素材库出现两份事实，裂图与体积问题照旧。

验证：`RichTextSanitizerTest`（表格结构、data-asset-id、text-align 归一化）、
`RichTextMediaGuardTest`（外链/base64/不存在 assetId 被拒，CDN 改写地址按 id 放行）、
`npm run build -w @fullmall/admin`。

## 3.10 已决策：权限说明的单一来源与生成（2026-10-30）

背景：权限矩阵页只有 `label` + `code`（来自 `permission` 表），运营看不出"勾了会怎样、取消会怎样"，
更看不出该权限拦的是哪些接口。手工在页面写说明会立刻与 `PermissionService.requiredPermission()` 的
路径映射说两套话（"取消 product.manage 后到底哪几个接口 403"只有那段 if 链知道）。

候选：

| 候选 | 结论 | 理由 |
|---|---|---|
| TypeDoc / vue-docgen-api | 不采用 | 面向 API 签名与组件 props；本需求是业务语义（取消后的可见后果），且权限映射在 Java 侧，TS 工具读不到 |
| Docusaurus / VitePress 文档站 | 不采用 | 成熟但只解决"人看手册"，页面 tooltip 仍需运行时数据；还要新增站点与端口，收益不匹配 |
| springdoc / OpenAPI | 不采用 | 能列出接口，但权限由自定义过滤器按 URL 前缀判定，注解里没有权限→接口关系 |
| 前端手写说明常量 | 否决 | 与代码两套事实，必然漂移（相当于再造一份字段权限矩阵） |
| **单一来源 + 生成器（采用）** | **采用** | 说明只写在 `docs/permissions.catalog.json`；接口映射由脚本解析 `PermissionService`；迁移播种用于交叉校验 |

落地：

- `scripts/generate-permission-help.mjs`：
  1. 解析 `PermissionService.requiredPermission()` 的"路径前缀 → 权限码"映射（含兜底分支）；
  2. 解析 Flyway 迁移里 `INSERT INTO permission` 的播种（code/label/group/sort 的真值）；
  3. **三方集合必须一致**：catalog 有说明但没人播种、有播种但没说明、有权限码但没有任何接口映射，全部失败退出；
  4. 生成 `web/packages/api-client/src/permissions.generated.ts`（页面 tooltip）与 `docs/PERMISSIONS.md`（内部手册）。
- `make contract-check` 增加 `--check` 模式：产物与源不一致即失败，等同于"help 手册随代码自动更新"的强制点。
- 页面：[PermissionsView.vue](web/apps/admin/src/views/PermissionsView.vue) 每项右侧 ⓘ 悬停显示
  "勾选后可做 / 取消后 / 关联接口 / 关系"；同时带原生 `title`（读屏与 alt 也能拿到），说明文案一条也不写在页面里。

已核实的关系事实（写进生成产物，页面直接展示）：

1. **没有权限继承、没有父子层级**：每个权限独立授予；
2. **ADMIN 短路**：`PermissionService.hasPermission` 对 ADMIN 直接返回 true，不参与矩阵；
3. **菜单按角色、不按权限**：取消权限后菜单还在，但该页请求 403（这是当前实现的真实行为，不是理想设计）；
4. **兜底权限**：未登记路径默认要求 `permission.manage`，新增接口忘了登记映射会落到它身上。

## 3.x 本机向量模型（2026-10-31）

需求：没有可用的远端 embedding 时，AI 客服也要能走语义检索。

| 候选 | 许可证/维护 | 结论 |
|---|---|---|
| `@xenova/transformers` 2.17.2 + `Xenova/bge-small-zh-v1.5`（ONNX 量化） | Apache-2.0；transformers.js 活跃；模型 23MB，CPU 可跑 | **采用**：独立进程、OpenAI 兼容 `/v1/embeddings`，主程序零改造 |
| Ollama + embeddinggemma / nomic | 需额外守护进程与模型体积；本机无 GPU 设备节点 | 不选（保留为可选供应商） |
| Python sentence-transformers + torch | 需 pip/torch（数 GB），宿主机无 pip | 不选 |

服务位置 `tools/embedding-service`，端口 41291（AGENTS §8），模型不入库；
失败时明确 5xx，不返回空向量。语义/关键词由 `app.ai.ask.retrieval` 显式选择。

## 3.1 自动汇率源（2026-10-31）

| 候选 | 许可证/维护 | 结论 |
|---|---|---|
| **Frankfurter**（`api.frankfurter.dev/v1`，ECB 参考汇率） | MIT；开源、无 API Key；CNY 在列 | **采用**：免费、无密钥、接口稳定；失败保留上次/手工汇率 |
| open.er-api.com | 免费层无 Key，但条款与频率限制需单独评估 | 暂不选（保留为后续适配器） |
| exchangerate.host / Fixer | 现多需 Key，Key 不能入库 | 不选；未来如接入，Key 走环境变量/密钥管理 |
| 爬银行牌价 | 无稳定契约、易被封、口径不透明 | 不选 |

集成位置 `IntegrationCatalog` 能力 `exchange_rate`；演示/开发未配置用 `mock`，
生产未配置不自动拉取；只存直接汇率，不做三角换算。

## 4. 禁止事项

- 禁止自研字段-角色权限矩阵作为默认方案。
- 禁止用 `@JsonView` 代替输入校验；输出裁剪不等于安全。
- 禁止在 Controller 手写行级 where；必须走统一 scope。
- 禁止全量 `BeanUtils.copyProperties` 覆盖实体。
- 禁止自研 PDF/Office 解析、图片处理、病毒扫描、汇率表、加密算法。
- 禁止生产 `ddl-auto=update`。
- 禁止生产缺配置回退 Mock。
- 禁止把“ThinkPHP 的做法”直接翻译成 Java 自研框架；先找成熟替代。
