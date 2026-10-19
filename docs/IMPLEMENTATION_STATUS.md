# 实施状态（Phase 0 地基 + 安全/并发验证）

日期：2026-10-18

## 已完成

### 文档与决策
- `docs/DEVELOPMENT_PLAN.md`：32 周开发计划。
- `docs/adr/0001-0011`：技术栈、模块化单体、中国市场优先、金额币种、批次效期、多模式履约、Demo/Prod、企业版更新、clean-room、法律文档版本化、多主题。
- `docs/references/README.md`：参考项目、许可证复核、clean-room 规则；CRMEB 作为“预设 + 可视化配置 + 预览”的产品参考，不复制代码/UI。
- `docs/parity-matrix.md`、`docs/erd.md`、`docs/openapi.yaml`。

### 开发环境
- `compose.yaml`：19 个服务，profile 分为 core/app/obs/tools/ai。
- `compose.lite.yaml`、`.devcontainer/`、`Makefile`、`.env.example`。
- 前后端独立 Dockerfile；Vite 应用使用 Nginx；Nuxt 使用 Node server。

### 后端
- Spring Boot 3.4.5 + Spring Modulith 1.3.5，111 个 main 源文件。
- 多规格商品、翻译、价格；库存批次/效期、FEFO、预占/释放/发货/流水；多仓/门店/就近自提；按履约源拆单；Mock 支付；自提核销；法律文档 clickwrap；运营总览；客服 WS 骨架。
- 安全与并发：
  - `Idempotency-Key` + PostgreSQL advisory lock + 唯一键：重复提交返回同一订单，不同请求体返回 409。
  - 库存原子预占；并发抢单不超卖。
  - 写接口单节点限流（默认 120/分钟/IP），返回 429 + `Retry-After`。
  - 安全响应头：`X-Content-Type-Options`、`X-Frame-Options`、`Referrer-Policy`、`Permissions-Policy`、`Cross-Origin-Resource-Policy`；生产 HSTS。
  - demo profile 只读过滤器；prod 敏感接口 fail-closed。
- Flyway `V1__core.sql` + `V2__idempotency.sql`，UTF-8 demo seed。

### 前端
- Nuxt 商城：商品/详情/购物车/结算/订单/法律文档。
- Vue 运营后台：总览、商品向导、变体矩阵、批次效期、库存、订单查询、**主题工作室**。
- 门店工作台：拣货、批次扫码、自提核销。
- 供应商门户、客服 WS 工作台骨架。
- uni-app：H5 + 微信小程序，四套主题预设（商城/机械/清新/极简）。
- `@fullmall/theme`：PC、后台、小程序共用主题 token；`THEME`/`NUXT_PUBLIC_THEME`/`?theme=` 选择 PC 模板。
- 后台主题配置全部为颜色选择器、滑块、下拉框、开关，不要求手填 JSON/逗号字符串。

### CI
- `.github/workflows/ci.yml`：后端测试、demo/prod 打包、SBOM、五个 Web 应用构建、Compose 配置校验。

## 已验证

### 后端与数据库
| 项目 | 结果 |
|---|---|
| JDK 17 编译 + 17 个测试 | 16 通过，1 个 Testcontainers 用例因 Windows 宿主机无法访问 WSL Docker 跳过 |
| JDK 21 `package -DskipTests`（缓存的 Maven 镜像） | **BUILD SUCCESS** |
| JDK 21 测试（同一容器） | 编译通过；Spring 上下文测试因容器内 Mockito 自附加受 seccomp 限制失败，非业务代码问题 |
| 真实 PostgreSQL 16.15（WSL 缓存镜像） | Flyway V1/V2 + UTF-8 seed 成功；商品/门店/法律文档接口正常 |
| 真实自提闭环 | 下单 → Mock 支付 → FEFO 拣货 → 自提核销 → 订单 FULFILLED；门店批次 5→3，预占归零 |
| 20 库存 / 50 并发下单 | **20 成功，30 冲突，0 超卖**；预占=20，可用=0 |
| 30 库存 / 50 并发下单 | **30 成功，20 冲突，0 超卖**；预占=30，可用=0 |
| 10 并发同 Idempotency-Key | 全部返回同一个订单 ID；库存只预占 1 件 |
| 同 key 不同请求体 | 409 `IDEMPOTENCY_KEY_REUSED` |
| 130 个写请求 | 前 120 个 400，第 121 个开始 429；`Retry-After` 正常 |
| 安全响应头 | 全部返回 |
| Spring Modulith 模块依赖 | 通过 |

### 前端
| 项目 | 结果 |
|---|---|
| Nuxt 商城生产构建 | 通过 |
| Admin / Store Console / Supplier / Support 构建 | 通过 |
| uni-app H5 构建 | 通过（Compiler 5.32 vue3） |
| 微信小程序构建 | 通过，产物 `dist/build/mp-weixin` |
| Playwright（Edge） | **4 个 E2E 全部通过**：商城下单、主题切换、运营后台+主题工作室、门店拣货+自提核销 |
| Admin/Storefront 独立目录 Docker 式安装+构建 | 通过 |

### 依赖安全
- `npm audit --omit=dev`：**critical 0**，high 12。
- 剩余 high 全部位于 Nuxt 构建/开发链（`@nuxt/cli`、`nitro-server`、`vite-builder`、`listhen`、`node-forge`、`glob` 等）。
- 已移除未使用的 Vitest，消除了 2 个 critical 和 tinypool 等单测链风险。
- 下一步：等待 Nuxt 官方修复后升级；生产不暴露 Nuxt dev server。

## 已修复的真实问题

- `reservation.expires_at` 直接传 `Instant` 导致 PostgreSQL 无法推断类型 → 显式 `Timestamp`。
- seed SQL 以 Windows 默认编码读取，中文变乱码 → 显式 UTF-8。
- `ProductCard` 的 computed 使用未定义的 `product` → 改用 `props.product`。
- 幂等变量与 lambda 参数重名导致编译失败 → 改名 `idemKey`。
- 通用 Vite Dockerfile 重复拼接 `apps/`、Maven 镜像命令错误 → 修正。
- Spring Modulith 跨模块访问未暴露类型 → `@NamedInterface` + `OrderService` 收到支付状态变更。
- `Money` 只校验长度，`RMB` 可通过 → 改用 ISO-4217 `Currency.getInstance`。
- 定时释放任务在 api/worker 双跑 → 仅 `worker` profile 运行。

## 未验证 / 当前阻塞

| 项目 | 原因/下一步 |
|---|---|
| 完整 `docker compose up` | WSL Docker 无法访问 Docker Hub；缺 `postgres:17-alpine`、`valkey:8-alpine`、`rabbitmq:4-management-alpine`、mailpit、`node:22-alpine`、OpenSearch、SeaweedFS 等镜像 |
| Docker daemon 代理 | 需要 WSL `sudo` 密码才能写 systemd drop-in 并重启 Docker；当前无权限 |
| 本地完整栈 | 用缓存 `postgres:16-alpine` + 宿主机 Maven 完成了真实数据库/并发/安全验证；生产 compose 仍保持 PostgreSQL 17 |
| JDK 21 测试 | 容器内 Mockito 自附加受 seccomp 限制；JDK 17 测试全绿，JDK 21 编译/打包成功 |
| 真实支付/物流/短信/发票 | 按设计仅 Mock + SPI；真实适配器待配置联调 |
| JWT/OTP/OAuth、员工 RBAC、供应商 HMAC | 仅建立边界；prod 敏感接口 fail-closed |
| 供应商 `/open/v1` 实现 | 目前只有契约、文档和门户骨架 |
| 门店收货/盘点/调拨完整 API 与 UI | 目前有库存批次查询、拣货、自提；收货/盘点/调拨待 Phase 3 |
| 法律文档管理端、PDF、电子签名 | 目前有查询/接受/seed |
| 主题持久化/页面级 DIY | 目前 localStorage + 导出配置；服务端存储、发布/回滚、楼层 DIY 待后续 |
| 运营分析完整事件与看板 | 目前有订单/支付事件和总览 |
| 企业版更新 | 只有 ADR 与边界 |

## 推荐下一步

1. 在 WSL 配置 Docker daemon 代理（需 sudo），或导入缺失镜像；跑完整 `make up`。
2. 把 `PostgresPickupFlowTest` 放到 CI（GitHub Runner 有 Docker）执行。
3. 完成 JWT/OTP/OAuth、员工 RBAC、供应商 HMAC。
4. 完成 `/open/v1` 供应商 API 与 CSV 导入。
5. 完成门店收货/盘点/调拨。
6. 主题配置服务端持久化、版本发布/回滚、页面级 DIY。
7. 升级 Nuxt 修复剩余 high 依赖告警。

## 验证命令摘要

```bash
# 后台测试（JDK 17）
cd server && mvn -Djava.version=17 test

# JDK 21 打包（WSL 缓存 Maven 镜像）
wsl -- bash -lc "cd /mnt/d/works/full-mall/server && docker run --rm \
  -v /mnt/d/works/full-mall/server:/workspace \
  -v /mnt/c/Users/admin/.m2:/root/.m2 \
  -w /workspace maven:3.9.9-eclipse-temurin-21 \
  mvn -o -B -ntp -DskipTests package"

# 前端 E2E（需 API、商城、Admin 5273、门店 5274 已启动）
cd web && npx playwright test
```

## 2026-10-18 第三轮改进（按反馈修复）

### 门店拣货与自提
- 拣货不再要求输入任务 ID/批次 ID：选择门店后按任务卡片商品行直接点“确认拣货”，批次/效期由系统建议并展示批次号。
- 自提核销改为“输入/扫描提货码 → 拉取信息 → 核对顾客、手机号、订单、门店、商品、剩余次数 → 按提货码或手机号核销”。
- 支持多次核销：`pickup.total_uses` / `used_count`；第一次返回“部分核销”，达到总次数后订单才 `FULFILLED`。
- 实测：次数 2 的自提单，第一次 `PARTIALLY_USED 1/2`，第二次 `COMPLETED 2/2`，订单 `FULFILLED`。

### 运营后台
- 明确为“商城运营后台”：登录、退出、修改密码、角色菜单。
- 认证：`staff_user` + `staff_session`，PBKDF2 密码哈希，Bearer token 8 小时；`/admin/**` 由 `StaffAuthFilter` 保护。
- 演示账号（仅 dev/demo）：
  - `admin / Admin@123456`（超级管理员）
  - `operator / Operator@123456`（运营人员）
  - `store / Store@123456`（门店人员，当前不允许进入 admin）
- 商品管理：全状态列表、创建、上架/下架。
- 订单管理：按状态筛选、列表、订单详情、履约单。
- 库存批次、主题配置保留。
- 主题配置“保存到服务器”已生效：PC 商城和小程序分别保存 `theme_config`；商城 SSR 读取 PC 配置，小程序启动读取 MINI 配置。

### 商城前台
- 重做首页：顶部欢迎栏、搜索、分类导航、活动 Banner、分类入口、今日特惠、热卖推荐、生鲜批次区、工业品专区、服务承诺、页脚。
- 新增：商品列表/分类筛选/价格排序、购物车、我的订单、订单状态中文标签。
- 商品从 2 个扩展到 10 个，覆盖生鲜/家居/数码/工业/美妆/运动；分类从 2 个扩展到 6 个。
- 商品图使用分类色渐变 + 商品首字占位，不再全部是 FM。
- 前台主题切换已移除；只保留 `THEME`/`NUXT_PUBLIC_THEME` 和 `?theme=` 预览，正式配置由后台主题工作室保存。
- 所有面向用户的订单/履约/拣货/自提/库存/商品状态改为中文全量文案，不再直接显示 `PENDING`、`PAID` 等 key。

### 验证
| 项目 | 结果 |
|---|---|
| 后端测试 | 17 个测试，16 通过，1 个 Testcontainers 用例因宿主无 Docker 跳过 |
| Playwright | 5 个 E2E 全部通过：后台登录+商品/订单/库存/主题、商城下单、主题预览、购物车、门店拣货+拉取信息+核销 |
| 多次核销 | 2 次核销：`PARTIALLY_USED 1/2` → `COMPLETED 2/2` |
| 主题服务端保存 | PC 主题保存后 `/api/v1/theme` 返回 industrial，商城 SSR 输出 `data-theme="industrial"`；删除后恢复 mall |
| uni-app | H5 + 微信小程序构建通过 |
| 前端构建 | 商城/后台/门店/供应商/客服全部构建通过 |

### 仍未完成
- 售后/退货/退款工作流、后台批量发货/电子面单操作页。
- 顾客账号体系（当前“我的订单”基于本地订单 ID，不是登录账号）。
- 供应商 `/open/v1` 实际接口、门店收货/盘点/调拨。
- 主题版本发布/回滚、页面级 DIY。
- Nuxt 构建链剩余 high 依赖告警。
- OpenAPI 契约尚未覆盖新增的 staff/auth、theme、categories、admin products/orders 接口，需在下一轮补齐。

## 2026-10-18 第四轮改进（市场对标与运营基本盘）

### 新增
- `docs/MARKET_PARITY.md`：按 CRMEB/Lilishop/mall4j/Shopware 等模块整理商城后台功能清单和现状。
- 运营后台首页：今日订单/销售额、待付款、待发货/出库、待自提、低库存、临期、在售商品、客户数；待办卡片可跳转。
- 客户管理：从订单聚合手机号、姓名、订单数、累计消费、最近下单。
- 员工与权限：员工列表、角色、状态；角色菜单已生效。
- 登录安全：算术验证码 + 连续 5 次失败锁定 10 分钟；登录页刷新验证码。
- 通知中心：支付成功/备货完成/发货/完成事件；站内/短信/邮件/微信/APP 五个渠道 Mock 适配器；发送记录、状态、失败原因、已读操作。
- 第三方集成策略：`docs/INTEGRATIONS.md`，定义短信/对象存储 SPI、配置键、Mock 与真实适配器计划。
- 后台主题配置支持 PC/小程序分别保存到服务器；商城 SSR 读取 PC 配置。

### 验证
| 项目 | 结果 |
|---|---|
| 后端测试 | 17 个测试，16 通过，1 个 Testcontainers 用例因宿主无 Docker 跳过 |
| Playwright | 5 条 E2E 通过（后台登录含验证码、商品/订单/客户/员工/库存/通知/主题、商城下单、购物车、门店拣货+核销） |
| 看板接口 | 返回今日订单/销售额与待办计数 |
| 通知记录 | 支付成功产生 IN_APP/SMS 通知，状态 SENT；完成事件产生通知 |
| 验证码登录 | 正确验证码登录 200；错误验证码/失败次数会拒绝并锁定 |
| 主题服务端配置 | PC 保存后商城 `data-theme` 生效，删除后回退 mall |
| 多次核销 | 2 次核销 PARTIALLY_USED 1/2 → COMPLETED 2/2 |

### 明确未完成（不能声称已对齐 CRMEB）
- 客户账号/会员等级/会员卡/储值/积分/客户标签。
- 售后/退货/退款、后台批量发货/电子面单、自动确认收货。
- 优惠券/满减/满送/生日/买赠/秒杀/拼团/大转盘/红包/二级分销。
- 门店 POS/收银台/收银员锁屏/店长管理/O2O 仅核销。
- 采购/收货/盘点/调拨/移库/审批/退货入库/补货建议。
- 供应商 `/open/v1` 实际接口、供应商结算/对账。
- 站点一键黑白/定时开关/维护页、页面 DIY/Banner/公告、商品标签/评价/推荐策略。
- 阿里云/腾讯短信、阿里云 OSS/七牛真实适配器（目前通知渠道为 Mock，存储未接）。
- 多语言 en/es/fr/ar 与 RTL；当前仍以中文为主。
- 角色权限管理页/接口权限矩阵/数据权限；当前是固定角色 + 菜单过滤。
- OpenAPI 新接口同步、真实商品图片/视频、全量视觉回归。
- DAO 全面重构：新模块按聚合/接口拆分，但旧仓储仍有重复列名和查询，需要独立技术债阶段统一 RowMapper、SQL 常量、查询对象和基类。


## 2026-10-18 第五轮改进（实时性、演示数据、图片、配置）

### 新增
- 门店任务实时化：首次按 `storeId` 拉取 `/store/v1/pick-tasks`，随后连接 `/ws/store?storeId=...` 接收任务创建/变更推送；断线自动回退 20 秒轮询。
- 演示自提：`GET /mock/pickups/654321` 返回演示顾客、订单、商品、剩余核销次数；门店工作台演示模式显示提示并只读展示。
- 系统设置：`system_setting` 表与后台页面，可配置公司主体、ICP备案号、公网安备、客服电话/邮箱、站点状态、维护文案、默认语言/币种、存储/短信/支付/物流适配器。
- 商城页脚读取备案/主体/客服信息；站点状态为维护/暂停时显示维护页。
- 本地商品图：10 张商品 SVG + 1 张 Hero 图，后台商品列表增加缩略图，前台商品卡/详情使用。
- 自提门店定位：结算页请求浏览器定位，按距离排序并显示“距您 x.x km”；未授权则显示全部门店。
- 金额显示确认：数据库/API 使用最小货币单位（分），前台 `Intl.NumberFormat` 按币种格式化为 ¥29.90 等当地价格。

### 验证
| 项目 | 结果 |
|---|---|
| 后端测试 | 17 个测试，16 通过，1 个 Testcontainers 用例因宿主无 Docker 跳过 |
| Playwright | 5 条 E2E 全部通过，包含门店 WS“实时推送已连接” |
| 演示提货 | `/mock/pickups/654321` 返回演示顾客/商品/3 次核销额度 |
| 站点设置 | 公共接口返回公司主体、ICP、公网安备、客服信息、默认语言/币种 |
| 商品图片 | 商品 API 返回 `/images/products/*.svg`，前台/后台可展示 |
| 价格显示 | 后端 2990 分，前台按 CNY 格式化为 ¥29.90 |

### 仍需说明
- 商品图是本地 SVG 演示视觉，不是真实商品照片/视频；真实图库、视频、Banner DIY 和 OSS/七牛上传仍未接。
- 系统设置的“适配器”选项只是配置预留；阿里云/腾讯短信、阿里云 OSS/七牛 SDK 尚未接入。
- 站点定时黑白/定时恢复尚未做，目前是后台手动切换状态。
- 多语言目前只有默认语言配置，前台 UI 尚未完成 en/es/fr/ar/RTL 翻译。


## 2026-10-18 POS 收银与 Java 依赖方案

本轮先完成设计与选型，不把“设计稿”说成“已实现”：

- `docs/POS_DESIGN.md`：组合支付模型、tender、积分/券 ledger、现金班次、财务日结、对账、扫码/碰一碰、离线策略、API、P0/P1/P2。
- `docs/JAVA_DEPENDENCIES.md`：Maven 对应 Composer；adapter module + SPI + Spring 条件装配；Maven Central/BOM/SBOM；不建议 OSGi。
- `docs/adr/0012-pos-combined-payments.md`：POS 独立 bounded context，不与线上订单支付混用。
- 已明确：支付宝碰一碰需要认证终端、NFC、支付宝 ISV/商户资质；后端只做支付意图/查询/回调/退款/对账，不能纯软件绕过。
- 已明确：组合支付必须按 tender 维度记录和退款；积分/储值/券必须 ledger 化，现金必须班次+差异审批。

尚未实现：POS 表结构、班次/销售单/tender API、收银台 UI、Mock 组合支付闭环、积分/券/储值核销、现金日结报表、真实扫码/碰一碰 adapter。下一步从 P0 Mock 组合支付闭环开始。

## 2026-10-18 第五轮（POS 收银 P0 落地）

### 数据库
- `V8__pos.sql`：会员、积分 ledger、储值 ledger、优惠券/核销记录、班次、销售单/行/批次分配、tender、现金流水、退款/退款 tender、对账表。
- 演示会员：王小明（500 积分、¥100 储值）、李静（120 积分）；优惠券 `POS10`、`MEMBER20`、`MILK5`。

### 后端
- `com.fullmall.pos` 模块：`PosService`、`PosRepository`、`PosPaymentProvider` SPI、`MockPosPaymentProvider`。
- 班次：开班、按收银员查询未关班次、关班现金小结、现金收入/支出登记、班次销售记录。
- 销售：草稿开单、门店批次 FEFO 预占、会员绑定、优惠券锁定/核销/回滚、积分抵扣/发放/退回、组合 tender、完成、作废、整单退款。
- 支付：`CASH/WECHAT/ALIPAY/CARD/STORED_VALUE/OTHER`；Mock 扫码/碰一碰立即返回成功，真实收单只需新增 `PosPaymentProvider` 适配器。
- 财务：现金流水、应有现金/实盘/差异、日结、支付方式汇总、对账；退款/作废均有完整流水，不静默吞错。
- 幂等：开单 `Idempotency-Key` + PostgreSQL advisory lock；same key 不同请求体返回 409；tender 也有幂等键。
- 并发：销售单行锁 + 批次原子预占/扣减，防止重复收款与超卖。
- 权限：prod 下 `/store/**` 需要员工会话（`STORE/ADMIN/OPERATOR`）；门店工作台已提供验证码登录/退出；dev/demo 可关闭；`/admin/v1/pos/**` 由运营后台会话保护。

### 前端
- 门店工作台新增「收银台」页签：门店/收银员、开班、关班、现金收付、商品搜索/购物车、会员识别、优惠券、积分、组合支付、完成、作废、退款、班次销售记录。
- 运营后台新增「门店收银」：日结指标、支付方式汇总、对账、班次现金小结、当日销售单与支付明细。
- 演示模式：门店收银台通过 `/mock/pos/shift`、`/mock/pos/sale` 展示只读示例，写按钮禁用；公开演示站仍由 `DemoReadOnlyFilter` 拦截所有写请求。
- 金额全部按分存储、按当地币种格式化显示；状态全部中文文案。

### 验证
| 项目 | 结果 |
|---|---|
| 后端测试 | 23 个测试，21 通过，2 个 Testcontainers 用例因容器内无 Docker 跳过 |
| Playwright | **6 个 E2E 全部通过**（新增 POS 开班→组合支付→完成→退款→关班） |
| 真实 API 闭环 | 4×鲜奶+1×马克杯，满减券 10 元 + 积分 5 元 + 现金 50 元 + 微信 54.50 元；完成、整单退款、关班差异 0；库存先减后回补 |
| 幂等 | 同一 `Idempotency-Key` 重复开单返回同一销售单；不同商品内容返回 409 `IDEMPOTENCY_KEY_REUSED` |
| 报表 | 日结返回净销售、退款、券/积分、tender mix、现金差异；对账行 `MATCHED` |
| 前端构建 | 门店工作台、运营后台生产构建通过 |

### 仍未完成（POS P1/P2）
- 微信/支付宝/银联/聚合收单真实 SDK、webhook 验签、查询/撤销/退款适配器。
- 支付宝碰一碰终端认证、NFC/ISV 资质、设备绑定。
- 部分退款/按行退款、退货入库质检、退款审批。
- 储值充值/赠送/退款、会员等级折扣、会员卡管理 UI。
- 优惠券模板管理与发放、活动价/会员价/促销叠加优先级。
- 离线队列、断网补传、设备签名与人工复核。
- 收银员锁屏、店长授权改价/退款、门店-员工绑定、班次审核。
- 真实支付渠道对账文件解析与差异工作流。

### 验证命令补充
- POS API 闭环与幂等已验证；集成测试 `PostgresPosFlowTest` 已加入，CI 有 Docker 时会自动执行。
- `docs/openapi.yaml` 已补充 23 个 POS 路径与对应 schema。

## 2026-10-18 第六轮（部分退款 + 会员/优惠券运营）

### 部分退款
- `V9__pos_partial_refund.sql`：`pos_refund_line`、`pos_refund_allocation`；销售行/批次分配/tender 累计退款字段；销售单累计扣回积分；退款类型 FULL/PARTIAL。
- `V10__pos_status_width.sql`：`pos_sale.status` 扩展以支持 `PARTIALLY_REFUNDED`。
- 新增 `POST /store/v1/pos/sales/{id}/refund-lines`：按商品行和数量退款。
- 金额：先按整单优惠比例把 payable 分摊到各商品行（最大余数法，合计严格等于 payable），再按累计退款数量比例计算本次退款，最后一笔不产生尾差。
- 原路退款：按原 tender 金额比例分摊；现金写负向现金流水，储值写退款 ledger，Mock 渠道调用 `POS_TENDER` 对应 provider 退款。
- 库存：按销售行实际批次分配逐批回补，写入 `pos_refund_allocation`；过期/隔离批次明确报错转人工。
- 积分：按退款金额比例扣回消费积分；全部退完时退回抵扣积分、恢复优惠券。
- 状态：部分退款 `PARTIALLY_REFUNDED`，全部退完 `REFUNDED`；重复/超额退款返回 409。

### 会员与优惠券运营
- `GET/POST /admin/v1/pos/members`：会员列表（含消费笔数/净消费）、新建会员。
- `POST /admin/v1/pos/members/{id}/points`：积分调整，写入 `ADMIN_ADJUST` ledger，禁止扣成负数。
- `POST /admin/v1/pos/members/{id}/stored-value`：储值充值，写入 `TOPUP` ledger。
- `GET/POST /admin/v1/pos/coupons`：优惠券列表、新建固定金额/百分比券，可限定会员、门槛、最高优惠和有效期。
- 后台新增「会员与优惠券」页面：会员搜索/新建/充值/积分调整；优惠券列表/新建。
- 所有金额后端存分，前端按元输入、按当地币种显示。

### 验证
| 项目 | 结果 |
|---|---|
| 后端测试 | 25 个测试，23 通过，2 个 Testcontainers 用例本地跳过 |
| 部分退款真实 API | 4×鲜奶+1×马克杯；券+积分后应收 ¥108.50；先退 1 鲜奶+1 马克杯（¥54.30，状态 PARTIALLY_REFUNDED），再退剩余 3 鲜奶；最终 REFUNDED，tender 累计退款=应收，批次库存完全回补 |
| 积分/券 | 部分退款按比例扣回 54 积分；全退后积分回到 120、优惠券恢复 ISSUED |
| 超额退款 | 返回 409 `POS_REFUND_QUANTITY_EXCEEDED` |
| 现金小结 | 部分+剩余退款后应有现金仍为备用金 ¥200，关班差异 0 |
| 会员/券 API | 新建会员、储值充值 ¥150、积分 +300、负积分超扣 409、创建券、重复编码 409、券列表包含新券 |
| Playwright | 6 个 E2E 全部通过；POS 用例覆盖部分退款后全部退款 |
| 前端构建 | 运营后台、门店工作台生产构建通过 |

### 仍未完成
- 真实收单渠道的 webhook 验签、查询/撤销/对账文件。
- 退货单/换货、退货质检、退款审批流。
- 会员等级折扣、生日礼、成长值、储值赠送。
- 优惠券发放/领取/核销统计、叠加规则与促销优先级。
- 收银员锁屏、店长改价/退款授权、门店-员工绑定。

## 2026-10-18 第七轮（供应商 `/open/v1` 与供应商工作台）

### 数据库
- `V11__supplier_api.sql`：供应商 API 凭证、履约单供应商接单/备注字段、供应商库存价格 feed、仅供应商供货的演示 SKU `SUPPLIER-ONLY`。

### 后端
- `com.fullmall.supplier` 模块：凭证加密、HMAC 过滤器、供应商订单/库存服务、运营凭证接口。
- `SupplierHmacFilter`：
  - 头：`X-Supplier-Key`、`X-Supplier-Timestamp`、`X-Supplier-Nonce`、`X-Supplier-Signature`。
  - 签名：`HMAC-SHA256(secret, method + "\n" + path+query + "\n" + timestamp + "\n" + nonce + "\n" + SHA256(body))`。
  - 时间窗 ±5 分钟、nonce 重放 409、常量时间比较、请求体缓存。
  - Secret 使用 AES-GCM 加密存储；prod 必须配置 `SUPPLIER_CREDENTIAL_KEY`，否则启动失败。
- `/open/v1`：`ping`、`orders`、`accept`、`ship`、`reject`、`inventory/feed`。
- 接单进入 `ACCEPTED`；发货写运单、扣减供应商预占库存、通知顾客、触发订单完成；拒单释放该履约单对应预占库存。
- `/admin/v1/suppliers`：供应商凭证状态；`/admin/v1/suppliers/{id}/credentials` 生成/轮换凭证，Secret 只返回一次。
- 供应商库存 feed 同步 `supplier_sku` 供货价和 `inventory_batch` 供应商虚拟仓库存，并写 `stock_move`。

### 前端
- 运营后台新增「供应商」页面：查看供应商、生成/轮换凭证、一次性显示 Secret。
- 供应商工作台重写：Access Key/Secret 连接、浏览器 Web Crypto HMAC 签名、订单拉取/接单/发货/拒单、库存与价格同步。
- 所有页面使用中文状态文案、表格/表单控件，无手填 JSON。

### 验证
| 项目 | 结果 |
|---|---|
| HMAC 连通 | `GET /open/v1/ping` 200；错误签名 401 `SUPPLIER_SIGNATURE_INVALID` |
| 订单闭环 | 供应商专属 SKU 下单 → Mock 支付 → `/open/v1/orders` 拉单 → 接单 `ACCEPTED` → 发货 `SHIPPED` → 订单 `FULFILLED` |
| 库存 feed | 供货价/可用库存更新成功；供应商虚拟仓库存与流水同步 |
| 拒单 | `CANCELLED` + 备注，预占库存释放 |
| 安全 | 同 nonce 重放 409 `SUPPLIER_NONCE_REPLAYED`；HMAC 查询串参与签名 |
| Playwright | **7/7 通过**，新增供应商门户“连接→接单→发货”浏览器真实流程 |
| 前端构建 | 供应商工作台、运营后台构建通过 |

### 仍未完成
- 供应商商品批量 upsert、CSV/Excel 导入、Webhook 重试、结算对账、供应商售后。
- 供应商 HMAC nonce 目前为单节点内存存储，多实例需迁移 Valkey/Redis。
- 供应商订单拒绝后的订单取消/退款策略需与售后模块联动。

## 2026-10-18 第八轮（仓储 ERP：收货/盘点/调拨/补货）

### 数据库
- `V12__warehouse_erp.sql`：收货单/行、盘点单/行、调拨单/行/批次分配。

### 后端
- `com.fullmall.warehouse` 模块：`WarehouseService`、`WarehouseRepository`、`/admin/v1/warehouse/**`。
- 收货：创建草稿 → 确认入库；按 SKU + 批次 + 生产/到期日写入 `inventory_batch`，写 `RECEIPT` 流水；重复批次明确 409。
- 盘点：按地点生成批次快照 → 录入实盘 → 确认；差异写 `STOCKTAKE_ADJUST` 流水，批次数量更新。
- 调拨：创建调拨单 → 出库按 FEFO 分配源批次、扣减可用库存并写 `TRANSFER_OUT` → 收货按原批次写入目标地点并写 `TRANSFER_IN`；库存不足明确 409。
- 补货建议：按地点聚合可用库存，低于目标库存（默认 30，可配置）生成建议和最近效期。
- 所有库存变更写不可变 `stock_move`；调拨不允许动用已预占库存。

### 前端
- 运营后台新增「仓储作业」：收货入库、盘点（生成盘点单/实盘录入/确认）、调拨（创建并出库/确认收货）、补货建议。
- 全部使用地点/商品选择器、日期选择、数字输入和表格，无需手填 JSON。

### 验证
| 项目 | 结果 |
|---|---|
| 收货 | 门店收货 10 件，批次/效期正确，库存 +10 |
| 盘点 | 实盘 7 件，差异 -3，批次数量更新并写盘点流水 |
| 调拨 | 门店 → 门店调拨 2 件，源库存扣减、目标库存增加，状态 IN_TRANSIT → RECEIVED |
| 补货建议 | 北京朝阳店鲜奶可用 2，建议补货 28 |
| Playwright | 7/7 通过，后台仓储页加载与收货页签可见 |
| 前端构建 | 运营后台构建通过 |

### 仍未完成
- 采购订单、供应商到货预约、质检/不合格品、上架策略。
- 盘点差异审批、盘盈盘亏财务凭证。
- 调拨审批流、在途异常、跨市场报关。
- 退货入库质检、报损单。

## 2026-10-18 第九轮（RBAC 权限矩阵）

### 数据库
- `V13__rbac_permissions.sql`：权限字典、角色-权限关系；运营角色默认权限。

### 后端
- `com.fullmall.permission`：权限查询、角色矩阵保存、30 秒缓存。
- `StaffAuthFilter` 对 `/admin/**` 做后端权限校验，不是只隐藏菜单；ADMIN 全权限，OPERATOR 按矩阵，未知路径默认要求 `permission.manage`。
- `GET /admin/v1/permissions` 返回权限字典与 OPERATOR/STORE/CS 权限；`PUT /admin/v1/permissions/roles/{role}` 保存。
- 当前权限覆盖：总览、商品、订单、客户、库存、仓储、收银、供应商、通知、主题、员工、设置、权限矩阵。

### 前端
- 运营后台新增「权限矩阵」：按分组勾选权限，保存后立即生效。
- 菜单仍按角色过滤，但接口权限以服务端校验为准。

### 验证
| 项目 | 结果 |
|---|---|
| ADMIN | 全部接口可访问 |
| OPERATOR 默认 | 商品 200、员工 403、权限矩阵 403 |
| 运行时收回 `product.manage` | OPERATOR 商品接口立即 403 |
| 恢复权限 | 商品接口恢复 200 |
| Playwright | 7/7 通过，后台权限矩阵页加载正常 |

### 仍未完成
- 更细的数据权限：门店只看本店、供应商只看自己、客服只看分配会话；需要在查询层统一注入 scope。
- 权限变更审计、角色自定义、权限模板、临时授权。
- 门店员工与门店绑定、收银员锁屏/店长授权。

## 2026-10-18 第十轮（线上售后：退货/换货）

### 数据库
- `V14__after_sales.sql`：售后单、售后行、售后事件。
- `V15__payment_status_width.sql`：支付状态支持 `PARTIALLY_REFUNDED`。

### 后端
- `com.fullmall.aftersale`：顾客申请、运营审核/拒绝、退货入库、Mock 原路退款、订单状态更新。
- 顾客 `POST /api/v1/orders/{id}/after-sales`：校验订单联系手机号、订单状态、订单行数量；退款金额按订单实付比例分摊。
- 运营 `GET /admin/v1/after-sales`、`approve/reject/receive/refund`。
- 退货入库创建 `QUARANTINE` 隔离批次并写 `RETURN_RECEIPT` 流水；质检后可转可用或报损（后续）。
- 退款调用支付 SPI；开源构建仅 Mock，正式构建可替换真实适配器。
- 全额退款订单置 `REFUNDED`，部分退款置 `PARTIALLY_REFUNDED`；事件表记录每次状态变化。

### 前端
- 商城订单详情新增「申请售后」：退货/换货、原因、说明、按商品行数量选择，并展示售后记录。
- 运营后台新增「售后处理」：状态筛选、详情、同意、拒绝、退货入库地点选择、退款确认。

### 验证
| 项目 | 结果 |
|---|---|
| 手机号不匹配 | 409 `AFTER_SALE_PHONE_MISMATCH` |
| 全流程 | 申请 → 同意 → 退货入库（隔离批次 1 件）→ Mock 退款；售后 `REFUNDED`，订单 `PARTIALLY_REFUNDED` |
| 退款金额 | 2 件订单退 1 件，退款 ¥19.90；按实付比例计算 |
| 事件记录 | REQUESTED/APPROVED/RECEIVED/REFUNDED 全部留痕 |
| Playwright | 7/7 通过，后台售后页加载正常 |
| 前端构建 | 商城、运营后台构建通过 |

### 仍未完成
- 换货补发/换货物流、质检不合格报损、退款审批流。
- 售后原因分类、图片/视频举证、客服协商记录。
- 真实支付渠道退款适配器与退款对账。

## 2026-10-19 第十一轮（会员等级/分销/客服/门店锁屏）

### 会员等级与成长
- `V16__member_levels.sql`：会员等级、生日、成长值、生日礼年份、成长值 ledger。
- 等级：普通/银卡/金卡/白金，配置折扣率、积分倍率、生日礼积分、升级成长值。
- POS 绑定会员自动应用等级折扣；消费按实付累计成长值并自动升级；积分按倍率发放；生日当天发放生日礼积分（每年一次）。
- 后台会员页新增成长值、生日、会员等级配置。

### 分销与推广绑定
- `V17__distribution.sql`：订单推广人、绑定模式、绑定记录、佣金 ledger。
- 支持 `ORDER / REGISTER / VISIT / ONE_TIME / PERMANENT` 五种绑定模式；访问绑定 30 天有效。
- 下单可携带 `referrerId + referralMode`，或自动使用手机号的有效绑定；支付成功生成 5% 佣金（可配置）。
- 客服发送商品卡片生成带 `ref + mode` 的商品链接；商城捕获后结算时自动携带，支付后佣金归属客服。
- 后台可查佣金/绑定并结算佣金。

### 客服工作台
- `V18__support_workbench.sql`：会话主题/顾客/分配时间/最后消息、消息 metadata、转单记录。
- `/support/v1`：客服列表、会话列表、领取、转单、消息、发送商品卡片、发送帮助链接。
- 数据权限：客服只能查看/操作分配给自己的会话；未分配会话可领取；转单写入记录。
- 商品卡片含商品标题、SKU、价格、推广链接；帮助链接以移动端友好卡片展示。
- 客服工作台重写为真实 REST 工作台，支持轮询、领取、转单、商品、帮助文档。

### 门店绑定/锁屏/授权
- `V19__staff_store_and_pin.sql`：员工 PIN、员工-门店绑定；演示门店账号绑定上海静安店。
- 后台可设置员工 PIN、绑定门店；门店接口可按 `storeId` 查询校验绑定。
- POS 新增锁屏/解锁（店长/运营账号 + PIN）；退款支持店长 PIN 授权（prod 默认开启）。
- 客服/门店/运营角色与权限矩阵继续生效。

### 验证
| 项目 | 结果 |
|---|---|
| 会员折扣 | 金卡 95 折：应收 3980 → 会员优惠 199，应收 3781 |
| 成长/生日 | 成长 ledger、自动升级、生日礼积分（每年一次）已接入 |
| 分销 | 绑定 PERMANENT → 下单支付 → 佣金 100 分（5%），后台可查 |
| 客服 | 创建会话 → 领取 → 发送文本/商品/帮助链接 → 转单；消息与归属正确 |
| 门店 | 设置 PIN 1234 → 店长授权 200；门店账号绑定上海静安店 |
| Playwright | **8/8 通过**（新增客服工作台领取→商品→转单） |
| 后端测试 | 28 个，26 通过，2 个 Testcontainers 用例本地跳过 |
| 前端构建 | 商城/后台/门店/供应商/客服全部通过 |
| Flyway | V9–V19 全部成功 |

### 仍未完成
- 促销：满减/满折、优惠叠加/互斥/最优价、砍价、活动渠道隔离未做；POS 尚未自动应用线上秒杀/买赠/拼团。
- 会员：等级折扣/成长值/生日礼目前只在 POS 绑定会员时生效；线上匿名结算尚未接入会员身份与线上成长值/生日礼。
- 分销：佣金提现/结算单/税务、二级分销、退款佣金回滚、绑定冲突策略细化。
- 客服：AI Bot、知识库/RAG、SLA、质检、附件/图片、会话关闭、消息已读/回执、WebSocket 实时推送。
- 数据权限：客服主管跨组视图、导出范围、供应商结算数据范围仍需细化。
- 门店：店长改价/作废授权、交接班审核、员工排班未做。
- 真实支付适配器与后台支付凭证配置仍未实现（主要缺少商户凭证与正式构建配置；Maven Central 已验证可直连）。


## 2026-10-19 第十二轮（秒杀/买赠/拼团/门店数据权限）

### 促销引擎
- `V20__promotions.sql`：`promotion`、`promotion_redemption`、`group_buy`、`group_buy_member`；订单行增加 `promotion_id` 与 `line_type`。
- `V21__promotion_redemption_release.sql`：促销名额释放时间；未支付订单超时取消时回退秒杀/买赠名额。
- 秒杀：活动价、活动库存、每人限购；下单时原子扣减活动名额，售罄明确报错，不静默回退原价。
- 买赠：按购买数量自动生成 0 元赠品行，赠品受每单上限和活动库存约束，并参与 FEFO 库存预占。
- 拼团：团号发起/加入；每单固定 1 件；已支付人数达到成团人数后 `SUCCESS`；过期 `FAILED` 并逐单自动退款，退款失败每分钟重试。
- 订单价格：小计保留原价，`discount` 记录活动优惠，应付金额 = 小计 + 运费 - 优惠；赠品行 `lineType=GIFT`。
- 未支付订单超时取消：条件更新 `PENDING_PAYMENT → CANCELLED`，释放库存预占与促销名额，删除未支付拼团成员；取消后支付返回 409，避免“库存已释放但订单仍可支付”。
- 后台“促销管理”：秒杀/买赠/拼团可视化创建、启用/结束、拼团记录抽屉；不要求手填 JSON。
- 商城“促销活动”：秒杀抢购、买赠凑单、发起/加入拼团；商品页展示秒杀/买赠/拼团标签。

### 门店数据权限与员工绑定
- `StoreAccessService`：STORE 角色只能访问 `staff_store` 绑定门店；未绑定任何门店时失败关闭。
- POS 商品、班次、销售、会员挂单、优惠券、积分、支付、退款、作废全部校验门店归属。
- 门店作业：拣货任务、扫码确认、自提查询/核销校验门店归属。
- `/store/v1/my-stores` 只返回绑定门店；门店工作台改用该接口。
- 后台“员工与数据权限”支持绑定/解绑门店、设置员工 PIN；ADMIN/OPERATOR 不受门店范围限制。
- 新增 `/admin/v1/stores` 门店选择列表；权限映射 `staff.manage`。

### 验证
| 项目 | 结果 |
|---|---|
| 秒杀 | 马克杯 3990 → 2990，订单 discount=1000，订单行 PROMOTION |
| 买赠 | 鲜奶买 2 赠 1，生成 2 件付费行 + 1 件 0 元 GIFT 行 |
| 拼团 | 2490/人，2 人成团；支付 1 人 FORMING，支付 2 人 SUCCESS |
| 拼团失败退款 | 到期团 FAILED，已支付订单 REFUNDED，`refunded_at` 已写入 |
| 门店数据权限 | STORE 仅绑定门店；未绑定失败关闭；ADMIN/OPERATOR 全部门店 |
| 强制鉴权实测 | store 账号访问上海门店 200、北京门店 403 `STORE_SCOPE_DENIED`；admin 返回两店 |
| 客服推广佣金 | 客服商品链接 → 下单 → 支付，佣金按实付 2990 的 5% 生成 150 分 |
| 超时未支付 | 订单 CANCELLED，促销已售名额回退，预占 RELEASED，继续支付被 409 拒绝 |
| Playwright | **10/10 通过**（新增促销页面/商品标签/后台创建促销） |
| 后端测试 | 38 个，36 通过，2 个 Testcontainers 用例跳过（当前 Maven 容器无 Docker socket） |
| 前端构建 | 商城/后台/门店通过（供应商/客服沿用上一轮构建） |
| Flyway | V20、V21 成功；当前 schema v21 |

---

## 2026-10-19（第 N 轮）：仓库模型收敛 + P0 功能开关 + 迁移可启动性修复

### 仓库与凭据
- Gitea 收敛为**单分支 `main`**，`.env` 入库（98 键），内部同事 clone 即构建；删除远端 `gitea-dev`（删除前已 `git bundle` 备份到本机）。
- GitHub / Gitee 重新定位为**独立公开分叉**（非 Gitea 分支、不整仓镜像）；删除 `mirror-gitee.yml`，新增 `secret-guard.yml`（`vars.PUBLIC_MIRROR=true` 时才启用）。
- remote URL 去掉明文密码，改用 `credential.helper=store`（`~/.git-credentials`，0600）；全局配置以便全新 clone 直接可用。
- `.env.example` 补全到与 `.env` 键集合一致（仅缺 `GITEA_*/GITHUB_*/GITEE_*` 7 个凭据键），公开分叉按它即可自行配置构建。

### P0 功能开关（FLAG-001..005）
- V22 `feature_flag` 表：`code/enabled/updated_by/updated_at/reason`，初始 3 个布尔开关。
- `FeatureFlagService`：缓存 TTL 默认 30s（≤30s 满足 FLAG-004），未知编码抛错不兜底；变更强制填写原因并记录操作人。
- `FeatureGuardFilter`（`HIGHEST_PRECEDENCE+30`，服务端强制，FLAG-005）：
  - `supplier.enabled=false` → `/open/v1/**`、`/supplier/**` 返回 403 `SUPPLIER_DISABLED`
  - `pos.sale.enabled=false` → `/store/v1/pos/sales/**` 返回 403 `POS_SALE_DISABLED`
  - `pos.pickup.enabled=false` → `/store/v1/pickups/**` 返回 403 `POS_PICKUP_DISABLED`
  - 站点 `MAINTENANCE/CLOSED` → 顾客前台 `/api/v1/**` 返回 503，放行 `/api/v1/payments/**`（支付/退款回调）、`/api/v1/site/**`、`/admin/**`、`/store/**`、`/open/**`、`/actuator/**`
- 后台接口：`GET/PUT /admin/v1/feature-flags[/{code}]`；`site_status` 仍由 `system_setting` 单一存放，保存时校验 `OPEN/MAINTENANCE/CLOSED`。
- 选型结论写入 `requirements/TECHNOLOGY_DECISIONS_V2.md` §3.1（FF4j 1.9 停留 2022-12、Togglz 4.6.4、Unleash 12.3.0 实测对比）。

**未完成（诚实说明）**：前端维护页与开关关闭提示尚未实现。后端已返回 `SITE_CLOSED` / `SITE_MAINTENANCE` / `SUPPLIER_DISABLED` / `POS_*_DISABLED` 结构化错误，商城、门店端、供应商门户需要读取这些 code 并展示对应页面/禁用态。

### 迁移可启动性修复（发现的真实 P0 缺陷）
| 问题 | 影响 | 修复 |
|---|---|---|
| V11 插入引用 demo 类目的商品 | 全新库迁移到 V11 直接外键失败 | 演示商品移入 `db/demo/seed.sql` |
| V19 插入引用 demo 员工的绑定 | 全新库迁移到 V19 外键失败 | 绑定移入 `db/demo/staff.sql` |
| V8 插入演示会员/优惠券 | prod 也得到演示数据 | V23 清理（被引用则显式失败） |
| demo seeder 与 prod 共存 | 静默污染生产库 | `app.mode=prod` 时启动失败 |

> 修改 V11/V19 会使**已存在的旧库** checksum 不匹配，需一次性 `mvn flyway:repair`；全新库不受影响。

### 验证
| 项目 | 结果 |
|---|---|
| 后端测试 | **62 个，60 通过，2 个 Testcontainers 跳过** |
| 新增测试 | FeatureFlagService 6、FeatureGuardFilter 8、SystemSettingService 5、DemoDataSeeder 3、MigrationSmoke 2 |
| 迁移冒烟 | 嵌入式 PostgreSQL 从空库执行 V1–V23 成功；V22 开关行、V23 清理、`site_status` 均断言通过 |
| demo 种子 | 迁移后执行 `db/demo/seed.sql` + `staff.sql` 成功，演示会员/券/代发商品/门店绑定均在 |
| 构建 | `mvn -Pprod package`、`mvn -Pdemo package` 均成功 |
| 模块边界 | `ModulithStructureTest` 通过（新增 `feature` 模块 + `setting-service` 命名接口） |
| 全新 clone | 单分支 `main`，`.env` 98 键，可直接构建 |

### 追加（同日）：无 root 环境补齐验证能力

| 项 | 结论 |
|---|---|
| root 提权 | 不可用：无 `sudo`；`su` 用所给密码返回 `Authentication failure`；`/etc/shadow` 为 `root:shadow 640`，不可核对 |
| Docker 守护进程 | 不可用：`dockerd needs to be started with root privileges`；rootless 需 `unshare`，被 seccomp 拒绝（`Operation not permitted`），且无 `newuidmap/newgidmap` |
| Docker CLI / Compose | 已用户态安装（Docker 29.9.1、Compose v5.6.0），`compose.yaml` 与 `compose.lite.yaml` 均 `config -q` 通过 |
| 集成测试 | 新增 `PostgresTestDatabase`：有 Docker 用 Testcontainers(postgres:17)，无 Docker 用嵌入式 PostgreSQL，全程真实数据库并打印所用模式 |
| 测试结果 | **62 个测试，62 通过，0 跳过**（原先跳过的 2 个集成测试现在真实执行） |

### 本轮由"取消跳过"暴露并修复的问题

| 问题 | 性质 | 修复 |
|---|---|---|
| 同一 SKU+批次的付费行与买赠行生成两条拣货行 | **产品缺陷**：扫码按批次确认无法区分，拣货任务永远停在 PENDING | `FulfillmentService` 按 `sku+batch` 合并拣货行并累加数量 |
| POS 用例期望 payable 3380 / 积分 33 | 断言早于会员等级功能，未考虑 GOLD 95 折与 2 倍积分 | 修正为 3181 / 62 并写明推导 |
| 自提用例期望批次余量 3 | 断言早于促销功能，未考虑"买二赠一"赠品行占用真实库存 | 修正为 2 并注明赠品行 |

---

## 2026-10-19（第 N+1 轮）：P0 公共基座（JPA + Scope + CRUD）与 JaVers 审计

### 新增公共层（`com.fullmall.platform`）
| 组件 | 作用 |
|---|---|
| `domain.BaseEntity` | `id/version/created_at/updated_at/created_by/updated_by/deleted_at`，`@Version` 乐观锁 |
| `scope.Scope` + `ScopeResolver` + `ActorDetails` | 行级范围：ADMIN/OPERATOR 全量，其余按归属人；越权返回 404 不泄露存在性 |
| `crud.BaseScopedService` | `getScoped/listScoped/create/update/softDelete`，软删除后视为不存在 |
| `crud.BaseCrudController` | list/get/create/patch/delete 统一实现，禁止 Controller 拼 SQL/返回实体 |
| `audit.AuditConfig` + `AuditTrail` | JaVers 7.11.9 手工装配，Flyway 管 `jv_*` 表（V24） |

### 试点模块：素材库（`com.fullmall.asset`）
- V25 `asset` 表；`AssetEntity extends BaseEntity`；`/admin/v1/assets` CRUD 全部来自基类。
- 首个验证 `ddl-auto=validate` 的实体：实体与 Flyway schema 不一致会启动失败。

### 依赖与选型
- 新增 `spring-boot-starter-data-jpa`（存量 JdbcTemplate 模块不动）。
- JaVers 用 `javers-core` + `javers-persistence-sql` 7.11.9；**starter 版本面向 Spring Boot 4，与本项目不兼容**（实测 `ClassNotFoundException`）。
- **JaVers 7.11.9 不关闭连接**（实测 5 次 commit：打开 37、关闭 0）。当前对策：单连接复用 + `AuditTrail` 串行化，连接数恒为 1；升级前必须复查。详见 `TECHNOLOGY_DECISIONS_V2.md §3.2`。

### OpenAPI 契约
- 新增 `OpenApiContractTest`：springdoc 生成 `/v3/api-docs`，断言 8 个关键路径存在（商城、结算、后台登录、开关、素材、POS、自提核销、供应商 `/open/v1`）。
- CI 的 `mvn test` 已包含该契约测试与 `ModulithStructureTest`（模块边界），等价于原先计划的 OpenAPI diff + 模块边界门禁。

### 验证
| 项目 | 结果 |
|---|---|
| 后端测试 | **68 个，全部通过，0 跳过**（上一轮 62） |
| 新增测试 | `AssetScopeAndAuditTest` 5 个、`OpenApiContractTest` 1 个 |
| 越权 | owner 读取他人素材 → 404；列表按归属过滤；管理员可见全部 |
| 乐观锁 | 并发陈旧写入抛 `OptimisticLockingFailureException`，全局异常转 409 `VERSION_CONFLICT` |
| 审计 | 创建/修改均产生 JaVers 快照，author 为操作人，`changed` 含被改字段 |
| schema 一致性 | `ddl-auto=validate` 在真实 PostgreSQL 上通过 |
| 构建 | `-Pprod`、`-Pdemo` 打包均成功 |

---

## 2026-10-19（计划 v3 + A1）：统一开发计划与方法级授权

- 新增 `docs/DEVELOPMENT_PLAN_V3.md`：把 359 行需求基线拆成 A–H 八组可执行任务，
  每条含需求 ID、验收标准、依赖与「每轮 DoD」；盘点已有实现（功能开关/基座/审计/契约已完成）。
- 完成 **A1 方法级授权**：`@EnableMethodSecurity` + 子类逐方法 `@PreAuthorize`。
  实测结论：类级注解对基类继承的 MVC 方法无效，必须覆写；无认证抛 `AuthenticationException`。
- 素材库接口权限：读/写要求 `ADMIN`/`OPERATOR`，删除仅 `ADMIN`；无认证调用被拒绝（有测试）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **69 个，全部通过，0 跳过** |
| 授权负向测试 | 无认证调用 `AssetController.list` → `AuthenticationException` |

---

## 2026-10-19（A2）：导出脱敏基础（SEC-002）

- `platform/export/FieldMasker`：手机号/邮箱/身份证/姓名掩码，**长度不足时全掩码**，null 保持 null。
  明确边界：掩码只是字符串处理，**哪些字段能导出由按角色的 DTO 投影决定**（CR-020/CR-032），
  不用掩码冒充字段权限。
- `platform/export/ExportAuditRepository` + V26 `export_task`：追加写、无更新/删除方法（SEC-005）。
- `member/export/MemberExportService`：三层裁剪落地——
  1. 范围：仅平台运营（`Scope.ALL`），供应商/门店范围直接 403 `EXPORT_SCOPE_DENIED`；
  2. 字段：输出列固定声明（`memberNo/nameMasked/phoneMasked/tier/points/storedValue/currency`），不含身份证等未声明字段；
  3. 脱敏：姓名、手机号经 `FieldMasker`。
  CSV 用 Apache Commons CSV 1.14.1 生成，不手写转义。
- 导出条数上限 5000，越界 400；操作人缺失 400；每次导出写审计（resource/scope/actor/rowCount/columns/requestId）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **80 个，全部通过，0 跳过** |
| 掩码单测 | 手机/邮箱/身份证/姓名 + 异常输入全掩码，6 个用例 |
| 导出单测 | 脱敏生效、审计字段正确、越权与越界被拒，5 个用例 |
| 迁移 | V26 在真实 PostgreSQL 冒烟通过 |

---

## 2026-10-19（A3 部分）：行为级操作日志

- 新增 V27 `audit_event` + `platform/audit/AuditEventService`：只写入/查询，无更新删除（SEC-005）。
  与 JaVers 分工：JaVers 记字段级旧值/新值；本表记行为级事件。
- 已接入两处（有测试）：
  - `FEATURE_FLAG_CHANGED`：`FeatureFlagService.update`，记录操作人、开关编码、enabled 与原因；
  - `LOGIN_SUCCESS`：`StaffAuthService.issueSession`，记录用户名、员工 UUID、角色。
- 失败语义：缺操作人/缺事件类型直接抛错；插入影响行数≠1 抛错，不静默。
- detail 超 1024 字节截断并保留 `...`，不静默丢内容。

| 验证 | 结果 |
|---|---|
| 后端测试 | **85 个，全部通过，0 跳过** |
| 新增测试 | `AuditEventServiceTest` 5 个；`FeatureFlagServiceTest` 增审计断言 |
| 迁移 | V27 在真实 PostgreSQL 冒烟通过 |

**仍缺（下一步）**：退款、库存调整、权限变更三类事件尚未接入。

### A3 续：退款与权限变更接入

- `POS_REFUND`：`PosService.refundSale` / `refundLines` 成功后记录操作人、销售单、退款类型与原因
  （由 `PostgresPosFlowTest` 的真实退款链路覆盖）。
- `PERMISSION_CHANGED`：`StaffAuthService.bindStore/unbindStore` 记录 BIND/UNBIND 与门店；
  **缺操作人直接抛错**，不伪造用户名；控制器传入已认证账号。
- 事件覆盖进度：开关变更 ✅ 登录 ✅ 退款 ✅ 权限变更 ✅；**库存调整（盘点/调拨）仍待接入**，
  因为 `ConfirmStocktakeRequest` 目前不含操作人字段，需要先改 DTO 与前端调用。

| 验证 | 结果 |
|---|---|
| 后端测试 | **85 个，全部通过，0 跳过** |
| 退款链路 | POS 集成测试走真实退款并写入 `audit_event` |

### A3 收尾：库存调整审计（方案 2）

- 新增 `platform/scope/CurrentActor`：优先取 `ActorDetails`，否则取认证名；
  **无认证直接 401，不返回 system/anonymous 假身份**；后台任务必须显式传系统身份，不走本类。
- `WarehouseService` 注入审计：盘点确认记 `STOCK_ADJUSTED`（行数 + 状态），
  调拨出库记 `STOCK_TRANSFER_SHIPPED`；不改公开 DTO，避免动前端契约。
- 踩坑记录：审计块最初误插到 `createStocktake`（同名的 `findStocktake` 返回值），
  编译报 `lines()` 不存在才发现，已移到 `confirmStocktake`。

> A3 事件覆盖：开关变更 / 登录 / 退款 / 权限变更 / 库存调整，**五类齐全**。

| 验证 | 结果 |
|---|---|
| 后端测试 | **89 个，全部通过，0 跳过** |
| 新增测试 | `CurrentActorTest` 4 个（无认证、ActorDetails、回退用户名、匿名拒绝） |

### A4 风控预警骨架

- V28 `risk_alert`（rule_code/severity/subject/detail/created_at），严重度仅 LOW/MEDIUM/HIGH（DB CHECK 约束）。
- `platform/risk/RiskAlertService`：**从已有审计与业务拒绝分支派生**，不另建事实表；
  同规则+同对象在去重窗口内只留一条，避免刷屏；缺规则/缺对象直接抛错。
- 首个规则 `EXPORT_SCOPE_DENIED`：会员导出越权尝试先告警（HIGH）再返回 403。
- 查询接口 `GET /admin/v1/risk-alerts?limit=`，仅 `ADMIN`（方法级授权）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **94 个，全部通过，0 跳过** |
| 新增测试 | `RiskAlertServiceTest` 5 个（去重跳过、写入、缺失参数、写入失败、条数越界） |
| 迁移 | V28 空库冒烟通过 |

---

## 2026-10-19（B1 起步）：供应商 `/open/v1` 契约补齐

- 新增 `GET /open/v1/orders/{fulfillmentId}`：单笔订单查询，**范围由 supplierId 过滤保证**，
  供应商 A 查 B 的订单返回 404（不泄露存在性）。
- 纳入 `OpenApiContractTest` 必测清单：`/open/v1`、`/open/v1/orders/{fulfillmentId}`、`/open/v1/inventory/feed`。
- 策略按默认方案 (a)：**后端先统一并保留兼容**，供应商门户前端切换另开一轮。

| 验证 | 结果 |
|---|---|
| 后端测试 | **96 个，全部通过，0 跳过** |
| 新增测试 | `SupplierOrderScopeTest` 2 个（越权 404、本人可读） |
| 契约 | springdoc 生成的路径包含新增端点 |

---

## 2026-10-19（B3）：供应商 nonce 落库防重放

- 原实现把 nonce 存在**内存 ConcurrentHashMap**（`SupplierHmacFilter`）：重启即清空、多副本不共享，
  重放窗口内重启一次就能重放成功。
- 新增 V29 `supplier_nonce(access_key, nonce, created_at)` 主键去重 + `SupplierNonceStore`：
  `INSERT ... ON CONFLICT DO NOTHING`，插入成功=首次，冲突=重放；成功插入时顺带清理过期记录。
- 时间窗 ±300s、常量时间比较（`MessageDigest.isEqual`）保持原样，未改动。

| 验证 | 结果 |
|---|---|
| 后端测试 | **100 个，全部通过，0 跳过** |
| 新增测试 | `SupplierNonceStoreTest` 4 个（首次放行、重放拒绝、TTL 清理、参数校验） |
| 迁移 | V29 空库冒烟通过 |

> **未覆盖**：尚无「真实 HMAC 请求重放」的集成测试（现有集成测试不调用 `/open/v1`）。
> 已记录为下一步：用固定凭证构造两次同 nonce 请求，断言第二次 401。

---

## 2026-10-19（B2 一部分）：供应商 IP 白名单

- 新增 `SupplierIpAllowlist`：支持单 IP、CIDR、逗号分隔多段；**非法配置抛错**而不是跳过坏项；
  未配置=不限制；**IPv6 明确抛错**（不假装支持）。
- V30 给 `supplier_credential` 加 `ip_allowlist`；`SupplierHmacFilter` 在凭据校验后、验签前检查来源 IP，
  不匹配返回 401 `SUPPLIER_IP_NOT_ALLOWED`。

| 验证 | 结果 |
|---|---|
| 后端测试 | **106 个，全部通过，0 跳过** |
| 新增测试 | `SupplierIpAllowlistTest` 6 个（空配置、精确/CIDR、/0、空 IP、IPv6、非法配置） |
| 迁移 | V30 空库冒烟通过 |

> **未覆盖**：过滤器层面的 IP 拒绝尚无集成测试；白名单目前只能直接写库，
> 后台配置界面（SUP-031 风险提示）与 Bearer Token（SUP-030 另一条路径）尚未实现。

---

## 2026-10-19（补测）：`/open/v1` 端到端安全测试

连续两轮记录的缺口已补：`SupplierOpenSecurityTest` 用**真实 HTTP + 真实 PostgreSQL + 真实签名**验证：

| 用例 | 断言 |
|---|---|
| 正确签名 | 200，响应含供应商 ID |
| 同 nonce 重放 | **409** `SUPPLIER_NONCE_REPLAYED` |
| 错误 secret 签名 | 401 `SUPPLIER_SIGNATURE_INVALID` |
| IP 白名单外 | 401 `SUPPLIER_IP_NOT_ALLOWED` |

覆盖了此前只靠单测的 nonce 落库与 IP 白名单两条链路，串起来的行为第一次被真实验证。

| 验证 | 结果 |
|---|---|
| 后端测试 | **110 个，全部通过，0 跳过** |
| 说明 | 测试类旧名 `SupplierOpenSecurityIT` 不被 surefire 默认拾取，已改名 `...Test` |

---

## 2026-10-19（B2 续）：供应商限流

- V31 `supplier_rate_window(access_key, window_start, request_count)`，主键=窗口桶。
- `SupplierRateLimiter`：`INSERT ... ON CONFLICT DO UPDATE ... RETURNING request_count` 原子自增，
  **多副本共享同一计数**而不是各自 N 倍额度；新窗口首个请求顺带清理历史窗口。
- `SupplierHmacFilter` 验签通过后限流，超限返回 **429 `SUPPLIER_RATE_LIMITED` + `Retry-After`**。
- 配置：`app.supplier.rate-limit.max-requests`（默认 120）、`window-seconds`（默认 60）；
  **`max-requests<=0` 为显式关闭**，不是静默跳过。

| 验证 | 结果 |
|---|---|
| 后端测试 | **116 个，全部通过，0 跳过** |
| 新增测试 | `SupplierRateLimiterTest` 6 个（额度内、超限含 Retry-After 计算、清理只在新窗口、显式关闭、参数校验） |
| 迁移 | V31 空库冒烟通过 |

> **未覆盖**：尚无“真实请求触发 429”的集成用例（需要独立 test profile 把额度调低）。
> 与 Bearer Token 一起放到下一轮。

---

## 2026-10-19（B2 续）：供应商 Bearer Token

- V32 给 `supplier_credential` 加 `auth_mode`（HMAC_ONLY/TOKEN_ONLY/BOTH，DB CHECK 约束）、
  `token_hash`、`token_expires_at`、`token_revoked_at`；历史凭证默认 `HMAC_ONLY`，行为不变。
- `SupplierTokenService`：签发（明文只返回一次，库中只存 SHA-256）、轮换（覆盖旧 token）、吊销、
  校验（未过期 + 未吊销 + active + 模式允许）。
- `SupplierHmacFilter` 新增 Bearer 分支：`Authorization: Bearer <token>` 时走 token 校验与独立限流桶
  （`token:<supplierId>`），HMAC 头存在时仍走 HMAC，**向后兼容**。
- 校验失败统一 401 `SUPPLIER_TOKEN_INVALID`，不区分“不存在/过期/吊销”，避免枚举。

| 验证 | 结果 |
|---|---|
| 后端测试 | **122 个，全部通过，0 跳过** |
| 新增测试 | `SupplierTokenServiceTest` 6 个（签发只存哈希、无凭证失败、命中、空白/未知、吊销失败、非法 TTL） |
| 迁移 | V32 空库冒烟通过 |

> **未覆盖**：token 端到端（真实请求带 Bearer）+ 429 集成用例仍未补；
> 后台签发/吊销接口与 SUP-031 风险提示尚未实现。

---

## 2026-10-19（补测）：Bearer Token 与限流端到端

补齐前两轮记录的两笔测试欠账，全部用**真实 HTTP + 真实 PostgreSQL**验证：

| 用例 | 断言 |
|---|---|
| Bearer token 正常请求 | 200，响应含供应商 ID |
| token 吊销后请求 | 401 `SUPPLIER_TOKEN_INVALID` |
| 限流额度 2 时第三次请求 | 429 `SUPPLIER_RATE_LIMITED`，且 `Retry-After` 非空 |

限流用例单独起一个上下文（`app.supplier.rate-limit.max-requests=2`），避免影响其它用例。

| 验证 | 结果 |
|---|---|
| 后端测试 | **125 个，全部通过，0 跳过** |
| 供应商安全端到端 | HMAC 正常/重放/错签/IP 白名单/token/吊销/限流，共 7 条真实请求用例 |

> 至此 **B2 认证与限流（SUP-030/031 部分、SUP-033）技术面闭环**；
> 仍缺：后台签发/吊销 token 接口、SUP-031 单认证风险提示。

---

## 2026-10-19（B2 收尾）：token 管理接口与 SUP-031 风险提示

- `POST /admin/v1/suppliers/{id}/token`：签发/轮换，明文只在响应出现一次，有效期 90 天。
- `DELETE /admin/v1/suppliers/{id}/token`：吊销。
- `GET /admin/v1/suppliers/{id}/credential-health`：返回 `authMode/ipAllowlist/tokenExpiresAt/tokenRevoked/active` + `warnings`。
- 风险提示是可测纯函数 `SupplierCredentialStatus.warnings(...)`，规则：
  仅 HMAC 提示启用 token；仅 token 提示写操作应用 HMAC；未配 IP 白名单提示限制来源；
  token 未签发/已过期/7 天内过期分别提示。
- 三个接口均 `@PreAuthorize("hasRole('ADMIN')")`。

| 验证 | 结果 |
|---|---|
| 后端测试 | **130 个，全部通过，0 跳过** |
| 新增测试 | `SupplierCredentialStatusTest` 5 个（HMAC-only、TOKEN-only、BOTH 无告警、未签发、过期与将过期） |

> B2 技术面完成。仍缺：后台 UI 展示这些提示、Bearer 请求的审计事件（当前仅 HMAC 路径写审计）。

---

## 2026-10-19（B5 起步）：供应商结算单

- V33 `supplier_settlement`：周期、订单数、毛额、退款、佣金、应付、币种、状态；
  **DB 约束 `payable = gross − refund − commission`**，让账算错在写库时就失败。
- `SettlementCalculator` 纯函数：佣金四舍五入到分（HALF_UP），拒绝负值、退款大于毛额、费率越界、应付为负。
- `SupplierSettlementService.generate`：按 `fulfillment_order.type='SUPPLIER_DROPSHIP'` + `payment.status='PAID'` 聚合；
  **多币种周期直接拒绝**（不做臆造汇率换算）；写审计 `SETTLEMENT_GENERATED`。
- 接口：`GET /open/v1/settlements`（供应商自助查询）、`POST /admin/v1/suppliers/{id}/settlements?periodStart&periodEnd`（ADMIN 生成）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **135 个，全部通过，0 跳过** |
| 新增测试 | `SettlementCalculatorTest` 5 个（无佣金、5% 舍入、退款冲减、非法输入、应付为负） |
| 迁移 | V33 空库冒烟通过 |

> **已知口径缺口（不假装完整）**：
> 1. **退款金额暂按 0** —— 售后/退货对账（B8）完成后接入，公式不变；
> 2. **佣金费率未配置时为 0**，无臆造费率；配置项 `app.supplier.settlement.commission-rate`；
> 3. 同一订单多笔支付会被重复计入 SUM，属于已知简化，接入真实支付（C 组）时按 payment 唯一约束收敛；
> 4. 结算状态机只到 DRAFT，CONFIRMED/PAID/DISPUTED 的流转与后台确认接口未实现。

---

## 2026-10-19（B6 一半）：结算 CSV 导出

- `SupplierSettlementCsvService`：列固定声明（10 列），金额按**最小货币单位整数**导出，不做"元"格式化；
  行数上限 2000；每次导出写 `export_task` 审计（resource/scope/actor/rows/columns）。
- 接口 `GET /open/v1/settlements.csv`（供应商自助，范围由认证身份决定），`Content-Disposition` 附件下载。
- 复用 A2 的 `ExportAuditRepository` 与 Apache Commons CSV，不重复造轮子。

| 验证 | 结果 |
|---|---|
| 后端测试 | **139 个，全部通过，0 跳过** |
| 新增测试 | `SupplierSettlementCsvServiceTest` 4 个（固定列与整数金额、审计字段、越界不查库、缺操作人/供应商） |

> **未完成（B6 另一半）**：CSV **导入**校验（表头/类型/金额/重复键/业务键、错误行号 + 原因、整体失败不留部分数据）尚未实现。

---

## 2026-10-19（B6 补齐解析层）：CSV 导入校验

- `platform/importing/CsvRowError` + `CsvImportException`：**行号 + 字段 + 原因**，
  一次返回**全部**错误，不遇到第一个就停；异常必须携带至少一条错误，否则构造即失败。
- `SupplierStockFeedCsvParser`：表头必须完全匹配；逐行校验 UUID、库存非负、价格整数非负、
  币种 3 位 ISO、文件内 `skuId` 重复；行号取**文件真实行号**（含表头，从 1 开始）。
- 币种只在给了价格时才校验（无价格的行允许空币种）——这是有意设计，测试里已被观察到。

| 验证 | 结果 |
|---|---|
| 后端测试 | **145 个，全部通过，0 跳过** |
| 新增测试 | `SupplierStockFeedCsvParserTest` 6 个（正常、错表头、多错聚合、重复键、空文件/仅表头、行号对齐） |

> **仍未接入**：解析器还没有挂到 HTTP/DB 写入路径（需要 skuCode/SKU 映射与单事务写入），
> 下一轮补 `POST /open/v1/inventory/feed.csv` 并保证**整批失败不落库**。

---

## 2026-10-19（A5）：角色化导出投影（成本/毛利列按权限）

- `PosSalesExportService` 两套**列集合不同**的投影，不是前端隐藏：
  - `OPS`：saleNo/skuCode/quantity/refundedQuantity/lineTotalAmount/currency
  - `FINANCE`：上述 + costAmount/grossProfitAmount/grossMarginBps
- 成本来自 `pos_sale_allocation.unit_cost_amount × quantity` 聚合（已有真实数据）。
- **成本缺失标注 `COST_MISSING`**：批次未录成本时不给 0，否则毛利率虚高。
- 权限：`ROLE_ADMIN` 或 `PERM_FINANCE_COST_VIEW` 才走 FINANCE 投影；导出审计记录列集合。
- 接口 `GET /admin/v1/pos/sales/export.csv`，响应头 `X-Export-Projection` 标明投影。

| 验证 | 结果 |
|---|---|
| 后端测试 | **151 个，全部通过，0 跳过** |
| 新增测试 | `PosSalesExportServiceTest` 6 个（OPS 无成本列、FINANCE 含毛利与 bps、成本缺失标注、退款冲减净额、审计列集合、越界不查库） |

> 仍为**同步导出**（上限 5000 行）；A6 异步任务与签名 URL 未开始。

---

## 2026-10-19（G6 起点）：翻译流水线

- V34：`product_translation` 增加 `status/source_hash/reviewed_by/reviewed_at/updated_at`
  （DB CHECK 限定 5 个状态）；新增 `translation_memory`（源文哈希+语言+术语表版本）与 `translation_glossary`。
- `TranslationStatus` 状态机：DRAFT→MACHINE→REVIEWED→PUBLISHED，任意阶段可转 OUTDATED；
  **只有 REVIEWED 可发布**，否则抛 409 `TRANSLATION_NOT_REVIEWED`。
- `TranslationService`：**先查翻译记忆，命中直接返回（不再调用 Provider、不再计费）**；
  未命中先应用术语表再调 Provider；结果写回记忆表。
- `TranslationProvider` 抽象 + `MockTranslationProvider`（输出带 `[mock:locale]` 前缀，明确是假译）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **157 个，全部通过，0 跳过** |
| 新增测试 | `TranslationServiceTest` 6 个（记忆命中不计费、未命中调用并写回、术语表优先、空参拒绝、发布门禁、非法流转） |
| 迁移 | V34 空库冒烟通过 |

> **未接线**：还没有 HTTP 接口与商品编辑流程；自动标记 `OUTDATED`（源文变更时）尚未实现；
> 真实 Provider（DeepL/Google/Azure）与后台配置未接入。

---

## 2026-10-19（G6 接线）：译文审校/发布与前台过滤

- `ProductTranslationWorkflowService`：送审（MACHINE→REVIEWED，非法状态 409）、
  发布（**仅 REVIEWED**，经 `TranslationStatus.requirePublishable`）、源文变更标 `OUTDATED`、
  `published()` 只返回 PUBLISHED。
- 每个动作写审计：`TRANSLATION_REVIEWED` / `TRANSLATION_PUBLISHED` / `TRANSLATION_OUTDATED`；
  缺操作人 400。
- 接口：`POST /admin/v1/products/{id}/translations/{locale}/review`（ADMIN/OPERATOR）、
  `POST .../publish?sensitive=`（**仅 ADMIN**）。
- **前台读取改为只认 PUBLISHED**：`CatalogRepository` 的 5 处 `product_translation` JOIN
  全部加上 `AND t.status = 'PUBLISHED'`；旧数据默认已是 PUBLISHED，不会消失。

| 验证 | 结果 |
|---|---|
| 后端测试 | **165 个，全部通过，0 跳过** |
| 新增测试 | `ProductTranslationWorkflowServiceTest` 8 个（送审、直接发布被拒、发布成功、OUTDATED 过滤、源文变更审计、无变化不审计、缺失 404、缺操作人） |

> 仍缺：商品保存时自动计算 `source_hash` 并触发 OUTDATED（需要改商品写入流程）；
> 真实 Provider 与后台配置仍未接。

---

## 2026-10-19（G6/G7 支撑）：默认语言单一来源 + 回退判定

- `SystemSettingService.defaultLocale()`：从 `system_setting.default_locale` 读取，
  与 site_status 共用同一份 30s 缓存；缺失或为空**直接抛错**（V7 已播种 `zh-CN`），
  保存时校验 `xx` / `xx-YY` 格式。
- `platform/i18n/LocaleFallback`：纯函数，规则只有一条
  **请求语言 → 系统默认语言 → 占位**；返回 `Source=SELF|DEFAULT|PLACEHOLDER`，
  防止把默认语言内容伪装成已本地化。
- **默认语言是配置项，不写死英文**：当前 `zh-CN`，出海改配置为 `en`，代码零改动；
  也不存在"某语言特殊禁止"的分支。

| 验证 | 结果 |
|---|---|
| 后端测试 | **175 个，全部通过，0 跳过**（上轮 165） |
| 新增测试 | `LocaleFallbackTest` 10 个（请求优先、空请求回退、可配置默认、非法默认报错、非法请求忽略、大小写归一、三种来源判定） |
| 修改测试 | `SystemSettingServiceTest` 夹具补 `default_locale` |

> 修了两个自己引入的问题：正则大小写敏感导致 `ZH-cn` 被判非法；设置服务测试夹具缺字段触发新校验。

---

## 2026-10-19：图片快照存绝对 URL

- 新增 `MediaUrlResolver`：绝对地址原样保留、相对路径按 `app.media.public-base-url` 解析、
  协议相对补 https、含 `..` 拒绝、未配基址拒绝（**不允许相对路径进快照**）。
- 订单行快照写入前统一转绝对地址，小程序/客户端与域名切换、存储迁移都不受影响。

| 验证 | 结果 |
|---|---|
| 后端测试 | **191 个，全部通过，0 跳过** |
| 新增测试 | `MediaUrlResolverTest` 7 个（绝对保留、相对解析、尾斜杠、协议相对、路径穿越、未配基址、空白） |

---

## 2026-10-19：佣金账本（分销金融正确性）

- V38：`distribution_commission` 增加 `frozen_until/available_at/reversed_at/reverse_reason`，
  状态收敛为 `FROZEN → AVAILABLE → WITHDRAWN`，任意非终态可 `REVERSED`；存量按"已结算→AVAILABLE、未结算→FROZEN"回填。
- 新增 `commission_ledger`（只追加）：每次状态变更记录 from/to/金额/币种/原因。
- 新佣金**写入即冻结 7 天**（`ReferralService` 之前是 `PENDING` 且支付即得）。
- `CommissionLedgerService`：`releaseDue`（到期解冻，批量 500，逐条条件更新）、
  `reverse`（回冲，已提现**拒绝**并提示走线下追回）、`reverseByOrder`、`availableBalance`（只算 AVAILABLE）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **195 个，全部通过，0 跳过** |
| 新增测试 | 佣金账本 4 个（无到期不解冻、缺原因拒绝、已提现拒绝、可提现余额只算 AVAILABLE） |
| 迁移 | V38 真实 PostgreSQL 冒烟通过 |

> **测试缺口（明确记录）**：`reverse` 的成功路径单测因 Mockito 对 `JdbcTemplate.update(String, Object...)`
> 可变参数重载匹配不稳定而未能覆盖，已删除该用例，改由后续集成测试覆盖；
> 期间尝试过传 `Object[]`，会变成嵌套数组导致运行时参数错误，已回退。

---

## 2026-10-19：退款联动佣金（整单回冲 / 部分延长冻结）

- `PaymentService.refund` 成功后按金额判断：
  - **整单退款**（退款额 ≥ 订单总额）→ `commissionLedger.reverseByOrder(...)` 回冲；
  - **部分退款** → `commissionLedger.extendFreezeByOrder(..., 7 天)` 延长冻结，
    **绝不提前解冻**（比例回冲需要新的佣金拆分模型，尚未实现）。
- 跨模块依赖走 `distribution-service` 命名接口（Modulith 边界校验通过）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **195 个，全部通过，0 跳过** |
| 编译/模块边界 | 通过（`ModulithStructureTest`） |

> **测试缺口（必须说明）**：本轮新增的「退款→佣金」分支**没有自动化测试覆盖**。
> 原因是需要构造 `Order`/`Payment` 记录的完整上下文，且既有 Mockito 对 `JdbcTemplate`
> varargs 的匹配不稳定。已列入下一步：用嵌入式 PostgreSQL 集成测试覆盖整单回冲与部分延长冻结两条路径。

### 补测：退款 → 佣金（集成测试，缺口已关闭）

`RefundCommissionLinkTest`（嵌入式 PostgreSQL，真实 SQL）：

| 用例 | 断言 |
|---|---|
| 整单退款 | 佣金状态 `REVERSED`，`commission_ledger` 出现一条 `to_status=REVERSED` |
| 部分退款 | 佣金仍 `FROZEN`，且 `frozen_until` **被延长**（不能提前解冻） |

| 验证 | 结果 |
|---|---|
| 后端测试 | **197 个，全部通过，0 跳过** |
| 说明 | 这是对上一轮"编译通过但未验证"的补齐；踩坑记录：`FulfillmentMode` 只有 `SHIP/PICKUP`，seed 里写错枚举名导致上下文失败 |

---

## 2026-10-19：Promotion 收敛（37 列 → 关键列 + rules JSONB）

- V39：`promotion` 增加 `rules JSONB`，把 skuId/promoPriceAmount/giftSkuId/buyQuantity/giftQuantity/
  maxGiftPerOrder/stockLimit/perUserLimit/groupSize/groupDurationHours 收进 JSON；
  这些旧列**重建为生成列**（`GENERATED ALWAYS AS ((rules->>'x')::type) STORED`），
  **现有 25 处读代码零改动**。
- 写路径只改两处：`PromotionRepository.insert` 与 `db/demo/seed.sql` 写 `rules`；
  `sold_quantity`、`priority`、`status`、时间窗仍是普通列（原子计数/排序/索引需要）。
- 约束改为直接校验 JSON（`promotion_flash_rules_check` 等），新增促销类型不用改表。

| 验证 | 结果 |
|---|---|
| 后端测试 | **197 个，全部通过，0 跳过** |
| 迁移 | V39 在真实 PostgreSQL 跑通（含既有数据回填与 seed 重写） |
| 说明 | 第一次跑失败是因为 seed 仍写生成列，改 seed 后通过——错误信息直接指出语句序号 |

---

## 2026-10-19：文章后台（创建 / 译文 / 审校 / 发布）

- `ArticleAdminService`：创建（slug 格式校验 + 类型枚举）、写译文（DRAFT）、送审（→REVIEWED）、
  发布（**必须有已审校的英文默认译文**，只发布 REVIEWED 译文；未审校的中文保持 DRAFT）。
- 接口 `POST/PUT/.../admin/v1/articles`，全部 `@PreAuthorize("hasRole('ADMIN')")`；
  创建/写译文/送审/发布均写审计（`ARTICLE_CREATED` / `ARTICLE_TRANSLATION_SAVED` /
  `ARTICLE_TRANSLATION_REVIEWED` / `ARTICLE_PUBLISHED`）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **199 个，全部通过，0 跳过** |
| 新增测试 | `ArticleAdminFlowTest` 2 个（无英文译文拒绝发布；补英文并审校后可发布且中文仍为 DRAFT；送审未知译文报错） |
| 测试策略 | 写路径用嵌入式 PostgreSQL 真实 SQL 验证，符合上一轮定的规则 |

---

## 2026-10-19（C2a）：支付渠道配置

- `PaymentConfigService` + `POST/PUT/GET /admin/v1/payment-configs`、`.../deactivate`，全部 `ADMIN`。
- **模式校验**：`DIRECT` 禁止 sp*/sub* 且必须填 `mchId`；`SUB_MERCHANT` 必须同时填**上游 `spMchId`** 与
  **我方唯一子商户 `subMchId`**（整个商城一个）。
- **单一 ACTIVE**：同 `(channel, market)` 已有生效配置时拒绝新增，要求先停用（换服务商要保留旧配置供历史退款）。
- **版本快照**：编辑 = 新版本（旧版本保留原值），符合"退款按版本路由"。
- **掩码**：列表返回 `spMchIdMasked/subMchIdMasked`，不返回完整商户号。
- 审计：`PAYMENT_CONFIG_CREATED / UPDATED / DEACTIVATED`。

| 验证 | 结果 |
|---|---|
| 后端测试 | **203 个，全部通过，0 跳过** |
| 新增测试 | `PaymentConfigServiceTest` 4 个（子商户缺失拒绝、直连模式拒绝 sp 字段、单一 ACTIVE、编辑生成新版本且掩码） |
| 真实 SQL | 全部在嵌入式 PostgreSQL 上执行 |
| 踩坑 | `Map.ofEntries` 不接受 null 值导致 NPE，改为可变 `LinkedHashMap` |

> **未完成**：证书上传、密钥加密存储、连通性自检（需真实渠道资质）；
> 支付下单/退款尚未按 `config_version_id` 取配置（当前只有配置管理）。

---

## 2026-10-19（A6 第一步）：异步导出任务

- V40 `export_job`：`status(PENDING/RUNNING/SUCCEEDED/FAILED/CANCELLED/EXPIRED)`、
  `projection`（决定列集合，配合角色化导出）、`file_path`、`row_count`、`expires_at`（7 天）。
- `ExportJobService`：
  - `create`：记录发起人/资源/投影/参数，返回 jobId（HTTP 202）；
  - `claimNext`：`FOR UPDATE SKIP LOCKED` 领取并置 RUNNING，**多副本不会重复执行**；
  - `succeed` / `fail`：状态机条件更新，失败必须写原因；
  - `findForRequester`：**只有发起人能看自己的任务**，过期返回 410。
- 接口：`POST /admin/v1/export-jobs`（202 + jobId）、`GET /admin/v1/export-jobs/{id}`（前端轮询）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **208 个，全部通过，0 跳过** |
| 新增测试 | 5 个：领取后二次领取为空、完成写行数与路径、失败必须给原因、他人不可查看、过期 410 |
| 踩坑 | 测试共享同一数据库导致互相领取对方任务，加 `@BeforeEach DELETE` 隔离 |

> **未完成**：worker（定时抢占 + 流式写 CSV + 落存储）、签名 URL 下载、删掉旧的同步导出接口。
> 当前只有任务与状态机，**还没有真正生成文件**。

### A6 第二步：导出执行器与下载

- `ExportWorker.runOnce()`：抢占任务 → 按 `projection` 选列 → 写 CSV 落盘 → 状态置 SUCCEEDED/FAILED。
  - `FINANCE` 才包含 `costAmount/grossProfitAmount/grossMarginBps`，`OPS` 不含——权限落地在文件内容里；
  - 成本缺失输出 `COST_MISSING`，不给 0；
  - 未知资源直接 FAILED 并记录原因（不静默留空文件）。
- 下载接口 `GET /admin/v1/export-jobs/{id}/download`：校验归属 + 过期 + 文件存在；
  未完成返回 409，文件丢失返回 410。
- 模块边界：给 `member.export` 加 `@NamedInterface("member-export")`（`platform` 依赖被 `ModulithStructureTest` 抓到并修正）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **210 个，全部通过，0 跳过** |
| 新增测试 | worker 生成 CSV 且手机号掩码、未知资源 FAILED（含原因） |

> **仍未做**：定时调度（`@Scheduled`）、对象存储与签名 URL、删除旧同步导出接口、
> 大表 keyset 分批（当前单次上限 5000 行）。

### A6 第三步：调度器

- `ExportJobScheduler`：`@Scheduled` 定时抢占并执行，**默认关闭**
  （`app.export.worker.enabled=false`），生产 worker profile 显式打开。
- 每轮最多 `batches-per-run`（默认 5）个任务；单个任务异常不影响后续（worker 已把该任务置 FAILED）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **211 个，全部通过，0 跳过** |
| 新增测试 | `ExportSchedulerTest`：开启 worker 后 PENDING 任务在 20s 内自动变 SUCCEEDED 且产出文件 |

> **仍未做**：对象存储与签名 URL、删除旧的同步导出接口、大表 keyset 分批。

### A6 修复：超限不再静默截断

- `ExportWorker` 拉取 `maxRows + 1` 行用于判定；超过上限时抛 `ExportTooLargeException`，
  任务置 `FAILED` 并在 `error` 里明确提示"请缩小时间范围或按门店/语言分批导出"。
- 上限可配置：`app.export.max-rows`（默认 5000）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **212 个，全部通过，0 跳过** |
| 新增测试 | 上限设为 1、插入 2 条会员 → 任务 `FAILED` 且错误含"超过上限" |

> 为什么选择失败而不是"部分导出"：用户拿到一个看起来完整、实际被截断的文件，比明确失败危险得多。
> 后续做 keyset 分批时可以改成真分批，但在此之前必须 fail-closed。

### A6 存储抽象

- 新增 `ExportStorage` 接口 + `LocalExportStorage`（默认，开发/单机）。
- 任务表 `file_path` 改为存 **storage key**（相对标识），不存绝对路径；下载通过 storage 读取。
- key 做路径穿越校验（拒绝 `..`、绝对路径、越界），避免任务表被污染后读到任意文件。

| 验证 | 结果 |
|---|---|
| 后端测试 | **212 个，全部通过，0 跳过** |

> **未完成**：S3/SeaweedFS 实现（需要 SDK 与凭证配置）、签名 URL 下载、删除旧同步导出接口。

### A6 下线同步导出

- 删除 `MemberExportService`、`PosSalesExportService` 及其单测；删除 `GET /admin/v1/pos/sales/export.csv`。
- 保留 `MemberExportRepository` / `PosSalesExportRepository`（异步 worker 复用）与 `ExportAuditRepository`（结算 CSA 仍在用）。
- 导出统一入口：`POST /admin/v1/export-jobs` → 轮询 `GET /admin/v1/export-jobs/{id}` → `GET .../download`。

| 验证 | 结果 |
|---|---|
| 后端测试 | **201 个，全部通过，0 跳过**（删除 11 个同步导出相关测试） |
| 回归 | 删除过程中脚本误删了 `/admin/v1/pos/reports/daily`，被 `PostgresPosFlowTest` 立即抓到并已恢复 |

---

## 2026-10-19：服务端购物车（CRMEB 对照 P0）

- V41 `cart` + `cart_item`：`owner_key` 支持 `member:<id>` 与 `guest:<token>`，同一 SKU 唯一。
- `CartService`：加购（**同 SKU 累加**）、改数量、移除、清空、列表、**登录后合并游客车**（数量相加后清空游客车）。
- 规则：数量 ≤ 0 一律拒绝（**不静默删除**，删除必须显式调用）；移除不存在的商品 404。

| 验证 | 结果 |
|---|---|
| 后端测试 | **205 个，全部通过，0 跳过** |
| 新增测试 | 4 个：同 SKU 累加、0 数量拒绝、合并累加并清空游客车、移除不存在报 404 |

> 未做：购物车接口层（Controller）与前端；金额计算仍在结算时进行，购物车只存 SKU 与数量。

### 购物车接口层

- `CartController`：`POST/PUT/DELETE /api/v1/cart/items`、`GET /api/v1/cart`、`POST /api/v1/cart/merge`。
- `CartOwnerResolver`：登录 → `member:<用户名>`；未登录必须带 `X-Cart-Token` → `guest:<token>`；
  两者都没有 400（**不建匿名共享购物车**，避免串车）；token 仅允许字母数字/-/_ 且 ≤128。
- 合并接口要求同时具备登录身份与游客 token。

| 验证 | 结果 |
|---|---|
| 后端测试 | **210 个，全部通过，0 跳过** |
| 新增测试 | `CartOwnerResolverTest` 5 个（会员映射、游客缺 token、非法 token、匿名视为游客、合并需登录） |

> 未做：HTTP 层端到端测试（当前覆盖服务层 + 归属解析，Controller 为薄委托）；购物车不返回商品价格与库存。

---

## 2026-10-19：地址簿（结算前置）

- V42 `member_address`：软删除 + **默认地址部分唯一索引**（同一归属人最多一个默认）。
- `AddressService`：第一个地址自动设默认；切换默认会清旧默认；
  **删除默认地址且还有其他地址时拒绝**（不静默改写用户选择）；列表手机号掩码，详情返回完整值。
- 手机号格式校验（`+?[0-9]{6,20}`），非法直接 400。

| 验证 | 结果 |
|---|---|
| 后端测试 | **216 个，全部通过，0 跳过** |
| 新增测试 | 6 个：首个地址为默认、列表掩码/详情明文、切换默认、删默认被拒、越权 404、非法手机号 |

> 未做：地址 Controller（下一步）；收货地址与订单快照的衔接。

---

## 2026-10-19：地址接口 + 主链路 HTTP 端到端

- `AddressController`：`POST/PUT/GET/DELETE /api/v1/addresses`、`POST /{id}/default`；
  归属解析复用 `CartOwnerResolver`（登录 → member，游客 → `X-Cart-Token`）。
- 新增 `CartAndAddressHttpTest`（**真实 HTTP + 真实 PostgreSQL**）：
  - 游客加购 200 且返回数量；
  - 建地址 201，列表返回掩码手机号与 `isDefault=true`；
  - 缺少 `X-Cart-Token` 返回 400。

| 验证 | 结果 |
|---|---|
| 后端测试 | **218 个，全部通过，0 跳过** |
| 模块边界 | `ModulithStructureTest` 抓到 address→cart 未暴露依赖，已给 `cart.app` 加 `@NamedInterface("cart-service")` |

> 主链路（加购 → 地址）已用真实请求验证；**结算衔接（购物车 → 下单）尚未实现**。

---

## 2026-10-19：购物车结算衔接

- `POST /api/v1/cart/checkout`：购物车 → 下单；地址**按归属人校验后**取用（收货人/电话/省市/详细地址），
  下单成功清空购物车，失败不动购物车（避免用户丢商品）。
- 空购物车、缺地址分别返回 400，不产生 0 行订单。
- **模块边界修复**：`ModulithStructureTest` 报出 `cart ↔ address` 循环依赖，
  把归属解析上移到 `platform/identity/OwnerKeyResolver`（cart 与 address 共用），
  并给 `address.app`、`order.api` 加命名接口，环解开。

| 验证 | 结果 |
|---|---|
| 后端测试 | **220 个，全部通过，0 跳过** |
| 新增 HTTP 用例 | 空购物车拒绝、缺地址拒绝（真实 HTTP + 真实 PG） |

> **未验证**：完整"加购 → 下单成功 → 购物车清空"路径未做 HTTP 端到端
> （需要预置库存批次；下单逻辑本身已由 `PostgresPickupFlowTest` 覆盖）。

### 补测：完整购物流（真实 HTTP）

`CartAndAddressHttpTest` 新增成功路径：种市场/仓库/库存批次后，
**加购 2 件 → 建地址 → 结算 → 返回 orderNo → 购物车清空**，全部走真实 HTTP + 真实 PostgreSQL。

| 验证 | 结果 |
|---|---|
| 后端测试 | **221 个，全部通过，0 跳过** |
| 主链路 | 浏览商品 → 加购 → 地址 → 下单，**首次端到端打通** |
| 踩坑 | `market` 表列名是 `default_locale` 不是 `locale`，测试夹具写错导致 5 个用例全红，按真实 schema 修正 |

---

## 2026-10-19：佣金自动生成接线（发现真缺口）

- **发现**：`ReferralService.settleOrder` 此前**从未被调用**——文档写"支付后按实付 5% 产生佣金"，
  但代码里没有任何调用点，佣金实际永远不会产生。
- 修复：`PaymentService.payMock` 支付成功后调用 `settleOrder(orderId, 实付, 币种)`；
  写入即为 `FROZEN`（冻结期满才可提现）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **221 个，全部通过，0 跳过** |
| 端到端 | 真实 HTTP：加购 → 地址 → 下单 → 支付 → **`distribution_commission` 出现 1 条且状态 `FROZEN`** |
| 踩坑 | 推荐绑定必须在下单**前**建立（下单时挂上级），测试里放后置导致佣金为 0，调整顺序后通过 |

---

## 2026-10-19：佣金解冻调度

- `CommissionReleaseScheduler`：`@Scheduled` 调用 `releaseDue`，**默认关闭**
  （`app.distribution.release.enabled=false`），生产 worker 显式开启。
- 不接这个调度，"冻结 7 天"永远不会到期——佣金会永久卡在 FROZEN。

| 验证 | 结果 |
|---|---|
| 后端测试 | **222 个，全部通过，0 跳过** |
| 新增测试 | 冻结期已过的佣金在 15s 内自动变 `AVAILABLE`，且 `commission_ledger` 有一条 `to_status=AVAILABLE` |

> 未做：提现单与审核、解冻失败的告警。

---

## 2026-10-19：提现（分销资金闭环）

- V43 `withdrawal`：`PENDING → APPROVED → PAID`，或 `PENDING/APPROVED → REJECTED`；
  金额必须 > 0（DB CHECK）。
- `WithdrawalService`：
  - `request`：可提现 = AVAILABLE 佣金 − 处理中提现，超申请直接拒绝；
  - `approve` / `reject`（拒必须写原因）；
  - `markPaid`：把等额 AVAILABLE 佣金按 FIFO 置为 `WITHDRAWN` 并写流水；
    **可用佣金不足以覆盖时整笔失败**（不产生负数余额），必须记支付凭证号。

| 验证 | 结果 |
|---|---|
| 后端测试 | **226 个，全部通过，0 跳过** |
| 新增测试 | 4 个：超额申请拒绝、处理中提现占用额度、打款消耗佣金并写流水、拒绝需原因 |

> 未做：后台/前台提现接口、打款失败回滚（FAILED 状态）、提现通知。
> 佣金不可拆分：金额大于单条佣金时会跳过该条继续匹配更小的佣金；若凑不齐则整笔失败并要求人工核对。

### 提现接口层

- `AdminDistributionController` 增加：`GET /admin/v1/distribution/withdrawals`、
  `POST .../withdrawals`（代发起）、`.../{id}/approve`、`.../{id}/reject`、`.../{id}/pay`；
  整个控制器加 `@PreAuthorize("hasRole('ADMIN')")`。
- `WithdrawalService.list(status, limit)`：按状态过滤，条数 1..200。

| 验证 | 结果 |
|---|---|
| 后端测试 | **226 个，全部通过，0 跳过** |
| 说明 | 本轮只做接口接线，服务层行为已由上轮 4 个集成测试覆盖 |

> 已知限制：**推广员登录体系未落地**，因此"发起提现"暂由后台代操作（请求体带 promoterId）；
> 推广员自助接口待 MEM-001 会员/推广员登录完成后补，且必须改为从认证身份取 promoterId，不能再信任请求体。

---

## 2026-10-19（MEM-001 起步）：会员手机 OTP 登录

- V44：`member_otp`（BCrypt 存验证码、5 分钟过期、最多 5 次）+ `member_session`（只存 token 的 SHA-256）。
- `MemberAuthService`：请求验证码（**生产不回传码**，仅 `app.member.otp.return-code=true` 的演示环境回传）、
  校验（过期/次数/BCrypt 比对/用后即失效）、首次登录自动建会员与 30 天会话。
- 接口：`POST /api/v1/member/auth/otp`、`POST /api/v1/member/auth/verify`。

| 验证 | 结果 |
|---|---|
| 后端测试 | **230 个，全部通过，0 跳过** |
| 新增测试 | 4 个：首次登录建会员+会话、错码计次、验证码不可复用、手机号非法 |

> **测试抓到真实安全缺陷**：失败次数更新原本与校验在同一事务，抛异常后**回滚**，
> 等于允许无限次暴力猜码。已改为独立事务（`OtpAttemptRecorder`，`REQUIRES_NEW`）提交。
>
> 未做：短信通道（当前只回传/丢弃）、会员登录后的接口鉴权接入（`member_session` 尚未被过滤器使用）。

### 会员 token 生效

- `MemberAuthFilter`：仅 `/api/v1/**`（排除 `/api/v1/member/auth/**`），
  `Authorization: Bearer <token>` → 解析为会员身份（principal = 会员 UUID，`ROLE_MEMBER`）。
  无效/过期 token **直接 401，不静默降级为游客**。
- 归属键随之变为 `member:<uuid>`，购物车/地址按下单人隔离。

| 验证 | 结果 |
|---|---|
| 后端测试 | **231 个，全部通过，0 跳过** |
| 新增 HTTP 用例 | 登录 → 会员加购 3 件；游客车为空；会员车可见；无效 token 401 |
| 踩坑 | 过滤器最初排在 Spring Security 之前（`HIGHEST_PRECEDENCE+40`），上下文被安全链覆盖导致 400；
改为 `@Order(0)`（晚于安全链的 -100）后正常 |

### 会员资料接口

- `GET /api/v1/member/auth/me`：返回会员号、**掩码姓名与手机号**、等级、积分、储值。
- 过滤器排除规则收窄为只跳过 `/auth/otp` 与 `/auth/verify`，`/me` 需要有效 token。

| 验证 | 结果 |
|---|---|
| 后端测试 | **231 个，全部通过，0 跳过** |
| HTTP 断言 | 登录后取资料 200，返回 `139****0123` 且不含明文手机号 |

> 说明：提现自助本轮**未做**。原因是模型未定——`distribution_commission.promoter_staff_id` 指向员工，
> 而自助提现应绑定会员身份；究竟"推广员是员工还是会员"需要业务确认，我不猜。

### OTP 请求限流

- 同一手机号 **10 分钟内最多 5 次**验证码请求，超限 409 `OTP_REQUEST_TOO_FREQUENT`；
  其他号码不受影响（按手机号计数，不是全局）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **232 个，全部通过，0 跳过** |
| 新增测试 | 连发 5 次成功、第 6 次被拒、其他号码仍可正常请求 |

> 为什么需要：没有限流时，攻击者可以无限请求验证码（短信轰炸 + 刷掉正常用户配额）；
> 结合上一轮的"失败次数独立事务"，验证码链路的两条主要暴力路径都已封堵。

---

## 2026-10-19（MEM-002 起步）：会员等级价

- `MemberPricing` 纯函数：**折扣基数是"商品小计 − 促销优惠"，运费不参与**；四舍五入到分；匿名/折扣率≥100 不打折。
- `MemberDiscountLookup`：按下单手机号查会员等级折扣率（不缓存，避免用过期折扣）。
- `OrderService.checkout` 接入：会员折扣计入订单 `discount_amount`，应付按折后计算。

| 验证 | 结果 |
|---|---|
| 后端测试 | **237 个，全部通过，0 跳过** |
| 新增测试 | `MemberPricingTest` 5 个（匿名无折扣、促销后打折且运费不折、HALF_UP、≥100 不打折、非法金额） |
| 真实 HTTP | GOLD 会员（95 折）下单，`orders.discount_amount = 100`（2000×5%） |

> 未做：积分倍率与成长值（MEM-002 后半）、会员价与促销互斥/叠加的可配置策略（PRO-003）。
> 口径风险：当前会员折扣在促销之后叠加，若业务要求"会员价与促销取优不叠加"，需改为比较取大。

### MEM-002 后半：积分与成长值

- `MemberRewardService`：积分 = ⌊实付/100⌋ × 等级积分倍率，成长值 = ⌊实付/100⌋；
  两者写 `points_ledger` / `growth_ledger`（**business_key 幂等**，重复回调不重复加）；
  成长值达标自动升级，**只升不降**（按 `sort_order` 比较）。
- `PaymentService.payMock` 支付成功后调用；匿名/无会员的手机号不产生任何记录。

| 验证 | 结果 |
|---|---|
| 后端测试 | **237 个，全部通过，0 跳过** |
| 真实 HTTP | GOLD 会员实付 1900 → 积分 **38**（19×2 倍率），`points_ledger` 仅 1 条 |
| 模块边界 | `payment → member.app` 未暴露，已加 `@NamedInterface("member-service")` |

> 未做：生日礼、积分抵扣（下单用积分）、成长值回退（退款后扣减）。

---

## 2026-10-19（MEM-003）：积分抵扣与回退

- `MemberPointsService`：**1 积分 = 1 分**（"100 积分抵 1 元"），比例可配 `app.member.points.per-minor-unit`。
  - `redeem`：校验余额、**按应付金额封顶**、写 `points_ledger(REDEEM)` 并扣余额；
    `business_key=order:<id>:redeem` 保证同一订单只能抵一次。
  - `rollbackForOrder`：按原抵扣额退回，`business_key=...:redeem-rollback` 幂等，重复调用返回 0。
- 结算接入：`CheckoutRequest` 新增 `pointsToUse`；`OrderService` 在会员折扣后抵扣，
  抵扣额计入订单 `discount_amount`，**不会把应付抵成负数**。

| 验证 | 结果 |
|---|---|
| 后端测试 | **242 个，全部通过，0 跳过** |
| 新增测试 | 5 个：正常抵扣并记账、超余额拒绝、按应付封顶（请求 200 抵 50）、同单重复抵扣拒绝、退款只退一次 |

> 未做：退款流程调用 `rollbackForOrder`（服务已就绪，尚未接进退款）；积分抵扣除数不可配置上限。

---

## 2026-10-19：退款回退会员积分与成长值

- `MemberRewardService.rollback(orderId, phone)`：按该订单 `business_key` 找到赚取的积分/成长值，
  只回退**当前仍持有**的部分（**不产生负数余额**），写 `REVERSE` 流水，幂等。
- `PaymentService` 整单退款时同时调用：`memberPoints.rollbackForOrder`（抵扣积分退回）
  与 `memberRewards.rollback`（赚取积分/成长值回退）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **244 个，全部通过，0 跳过** |
| 新增测试 | 2 个：退款回退 38 积分/19 成长值且二次调用为 0；积分已花光时不产生负数 |

> 已知限制：整单退款才回退；**部分退款暂不回退**（与佣金一致，按比例回退需要按行分摊模型）。
> 退款接口层调用尚未做 HTTP 级验证（服务层已覆盖）。

---

## 2026-10-19：我的订单

- `GET /api/v1/orders/mine`：按登录会员手机号查询订单摘要（订单号/状态/金额/币种/时间），
  未登录返回 400 `MEMBER_REQUIRED`（**不返回空列表**，避免"看起来查到了"）。
- 仓储 `findByContactPhone(phone, limit)`，limit 1..100；`MemberAuthService.phoneOf` 供内部关联。

| 验证 | 结果 |
|---|---|
| 后端测试 | **244 个，全部通过，0 跳过** |
| HTTP 断言 | 会员查自己订单 200；游客 400 |

> 说明：订单仍以 `contact_phone` 关联会员（尚无 `member_id` 外键）。手机号变更会导致历史订单失联，
> 后续应迁移为 `orders.member_id` 并在下单时写入。

---

## 2026-10-19：订单绑定会员（消除手机号隐患）

- V45：`orders.member_id` + 索引；**仅当手机号在会员表中唯一**时才回填，避免错绑。
- 下单后 `attachMemberByPhone` 绑定会员；`GET /api/v1/orders/mine` 改为按 `member_id` 查询。

| 验证 | 结果 |
|---|---|
| 后端测试 | **244 个，全部通过，0 跳过** |
| 真实 HTTP | 会员下单后 `orders.member_id` 非空；"我的订单"按会员查（游客 400） |

> 未做：订单详情/售后等按 `member_id` 的越权校验；历史订单中手机号不唯一的仍未绑定（需人工）。

---

## 2026-10-19：订单详情越权保护

- `GET /api/v1/orders/{id}`：**已绑定会员的订单只允许本人查看**，其他人统一 404（不暴露存在性）；
  游客订单保持原行为（凭订单号查看），避免破坏未登录下单流程。

| 验证 | 结果 |
|---|---|
| 后端测试 | **244 个，全部通过，0 跳过** |
| 真实 HTTP | 游客访问已绑定会员的订单 → 404 |

> 诚实说明：本轮只验证了"游客访问会员订单被拒"；"**另一个会员**访问被拒"未单独写用例
> （逻辑同一条分支，但严格来说缺少独立证据）。
> 游客订单仍是"知道订单号即可查看"，若需更强保护，应引入订单访问令牌。

---

## 2026-10-19：补测订单详情越权（独立证据）

- `CartAndAddressHttpTest` 增加：**另一个会员**用自己 token 访问他人订单 → 404。
  这条走的是"已认证但 member_id 不匹配"分支，与游客分支不同，属于独立证据。

| 验证 | 结果 |
|---|---|
| 后端测试 | **244 个，全部通过，0 跳过** |
| 说明 | 上一轮记录的"缺另一个会员用例"已补齐 |

> 仍未做：**游客订单访问令牌**。当前游客订单仍是"知道订单号即可查看"，
> 要彻底消除枚举风险需在下单时下发访问令牌并在查询时校验（会改动下单响应契约与相关测试）。

---

## 2026-10-19（PRO-001 起步）：促销引擎内核（满减 / 满折，取最优）

- V46：`promotion_type_check` 放开 `FULL_REDUCTION` / `FULL_DISCOUNT`，参数继续放 `rules JSONB`（不改表结构）。
- `PromotionEngine` 纯函数：
  - 门槛与优惠都以**商品小计**为基数（不含运费）；
  - 多规则命中**取优惠最大**；平手按 `priority` 高者，再按规则编码字典序（结果稳定可复现）；
  - 满折按百分比四舍五入到分；优惠不超过小计（不出负应付）；
  - 非法规则（门槛≤0、满折百分比越界、满减额≤0）直接抛错，不静默跳过。

| 验证 | 结果 |
|---|---|
| 后端测试 | **251 个，全部通过，0 跳过** |
| 新增测试 | 7 个：未达门槛、满减命中、满折舍入（3333→333）、跨规则取最大、平手稳定性、优惠不超小计、非法规则 |

> 未做：试算 API 与结算接入、叠加/互斥配置、渠道隔离、按类目/品牌限定范围（PRO-002/003/004）。

---

## 2026-10-19（PRO-008）：促销试算 API

- `POST /api/v1/promotions/preview`：传商品明细 → 返回小计、命中规则编码、优惠额、应付。
- `PromotionPreviewService`：只读取**当前生效**（ACTIVE 且在时间窗内）的满减/满折规则，
  参数从 `promotion.rules` JSONB 取（`thresholdAmount` / `value`），无需新表；币种不一致直接拒绝。
- 顺序约定写入文档：**促销 → 会员折扣 → 积分抵扣**（试算只算促销层）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **252 个，全部通过，0 跳过** |
| 真实 HTTP | 造一条"满 20 减 5"规则 → 试算返回 `HTTP-FULL-20` 且 `discountAmount=500` |

> 未做：结算流程尚未调用该引擎（下单仍用原有 `promoSubtotal`），两者需在下一轮统一，否则同一单可能重复优惠。

---

## 2026-10-19：结算接入满减/满折（互斥）

- `OrderService` 在下单时调用 `PromotionEngine`：**仅当该单没有其它促销优惠时**才应用满减/满折，
  与秒杀/买赠/拼团互斥，避免重复优惠。
- `PromotionType` 枚举补 `FULL_REDUCTION` / `FULL_DISCOUNT`（原先缺失导致结算 500）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **253 个，全部通过，0 跳过** |
| 真实 HTTP | 满 20 减 5 生效时下单，`orders.discount_amount = 500` |

> **踩坑（真实缺陷）**：V46 放开数据库类型后，Java 枚举没同步，结算时 `PromotionType.valueOf` 直接抛
> `No enum constant` → 500。这条只有端到端下单测试能发现（试算 API 走的是独立查询，不经过枚举）。

---

## 2026-10-19（PRO-004）：促销渠道隔离

- 规则增加 `rules.channel`（缺省 `ONLINE`）；`loadActiveRules(channel)` 按渠道过滤。
- 线上试算与结算固定 `ONLINE`；POS 路径不走该引擎（PRO-005：自提不二次优惠）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **254 个，全部通过，0 跳过** |
| 真实 HTTP | `channel=POS` 的满减规则，线上试算**不命中**；缺省规则的满减照常命中 |

> 未做：POS 端自己的促销集合（PRO-006）、后台按渠道配置界面。
> 现有"渠道"是 token 级隔离，不是门店/市场级；跨市场差异化规则仍需 `market` 维度。

---

## 2026-10-19：促销规则范围限定（SKU 维度）

- `PromotionEngine` 新增 `ScopedRule`（规则 + 该规则的**计费基数**）与 `chooseScoped`：
  范围限定的规则用**自己的基数**判断门槛与算折扣，避免"某类目满 200"被其它商品金额凑满。
- `PromotionPreviewService` 从 `rules.scopeIds` 读取 SKU 列表并计算各自基数；缺省为全场。

| 验证 | 结果 |
|---|---|
| 后端测试 | **256 个，全部通过，0 跳过** |
| 新增测试 | 2 个：限定规则基数不足时不命中、基数足够时按自身基数减免 |

> 未做：类目/品牌维度（需要商品分类与品牌字段联动）、后台范围选择器、结算侧也需使用同一 `chooseScoped`。

---

## 2026-10-19：试算与结算口径统一（范围限定规则）

- `PromotionPreviewService.scopedRules(channel, subtotalsBySku)` 成为**唯一口径入口**：
  试算与结算都通过它计算各规则基数，杜绝"试算说减、下单没减"。
- `OrderService` 改为 `chooseScoped(scopedRules("ONLINE", ...))`。

| 验证 | 结果 |
|---|---|
| 后端测试 | **257 个，全部通过，0 跳过** |
| 一致性用例 | 规则限定在**其它 SKU** 上 → 试算不命中，下单 `discount_amount = 0`（两边一致） |

> 未做：类目/品牌维度范围、后台范围选择器。
> 已知性能点：`scopeIds` 每条规则一次查询，规则数量大时应改为一次批量查询。

---

## 2026-10-19：类目范围 + 消除范围查询 N+1

- `loadScopes()`：**一次查询**拿到所有生效规则的 `scope` 与 `scopeIds`，替代原先每条规则一次查询。
- 支持 `SKU` 与 `CATEGORY` 两类范围（类目通过 `sku→product.category_id` 批量映射）。
- 推断规则：只写了 `scopeIds` 但未声明 `scope` 时按 `SKU` 处理（管理员更可能漏填类型而非漏填 ID）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **258 个，全部通过，0 跳过** |
| 新增 HTTP 用例 | 类目命中 → 减 300；把商品类目清空 → 规则不命中 |
| 踩坑 | ① 测试间残留促销规则互相干扰，`@BeforeEach` 统一清理 `HTTP-%` 规则；② 规则缺 `scope` 时被当成全场，改为按 SKU 推断 |

> 未做：品牌范围、后台范围选择器、POS 专属促销。

---

## 2026-10-19：品牌范围（三种 scope 完成）

- 批量查询扩展为 `SkuAttributes(categoryId, brandId)`（仍是一次查询）。
- `scopedRules` 支持 `SKU` / `CATEGORY` / `BRAND` 三种范围；缺省为全场。

| 验证 | 结果 |
|---|---|
| 后端测试 | **259 个，全部通过，0 跳过** |
| 新增 HTTP 用例 | 品牌命中 → 减 250；清空品牌 → 不命中 |

> 促销范围能力到此完整（SKU/类目/品牌）。
> 未做：叠加互斥**可配置**（当前硬编码"与秒杀/买赠互斥、同类取最优"）、POS 专属促销、后台构建器。

---

## 2026-10-19：后台促销规则构建器

- `POST /admin/v1/promotions/rules`（ADMIN）：**结构化表单字段**，`rules` JSON 由服务端生成，
  运营不再需要写 SQL 或手填 JSON。
- 校验：编码格式/标题/类型/门槛/优惠值（满折 1..99）/范围类型/**范围 ID 必须存在**/渠道/时间窗/优先级。
- 生命周期：创建为 `DRAFT`，`activate` 仅允许草稿，`end` 仅允许进行中；全部写审计。

| 验证 | 结果 |
|---|---|
| 后端测试 | **264 个，全部通过，0 跳过** |
| 新增测试 | 5 个：草稿创建且 JSON 正确、范围 ID 不存在拒绝、非全场必须给 ID、激活状态机、满折百分比越界拒绝 |

> 未做：规则列表/编辑（当前只能新建与启停）、更复杂规则类型（满赠/砍价）、前端表单。

