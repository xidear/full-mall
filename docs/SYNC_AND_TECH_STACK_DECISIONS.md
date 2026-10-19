# 决策：轮询 vs WebSocket、Spring Boot 版本、JaVers 连接问题

> 日期：2026-10-19
> 背景：用户提出「拉取+轮询 vs 拉取+WS 怎么选」「为什么不用 Spring Boot 4」「JaVers 不关连接必须彻底解决，是 bug 还是配置」「系统未上线，不考虑兼容，一步到位」

## 1. 轮询 vs WebSocket

两者不是二选一，按「**是否需要亚秒级推送**」分流：

| 场景 | 选择 | 理由 |
|---|---|---|
| 异步导出任务进度 | **轮询**（指数退避 1s→2s→5s→15s，最多到 60s） | 任务是分钟级；轮询实现简单、可缓存、断网自愈，无长连接成本 |
| 报表/对账结果 | 轮询 + `ETag`/`Last-Modified` | 大部分轮询会命中 304，几乎零成本 |
| 供应商待办中心 | **WS 增量 + 断线全量拉取**（seq/ack） | 需要事件级实时；但**首次与重连必须能纯拉取**，WS 只是加速 |
| 客服会话 | **WS**（ticket/seq/ack/心跳） | 交互式，延迟直接影响体验 |
| 库存/价格变动通知 | WS 可，但**必须有轮询兜底** | 不能因为 WS 断了就"看不见变化" |

**性能对比（同规模下）**：

- 轮询成本 ≈ `在线客户端数 × 轮询频率`；1000 客户端 × 10s = 100 req/s，可用缓存与条件请求（304）压到很低。
- WS 成本 ≈ 长连接内存 + 心跳；1000 连接很轻，但**运维复杂**（网关超时、重连风暴、多副本广播需 Redis/RabbitMQ 扇出）。
- 结论：**低频（>30s 一次）用轮询；高频/交互用 WS**。WS 永远配「断线补拉」，否则丢消息不可接受。

**对异步导出的决定：轮询**。任务状态机 `PENDING→RUNNING→SUCCEEDED/FAILED` 由前端按退避策略查询；
不引入 WS 依赖，也不新增推送通道。完成时可选发一条站内通知（已有 notification 模块）。

## 2. Spring Boot 版本

事实核对（Maven Central，2026-10-19）：

| 版本 | 状态 |
|---|---|
| `spring-boot-starter-parent` 4.1.1 | **GA（最新稳定）** |
| 4.2.0-M1 / M2 | 里程碑 |
| 4.1.0-M4 / RC1 | 预览 |

当前项目在 **3.4.5**。当初选择 3.4 的真实原因：**不是技术评估结论，而是跟随仓库既有代码**（`pom.xml` 已锁定 Boot 3 + Spring Modulith 1.3.x）。这一点必须承认——项目规则要求"按长期需求选型"，而我沿用了现状。

**结论：升级到 4.1.1**。系统未上线，没有兼容包袱；越晚升级代价越大（Spring Framework 7、Jakarta、Security 6→7 的迁移面会随代码量增长）。

升级前置条件与风险（需先做 spike，不盲升）：

1. **Java 基线**：需确认 Boot 4 要求的 JDK 版本（当前 JDK 21，大概率满足，但要实测）。
2. **Spring Modulith**：必须换到支持 Boot 4 的主线版本（当前 1.3.5 面向 Boot 3）。
3. **Spring Security 7**：`SecurityFilterChain` 写法、`authorizeHttpRequests` 细节、方法级授权可能有变化。
4. **Hibernate 7 / JPA**：实体与 `ddl-auto=validate` 行为需重跑 `AssetScopeAndAuditTest`。
5. **Testcontainers / H2 / Flyway**：版本对齐。
6. **JaVers starter**：`javers-spring-boot-starter-sql` 7.11.9 已面向 Boot 4（`org.springframework.boot.jpa.autoconfigure.JpaProperties`），升级后**可以直接用 starter** 替代手工装配。

升级步骤：单开分支 → 只改依赖与编译错误 → 全量测试 → 打包两 profile → 合并。禁止和业务改动混在一个提交。

## 3. JaVers 连接问题的真实根因（字节码证据）

用户要求彻底解决，不是文档约束。查证结论：

1. `org.polyjdbc.core.transaction.ExternalTransactionState.close()` 的实现是：
   ```
   logger.trace("Clode called on unmanaged ExternalTransaction, ignoring.");
   return;
   ```
   **它根本不关闭连接**，且这是库的**显式设计**（"unmanaged external transaction"）。
2. `SqlRepositoryBuilder` **只有** `withConnectionProvider(...)`，没有 `withDataSource(...)`。
3. `javers-spring-boot-starter-sql` 的 `JaversSqlAutoConfiguration.javersSqlRepository(ConnectionProvider)` 同样接收 `ConnectionProvider`。

**因此**：给了 `ConnectionProvider` 就等于声明"连接生命周期由调用方（也就是我们）管理"，而 JaVers 从不回调关闭。
→ **不是版本 bug，也不是配置错**，是**所有权契约**没被满足。升级版本不会自动消失。

**outbox 能不能修？** 要分清两件事：

- outbox 解决的是「业务事务与审计写入的原子性」（业务回滚但审计已写、或审计失败但业务成功）。
- **它不直接修连接泄漏**，但它是让"单连接串行审计"变得可接受的正解：
  业务侧只写 `audit_outbox`（同事务），由**单个消费者**用**一条连接**串行写入 JaVers，失败重试、进死信。
  这样连接数恒为 1、业务线程不再被审计阻塞、审计至少一次投递。

**最终方案**：outbox + 单消费者审计写入器（替代当前 `synchronized javers.commit`），并把 `AuditTrail` 的 `synchronized` 去掉。
连接数仍为 1，但不再串行业务线程；升级 Boot 4 后重新评估 starter 是否改变该契约。

## 4. 取消兼容期（系统未上线）

之前为"不破坏现存调用"做的妥协，全部作废，直接一步到位：

| 原妥协 | 现在做法 |
|---|---|
| 同步导出 + 行数上限 | 直接换异步 `export_job`，**删除同步导出接口**，前端一次改完 |
| 供应商 `/open/v1` 保留旧路径 | 直接统一，旧路径不保留 |
| 翻译/媒体读旧字段 | 直接切新模型，数据用迁移脚本一次性转换 |
| `ServerContextTest` 的 `ddl-auto=none` 例外 | 升级后评估能否去掉（有迁移就该验证） |
| JaVers 单连接 + `synchronized` | 改 outbox + 单消费者 |
| 双认证（HMAC/token）兼容 | 保留两种**是需求（SUP-030）**，不是兼容包袱；但默认值改为 `BOTH` 前需业务确认 |

> 注意：供应商对外的 `/open/v1` **必须保留版本化与弃用策略**（API-001/004），这是对外契约，不属于"内部兼容"。

## 5. 执行顺序（下一轮开始）

1. **Boot 4.1.1 升级 spike**（独立分支，先跑通编译 + 全量测试）
2. 升级完成后接 **JaVers starter**，再上 **outbox + 单消费者审计**
3. **A6 异步导出**（轮询 + 指数退避），删除同步导出
4. 其余 G 组（媒体本地化、AI 生成）按原计划
## 6. Boot 4.1.1 升级 spike 实测结果（2026-10-19）

分支：`spike/spring-boot-4`（**未合并**，main 保持 3.4.5 绿色）。

| 项 | 实测 |
|---|---|
| `spring-boot-starter-parent` | 4.1.1 可解析（Spring Framework 7.0.9） |
| `spring-modulith-bom` | 1.3.5 → **2.1.1**（Boot 4 主线） |
| `springdoc-openapi-starter-webmvc-ui` | 2.8.5 → **3.1.1** |
| `org.testcontainers:*` | **Boot 4 不再托管版本**；且 TC 2.x 改了 artifact 名（`testcontainers-postgresql`），需同步改依赖坐标 |
| 编译 | POM 阶段即失败（版本缺失），尚未进入代码级错误 |

结论：升级**可行且必须做**，但属于"跨大版本"改造，范围至少包含：
1. Testcontainers 2.x 坐标与 API（`PostgreSQLContainer` 包路径/构造）；
2. Spring Modulith 2.x（`ApplicationModules.verify()` 约定、启停 API）；
3. Spring Security 7（`SecurityFilterChain`、方法级授权实现类可能改名）；
4. springdoc 3.x（OpenAPI 生成器 API 变化）；
5. JaVers starter 改用（Boot 4 版），并据此重构审计连接方案（见 §3）。

执行要求：**单独分支、先依赖后代码、每步跑全量测试**；不与业务改动混提交。
