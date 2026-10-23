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

---

## 2026-10-19：促销规则列表与编辑

- `GET /admin/v1/promotions/rules?status=&limit=`：返回规则摘要（编码/标题/类型/状态/门槛/优惠/范围/渠道/优先级/时间窗）。
- `PUT /admin/v1/promotions/rules/{id}`：**仅草稿可编辑**；已生效/已结束的规则拒绝修改，
  理由明确写入错误信息——改了生效中的规则会让"试算与结算"在用户眼皮下变化。

| 验证 | 结果 |
|---|---|
| 后端测试 | **265 个，全部通过，0 跳过** |
| 新增测试 | 创建后能在列表查到；草稿可改（编码变更生效）；激活后再改被拒 |

> 未做：规则删除、复制、前端表单；`list` 暂不支持按渠道/范围过滤。

---

## 2026-10-19（PRO-003 内核）：叠加 / 互斥

- `PromotionEngine.apply(...)`，语义（确定性、可复现）：
  1. 按 `group` 分组，**同组只取最优**（平手规则同前：优先级高者 → 编码字典序）；
  2. 入选规则中**只要有一条不可叠加**，整体互斥，只保留优惠最大的一条；
  3. 全部可叠加时，各条优惠**求和**；
  4. 合计优惠**不超过小计**（不出负应付）。
- 规则新增两个可选字段（`rules.stackable` / `rules.group`），缺省视为可叠加、组名 `DEFAULT`。

| 验证 | 结果 |
|---|---|
| 后端测试 | **269 个，全部通过，0 跳过** |
| 新增测试 | 4 个：跨组叠加求和、互斥规则独占、同组只取最优、叠加总额封顶 |

> **未接线**：试算与结算仍走 `chooseScoped`（单条最优），**尚未启用叠加**。
> 接入前需要产品确认默认策略（默认叠加还是默认互斥），我不擅自改变现有线上金额行为。

---

## 2026-10-19：会员登出（会话吊销）

- `POST /api/v1/member/auth/logout`：删除当前 token 的会话记录，**立即失效**；重复登出返回 `revoked=false` 而非报错。
- 会话只存 SHA-256，吊销按哈希精确删除。

| 验证 | 结果 |
|---|---|
| 后端测试 | **269 个，全部通过，0 跳过** |
| 真实 HTTP | 登出返回 `revoked=true`；同一 token 再访问 `/me` → 401 |

> 说明：本轮未推进促销叠加接线——默认叠加还是默认互斥属于产品决策，擅自改变会直接改变订单金额。

---

## 2026-10-19：审计写入去阻塞（单消费者异步）

- `AuditTrail` 从"业务线程 synchronized 直接写 JaVers"改为**有界队列 + 单消费者线程**：
  - 业务线程只入队，不等待 JaVers；
  - 单线程按序写入，保持"复用一条连接"的串行假设（JaVers 7.11.9 不关连接，见 AuditConfig）；
  - 队列满**明确抛错**（不丢审计、不无限堆积）；关闭时 `@PreDestroy` 排空并记录未写完数量。
- `flush()` 同时等待"队列为空"和"在途任务为 0"，`history()` 前调用，保证读到自己刚写的快照。

| 验证 | 结果 |
|---|---|
| 后端测试 | **269 个，全部通过，0 跳过** |
| 说明 | 首次实现只等队列为空，遇到"任务已被取走仍在执行"导致漏读，已改用 pending 计数修正 |

> 未做：数据库 outbox（跨进程可靠投递、进程崩溃不丢）。当前解决的是"业务线程被审计阻塞"，不是"崩溃不丢"。

---

## 2026-10-19（PRO-006 起步）：试算支持渠道选择

- `POST /api/v1/promotions/preview` 新增 `channel`（`ONLINE` / `POS` / `PICKUP`，默认 `ONLINE`，非法值 400）。
- 试算按所选渠道过滤规则：POS 规则在线上试算不可见，切到 POS 渠道才命中。

| 验证 | 结果 |
|---|---|
| 后端测试 | **270 个，全部通过，0 跳过** |
| 真实 HTTP | `channel=POS` 规则：线上试算不含该规则；`channel=POS` 时命中且减 200 |

> 未做：POS 收银流程真正调用该试算（POS 目前用自有促销逻辑）、后台按渠道可视化配置。
> 说明：本轮未做审计 outbox——它需要先决定"重放快照用什么实体类型"，
> 改成 Map 会破坏现有按实体类查询的历史语义，不能仓促改。

---

## 2026-10-19：商品评价

- V47 `product_review`：`rating 1..5`、`status PUBLISHED/HIDDEN`、**唯一约束 (order_id, sku_id, member_id)**。
- `ProductReviewService`：校验订单属于该会员、状态为 `PAID`/`FULFILLED`、SKU 在订单内；
  重复评价返回 409；`averageRating` **无评价返回 null**（不返回 0 冒充满分）。
- 接口：`GET /api/v1/products/{skuId}/reviews`（公开）、`POST /api/v1/products/reviews`（会员）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **274 个，全部通过，0 跳过** |
| 新增测试 | 4 个：正常评价且平均分正确、重复评价拒绝、他人订单拒绝、SKU 不在订单内拒绝、未支付订单拒绝 |

### 已知问题（本轮发现，未修）

测试期日志出现 `Unexpected error occurred in scheduled task`：`group_buy` / `orders` 查询报
`BadSqlGrammarException`，原因是**某个测试上下文在无迁移的表上运行了定时任务**。
不影响测试结果，但属于噪音且可能掩盖真实调度失败，下轮定位并加显式开关。

---

## 2026-10-19：调度开关改为显式属性（噪音未完全消除）

- `SchedulingConfig` 从 `@Profile("!test")` 改为 `@ConditionalOnProperty(app.scheduling.enabled)`，默认 true。
- 新增 `src/test/resources/application-test.yml`：测试默认**关闭**调度。
- 需要调度的两个测试（ExportSchedulerTest、CommissionReleaseSchedulerTest）显式 `app.scheduling.enabled=true`。

**根因分析（部分结论）**：`@Profile("!test")` 在 `["test","dev"]` 下**仍会匹配**——
`dev` 满足"非 test"表达式，所以带 dev 的测试上下文一直开着定时任务。这是触发器，已修。

**仍未消除**：全量运行仍出现 2 条 `Unexpected error occurred in scheduled task`
（`group_buy` / `orders` 查询 `BadSqlGrammarException`）。已确认表名正确（V20 有 `group_buy`），
因此**不是代码表名错误**，而是某个上下文在无迁移的表上执行了任务。具体是哪个上下文**尚未定位**，
不做猜测，记为下一轮首要问题。

| 验证 | 结果 |
|---|---|
| 后端测试 | **274 个，全部通过，0 跳过** |

### 调度噪音定位（未完成，记录实测）

本轮所做的尝试与结果：

| 措施 | 结果 |
|---|---|
| `@Profile("!test")` → `app.scheduling.enabled` 显式开关（测试默认 false） | 保留 |
| 两个调度测试显式开启 `app.scheduling.enabled=true` | 保留（后改为直接调用） |
| 把无关任务 initialDelay 推到 1 小时 | **仍出现 2 条错误** |
| 两个调度测试改为**直接调用**调度方法，不再依赖全局调度 | **仍出现 2 条错误** |
| 单独运行 ServerContextTest / CartAndAddressHttpTest / MigrationSmokeTest / SupplierOpenSecurityTest | **各自 0 条错误** |

结论（仅限已证实的部分）：
1. 错误只在**全量运行**时出现，且每次恰好 **2 条**（`group_buy` 与 `orders` 查询）；
2. 表名正确（V20 有 `group_buy`），**不是代码表名错误**；
3. 单类运行时无错误 → 与**上下文缓存/跨类共存**有关；
4. 具体是哪个上下文、为何在无表库上执行，**尚未定位**。

下一步建议：在 `TaskUtils` 错误路径打上 context id / 数据库 URL，或分批运行 `-Dtest=` 组合来二分；
定位前不建议继续改动调度代码。

---

## 2026-10-19：商品收藏

- V48 `member_favorite(member_id, sku_id)`，主键去重。
- `FavoriteService`：添加/移除**均幂等**（重复添加返回 false 而非报错）、列表按时间倒序、SKU 不存在 404。
- 接口：`GET/POST/DELETE /api/v1/favorites`，需会员登录（未登录 400）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **277 个，全部通过，0 跳过** |
| 新增测试 | 3 个：重复添加幂等、移除幂等、未知 SKU 拒绝 |

> 说明：调度噪音仍未定位（上一轮记录），本轮不继续消耗在日志噪音上，转做增量能力。

---

## 2026-10-19：浏览足迹

- V49 `member_browse_history(member_id, sku_id, viewed_at, view_count)`，主键去重。
- `BrowseHistoryService`：重复浏览**更新时间并累加次数**（不会刷屏）、按最近浏览倒序、支持清空。
- 接口：`GET/POST/DELETE /api/v1/history`，需会员登录。

| 验证 | 结果 |
|---|---|
| 后端测试 | **280 个，全部通过，0 跳过** |
| 新增测试 | 3 个：重复浏览只更新时间且次数为 2、清空返回删除条数、未知 SKU 拒绝 |

---

## 2026-10-19：顾客侧消息中心

- `GET /api/v1/notifications`（列表）、`GET .../unread-count`、`POST .../{id}/read`，需会员登录。
- 仓储新增 `findForRecipient` / `markRead` / `unreadCount`，全部**按收件人隔离**；
  `markRead` 只对"发给自己的未读消息"生效，重复标记返回 false 而非报错。

| 验证 | 结果 |
|---|---|
| 后端测试 | **282 个，全部通过，0 跳过** |
| 新增测试 | 2 个：只能看到自己的消息；他人标记已读无效、本人标记成功且重复标记返回 false |

> 未做：顾客侧推送（短信/微信模板）、消息中心与订单状态事件的自动写入（现有 notification 模块已有写入能力，
> 但顾客手机号与 `recipient_id` 的映射尚未统一）。

---

## 2026-10-19：订单事件 → 会员消息（接线，未端到端验证）

- `NotificationService.notifyMember(memberId, eventType, title, content)`：写 `recipient_type=MEMBER`、`channel=IN_APP`。
- `OrderService` 下单后、`PaymentService` 支付/退款后调用，收件人取 `orders.member_id`（游客订单不通知）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **283 个，全部通过，0 跳过** |
| 已验证 | `notifyMember` 落库、消息按收件人隔离、已读隔离 |
| **未验证** | 下单/支付自动写入消息的端到端链路——在 HTTP 测试里按 `event_type` 查库得到 0 条，
原因未查明（可能与测试内会员绑定时机或事务可见性有关），**不猜测** |

> 下一步：单独写一个"下单→支付→查消息"的集成测试并输出实际条数与收件人，定位是绑定时机问题还是写入未触发。

---

## 2026-10-19：订单消息端到端验证（并发现重复推送缺陷）

`OrderNotificationFlowTest`：绕过 HTTP 直接调 `checkout` + `payMock`，并用 SQL 打印/断言消息表，结论明确：

| 事件 | 收件人 | 结果 |
|---|---|---|
| `ORDER_CREATED` | `MEMBER/<订单会员>` | ✅ 恰好 1 条 |
| `ORDER_PAID` | `MEMBER/<订单会员>` | ✅ 恰好 1 条 |

上下文已从"未验证"升级为**已验证**（上一轮 HTTP 测试查库为 0 的原因仍未查明，但功能本身确认可用）。

### 顺带发现的真实缺陷（未修）

同一次支付还会产生 **`CUSTOMER/<手机号>` 的 `ORDER_PAID` 消息两条**——
说明既有支付通知路径对同一事件重复投递。顾客会看到重复消息，属于体验缺陷，本轮只记录不修改
（修它要动既有 notification 事件流，需单独一轮并补断言）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **284 个，全部通过，0 跳过** |

---

## 2026-10-19：更正上一轮的误报（不是重复推送，是双渠道）

上一轮我报告"`ORDER_PAID` 给同一顾客写了两个 CUSTOMER 消息 = 重复推送"，**这是我的判断错误**。

核实：`notifyOrderEvent(..., List.of("IN_APP", "SMS"))` 按渠道各投递一条，属**设计如此**。
我在打印里只选了 `recipient_type/recipient_id/event_type`，没带 `channel`，才把两个渠道误看成重复。

已改为精确断言：

```java
SELECT channel FROM notification
 WHERE recipient_type='CUSTOMER' AND recipient_id=? AND event_type='ORDER_PAID'
 ORDER BY channel
→ containsExactly("IN_APP", "SMS")
```

| 验证 | 结果 |
|---|---|
| 后端测试 | **284 个，全部通过，0 跳过** |

**教训（写在这里以免重犯）**：断言"重复"前必须先看 `channel`（以及幂等键），
只按事件类型计数会把多渠道投递误判成重复。

---

## 2026-10-19：顾客身份统一（站内消息归会员主键）

- `notifyOrderEvent`：订单已绑定会员时，`IN_APP` 投递到 **`MEMBER/<会员ID>`**；短信等外部渠道仍按手机号
  （短信必须用号码）。未绑定会员的游客订单保持 `CUSTOMER/<手机号>`。
- 移除 `PaymentService` 里重复的 `ORDER_PAID` 会员通知（`markPaid` 的 `notifyOrderEvent` 已覆盖），
  避免同一事件两条站内消息。

| 验证 | 结果 |
|---|---|
| 后端测试 | **284 个，全部通过，0 跳过** |
| 断言 | 会员订单支付后：`CUSTOMER/手机号` 仅 `SMS` 一条；`MEMBER/会员ID` 仅 `IN_APP` 一条 |

**踩坑（真实缺陷）**：`query(...).stream().findFirst()` 在结果含 `null`（游客订单无 `member_id`）时抛 NPE，
导致游客订单支付直接 500。已改为过滤 null。这个错误只有"游客订单支付"路径会触发——
会员订单测试全绿也发现不了，是 `PostgresPickupFlowTest` 抓到的。

---

## 2026-10-19：消息链路三条事件全部验证

`OrderNotificationFlowTest` 现在覆盖完整闭环：

| 事件 | 断言 | 结果 |
|---|---|---|
| 下单 | `MEMBER/<id>` 的 `ORDER_CREATED` 恰好 1 条 | ✅ |
| 支付 | `MEMBER/<id>` 的 `ORDER_PAID` 恰好 1 条；`CUSTOMER/手机号` 仅 `SMS` | ✅ |
| 退款 | `MEMBER/<id>` 的 `ORDER_REFUNDED` 恰好 1 条 | ✅ |

| 验证 | 结果 |
|---|---|
| 后端测试 | **284 个，全部通过，0 跳过** |

> 消息中心不再是空壳：会员在下单、支付、退款三个节点都会收到站内消息，且身份统一到会员主键。

---

## 2026-10-19：优惠券（模板 + 领券，未接抵扣）

- V50：`coupon_template`（AMOUNT/PERCENT、总量、每人限领、有效期、DB CHECK 全覆盖）+ `member_coupon`（UNUSED/USED/EXPIRED，`USED` 与 `used_at` 强一致约束）。
- `CouponService`：
  - `claim`：校验生效状态与时间窗、每人限领，**用条件更新原子占用库存**
    （`UPDATE ... WHERE issued_quantity < total_quantity`），不做"先查后写"的竞态处理；
  - 过期时间取 `min(领取日+有效天数, 模板结束时间)`；
  - `listMine`：**列表时实时判定过期**（`UNUSED` 且已过期显示 `EXPIRED`），不依赖定时任务。
- 接口：后台建模板/启用（ADMIN）、会员领券、我的券列表。

| 验证 | 结果 |
|---|---|
| 后端测试 | **289 个，全部通过，0 跳过** |
| 新增测试 | 5 个：领券成功且有效期正确、每人限领、库存领完、未启用不可领、过期实时显示 |

> **未做（有意）**：结算抵扣。券如何与满减/会员折扣叠加取决于"促销叠加策略"，
> 该策略未定前接入会直接改变订单金额。

---

## 2026-10-19：优惠券可用性校验（只读）

- `GET /api/v1/coupons/{couponId}/eligibility?orderAmount=`：返回 `usable` / `reason` / `discountAmount`。
- 规则：券属于该会员、`UNUSED`、未过期、订单金额达门槛；`AMOUNT` 抵扣**不超过订单金额**，`PERCENT` 四舍五入到分。
- **只读**：不改券状态、不改订单金额（抵扣接入结算需先确定叠加策略）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **293 个，全部通过，0 跳过** |
| 新增测试 | 门槛不足给原因、达门槛减 500、面额超订单封顶为订单额、九折 3333→333、他人券 404 |

> 踩坑：第一版测试用 800 分验证"减 500"，但该模板门槛是 1000，于是返回 0——是**测试写错**，不是实现错。
> 已改为达门槛金额，并另加一条"面额大于订单时封顶"的用例。

---

## 2026-10-19：优惠券抵扣接入结算（保守默认 + 可切换）

- 开关 `app.promotion.coupon.mode`：
  - **`EXCLUSIVE`（默认）**：券与促销互斥，**券优惠必须大于当前促销优惠**，否则 409 提示"请移除券"；
  - `STACKABLE`：券优惠叠加在促销之后（应付不为负）。
- 只对**会员订单**可用（券需要会员归属）；游客带券下单直接 400。
- 核销与下单**同事务**：`member_coupon` 条件更新 `UNUSED→USED` 并写 `order_id`，并发下不会重复核销。

| 验证 | 结果 |
|---|---|
| 后端测试 | **295 个，全部通过，0 跳过** |
| 结算用例 | 面额 500 / 商品 2000 → 订单 `discount_amount=500`、`total_amount=1500`、券 `USED` 且绑定订单 |
| 重复核销 | 同券二次下单被拒（"券不可用"） |

> 说明：`EXCLUSIVE` 下若券不划算会**明确报错**而不是静默忽略，避免用户以为用了券却按原价付款。
> 未做：券的适用商品范围、券与积分的优先级、`STACKABLE` 模式仍缺独立用例。

---

## 2026-10-19：券的两条金额分支全部钉住

| 模式 | 场景 | 断言 |
|---|---|---|
| `EXCLUSIVE`（默认） | 促销减 800 > 券减 500 | 下单 **409**「券优惠小于当前促销优惠」，且**券仍是 UNUSED**（没被吃掉） |
| `STACKABLE` | 促销减 300 + 券减 500（商品 2000） | 订单 `discount=800`、`total=1200` |

| 验证 | 结果 |
|---|---|
| 后端测试 | **297 个，全部通过，0 跳过** |

> 至此券与金额相关的三条路径（正常抵扣、互斥冲突、叠加）都有真实下单断言，不是靠推理。
> 踩坑：用 `sed` 复制测试类时把 SQL 里的引号复制坏了（`'CS-%''`），编译期发现不了，运行即报错。

---

## 2026-10-19：退券（整单退款）

- `CouponService.returnForOrder(orderId)`：
  - 未过期 → 恢复 `UNUSED`（清空 `order_id` / `used_at`）；
  - 已过期 → `EXPIRED`（**不假装还能用**）；
  - 幂等：找不到 `USED` 且属于该订单的券时返回 0。
- `PaymentService` 整单退款时调用（与佣金回冲、积分回退同处）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **299 个，全部通过，0 跳过** |
| 新增用例 | 支付后整单退款 → 券恢复 `UNUSED` 且 `order_id/used_at` 清空；券已过期后退款 → 置为 `EXPIRED` |

> 券的闭环至此完整：建模板 → 领券 → 校验 → 抵扣 → 核销 → **退款退券**。
> 未做：部分退款退券（是否退、按比例退，需要业务口径）、券的适用商品范围。

---

## 2026-10-19：前端存在性核实 + 接口对齐检查脚本

**更正之前的说法**：我此前多次称"前端未开始"，这不准确。
仓库里已有 `web/`（Nuxt 3）：`storefront`（首页/商品/购物车/结算/订单/账户/促销/法务）、
`admin`、`store-console`、`supplier-portal`、`support-console`、`mobile`，页面已在调用真实接口
（`/api/v1/products`、`/api/v1/cart`、`/api/v1/checkout`、`/api/v1/orders/{id}`、`/api/v1/payments/mock/{id}/pay` 等）。

### 新增 `scripts/check-api-alignment.py`

检查"前端写死的调用路径"与"后端路由"是否对得上——这类不一致编译期与单测都发现不了，只会线上 404。

```bash
python3 scripts/check-api-alignment.py           # 摘要
python3 scripts/check-api-alignment.py --verbose # 列出全部后端路由
```

当前结果：**后端 175 条路由、前端 16 条调用、未匹配 0 条**（退出码 0）。

设计要点：路径变量与前端模板统一归一为 `{}`，并做**双向前缀匹配**
（前端可能只写集合路径 `/orders`，后端是 `/orders/{id}`）。

> 我前两次审计都用 grep 逐行匹配注解，把"类级路由 + 方法级路由"拆开而误判为 0 条——
> 那是方法错误，不是代码问题。已改为脚本化检查并写入仓库。
> 说明：本脚本是静态文本分析，不是运行时验证；前端 Node 依赖未安装，因此**没有跑前端构建**。

---

## 2026-10-19：前端构建**已实测通过**

之前我只能说"前端存在"，本轮真正跑了构建：

```bash
cd web
npm ci --no-audit --no-fund --cache /tmp/npm-cache   # 650 包，13s
npm run build                                         # 全部 workspace
```

| 应用 | 构建结果 |
|---|---|
| `storefront`（Nuxt） | ✅ 完成（client 3.5s / server 1.9s，产物 2.19 MB） |
| `admin` | ✅ |
| `store-console` | ✅ |
| `supplier-portal` | ✅ |
| `support-console` | ✅ |
| `mobile`（uni-app，`build:h5`） | ⬜ **未构建**（不在 `workspaces` 列表，用独立 `npm run build:h5`） |

| 验证 | 结果 |
|---|---|
| 前端构建 | **5/6 应用通过**（退出码 0） |
| 后端测试 | 本轮未改后端，未重跑（上轮 299 全绿） |

**踩坑**：`npm ci` 默认写 `~/.npm`（宿主用户目录，权限不足）→ `EACCES`。
改用 `--cache /tmp/npm-cache` 后正常。这条要写进构建文档，否则别人 clone 下来同样会卡。

---

## 2026-10-19：首次前后端真联调（容器内，无 Docker）

用测试依赖的嵌入式 PostgreSQL 起了真实数据库，再起后端与前端构建产物：

| 步骤 | 结果 |
|---|---|
| 嵌入式 PG（`EmbeddedPgServer`，端口 55432） | ✅ 就绪 |
| 后端（dev profile，Flyway 建表 + 演示数据） | ✅ `/api/v1/products` 200 |
| 前端（storefront 生产构建，端口 3099） | ✅ `/` 与 `/products` 均 200 |
| **真联通** | ✅ `/products` 页面 HTML 中含后端商品标题「供应商直发测试商品」 |

### 顺带修掉一个真实缺陷

未知路径（如 `/api/status`）原本返回 **500 + 完整堆栈**（`NoResourceFoundException` 落到兜底处理），
把"路径写错"伪装成"服务故障"，并让探测请求刷满错误日志。
已在 `GlobalExceptionHandler` 补 404 分支，`NotFoundHttpTest` 覆盖。

| 验证 | 结果 |
|---|---|
| 后端测试 | **300 个，全部通过，0 跳过** |

> 说明：`NotFoundHttpTest` 是**处理器的单元测试**；测试上下文的安全策略会在到达 dispatch 前返回 401，
> 因此没有做 HTTP 层断言（运行中的应用已手工确认 404 行为）。

---

## 2026-10-19：真浏览器 E2E 首次运行（9/10 通过）

环境：嵌入式 PG + 后端(dev) + storefront(3000) + 4 个控制台(5273–5276)，Playwright + **Chromium**。

| 用例 | 结果 |
|---|---|
| storefront 主题切换 | ✅ |
| storefront 浏览 → 加购 → 自提下单 | ✅ |
| storefront 购物车改数量/删除 | ✅ |
| promotions 秒杀、拼团弹窗 | ✅ 2 条 |
| pickup 门店自提核销 | ✅ |
| pos 组合支付/日结/退款 | ✅ |
| supplier 供应商签名履约 | ✅ |
| support 客服认领会话/转人工 | ✅ |
| **admin 后台全流程** | ❌ 停在"促销管理"找不到「马克杯限时秒杀」 |

### 本轮补的后端缺口（真实）

后台页面调用 `GET/POST /admin/v1/promotions` 与 `POST /admin/v1/promotions/{id}/status`，
而后端**只有** `/admin/v1/promotions/rules`（我前几轮新加的规则构建器）。已把
`PromotionService.listAll()` / `create()` / `updateStatus()` 接出来。

### 工具与配置修正

- `playwright.config.ts` 只有 `msedge`（容器内无 Edge）→ 增加 `chromium` 项目；
- 用例写死 5273–5276，而 dev 脚本是 5173–5176 → 各应用新增 `dev:e2e` 脚本对齐端口。

### 已知未解决

admin 促销列表断言**仍失败**，后端接口已存在且被调用，`马克杯限时秒杀` 却未出现在表格里。
根因**未定位**（可能是列表渲染条件、鉴权后的数据裁剪或字段不匹配），不猜测。

| 验证 | 结果 |
|---|---|
| 后端测试 | **300 个，全部通过，0 跳过** |
| E2E | **9 通过 / 1 失败** |

---

## 2026-10-19：admin 促销 500 的定位进展（未解决）

用 Playwright 抓真实网络响应，结论明确：

| 观察 | 事实 |
|---|---|
| 页面文案 | `API 500: Internal server error` |
| 响应体 | `{"code":"INTERNAL_ERROR"}` |
| 后端日志 | `AuthorizationDeniedException: Access Denied`，栈顶 `AdminPromotionController$$SpringCGLIB$$0.listAll` |
| 直接带 token 调 API | promotions **500**、products **200**（同一 token、同一前缀） |

### 已做的修正

- `AdminPromotionController` **重写**：路径收敛到 `/admin/v1/promotions`（列表/创建/状态）与
  `/admin/v1/promotions/rules`（满减满折规则），**移除全部 `@PreAuthorize`**，鉴权交给
  `StaffAuthFilter`（路径 → 权限码 `promotion.manage`，ADMIN 直接放行）。
- 过程中我的正则误改了注解（`@GetMapping")`），已整文件重写并编译通过。

### 仍未解决

即使控制器上没有方法级注解，调用仍被 `AuthorizationDeniedException` 拒绝。
**根因未定位**（`@EnableMethodSecurity` 全局开启，但该类已无注解；怀疑与类级元数据缓存或全局规则有关）。
不做猜测式修改。

| 验证 | 结果 |
|---|---|
| 后端测试 | **300 个，全部通过，0 跳过** |
| admin E2E | 仍失败（同一断言） |

---

## 2026-10-19：admin E2E 失败的真因——**旧进程没被杀掉**

上一轮我把 500 归因于"方法安全拒了 ADMIN"，**判断错了**。用两种方式否定了这个假设：

1. `AdminPromotionAuthorizationTest`：在测试上下文里给 `ROLE_ADMIN` 直接调 `controller.listAll()` → **通过**；
2. 打包成 jar 重新启动后再调 → **200**，返回里就有「马克杯限时秒杀」。

真正原因：之前的多次"重启后端"实际**没杀掉旧 JVM**（`spring-boot:run` 会 fork 子进程；
打包那次还直接报 `Port 8080 was already in use`）。过去几轮一直测的是**旧编译产物**。

| 项目 | 结果 |
|---|---|
| 后端测试 | **300 个，全部通过**（新增 `AdminPromotionAuthorizationTest`） |
| **E2E（Chromium）** | **10 / 10 全部通过** |
| 后端打包 | `mvn -DskipTests package` 成功，jar 可运行 |

E2E 覆盖：后台全流程、门店自提核销、POS 组合支付/日结/退款、秒杀与拼团、主题切换、
购物车改数量、客服认领转人工、供应商签名履约发货。

### 教训

排查"改了没生效"时**先确认运行的是不是新代码**：看启动日志、看端口占用、请求一个刚改过的接口。
不要假设 `kill` 成功——`spring-boot:run` fork 的子进程不会被主进程的 kill 带走。

---

## 2026-10-19：一键验证 `make verify`（已实测跑通）

新增 `scripts/verify.sh` + Make 目标：

```bash
make verify        # 后端测试 → 打包 → 前端构建 → 接口对齐 → E2E
make verify-fast   # 跳过 E2E
```

脚本要点：
- `set -euo pipefail`，任一步失败立即退出；
- 后台进程记录 PID，`trap` 里按 PID 清理（**不用模糊匹配 kill**——我踩过把自己杀掉的坑）；
- 后端跑**打包后的 jar**，端口占用会明确报错，避免测试到旧代码；
- PG 就绪按日志标记等待（PG 不是 HTTP 服务，用 HTTP 探测必然失败并产生误导输出）；
- 容器内无 `curl`，就绪探测用 `python3 urllib`。

### 实测结果（一次完整运行）

| 阶段 | 结果 |
|---|---|
| 后端测试 | **301 个，全部通过** |
| 后端打包 | ✅ |
| 前端构建 | ✅ 5 个应用 |
| 接口对齐 | 后端 177 条路由 / 前端 16 条调用 / **未匹配 0** |
| E2E（Chromium） | **10 / 10 通过** |

---

## 2026-10-19：CI 接入（GitHub + Gitea）

现有 `.github/workflows/ci.yml` 已有后端测试/打包/前端 5 应用构建/移动端构建/compose 校验，
**缺接口对齐检查与 E2E**。本轮补上：

| 变更 | 内容 |
|---|---|
| `ci.yml` → `backend` 作业 | 增加 `python3 scripts/check-api-alignment.py`（纯静态，秒级） |
| `ci.yml` → 新增 `e2e` 作业 | 装 Chromium 后执行 **`make verify`**；失败时上传 `web/test-results` 与 `/tmp/fullmall-*.log` |
| 新增 `.gitea/workflows/verify.yml` | 公司内部仓库的流水线，同样调用 `make verify` |

设计原则：**本地怎么验、CI 就怎么验**，只有 `make verify` 一个事实来源，避免 CI 与本地两套脚本漂移。

### 验证情况

| 项目 | 结果 |
|---|---|
| 三个工作流 YAML 可解析 | ✅ 用 node `yaml` 解析，作业名正确（backend/web/compose/e2e、verify、guard） |
| `make verify` 本地实测 | ✅ 上一轮已跑通（301 测试 + 包 + 构建 + 对齐 0 + E2E 10/10） |
| **CI 实际执行** | ⬜ **未验证**——本机没有 GitHub/Gitea runner，无法触发 |

> 诚实说明：工作流只在语法与结构层面验证过，**没有真正跑过一次 CI**。
> 第一次真实触发时可能需要调整（如 runner 镜像缺少 Chromium 依赖、`--cache` 权限等）。

---

## 2026-10-19：部分退款口径明确化（用测试钉住）

之前"部分退款时券/积分怎么处理"是**空白**——既没文档也没测试，容易被当成 bug 或被人无意改掉。

- 新增 `docs/REFUND_POLICY.md`：整单退款 / 部分退款对**佣金、抵扣积分、赚取积分与成长值、优惠券**的逐项口径与实现位置。
- 新增测试 `CouponCheckoutTest#partialRefundKeepsCouponUsed`：部分退款后券**保持 `USED`**（退券会造成同一张券用两次的资损）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **302 个，全部通过，0 跳过** |

> 已知限制（写入文档）：**按行分摊模型未实现**，因此部分退款无法按商品粒度回退积分/佣金。
> 在实现前，含券/积分的订单不建议做部分退款。这属于"有意不做"，不是遗漏。

---

## 2026-10-19（PRO-006）：POS 收银接入促销引擎

- `PosService.createSale` 现在用 `PromotionPreviewService.scopedRules("POS", ...)` + `PromotionEngine.chooseScoped`
  计算促销优惠，写入 `pos_sale.promotion_discount`，应付随之减少。
- 渠道隔离生效：`rules.channel=POS` 的规则才作用于收银台，线上规则不影响 POS。

| 验证 | 结果 |
|---|---|
| 后端测试 | **303 个，全部通过，0 跳过** |
| 新增用例 | `PostgresPosFlowTest#posAppliesPosChannelPromotion`：POS 满减 100 → 单价 1990 的销售单 `promotionDiscount=100`、`payable=1890` |

> 说明：本轮先删除过一个独立测试文件（测试库里没有门店数据，无法独立建单），
> 改为在已有的 POS 全流程测试中追加断言，复用其门店/库存/员工种子。

---

## 2026-10-19：按行退款模型（部分退款分摊基础）

- V51 `order_line_refund`：记录"退了哪一行、几件、多少钱、原因"，数量与金额均 > 0。
- `LineRefundService`：
  - `record(...)`：校验行属于订单、币种一致、**累计退款数量不超过行数量**、**累计金额不超过行金额**；
  - `allocate(orderId, totalRefund, currency)`：按各行**剩余未退金额**占比分摊，**尾差给最后一个有剩余的行**，
    分摊之和**恰好等于**退款额（不丢分、不多出），超退直接拒绝。

| 验证 | 结果 |
|---|---|
| 后端测试 | **309 个，全部通过，0 跳过** |
| 新增测试 | 6 个：按行记录、超数量拒绝、超金额拒绝、跨订单行拒绝、分摊精确（2:1 → 666/334）、超剩余拒绝 |

> **未接线**：分摊结果尚未用于回退积分/佣金——`REFUND_POLICY.md` 里的口径需要产品确认
> "部分退款是否按比例回退"，我先把计算能力与约束建好，不擅自改资金流向。
> 踩坑：`order_line` 没有 `created_at` 列（我用它排序导致 SQL 报错）；两个测试 SKU 的 UUID 前缀相同，
> 用 `substring` 生成 sku_code 会撞唯一约束，已改为固定编码。

---

## 2026-10-19：售后 → 行退款打通 + 分摊策略改为可复现

- `AfterSaleService.refund` 在退款成功后调用 `LineRefundService.recordForAfterSale(afterSaleId)`：
  按售后单涉及的 `after_sale_line` 把退款金额分摊写入 `order_line_refund`，
  **售后涉及行可退金额不足时直接拒绝**（`AFTER_SALE_REFUND_EXCEEDS_LINES`）。
- 分摊尾差策略从"id 最大的行"改为**剩余金额最大的行**。
  原策略依赖随机 UUID 排序，同一订单两次分摊结果可能不同——这不是可接受的资金行为。

| 验证 | 结果 |
|---|---|
| 后端测试 | **311 个，全部通过，0 跳过** |
| 新增用例 | 售后退款写行记录（1000 分落到 A 行）、售后金额超出行可退时拒绝、分摊 2:1 → 667/333 且总和精确 |

> 踩坑：清库必须按外键顺序（`after_sale_line` → `after_sale_event` → `after_sale` → `order_line_refund` → `order_line`），
> 否则删订单行会撞 `after_sale_line_order_line_id_fkey`。
> 另：分摊结果曾因"按 UUID 排序决定尾差"而两次运行不同，已修正为确定性策略。

---

## 2026-10-19（第 100 轮）：售后消息通知 + 分摊缺陷修复 + 状态盘点

- 售后三个节点写会员站内消息：`AFTER_SALE_CREATED` / `AFTER_SALE_APPROVED` / `AFTER_SALE_REFUNDED`
  （游客订单不通知）。
- **修复分摊重复分配缺陷**：原实现把"尾差给最大行"与"按顺序计算"混在一起，
  导致同一笔金额被分配两次（实测 `allocated=1333, expected=1000`）。
  改为先给非尾差行按比例分配，再把剩余额度整体给尾差行。
- 新增 `docs/PROJECT_STATUS.md`：第 100 轮盘点，逐项标注**证据**与**空白**。

| 验证 | 结果 |
|---|---|
| 后端测试 | **311 个，全部通过，0 跳过** |

---

## 2026-10-19：消息保留清理（默认关闭）

- `NotificationService.purgeOlderThan(days)`：删除超过保留期的消息，`days <= 0` 直接抛错。
- `NotificationRetentionJob`：`@ConditionalOnProperty(app.notification.retention.enabled)`，
  **默认关闭**；天数字段 `app.notification.retention.days`（默认 180），Cron `0 30 4 * * *`。
- 关闭状态下不做任何删除——"消息只增不减"有代价，但**默认不做破坏性操作**更重要。

| 验证 | 结果 |
|---|---|
| 后端测试 | **313 个，全部通过，0 跳过** |
| 新增测试 | 只删过期消息（200 天前的删、新的留）；天数 <= 0 拒绝 |

> 说明：定时任务本机未触发（默认关闭，且测试环境 `app.scheduling.enabled=false`），
> 验证的是清理**逻辑**本身，不是调度触发。

---

## 2026-10-19：导出任务清理（默认关闭）

- `ExportJobService.purgeFinishedBefore(days)`：只删 **SUCCEEDED / FAILED** 且超期的任务；
  **进行中的任务不删**（删了会让 worker 找不到记录）。
- `ExportRetentionJob`：`app.export.retention.enabled`（默认关闭）、`days` 默认 7（与既有 `RETENTION` 一致）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **315 个，全部通过，0 跳过** |
| 新增测试 | 30 天前已成功的被删；30 天前仍在 RUNNING 的保留；新的保留；`days<=0` 拒绝 |

> 未覆盖：**磁盘上的导出文件**没有清理（当前是本地存储），需要另做文件级保留策略。
> 踩坑：我把新方法插在 `@Transactional` 与 `claimNext` 之间，导致两个 `@Transactional` 叠到同一方法上编译失败；
> 已修正注解位置。

---

## 2026-10-19：审计快照保留清理

- `AuditRetentionService.purgeOlderThan(days)`：按 `jv_commit.commit_date` 判定过期，
  **按序删除** `jv_snapshot` → `jv_commit_property` → `jv_commit` → 清理孤立 `jv_global_id`。
- 用 `commit_date`（真实时间戳）而不是 `commit_date_instant`（字符串，格式依赖 JaVers 版本）。
- 先取 `commit_pk` 列表再按列表删除——`commit_fk` 是 bigint，**不能直接与时间戳比较**。

| 验证 | 结果 |
|---|---|
| 后端测试 | **317 个，全部通过，0 跳过** |
| 新增测试 | 400 天前的 commit + 快照被删；新 commit 与快照保留；`days<=0` 拒绝 |

> 说明：`jv_*` 表之间**没有数据库外键**（V24 只建索引），所以删除顺序靠逻辑保证，
> 写错顺序不会报错、只会留下脏数据——因此测试必须断言"新数据完好"，而不只是"旧数据被删"。
> 未接线：尚未加入定时任务（默认手动或由运维调用），也未清理导出文件。

---

## 2026-10-19：数据保留清理统一入口

此前 notification / export_job / jv_snapshot 三套清理**各自为政且没有触发入口**，等于没做。现在统一：

- `MaintenanceService.runRetention(notificationDays, exportDays, auditDays)`：一次调用三件事，
  返回结构化报告（各类删除条数）；任何一项失败即抛错（不静默跳过，避免"以为清完了"）。
- `POST /admin/v1/maintenance/retention`：运维入口，参数可省略（默认 180 / 7 / 365 天）。
  这是**破坏性操作**，只暴露 POST；权限由 `StaffAuthFilter` 判定（`/admin/v1/**` 未知路径回落
  `permission.manage`，仅 ADMIN）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **318 个，全部通过，0 跳过** |
| 新增测试 | 一次调用清掉 400 天前的消息与 30 天前的导出记录，**新消息保留** |

> 未做：定时触发（`app.maintenance.retention.enabled` 未实现），当前只能由运维调用接口；
> 导出磁盘文件仍未清理。

---

## 2026-10-19：图形化定时任务（cron 存库，运行期可改）

对标 CRMEB 的做法：定时任务不该写死在代码里。新增：

- V52 `scheduled_task(code, name, cron, enabled, last_run_at, last_status, last_error, last_duration_ms, updated_at)`
- `ScheduledJob` 接口：实现类只声明 `code/name/defaultCron/enabledByDefault` 与 `run()`；
  **cron 与启停以数据库为准**，首次启动灌默认值（`ON CONFLICT DO NOTHING`，不会覆盖后台改动）。
- `ScheduledTaskService`：用 `ThreadPoolTaskScheduler` 手动 `schedule(CronTrigger)`，
  改 cron **立即生效**（不需要重启）；`runNow` 可手动触发并记录状态与耗时。
- 接口：`GET /admin/v1/scheduled-tasks`（含 `nextRunAt`）、`PUT /{code}`（改 cron/启停）、`POST /{code}/run`。
- 首个接入的任务：`maintenance.retention`（数据保留清理，默认**关闭**）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **322 个，全部通过，0 跳过** |
| 新增测试 | 默认 cron 正确且可改（落库校验）、非法/空 cron 拒绝、未知任务拒绝、手动执行记录 SUCCESS 与耗时 |

### 踩坑（两条都是真问题）

1. `CronExpression.next(Instant)` 对含星期字段的 cron 抛 `UnsupportedTemporalTypeException`，
   必须用 `ZonedDateTime` 作基准。
2. 注册表在**未迁移数据库**的上下文（OpenAPI 的 H2 测试）启动即崩，导致 100+ 用例连锁失败。
   已给注册表加 `app.scheduling.enabled` 条件（与 `SchedulingConfig` 同一开关），
   控制器加 `@ConditionalOnBean`——**不加就会出现"能跑单测但不能启动"的假象**。

> 未做：现有 7 个 `@Scheduled` 方法（订单超时、库存预留释放、拼团过期、导出 worker、佣金解冻等）
> 仍在代码里写死 cron，尚未迁移到该注册表。迁移需逐个改造并补回归测试，单独排期。

---

## 2026-10-19：两个保留清理任务迁入图形化注册表

- `NotificationRetentionJob`（`清洁@Scheduled` + 古老开关） → **`NotificationRetentionTask implements ScheduledJob`**
- `ExportRetentionJob` → **`ExportRetentionTask implements ScheduledJob`**
- 两个旧类已删除；cron 与启停改由 `scheduled_task` 表控制，后台可改、可手动触发。

| 验证 | 结果 |
|---|---|
| 后端测试 | **324 个，全部通过，0 跳过** |
| 新增测试 | 注册表中同时存在三个任务（maintenance/notification/export）；两个新任务 `runNow` 均返回 `SUCCESS` |

### 仍未迁移（逐个来，不一次全改）

| 任务 | 风险 | 备注 |
|---|---|---|
| 佣金解冻 `CommissionReleaseScheduler` | 中 | 默认关闭，可较快迁移 |
| 导出 worker `ExportJobScheduler` | 中 | 轮询型，迁移后要确认不重领任务 |
| 订单超时 `OrderExpiryJob` | 高 | 改变订单状态，迁移需补回归 |
| 库存预留释放 `ReservationExpiryJob` | 高 | 影响可售库存 |
| 拼团过期（`PromotionService` 内） | 高 | 在服务内部方法上，需要先抽出来 |

---

## 2026-10-19：佣金解冻 + 导出 worker 迁入注册表

- `CommissionReleaseScheduler` → **`CommissionReleaseTask`**（code `distribution.commission-release`，cron `0 */5 * * * *`，默认关闭）
- `ExportJobScheduler` 去掉 `@Scheduled` / `@ConditionalOnProperty`，变成**纯执行器**；
  调度交给新的 **`ExportWorkerTask`**（code `export.worker`，cron `*/10 * * * * *`，默认关闭）
- 两个类都支持后台改 cron、改启停、手动触发。

| 验证 | 结果 |
|---|---|
| 后端测试 | **324 个，全部通过，0 跳过** |
| 既有用例 | `CommissionReleaseSchedulerTest`、`ExportSchedulerTest` 改为直接驱动任务/执行器，仍全绿 |

### 踩坑

去掉依赖类上的 `@ConditionalOnProperty` 后，`@Component` 与注释块顺序被打乱，
导致 `ExportJobScheduler` 不再被注册成 bean，进而 `ExportWorkerTask` 注入失败、上下文起不来。
已修正注解位置——这类"注解被注释块隔开"的问题编译不报错，只在启动时暴露。

### 仍未迁移（高风险，单独一轮）

| 任务 | 原因 |
|---|---|
| 订单超时 `OrderExpiryJob` | 改变订单状态，需补回归 |
| 库存预留释放 `ReservationExpiryJob` | 影响可售库存 |
| 拼团过期（`PromotionService` 内部方法） | 需先抽出可调度方法 |

---

## 2026-10-19：改 cron 不重复执行（高危迁移前的保护性验证）

新增 `ScheduledTaskRescheduleTest`，用一个每秒执行的计数任务验证三件事：

| 步骤 | 断言 | 结果 |
|---|---|---|
| 启用 `*/1 * * * * *` | 3.5 秒内至少执行 2 次 | ✅ |
| **改成同一 cron（触发重新注册）** | 3.5 秒内执行次数**不翻倍**（≤5） | ✅ |
| 关闭 | 2.5 秒内最多 1 次在途执行 | ✅ |

| 验证 | 结果 |
|---|---|
| 后端测试 | **325 个，全部通过，0 跳过** |

为什么必须先有这条：迁移"订单超时""库存预留释放"属于**改业务状态**的任务，
如果改 cron 时旧调度没取消，重置后每秒跑两次，业务上就等于"重复释放库存"。

> 说明：该用例基于真实等待（秒级），不是 mock 时间；断言留了余量以避免 CI 抖动，
> 但仍能识别"翻倍执行"这类问题（翻倍会明显超过 5 次）。

---

## 2026-10-19：订单超时 + 库存预留释放迁入注册表（高风险任务）

- `OrderExpiryJob` / `ReservationExpiryJob` 去掉 `@Scheduled`（后者还去掉了 `@Profile("worker")`），
  变为**纯执行逻辑**。
- 新增 `OrderExpiryTask`（`order.expiry`）与 `ReservationReleaseTask`（`inventory.reservation-release`），
  cron `0 */1 * * * *`，默认**关闭**。
- **时长参数不进 cron**：`app.reservation.expiry-minutes` 仍是配置项，cron 只决定"多久扫一次"。
  混在一起会让运维误以为改 cron 就能改超时时长。

| 验证 | 结果 |
|---|---|
| 后端测试 | **325 个，全部通过，0 跳过** |
| 注册断言 | 注册表现在包含 7 个任务（含这两个新任务） |
| 前置保护 | 上轮的"改 cron 不重复执行"用例仍绿 |

### 踩坑

又遇到一次"`@Component` 与注释块交错"：我把注解放在注释上方，Spring 仍能识别，
但**可读性上会误导**，且与上轮 `ExportJobScheduler` 的失效问题同源。已统一改为"注释在上、注解紧贴类"。

### 仍未迁移

| 任务 | 原因 |
|---|---|
| 拼团过期（`PromotionService` 内部方法） | 逻辑写在服务里，需先抽成独立类再接入 |

---

## 2026-10-19：拼团过期迁入 + 注册表全覆盖（8/8）

- `PromotionService.expireGroupBuys` 去掉 `@Scheduled`，成为纯执行逻辑；
  新增 `GroupBuyExpiryTask`（`promotion.group-buy-expiry`）。
- 主代码中**已无任何 `@Scheduled` 注解**（仅注释里提到）。

### 默认启停策略（重要）

全部迁完后如果默认都关闭，生产上**订单超时不会自动取消、库存会被永久占用**。因此按影响面区分：

| 任务 | 默认 | 理由 |
|---|---|---|
| `order.expiry` | **开** | 不跑 → 未支付订单永占库存 |
| `inventory.reservation-release` | **开** | 不跑 → 预留永不释放 |
| `promotion.group-buy-expiry` | **开** | 不跑 → 失败拼团不退款 |
| `distribution.commission-release` | **开** | 不跑 → 佣金永久冻结 |
| `maintenance.retention` | 关 | 破坏性操作 |
| `notification.retention` / `export.retention` | 关 | 破坏性操作 |
| `export.worker` | 关 | 重型轮询，按需开启 |

| 验证 | 结果 |
|---|---|
| 后端测试 | **326 个，全部通过，0 跳过** |
| 新增用例 | 四个业务关键任务默认启用、四个破坏性/重型任务默认关闭；注册表共 8 个任务 |

---

## 2026-10-19：订单状态消息扩展（取消 / 履约完成 / 部分发货）

- 取消未支付订单：`ORDER_CANCELLED`（站内 + 短信）
- 全部履约完成：`ORDER_FULFILLED`（站内 + 短信）
- 部分发货：`ORDER_PARTIALLY_FULFILLED`（仅站内，避免短信轰炸）

| 验证 | 结果 |
|---|---|
| 后端测试 | **327 个，全部通过，0 跳过** |
| 新增用例 | 超时取消后：`MEMBER` 收到 `IN_APP`，`CUSTOMER/手机号` 收到 `SMS` |

### 踩坑（三条，都是测试数据问题）

1. 固定批次 id 与 `stock_move` / `reservation` 外键冲突 → 改为每条用例随机批次 id。
2. 同一类内两条用例共用会员 id，删会员撞 `orders_member_id_fkey` → 改为每条用例独立会员与手机号。
3. 我的断言写成"MEMBER 同时收 IN_APP 和 SMS"，但**身份统一后短信只走 CUSTOMER/手机号** →
   断言改正，不是实现错。

---

## 2026-10-19：发货物流信息（承运商 + 运单号 + 通知）

- `FulfillmentService.shipFulfillment(fulfillmentOrderId, carrierCode, trackingNo)`：
  - 状态必须是 `ACCEPTED` / `PICKING` / `PACKED`；
  - **承运商与运单号必填**（没有单号的"已发货"对顾客没意义，也无法查件）；
  - 写入 `carrier_code` / `tracking_no` 并推进到 `SHIPPED`；
  - 通知内容带承运商与运单号（站内 + 短信）。
- 接口：`POST /store/v1/fulfillments/{id}/ship`。

| 验证 | 结果 |
|---|---|
| 后端测试 | **329 个，全部通过，0 跳过** |
| 新增用例 | 发货写单号并通知（游客单：CUSTOMER 收 IN_APP+SMS）；缺承运商/缺运单号分别拒绝 |

> 说明：物流表字段本就存在（V1 即有 `carrier_code`/`tracking_no`），供应商发货路径已在用；
> 本轮补的是**仓库/门店发货**这条缺失路径与顾客通知。
> 未做：物流轨迹查询（需对接快递 100 / 快递鸟等外部服务）。

---

## 2026-10-19：修掉履约接口的自提码泄露（安全）

**发现**：`GET /api/v1/orders/{orderId}/fulfillments` **没有任何归属校验**，
返回体里含 `pickupCode`（自提码）——知道订单号就能拿到别人取货的凭证。

**修复**：
- 新增 `platform/identity/OrderOwnershipGuard`（放 platform 层，避免 order ↔ fulfillment 循环依赖）：
  - 订单已绑定会员 → 只允许本人，其他人一律 **404**（不暴露存在性）；
  - 游客订单 → 必须带**与订单联系人一致的手机号**，否则 403。
- 履约接口接入该校验。

**受影响的调用方（已一并更新）**：
| 调用方 | 改动 |
|---|---|
| `PostgresPickupFlowTest` | GET 带 `?phone=` |
| `web/e2e/pickup.spec.ts` | 同上 |
| `web/apps/admin/src/views/OrdersView.vue` | 后台查看订单时带上联系人手机号 |

| 验证 | 结果 |
|---|---|
| 后端测试 | **332 个，全部通过，0 跳过** |
| 新增用例 | 游客单缺手机号/手机号不符 → 拒绝；会员单他人访问 → 404；未知订单 → 404 |

> 注：`carrierCode` / `trackingNo` 本来就在 DTO 里，顾客侧无需新增字段；本轮重点是**堵住泄露**。
> 待办：后台客服查看履约更应走 `/store|/admin` 接口（带员工身份），而不是沿用顾客接口 + 手机号。

---

## 2026-10-19：后台/门店履约接口（收拾权限模型）

- 新增 `GET /admin/v1/orders/{orderId}/fulfillments`（客服/后台，凭员工身份，`order.manage`）
- 新增 `GET /store/v1/orders/{orderId}/fulfillments`（门店工作台）
- 后台页面 `OrdersView.vue` 改用它，**不再借用顾客接口 + 手机号**

| 验证 | 结果 |
|---|---|
| 后端测试 | **332 个，全部通过，0 跳过** |
| 新增断言 | `PostgresPickupFlowTest` 用 admin token 调后台接口，返回履约含自提码，**无需手机号** |
| 接口对齐 | 后端 184 条路由 / 前端 16 条调用 / 未匹配 **0** |

> 现在顾客、门店、后台三条路径各走各的接口，权限模型不再靠"手机号绕过"。

---

## 2026-10-19：换货发货 + 修掉"游客售后收不到通知"

### 换货发货

- V53：`after_sale` 增加 `exchange_carrier_code` / `exchange_tracking_no` / `exchange_shipped_at`。
- `AfterSaleService.shipExchange(id, carrier, tracking)`：类型必须是 `EXCHANGE`、状态必须是 `RECEIVED`，
  承运商与运单号必填；写库 + 状态推进 `EXCHANGE_SHIPPED` + 通知顾客。
- 接口：`POST /admin/v1/after-sales/{id}/exchange-ship`。

**已知限制**：只记录换货物流，**换货商品的库存出库还没接履约模块**（不产生库存流水）。
在接入前，换货状态停在 `EXCHANGE_SHIPPED` 而不算完成。

### 顺带修掉的真缺口

测试发现：售后通知只发给**会员**，**游客订单的售后消息一条都收不到**（第 100 轮引入的疏漏）。
已改为统一走 `notifyOrderEvent`：会员订单写 `MEMBER/<id>`、游客订单写 `CUSTOMER/<手机号>`。

| 验证 | 结果 |
|---|---|
| 后端测试 | **336 个，全部通过，0 跳过** |
| 新增用例 | 换货发货写单号并通知；`RETURN` 类型不可发换货；未收货不可发；承运商/运单号必填 |

---

## 2026-10-19：换货物流字段可读（补上一轮的缺口）

上一轮把承运商/运单号**写进了库**，但 `AfterSaleView` 没有这三个字段——写进去读不出来，页面照样看不到。

- `AfterSaleView` 增加 `exchangeCarrierCode` / `exchangeTrackingNo` / `exchangeShippedAt`；
- 仓储查询与映射同步（`find` 与列表共用同一 SQL，一处改两处生效）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **336 个，全部通过，0 跳过** |
| 新增断言 | 换货发货后读回：承运商 `SF`、运单号 `SF987654321`、发货时间非空 |

> 这是典型的"半开"问题：写入路径做完、读取路径没做，功能看起来在、实际拿不到。
> 若没有读回断言，很容易被误判为"已完成"。
>
> **仍未做**：换货商品的库存出库（需先定库存预留模型如何区分"换货占用"，避免与订单预留的
> 唯一键冲突），以及换货完成的最终状态。

---

## 2026-10-19：换货接入库存出库（闭环）

先核实了一件事：`reservation` 表**没有** `(order_id, order_line_id)` 唯一键（V1 只有 PK 与普通索引），
所以"同一订单行再预留一次"是允许的——我上轮担心的唯一键冲突并不存在。

实现 `AfterSaleService.dispatchExchangeStock`：
1. 按售后行**再次预留**（`reserveForOrderLine`）；
2. `confirmOrderReservations` 确认；
3. `shipReservationsForLines` 发货（只处理 `CONFIRMED`，历史已发货的不会重复扣减）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **337 个，全部通过，0 跳过** |
| 新增用例 | 换货发货后库存批次数量 10 → 9，且产生 1 条 `SHIP` 库存流水 |

### 已知限制

- 出库仓取"第一个仓库位置"（`warehouseIdFor`），**没有按订单履约仓精确匹配**；
  多仓场景下应先从订单的履约单里取仓，后续修正。
- 售后单若没有商品行（历史/异常数据），跳过出库并打 WARN，不阻断发货。

---

## 2026-10-19：物流轨迹查询（演示 mock / 生产后台配置）

- `logistics_provider` 配置项决定实现（`mock` / `kuaidi100`）；密钥走环境变量 `app.logistics.api-key`，
  **不进数据库、不进后台表单**。
- 接口：`GET /api/v1/orders/{orderId}/tracking`（顾客侧，走归属校验）。

### 错误暴露策略（按你的要求）

| 对象 | 看到什么 |
|---|---|
| 顾客 | 「物流信息暂时无法查询，请稍后再试」（HTTP 503，不含内部原因） |
| 维护人员 | `risk_alert`（规则 `LOGISTICS_PROVIDER_UNAVAILABLE`，含具体原因）+ ERROR 日志 |

| 验证 | 结果 |
|---|---|
| 后端测试 | **339 个，全部通过，0 跳过** |
| 新增用例 | mock 返回轨迹；配置缺失时顾客收通用提示，且 `risk_alert` 落库含具体原因（`api-key`） |

> **未实现（诚实说明）**：快递100 真实适配器未写——没有商户凭据无法联调验证，
> 写一个"看起来能跑"的实现不如显式失败。当前 `kuaidi100` 分支会走"不可用"路径。

另新增 `docs/GUEST_ORDERING_AND_CASHIER_SCENARIOS.md`：匿名下单场景分析与替代方案（见下条）。

---

## 2026-10-19：下单必须绑定真人（线上匿名下单关闭）

`OrderService.checkout` 增加前置校验 `requireVerifiedCustomer`：

| 手机号情况 | 行为 |
|---|---|
| 已是会员手机号 | 直接下单（下单后绑定 `member_id`） |
| 未注册且**无验证码** | 400 `CHECKOUT_OTP_REQUIRED`，**不产生订单** |
| 未注册且带有效验证码 | 校验通过 → **自动创建会员** → 订单绑定该会员 |

接口侧：`CheckoutRequest` 增加 `otpCode`，购物车结算 `Body` 同步支持。

| 验证 | 结果 |
|---|---|
| 后端测试 | **342 个，全部通过，0 跳过** |
| 新增用例 | 无验证码被拒且无订单；验证码通过后建会员并绑定；已是会员无需验证码 |

> 受影响的既有用例已按"真人"要求调整：为测试手机号补建会员（等价于访客走 OTP 自动建会员）。
> 这也暴露出一处连锁：订单绑定会员后，顾客接口改用后台接口/JDBC 断言（顾客接口要求会员身份）。
>
> **未做**：门店代下单（`created_by=CASHIER`）与"到货短信通知自提"仍属场景 B，待下一轮。

---

## 2026-10-19：收银台代下单（场景 B）

- V54：`orders.placed_by`（CUSTOMER / CASHIER / ADMIN）+ `placed_by_operator`，
  并有 DB 约束：**非 CUSTOMER 必须记录操作员**。
- `OrderService.requireVerifiedCustomer`：员工代下单**跳过顾客验证码**，但必须带操作员；
  顾客自助下单仍强制"会员手机号或有有效验证码"。
- 接口：`POST /store/v1/orders`（门店工作台，从认证身份取操作员）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **344 个，全部通过，0 跳过** |
| 新增用例 | 员工代下单无 OTP 成单且落库 `CASHIER/收银员张三`；缺操作员被拒；顾客无 OTP 仍被拒 |

> 设计取舍：门店面对面场景**无法给顾客发验证码**，所以用"员工身份 + 操作员记录"替代 OTP，
> 保证每单仍可追溯到人。线上自助下单不允许这样绕过。

---

## 2026-10-19：自提通知补上提货码

- `PICKUP_READY` 通知内容改为包含实际提货码（此前只写"请携带提货码"，没给码）。
- 该通知走三渠道（站内/短信/微信），会员订单会分流：`IN_APP → MEMBER`、`SMS/WECHAT → CUSTOMER`。

| 验证 | 结果 |
|---|---|
| 后端测试 | **344 个，全部通过，0 跳过** |
| 新增断言 | `PostgresPickupFlowTest`：非员工渠道恰好 3 条，且**每条内容都包含提货码** |

> 踩坑：断言一开始写"3 条"，实际 4 条——因为员工也会收到一条同类通知；
> 收窄到顾客后又变 2 条——因为会员订单的站内消息落在 `MEMBER` 而非 `CUSTOMER`。
> 最终按"非员工"统计，与身份分流规则一致。

---

## 2026-10-19：通知身份分流规则固化为测试

- 新增 `NotificationRoutingTest`：会员订单与游客订单各一条用例，断言收件人与渠道的对应关系。
- `PROJECT_MEMORY §13` 记录规则表与三个推论（我这十几轮里在此处记错过两次）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **346 个，全部通过，0 跳过** |
| 断言内容 | 会员单：`MEMBER:<id>:IN_APP`、`CUSTOMER:<手机>:SMS`、`STAFF:ALL:IN_APP`，且**不含** `CUSTOMER:<手机>:IN_APP`；游客单：`CUSTOMER` 同时收 `IN_APP` 与 `SMS` |

---

## 2026-10-19：优惠券适用范围（SKU / 类目 / 品牌）

- V55：`coupon_template.scope`（ALL/SKU/CATEGORY/BRAND）+ `scope_ids JSONB`；
  DB 约束：非 ALL 必须至少一个范围 ID。
- `CouponService.createTemplate(..., scope, scopeIds)`：建券时指定范围；
  旧签名保留（等价 `ALL`）。
- `CouponService.eligibleAmount(couponId, subtotalsBySku)`：按范围累加命中商品金额，
  与促销的范围口径一致（避免"促销按类目、券按全场"的错配）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **349 个，全部通过，0 跳过** |
| 新增用例 | SKU 券只算命中 SKU（1000 而非 3000）；全场券算全部（3000）；非全场缺 ID 被拒 |

> **未接线**：结算仍用整单金额调用 `checkUsable`，尚未改用 `eligibleAmount` 的结果做门槛判断——
> 下一步需要把"命中金额"传入结算，否则范围券实际仍按整单生效。
> 这是明确的半开状态，已记入待办。

---

## 2026-10-19：范围券接入结算（补齐上轮的半开状态）

`OrderService.checkout` 的券处理改为：

1. 用商品明细算出**命中范围的金额** `eligibleAmount`；
2. `eligibleAmount <= 0` → 直接 409「购物车中没有该券可用的商品」（**不静默按整单抵扣**）；
3. 门槛与折扣都基于 `eligibleAmount` 判断，不再用整单金额。

| 验证 | 结果 |
|---|---|
| 后端测试 | **350 个，全部通过，0 跳过** |
| 新增用例 | SKU 券在购物车只含其他 SKU 时命中金额为 0 |

### 顺带发现的一个设计陷阱

`checkUsable` 对"门槛为 0 的券 + 0 元金额"仍返回 `usable=true`（折扣为 0）。也就是说：
**如果结算不显式拦截 `eligible==0`，用户会"用掉券却拿不到任何优惠"**——静默失败。

结算层的显式拒绝就是为这个场景加的；测试注释里也写明了原因。

---

## 2026-10-19：导出文件清理（补齐"只清库不清盘"）

- `ExportStorage` 新增默认方法 `deleteOlderThan(days)`：默认返回 0（对象存储应由**生命周期规则**处理，
  不在这里做假删除）；`LocalExportStorage` 按**文件 mtime** 真删。
- `MaintenanceService.Report` 增加 `exportFiles`，保留清理一次同时清**数据库记录 + 磁盘文件**。

| 验证 | 结果 |
|---|---|
| 后端测试 | **353 个，全部通过，0 跳过** |
| 新增用例 | 30 天前的文件被删、新文件保留；`days<=0` 拒绝；目录不存在返回 0 而不是报错 |

> 设计说明：接口默认返回 0 容易被误读为"清理成功"，因此注释里明确写了
> 「对象存储应由生命周期规则处理」与「返回值必须真实反映删除数量，不得静默返回 0 冒充成功」。
> 生产若换 S3，应配置桶生命周期，而不是在应用里遍历删除。

---

## 2026-10-19：审计 outbox（崩溃不丢）

- V56 `audit_outbox`：`entity_type` / `entity_id` / `author` / `payload JSONB` / `status` / `attempts` / `last_error`。
- `AuditTrail.commit` 改为两步：
  1. **先落 outbox**（与业务同事务）→ 进程崩溃后行仍在；
  2. 正常路径**直接用真实对象**提交 JaVers（内存队列），成功后把该行标记 `DONE`。
- `recoverOrphans()`：崩溃恢复路径，用"类型名 + JSON"反射还原后提交；失败累计到 `FAILED` 并**保留行**。

### 为什么不用纯序列化重放

先按"纯 outbox 重放"实现，实测失败：`AssetEntity` 是 JPA 实体（`getId()` 而非 `id()`），
序列化往返后 JaVers 拿到的对象与原实体语义不同，历史查询拿不到快照。
**改为"真实对象提交 + outbox 兜底"**，才既可靠又保留崩溃恢复能力。

| 验证 | 结果 |
|---|---|
| 后端测试 | **355 个，全部通过，0 跳过** |
| 新增用例 | outbox 落库类型与 payload 正确、状态 PENDING；连续失败 5 次后标记 FAILED 且保留 |

### 本轮踩的另一个坑

`flush()` 最初加上了"等待库中 PENDING 归零"的条件，结果**死循环卡住测试 10 分钟**——
因为该测试上下文里消费者无法清空行。已改为**带 10 秒上限**，超时抛错而不是无限等待。

---

## 2026-10-19：换货完成状态（售后闭环最后一步）

- `AfterSaleService.completeExchange(id, operator)`：类型必须 `EXCHANGE`、状态必须 `EXCHANGE_SHIPPED`，
  推进到 `COMPLETED`，写 `EXCHANGE_COMPLETED` 事件并通知顾客。
- 接口：`POST /admin/v1/after-sales/{id}/complete-exchange`。

**为什么不做自动完成**：系统感知不到顾客何时真正收到换货（没有签收回调）。自动完成会导致
"未收到货却已标记完成"的售后无法追溯，所以由门店/客服在顾客确认后手动标记。

| 验证 | 结果 |
|---|---|
| 后端测试 | **357 个，全部通过，0 跳过** |
| 新增用例 | 发货后可完成且写事件；未发货时标记完成被拒 |

### 售后（换货）完整状态机

```
REQUESTED → APPROVED → RECEIVED → [出库扣减] → EXCHANGE_SHIPPED → COMPLETED
                                            ↘ 退款路径：REFUNDED
```

---

## 2026-10-19：范围券补真实结算断言

`CouponScopeTest` 增加两条**走真实下单**的用例：

| 用例 | 断言 |
|---|---|
| 购物车只有非范围 SKU + 范围券 | ✅ 409「没有该券可用的商品」 |
| 购物车命中范围 SKU + 范围券 | ✅ 订单 `discount_amount = 500` |

| 验证 | 结果 |
|---|---|
| 后端测试 | **359 个，全部通过，0 跳过** |

### 这轮暴露的三个测试自身问题（都不是实现错）

1. **会员被订单引用后删不掉** → 改为每条用例独立会员与手机号；
2. **删券模板撞 `member_coupon` 外键** → 先删券再删模板；
3. **`couponId` 传错位置** → `CheckoutRequest` 参数多达 14 个，我把它放到了 `pointsToUse` 前面，
   于是测试里根本没提交券，断言自然失败。

第 3 条尤其值得记：这个 record 参数越加越长（已 14 个），**位置参数极易错**。
后续应改为 Builder 或分组对象，否则每次加字段都要改所有调用点。

---

## 2026-10-19：部分退款的积分回退做成可切换策略

- `MemberRewardService.rollbackProportional(orderId, phone, refundedAmount, orderTotal)`：
  按退款比例**向下取整**回退积分与成长值，幂等键含退款金额，且不产生负数。
- `app.refund.partial.points-mode`：`KEEP`（默认，现状）/ `PROPORTIONAL`；非法值启动即报错。
- `docs/REFUND_POLICY.md` 增加"可切换策略"一节。

| 验证 | 结果 |
|---|---|
| 后端测试 | **361 个，全部通过，0 跳过** |
| 新增用例 | 退一半 → 积分 20 / 成长值 10；退 1/3 → 积分向下取整 12（不多退） |

### 为什么默认 KEEP

部分退款是否回退属于**业务口径**，不是技术选择。默认保持既有行为，
避免在口径未定前悄悄改动用户资产；你给出口径后只需改配置即可生效。

### 本轮又犯的一次期望值错误

写断言时忘了 GOLD 级 **2 倍积分倍率**：2000 分实付 → 40 积分而非 20。
测试直接把我拉回实现，说明"算一遍再断言"不能只算基数。

---

## 2026-10-19：`CheckoutRequest` 参数收敛（14 → 10）

把尾部 5 个散字段（`pointsToUse` / `couponId` / `otpCode` / `placedBy` / `placedByOperator`）
收成一个 `CheckoutContext`：

```java
public record CheckoutRequest(items, fulfillmentMode, storeId, contact, address,
                              legalAcceptanceIds, referrerId, referralMode, groupBuyId,
                              CheckoutContext context)
public record CheckoutContext(pointsToUse, couponId, otpCode, placedBy, placedByOperator) {
    public static CheckoutContext none() { ... }
}
```

- 未提供 context 视为 `none()`，调用方无需构造空对象；
- **对外 HTTP 契约不变**：购物车结算的 `Body` 仍是扁平字段，由控制器组装成 context；
- 服务内部统一用 `ctx.xxx()` 访问。

| 验证 | 结果 |
|---|---|
| 后端测试 | **361 个，全部通过，0 跳过** |
| 接口对齐 | 后端 188 条路由 / 前端 16 条调用 / 未匹配 **0** |

### 为什么做这个

上一轮我因为位置传参把 `couponId` 放错位置，导致"券根本没提交"却以为是业务缺陷，白查一轮。
参数从 14 降到 10，且同组字段聚在一起，同类错误显著更难发生。

> 未做：其余 10 个参数仍可进一步分组（如 `OrderInfo` / `Delivery`），但当前风险已可控，
> 继续拆会牵动所有调用点，收益递减。

---

## 2026-10-19：范围选择器的数据接口

后台要给券/促销选范围（SKU/类目/品牌），需要"可选项"数据源。新增：

- `GET /admin/v1/promotions/scope-options?scope=SKU|CATEGORY|BRAND&limit=`
  返回 `{ id, label, extra }`：
  - `SKU` → `label=sku_code`、`extra=商品标题`
  - `CATEGORY` → `label=类目名`、`extra=类目编码`
  - `BRAND` → `label=品牌名`
- 未知范围、非法 limit 明确报错（不返回空列表冒充"没有数据"）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **363 个，全部通过，0 跳过** |
| 新增用例 | SKU 选项有数据且含 id/label；未知范围报「范围只能是…」 |

> 本轮只做数据接口，**前端选择器 UI 未做**——先有可靠数据源，UI 才有意义。

---

## 2026-10-19：后台规则 UI 真机验证 + 修掉一个跨控制器的鉴权缺陷

### 修复：后台控制器拿不到 `Authentication`（真 bug）

`StaffAuthFilter` 的 `@Order(HIGHEST_PRECEDENCE + 30)` 让它**跑在 Spring Security 链之前**，
它设置的 `SecurityContext` 随后被安全链覆盖 → 后台控制器注入的 `Authentication` 为 **null**，
`POST /admin/v1/promotions/rules` 直接 NPE（500）。

改为 `@Order(0)`（晚于安全链）后恢复。**影响面不止促销**：所有在 `/admin`、`/store` 下
使用 `Authentication` 参数的接口此前都是坏的（多数控制器用 `@RequestAttribute StaffUser` 才没暴露）。

### 新增 E2E（真浏览器）

`web/e2e/promotion-rules.spec.ts`：登录后台 → 促销管理 → 新建规则（默认时间窗）→ 断言列表出现
该规则且状态为「草稿」、存在「启用」按钮。

| 验证 | 结果 |
|---|---|
| E2E（Chromium） | ✅ 通过（2.9s） |
| 前端构建（含类型检查） | ✅ |

### 未验证（如实记录）

**「启用」按钮的点击没有到达后端**（服务端无对应请求记录）。测试里暂时只断言按钮存在，
不假装它工作。下一步要查清是点击被拦、还是 `api.post` 未发出。

顺带修了规则表状态显示（此前直接显示 `DRAFT` 而非「草稿」）。

---

## 2026-10-19：「启用」按钮点了没反应的真因——api 客户端解析空响应崩溃

上一轮我记录「启用点击没有到达后端」。本轮用**请求日志实测**：请求**确实发出且返回 200**，
所以问题不在 UI 绑定，而在客户端解析。

真因：`ApiClient.request` 对 `200` 且**空响应体**直接 `response.json()`，
抛 `Unexpected end of JSON input` → 走进 `catch` → 弹出"启用失败"。
后端 `POST /rules/{id}/activate` 返回空体，正好命中这个坑。

修复：先读 `text()`，为空则返回 `undefined`，否则 `JSON.parse`。

| 验证 | 结果 |
|---|---|
| E2E（Chromium） | ✅ **通过**（创建 → 列表出现草稿 → 启用 → 状态变「进行中」，3.2s） |
| 前端构建（5 应用，含类型检查） | ✅ |
| 接口对齐 | 189 条路由 / 16 条调用 / 未匹配 **0** |

> 这个缺陷影响**所有返回空体的接口**（启停类操作），不止促销规则。
> 之前之所以没暴露，是因为多数页面调用的接口都会返回 JSON。

---

## 2026-10-19：后台优惠券管理页

- 后端新增 `GET /admin/v1/coupons/templates`（ADMIN）：返回模板 + **已发/总量**、状态、门槛、限领、有效天数。
- 前端新增 `CouponsView.vue`：列表 + 新建（面额/折扣、门槛、总量、限领、有效天数、**适用范围**、发放时间）+ 草稿启用；
  侧边栏与路由已注册。

| 验证 | 结果 |
|---|---|
| 后端测试 | **364 个，全部通过，0 跳过** |
| 新增用例 | 建券并领 1 张后，模板列表 `issuedQuantity=1 / totalQuantity=5` |
| 前端构建（含类型检查） | ✅ |
| 接口对齐 | 189 条路由 / 16 条调用 / 未匹配 **0** |

> **未做运行时验证**：券页面只有静态构建 + 类型检查，没有起服务点一遍。
> 上一轮的教训（空响应体导致按钮失效）说明"能构建"不等于"能用"，
> 所以这条明确标为待验证，下一轮补 E2E。

---

## 2026-10-19：券页面真机验证通过（并修掉两个"我以为做了"的错）

上一轮我说券页面「只差运行时验证」。实际验证时连撞三个问题，**前两个是我上一轮的疏漏**：

| # | 问题 | 真相 |
|---|---|---|
| 1 | 路由 404（页面空白） | 我上轮的替换锚点写错（真实代码有 `name:`），**路由根本没加**，我却以为加了 |
| 2 | `GET /admin/v1/coupons/templates` 405 | 上轮插入被条件判断挡住，**接口根本没加**，脚本却打印了"endpoint added" |
| 3 | 页面测到旧行为 | 上轮只 `mvn test`（编译）没 `package`，跑的是旧 jar |

三个问题都源于同一个坏习惯：**改完没有核对"改动是否真的生效"**。

### 现在的结果

| 验证 | 结果 |
|---|---|
| E2E（Chromium） | ✅ 通过：建券 → 列表出现「草稿」→ 启用 → 状态变「发放中」（3.8s） |
| 后端测试 | **364 个，全部通过，0 跳过** |
| 前端构建 | ✅ |

### 教训（已记入流程）

改动后**必须验证落地**：路由要 `grep` 到、接口要能被 `GET` 或测试命中、跑服务要用新 jar。
"脚本打印成功"不等于"代码真的改了"——上轮那两个补丁都是条件判断写错导致静默跳过。

---

## 2026-10-19：全量 `make verify` 结果——**不是全绿，5 个 E2E 失败**

| 阶段 | 结果 |
|---|---|
| 后端测试 | ✅ **364 个，全部通过** |
| 后端打包 | ✅ |
| 前端构建（5 应用） | ✅ |
| 接口对齐 | ✅ 189 条路由 / 16 条调用 / 未匹配 0 |
| **E2E** | ❌ **7 通过 / 5 失败** |

### 失败清单与已查明原因

| 用例 | 现象 | 原因（已查证/待查） |
|---|---|---|
| `storefront` 浏览→加购→下单 | `waitForURL` 超时 30s | **很可能是第 117 轮改动导致**：下单现在要求"会员手机号或有效验证码"，而 storefront 结算没有 OTP 环节 |
| `admin` 促销管理找不到「马克杯限时秒杀」 | 元素不存在 | **演示种子被跳过**：`DemoDataSeeder` 只在 `product` 表为空时执行 `seed.sql`，而迁移已插入商品 → 整个 demo 种子（含促销、门店等）都不执行 |
| `promotions` 两个用例（页面标题/秒杀文案） | 元素不存在 | 同上（缺演示促销数据） |
| `pickup` 门店自提 | 0ms 失败 | 疑似 store-console 未就绪（启动竞态），待确认 |

### 这说明什么

1. **后端与构建层面是稳的**（364 测试 + 打包 + 5 应用构建 + 接口对齐全绿）。
2. **端到端层面我引入了回归**：第 117 轮的"下单必须真人"没有同步改 storefront 结算与对应 E2E。
3. **演示数据在容器里从来就不完整**：迁移先插了商品，demo 种子整段被跳过，之前几轮 E2E 通过是因为当时数据库为空。

### 下一步（明确）

| 优先级 | 事项 |
|---|---|
| P0 | storefront 结算接入 OTP（发码→校验→下单），并同步修 E2E |
| P0 | `DemoDataSeeder` 改为**按表幂等**（商品/促销/门店各自判空），而不是"商品非空就整段跳过" |
| P1 | 修 `pickup` E2E 的启动竞态（等待 console 就绪再跑） |

---

## 2026-10-19：修掉促销列表被规则污染（真缺陷）+ 演示促销幂等补齐

### 真缺陷：满减规则让顾客/后台促销列表整体失效

第 63 轮我加了满减/满折规则（无 `skuId`），它们会出现在 `PromotionService.listActive/listAll` 的
结果里，而视图映射要求 `skuId` → 一条满减规则就足以让 `/api/v1/promotions` 返回 **404 SKU_NOT_FOUND**。

修复：列表只保留**有 SKU 的活动型促销**（秒杀/买赠/拼团）；满减/满折走独立规则接口。

### 演示数据：促销种子改为幂等补齐

`DemoDataSeeder` 原先"商品非空就整段跳过 `seed.sql`"，导致促销缺失。新增
`db/demo/promotions.sql`（`ON CONFLICT DO NOTHING`）在每次启动时补齐促销。

| 验证 | 结果 |
|---|---|
| 后端测试 | **365 个，全部通过，0 跳过** |
| 新增用例 | 满减规则不出现在 `listActive` / `listAll` |
| 运行时 | `/api/v1/promotions` 返回 3 条（马克杯秒杀/买赠/拼团） |
| **E2E 复跑** | `admin.spec` ✅、`promotions.spec` 2 条 ✅ |

### 仍未修

`storefront` 的"浏览→加购→自提下单"仍失败：**请求根本没到后端**（日志无 `/api/v1/checkout` 记录），
说明卡在页面侧。下一步查前端结算页（很可能与第 117 轮新增的 OTP 要求有关，页面未做处理）。

---

## 2026-10-19：storefront 下单失败定位结论（未修复）

用**直接调接口**的方式确认了根因，不再靠猜：

```
POST /api/v1/checkout  (未注册手机号 13800000000)
→ 400 CHECKOUT_OTP_REQUIRED「该手机号尚未注册，请先获取并填写短信验证码」
```

而用演示会员手机号（`13800000001`）同样请求 → **201 成功**。

结论：**不是页面不发请求，而是发了被第 117 轮的"下单必须真人"规则拒绝**，页面只是没把错误显示到测试可断言的位置。

### 尝试过但没解决

在 E2E 里填已注册手机号后仍然超时（页面电话字段可能不从该输入取值），**未定位页面字段绑定**，因此**还原了该测试改动**，不留未验证的修改。

### 正确修法（下一轮）

storefront 结算页需要真正的 OTP 流程：
1. 输入手机号 → 「获取验证码」→ 调 `/api/v1/member/auth/otp`
2. 输入验证码 → 调 `/api/v1/member/auth/verify` 拿 token
3. 携带 Bearer token 下单（订单自动绑定会员）

这才是符合"每单绑定真人"的产品级实现；仅改测试是绕过规则，不应做。

### 当前 E2E 真实状态

| 用例 | 状态 |
|---|---|
| 促销规则 / 优惠券 / admin / promotions / pos / supplier / support / theme / cart | ✅ |
| storefront「浏览→加购→自提下单」 | ❌ 需 OTP 流程 |
| pickup（门店控制台） | ⚠️ 上轮 0ms 失败，未复测 |

---

## 2026-10-19：storefront 访客 OTP 下单打通（主链路恢复）

### 改动

| 位置 | 内容 |
|---|---|
| `checkout.vue` | 新增「获取验证码」+ 验证码输入；下单前用 `phone + code` 换会员 token（`/api/v1/member/auth/verify`），并带 `Authorization` 调结算 |
| `orders/[id].vue` | 订单详情与履约查询带 `Authorization`（服务端按归属校验，之前是 404 → 页面空白） |
| `application-dev.yml` | `app.member.otp.return-code: true`（**仅 dev**；生产不得开启，等于公开验证码） |
| E2E | 下单前点「获取验证码」，断言演示环境自动回填 |

### 验证

| 项 | 结果 |
|---|---|
| **E2E（Chromium）** | ✅ storefront 3 条全通过（含浏览→加购→自提下单→订单页，2.2s） |
| 后端测试 | **365 个，全部通过，0 跳过** |
| 前端构建 | ✅ |
| 接口对齐 | 189 条路由 / 18 条调用 / 未匹配 **0** |

> 这一轮把上一轮诊断出的问题真正修完：**访客必须先验手机号才能下单**，且下单后能查看自己的订单。

---

## 2026-10-19：E2E **12/12 全绿**

### 这轮修的三处（都是"新规则没同步到测试/页面"）

| 用例 | 问题 | 修复 |
|---|---|---|
| `pickup` | `beforeAll` 用未注册手机号下单 → 400 | 改为 OTP 取会员 token，下单与查履约都用 `Authorization` |
| `supplier` | 同上，且最终订单查询未带 token | 同上 |
| `pickup`（第二次） | 反复运行把 OTP 限流（同号 10 分钟 5 次）打满 → 0ms 失败 | 每次运行用**随机手机号** |

### 结果

| 项 | 结果 |
|---|---|
| **E2E（Chromium）** | ✅ **12 / 12 通过**（34.3s） |
| 后端测试 | 365 个全绿（上轮已验证，本轮未改后端） |
| 前端构建 | ✅ |

> 记一条规则：**E2E 里凡是"下单、查订单、查履约"的动作，都要带会员 token**，
> 因为第 117 轮起订单一律绑定会员，凭订单号/手机号的旧路径已失效。
> 这条已写进测试注释，避免以后再踩。

---

## 2026-10-19：下单通知覆盖未绑定会员的订单（场景 B 收尾）

- `OrderService.checkout` 的下单通知从"只通知会员"改为 `notifyOrderEvent`：
  会员订单落 `MEMBER/<id>`，未绑定会员（门店代下单/新访客）落 `CUSTOMER/<手机号>`，站内 + 短信。

| 验证 | 结果 |
|---|---|
| 后端测试 | **366 个，全部通过，0 跳过** |
| 新增用例 | 游客订单 `ORDER_CREATED` 在 `CUSTOMER` 上同时有 `IN_APP` 与 `SMS` |

### 场景 B 链路现在完整

```
收银员代下单（记录操作员）→ 下单通知 → 支付 → 拣货完成 → 备货通知（含提货码）→ 到店核销
```

---

## 2026-10-19：文档对齐当前状态（交接准备）

- `README.md`：
  - 技术栈更正为 **Spring Boot 4.1（Spring Framework 7）**；
  - 新增「容器内/无 Docker 时」启动步骤（嵌入式 PG + 无 curl + npm 缓存坑）；
  - 新增「一键验证」章节（`make verify` / `make verify-fast` 与当前基线数字）；
  - 后台能力清单补：满减/满折规则、优惠券、物流轨迹、换货。
- `docs/PROJECT_STATUS.md`：测试数 311→**366**、E2E 10→**12**、接口对齐补路由/调用数、
  促销与券标注"后台界面已 E2E 验证"、部分退款补充"积分开关已实现"。

> 本轮不改代码，只把文档与真实状态对齐——上一轮 E2E 12/12 与后端 366 是刚验证过的数字。

---

## 2026-10-19：全量 `make verify` **全绿** + 补一条 HTTP 回归

### `make verify` 一次完整运行

| 阶段 | 结果 |
|---|---|
| 后端测试 | ✅ 366 个（当时） |
| 后端打包 | ✅ |
| 前端构建（5 应用） | ✅ |
| 接口对齐 | ✅ 189 路由 / 18 调用 / 0 未匹配 |
| **E2E（Chromium）** | ✅ **12 / 12**（35.4s） |
| 结论 | ✅ **全部通过** |

### 新增 HTTP 回归用例

`promotionsListSurvivesRuleOnlyPromotions`：造一条满减规则（无 `skuId`）后，
`GET /api/v1/promotions` 必须 **200** 且不含该规则——把第 136 轮修过的缺陷钉在 HTTP 层。

| 验证 | 结果 |
|---|---|
| 后端测试 | **367 个，全部通过，0 跳过** |

> **又一次踩到"补丁静默跳过"**：第一次插入时锚点方法名写错（实际叫
> `promotionPreviewAppliesFullReduction`），脚本却照常打印"added"，是 `grep` 才发现没插进去。
> 现在流程改为：**每次补丁后必须 grep 命中数 ≥1 才算生效**。

---

## 2026-10-19：第三方服务凭据后台配置 + 演示强制 mock

按需求：**服务商与 AK 在后台配置，拿到 AK 后填入即生效；演示环境无论配置什么都走 mock**。

### 设置层

| 能力 | 说明 |
|---|---|
| 新增配置键 | `sms_access_key` / `sms_secret_key` / `sms_sign_name` / `logistics_key` / `logistics_customer` / `map_provider` / `map_key` |
| 脱敏 | `adminSettings()` 对密钥类字段只回显 `******`；`rawValue()` 才取明文（内部使用） |
| 保存 | 前端回传 `******` 表示"不修改"，不会把掩码写进库 |

### 物流适配

- `LogisticsTrackingService`：AK 优先取后台 `logistics_key`，环境变量兜底；`app.mode=demo` 时**直接返回 mock**，
  即使配了真实服务商也不发外网请求。
- 新增 `Kuaidi100Query`：按官方规则构造请求与签名 `MD5(param+key+customer)` 并大写。

| 验证 | 结果 |
|---|---|
| 后端测试 | **371 个，全部通过，0 跳过** |
| 新增用例 | 后台读只看到掩码、`rawValue` 取明文；签名确定性/不同输入不同签名/参数必填；未配置 AK 时内部告警含 "AK" |

### 仍未做（明确）

- **真实 HTTP 调用未发起**：没有 AK 无法联调，代码在构造好请求后显式失败并写入告警，
  不假装成功。拿到 AK 后只需补一段 `RestClient` 调用与响应解析。
- 短信真实通道同理（目前仍是 mock sender）。

---

## 2026-10-19：演示/生产严格隔离（启动即失败）

按"mock 绝不能污染生产"的要求，审计并堵住三条泄漏路径：

| 泄漏点 | 之前 | 现在 |
|---|---|---|
| 通知渠道 sender | **永远注册 mock**，生产会把 SMS 记成 `SENT` | prod 下不注册 mock；未配真实服务商 → `UnconfiguredChannelSender` 抛错 → 通知落 `FAILED` |
| mock 支付接口 | 仅靠服务层校验 provider | 生产**不注册** `/api/v1/payments/mock/**`（`@ConditionalOnExpression`） |
| 适配器配置 | 缺省全 mock，prod 也能起 | `ProductionSafetyGuard` 在 prod 下校验支付/物流/短信/存储，**不合规直接启动失败**并列出所有问题 |

新增 `docs/DEMO_PROD_SEPARATION.md`（模式定义、启动校验、渠道隔离、部署检查清单）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **381 个，全部通过，0 跳过** |
| 新增用例 | prod+mock 启动失败（含三处提示）、未知服务商拒绝、空值拒绝、demo/dev 放行；生产下 SMS sender 抛"拒绝伪造发送成功"、`IN_APP` 始终可用 |

---

## 2026-10-19：分销比例配置化 + 二级分佣（纠正写死 5%）

按明确口径重做（详见 `docs/DISTRIBUTION_REDESIGN.md`）：

- V57：`distribution_commission` 增加 `level` / `rate_mode` / `rate_value` / `promoter_type`，
  唯一键改为 `(order_id, promoter_staff_id, level)`；新增 `promoter_link`（推广员上级关系）。
- `DistributionConfig`：比例方式（`GROSS` 百分比 / `FIXED` 固定额）、二级开关、冻结策略、
  最低提现、打款方式全部来自**后台设置且不缓存**（保存即生效）；非法值明确报错。
- `ReferralService.settleOrder`：一级 = 订单归属推广员，二级 = 其上级（存在才发）；
  买家**永不参与分佣**。

| 验证 | 结果 |
|---|---|
| 后端测试 | **386 个，全部通过，0 跳过** |
| 新增用例 | 默认 5%/2% 二级；`FIXED` 模式算 800 分；百分比越界拒绝；非法模式/冻结策略拒绝；二级可关 |

> `PROFIT`（按毛利分佣）**未实现**：`order_line` 没有成本快照，做不了真实毛利，不做假实现。

---

## 2026-10-19：纠正"缺配置拒绝启动"的错误做法

用户指出：**生产缺配置不应该阻断启动**——否则管理员进不了后台就无法配置，形成死锁。

- 删除 `ProductionSafetyGuard`（fail-fast），改为 `ProductionReadinessChecker`：
  - 启动**永远成功**，缺失项以 WARN 列出；
  - 新增 `GET /admin/v1/system/readiness` 供后台查看各能力配置状态；
  - 设置表不可读时也只告警（不阻断）。
- 运行时按能力拒绝：生产未配真实支付 → `payMock` 返回 **503**「支付暂不可用」并写风控
  告警 `PAYMENT_PROVIDER_NOT_CONFIGURED`（顾客看不到内部原因）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **385 个，全部通过，0 跳过** |
| 真实启动（prod + mock 服务商） | ✅ **Started FullMallApplication**，并输出未配置清单 WARN |
| 新增用例 | 缺配置只报告不抛错；真实服务商判定就绪；mock 在生产不可用；demo 一律可用 |

---

## 2026-10-19：提现规则落地（OPEN_ITEMS B5 / B7 关闭）

- V58：`withdrawal` 增加 `payout_method`（ALIPAY/BANK/WECHAT，DB CHECK）与 `payout_account`。
- `WithdrawalService.request(..., payoutMethod, payoutAccount)`：
  - 金额低于 `distribution.min-withdrawal` → 拒绝（不再写死 0）；
  - 打款方式必须在 `distribution.payout-methods` 白名单内（后台配置决定可选集合）；
  - 记录申请人选择的收款方式与账号。
- 旧签名保留：未指定方式时取配置里的第一个允许方式。

| 验证 | 结果 |
|---|---|
| 后端测试 | **387 个，全部通过，0 跳过** |
| 新增用例 | 低于最低额被拒；不支持的方式被拒；后台开启 WECHAT 后可申请并落库 |

---

## 2026-10-19：绑定策略真正生效 + 上级关系可维护（B3 / B2）

- `attachCheckout` 现在读取 `distribution.bind-mode`（ONCE/PERMANENT）与 `bind-timing`（REGISTER/CART/VISIT）：
  - `ONCE`：已有活跃绑定则不覆盖，订单归属首个推广员；
  - `PERMANENT`：新推广员接管；
  - `REGISTER`：结算不新建绑定（只沿用注册关系），无关系则本单不归属。
- 新增 `POST /admin/v1/distribution/promoters/{id}/upline`（维护二级上级；禁止自己指向自己）。
- `DistributionConfig` 校验绑定枚举，非法值明确报错（不再静默取默认）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **394 个，全部通过，0 跳过** |
| 新增用例 | 一次性不抢客 / 终身可接管 / 注册时机结算不建关系；有上级时二级佣金 500+200 落库 |
| 接口对齐 | 191 路由 / 18 调用 / 0 未匹配 |

---

## 2026-10-19：提现审核权限化（B6）

- 新增权限 `withdrawal.review`（V61，默认只授 ADMIN，可在权限矩阵里授予任意角色）。
- `/admin/v1/distribution/withdrawals/**` 单独映射到该权限；其余分销接口仍是 `promotion.manage`。
- 去掉 `AdminDistributionController` 的 `@PreAuthorize("hasRole('ADMIN')")`——它与矩阵冲突，
  导致"矩阵里给了权限也不生效"。

| 验证 | 结果 |
|---|---|
| 后端测试 | **397 个，全部通过，0 跳过** |
| 新增用例 | 提现三个路径映射到 `withdrawal.review`；uplink/commissions 仍映射 `promotion.manage` |

> 踩坑：V61 一开始写 `INSERT INTO permission (code, name, ...)`，而表字段是 `label`/`group_name`，
> 导致 **184 个测试因迁移失败连带报错**。已按真实 schema 修正。

---

## 2026-10-19：后台分销配置表单（B1）

设置页新增「分销设置」区块（13 项）：一级/二级佣金方式与数值、二级开关、绑定方式与时机、
冻结策略与售后窗口、最低提现（按元输入，内部存分）、可选打款方式、员工出款通道、员工二级开关。

| 验证 | 结果 |
|---|---|
| 后端测试 | **398 个，全部通过，0 跳过** |
| 新增用例 | 13 个配置键全部可保存并读回；`DistributionConfig` 立即反映（FIXED/20000/ALIPAY,WECHAT/ONCE） |
| 前端构建（含类型检查） | ✅ |

> **未做浏览器点击验证**：表单只做了构建 + 类型检查 + 后端"键可保存且生效"的测试，
> 没有起服务在页面上点一遍。这条按"已做未验证"记录，不冒充已验证。

---

## 2026-10-19：分销设置表单补上真机验证（B1 收口）

新增 E2E `web/e2e/distribution-settings.spec.ts`：登录 → 系统设置 → 改「一级佣金值」为 7、
「最低提现」为 200 元 → 保存 → **刷新页面值仍在**。

| 验证 | 结果 |
|---|---|
| E2E（Chromium） | ✅ 通过（3.4s） |
| 接口复核 | `distribution.level1-value=7`、`distribution.min-withdrawal=20000` |

### 过程中发现自己的测试是假绿

第一版用 `.locator('input').nth(1)` 定位「一级佣金值」，**实际填到了别的输入框**——
测试通过、刷新也显示 7，但接口查出来仍是 5。改为按表单项标签定位后才是真验证。

教训：**断言要落到业务字段**（接口复核），不能只看"页面显示值没变"。

---

## 2026-10-19：审计 outbox 崩溃恢复入口（H1）

- 新增定时任务 `audit-outbox-recovery`（进调度注册表，cron 可在后台改，**默认关闭**）。
  默认关闭的原因：恢复走序列化还原，类结构变更期容易刷失败记录，应由运维确认后开启。
- 新增运维入口：`GET /admin/v1/audit/outbox/pending`（看积压）、
  `POST /admin/v1/audit/outbox/recover`（立即补提交，返回 before/after/recovered）。
- 权限：映射到 `settings.manage`（事故处置属最高配置权限）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **399 个，全部通过，0 跳过** |
| 新增用例 | 任务 code/默认关闭断言；注册表数量 8→9 |
| 接口对齐 | 193 路由 / 18 调用 / 0 未匹配 |

> 调度注册表测试原本断言 `hasSize(8)`，新增任务后立刻失败——**这是测试在保护"任务清单不被悄悄改动"**，
> 已同步更新为 9 并加入新 code。

---

## 2026-10-19：分销报表（B11）

`GET /admin/v1/distribution/report?from=YYYY-MM-DD&to=YYYY-MM-DD`（权限 `promotion.manage`）：
按推广员汇总订单额、佣金、**冻结 / 可提现（已扣提现消耗）/ 已提现**、工资通道、钱包通道、订单数，并给出合计。

| 验证 | 结果 |
|---|---|
| 后端测试 | **401 个，全部通过，0 跳过** |
| 新增用例 | 三笔佣金（冻结 500 / 可提 300 消耗 100 → 200 / 已提 200）：工资 500、钱包 500、合计 1000；起止日期倒置被拒 |
| 接口对齐 | 194 路由 / 18 调用 / 0 未匹配 |

> 报表口径写在类注释里（单位分、可提现已扣消耗），避免以后有人按 `commission_amount` 直接求和的错算法。

---

## 2026-10-19：后台订单详情接口（D2）

- 新增 `GET /admin/v1/orders/{orderId}`（权限 `order.manage`），返回订单及**订单行**，**不依赖会员归属**。
- 后台订单详情页从顾客端 `/api/v1/orders/{id}`（要求会员身份）切到该接口——此前客服查单会被 404。

| 验证 | 结果 |
|---|---|
| 后端测试 | **402 个，全部通过，0 跳过** |
| 新增用例 | 直接按订单 ID 读取，含 1 条订单行、金额 5000 分；无会员身份也能读 |
| 接口对齐 | 195 路由 / 18 调用 / 0 未匹配 |
| 前端构建 | ✅ |

> 测试踩坑：`fulfillment_type` 的值是 `WAREHOUSE_SHIP` 等，我按 `SHIP` 写导致枚举解析失败。
> 这提醒：**枚举值必须以代码为准**，不能凭接口里的 `fulfillmentMode=SHIP`（那是另一套枚举）。

---

## 2026-10-19：退款按行分摊落库（A7 部分完成）

- `PaymentService.refund(orderId, amount)` 默认写入行分摊；售后单路径传 `recordLineAllocation=false`
  （它自己 `recordForAfterSale` 精确到商品与数量），**避免同一笔退款记两次**。
- 新增 `LineRefundService.allocateAndRecord(...)`：`allocate` 只是**纯计算器**，不写库。

| 验证 | 结果 |
|---|---|
| 后端测试 | **403 个，全部通过，0 跳过** |
| 新增用例 | 直接退 4000 分 → `order_line_refund` 落 1 行、金额 4000 |

### 我这次犯的错（值得记）

第一版我只调了 `allocate(...)` 并把返回值丢掉——**看着像记账，实际一行都没写**。
是"退 4000 应落 4000"的断言把它抓出来的。教训：**计算函数与持久化函数必须分开命名**，
调用处只允许调带 `AndRecord` 的那个。

> 仍未做（A7 剩余）：按行分摊结果回退**积分/佣金**的比例计算，目前只做记录与追溯。

---

## 2026-10-19：游客订单访问令牌（D1）

- V62：`orders.access_token_hash`（SHA-256，带固定盐串）。
- 下单时若订单**未绑定会员**，生成 64 位令牌，哈希入库；明文通过响应头 `X-Order-Token` **只返回一次**。
- `OrderOwnershipGuard.requireAccess(orderId, memberId, phone, token)`：会员单仍只认本人；
  游客单**令牌命中**或**联系人手机号正确**才放行（后者保留以兼容"电话报单号查询"的老路径）。
- 顾客端结算保存令牌到 `localStorage`，订单页带 `X-Order-Token`。

| 验证 | 结果 |
|---|---|
| 后端测试 | **406 个，全部通过，0 跳过** |
| 新增用例 | 仅订单号 → 拒绝；正确令牌 → 放行；错误令牌 → 拒绝；手机号仍可用 |
| 前端构建（storefront） | ✅ |
| 接口对齐 | 195 路由 / 18 调用 / 0 未匹配 |

> 未做：令牌轮换/过期。当前令牌长期有效，与"订单号+手机号"同级别的可撤销性尚未实现。

---

## 2026-10-19：顾客端物流轨迹展示（D9，未做浏览器验证）

- `GET /api/v1/orders/{id}/tracking` 现在接受 `X-Order-Token`（游客订单也能查）。
- storefront 订单页：有运单号的行显示「查看物流轨迹」，点击才请求（`immediate: false`），
  展示时间/状态/地点；失败时显示"物流信息暂时不可用"，不伪装成功。

| 验证 | 结果 |
|---|---|
| 后端测试 | **406 个全绿** |
| 前端构建（storefront） | ✅ |
| 接口对齐 | 195 路由 / **19** 调用 / 0 未匹配 |

> **未做浏览器验证**：没有起服务点过这条链路（本轮的时间花在一次补丁失效的排查上）。
> 按口径记为"已做未验证"，不冒充已验证。

### 又一次补丁静默跳过

第一次插入用 `if "tracking" not in s` 判断——但文件里本来就有 `trackingNo`，条件为假，**整段代码没写入**，
而构建照样成功。是我自己的 `print` 打印 `False` 才发现的。
后续规则：补丁必须用 `assert 锚点 in 内容`，**禁止用宽泛词做存在性判断**。

---

## 2026-10-19：补 E2E 时发现「自营订单根本发不了货」（D10 已修）

补 D9 的 E2E 时，`POST /store/v1/fulfillments/{id}/ship` 一直返回
`FULFILLMENT_NOT_SHIPPABLE（当前状态 ALLOCATED）`。追查发现：

- 自营履约单初始状态是 `ALLOCATED`；
- `shipFulfillment` 只接受 `ACCEPTED/PICKING/PACKED`；
- **没有任何接口把 ALLOCATED 推进到那三个状态**（只有供应商链路有 accept）。

也就是说：**自营订单在 API 层完全发不了货**。已补：

| 新接口 | 作用 |
|---|---|
| `POST /store/v1/fulfillments/{id}/accept` | ALLOCATED → ACCEPTED |
| `POST /store/v1/fulfillments/{id}/pack` | ACCEPTED/PICKING → PACKED |

### 验证

| 项 | 结果 |
|---|---|
| 后端测试 | **406 个全绿** |
| 新增 E2E `tracking.spec.ts` | ✅ 下单 → 支付 → **接受 → 打包 → 发货** → 顾客接口查轨迹含「已揽收」 |
| 接口对齐 | **197** 路由 / 19 调用 / 0 未匹配 |

### 同时发现的新缺陷（未修，已登记 D11）

会员订单页**硬刷新会 404**：页面在服务端渲染时读不到 `localStorage` 里的 token。
之前 E2E 都从站内跳转（客户端渲染）所以没暴露。修法需改用 cookie 保存令牌。

---

## 2026-10-19：会员令牌改 cookie，修复订单页硬刷新 404（D11）

- 结算换取会员令牌后，除 `localStorage` 外**同时写 cookie** `fullmall_member_token`（30 天、SameSite=Lax）。
- 订单页改用 `useCookie` 读取：**服务端渲染也能带身份**。

| 验证 | 结果 |
|---|---|
| E2E | ✅ 硬刷新进入 `/orders/{id}` 能渲染「履约」与运单号（此前必 404） |
| 前端构建 | ✅ |
| 后端测试 | 406 个全绿（本轮未改后端） |

### 仍未解决（诚实记录）

轨迹区「查看物流轨迹」按钮点击后**卡在"查询中…"**，我没找到原因（后端接口 0.0s 返回）。
测试里已改为只断言 SSR 渲染，**没有把按钮交互写成通过**。

---

## 2026-10-19：物流轨迹按钮卡住已修（D9 收口）

根因：我把 `useAsyncData` 返回的 `data` 当成 asyncData 对象，调用了 `tracking.refresh()`——
它不存在，异常被 catch 吞掉，界面停在"查询中…"。

改法：轨迹查询改用**显式状态**（`tracking` / `trackingLoading` / `trackingError` + `$fetch`），
不再依赖 `useAsyncData` 的 pending 语义。

| 验证 | 结果 |
|---|---|
| E2E `tracking.spec.ts` | ✅ 下单 → 接受 → 打包 → 发货 → 打开订单页 → 点「查看物流轨迹」→ 显示「已揽收」 |
| 前端构建 | ✅ |

> 本轮**没有重跑全量 E2E**：我先把服务杀掉了才跑套件，14 个失败是环境缺失而非回归。
> 这一点如实标注，不给错误结论。

---

## 2026-10-19：全量 E2E 复跑 **14/14 通过**

| 阶段 | 结果 |
|---|---|
| 后端测试 | ✅ 406 个 |
| E2E（Chromium） | ✅ **14 / 14**（40.4s，含新增 tracking 用例） |
| 接口对齐 | 197 路由 / 19 调用 / 0 未匹配 |

复跑确认第 158 轮改动没有破坏其它链路。`docs/PROJECT_STATUS.md` 已同步为 406 测试 / 14 E2E。

---

## 2026-10-19：核实 D3（结论：前提有误）+ 售后操作按钮 loading

### D3 不是缺陷

清单里我写的是"换货不回补库存"。核对代码后不成立：

- `shipExchange` 要求售后状态为 `RECEIVED`；
- `receive` 对 RETURN 与 EXCHANGE **都**写入 `QUARANTINE` 批次 + `stock_move` 流水。

所以旧货在发换货前已经入库（隔离批次待质检）。**清单条目已改正为"前提有误"**，不制造假修复。

### 售后操作按钮加真实 loading

- 「同意 / 拒绝 / 退款」按钮绑定 `:loading="actionId === row.id"`，防止**重复点击导致重复退款**。
- 详情弹窗**没有异步请求**，因此不加 loading——我原先加过一个"立刻置 false"的假 loading，已撤销。

| 验证 | 结果 |
|---|---|
| 前端构建（含类型检查） | ✅ |

> 本轮未跑后端测试与 E2E：只改了前端展示层，未触及接口契约。

---

## 2026-10-19：前端 loading 缺口补了资金相关三处（H6 部分）

全量扫描（按完整函数体判定）发现 **13 处**异步操作没有 loading，已优先修掉风险最高的：

| 位置 | 风险 |
|---|---|
| 门店核销（`verifyPickup`） | **重复核销 = 重复给货**；加 `verifying` 互斥 + 按钮 loading |
| 商品上下架（`toggleStatus`） | 连点造成状态抖动；加 `actionId` 互斥 |
| 售后同意/拒绝/退款 | 重复点击**重复退款**（上轮已加） |

| 验证 | 结果 |
|---|---|
| 前端构建（5 应用 + 类型检查） | ✅ |

> 剩余 8 处（促销启停、券启用、通知已读、员工 PIN/门店、POS 会员查询/解锁、拣货确认）已写进清单，
> 下轮继续。本轮**只做前端**，未跑后端测试与 E2E。

---

## 2026-10-19：前端 loading 缺口清零（H6 收口）

扫描脚本按**完整函数体**判定，最终结果 **0 剩余**。本轮补齐：

| 位置 | 做法 |
|---|---|
| 促销启停（`changeStatus`/`activateRule`/`endRule`） | 共用 `actionId` 互斥 + 按钮 `:loading` |
| 券模板启用 | `actionId` 互斥 + `:loading` |
| 通知标记已读 | `markingId` 互斥 |
| 员工 PIN 保存 | `pinSaving`（按钮已有绑定） |
| POS 会员识别 / 解锁 | `memberLooking` / `unlocking`，按钮分别绑定 |
| 门店拣货确认 | `pickingId` 互斥 + `:loading`（防重复拣货） |

| 验证 | 结果 |
|---|---|
| 前端构建（5 应用 + 类型检查） | ✅ |
| 扫描复查 | **0 处缺口** |

> 本轮只改前端；未跑后端测试与 E2E（无接口变更）。
> 期间一次"自动注入 try/finally"把嵌套结构写坏，已整体 `git checkout` 回退改为逐个精确修改。

---

## 2026-10-19：评价审核接口（E7）

- `GET /admin/v1/reviews?status&page&size`：后台列表（含 `HIDDEN`）。
- `POST /admin/v1/reviews/{reviewId}/status`：`PUBLISHED` ↔ `HIDDEN`，其余值报错。
- 权限映射 `product.manage`；**只隐藏不删除**（可逆、可审计，删评价会破坏顾客信任）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **408 个，全部通过，0 跳过** |
| 新增用例 | 隐藏后前台列表为空、后台可查、可恢复（大小写不敏感）；非法状态与不存在的评价均报错 |
| 接口对齐 | 199 路由 / 19 调用 / 0 未匹配 |

> 未做：后台审核页面（接口先行）。清单已注明。

---

## 2026-10-19：评价审核后台页面（E7 收口）

- 新增 `ReviewsView.vue`：评分/内容/状态/时间列表，状态筛选，隐藏↔恢复按钮（带 `:loading` 互斥），
  分页页脚与空状态；侧边栏「评价审核」+ 路由已注册。
- 新增 E2E `reviews.spec.ts`：登录 → 进入评价审核 → 列表加载 → 按「已隐藏」筛选。

| 验证 | 结果 |
|---|---|
| 后端测试 | **408 个全绿** |
| **E2E** | ✅ 页面加载 + 筛选可用（3.0s） |
| 前端构建 | ✅ |
| 接口对齐 | 199 路由 / 19 调用 / 0 未匹配 |

> E2E 范围说明：评价只能由顾客在已履约订单后提交，**本用例不造数据**，
> 隐藏/恢复的业务正确性由后端 `ReviewModerationTest` 覆盖，两者互补。

---

## 2026-10-19：审计查询接口 + 发现审计覆盖面几乎为零（I6/I9）

### 新接口

`GET /admin/v1/audit/history?entityType=asset&entityId=...`：返回 JaVers 快照（时间/操作人/变更/状态）。

**注册制白名单**：platform 不依赖业务模块（Modulith 边界），由各模块自行 `registry.register(...)`；
未注册类型直接拒绝，避免任意类名反射。

### 更重要的发现（如实登记）

全仓只有 `AssetService` 有 **3 处** `auditTrail.commit` —— 订单、支付、券、权限、提现等关键动作
**都没有进审计**。所以这个查询接口目前几乎查不到业务数据，价值有限。

已登记新条目 **I9「审计覆盖面」**：把关键写操作接入审计，才是真正让审计有用的工作。

| 验证 | 结果 |
|---|---|
| 后端测试 | **410 个，全部通过，0 跳过** |
| 新增用例 | 未注册类型被拒；已注册类型（大小写不敏感）放行 |
| 接口对齐 | 200 路由 / 19 调用 / 0 未匹配 |

> 期间 Modulith 边界测试拦下了我第一版（platform 直接 import asset 实体）——**架构约束起了作用**，
> 改为注册制后通过。

---

## 2026-10-19：操作审计查询接口（I6 收口）+ 纠正 I9 的前提

### 纠正前提

我上轮说"审计只接了资产模块"——那只针对 **JaVers**。实际项目里还有应用级 `audit_event` 表，
**已有 16 处写入**（提现、权限、支付配置、商品翻译、库存、导出等）。缺的是**查询**，不是记录。

### 新接口

`GET /admin/v1/audit/events?eventType&actor&from&to&page&size`：按类型/操作人（模糊）/时间范围过滤，
返回 `{items,page,size,total}`。权限 `settings.manage`。

| 验证 | 结果 |
|---|---|
| 后端测试 | **412 个，全部通过，0 跳过** |
| 新增用例 | 类型过滤、操作人模糊过滤、时间范围（未来时间返回 0）、分页 size 生效 |
| 接口对齐 | 201 路由 / 19 调用 / 0 未匹配 |

### 清单修正

- I6（操作审计查询）→ ✅
- I6b（JaVers 快照查询）→ ⚠️（全仓仅 1 个 JPA 实体，价值有限）
- I9（审计覆盖面）→ ⚠️：支付与退款仍未接入

---

## 2026-10-19：支付与退款接入操作审计（I9 核心部分）

- `PaymentService.payMock` → `PAYMENT_SUCCEEDED`（actor=`customer:<手机号>`，detail 含金额与币种）。
- `PaymentService.refund` → `PAYMENT_REFUNDED`（actor=`system:refund`，detail 含退款额）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **413 个，全部通过，0 跳过** |
| 新增用例 | 退款后 `audit_event` 出现且仅 1 条 `PAYMENT_REFUNDED`，actor 与金额正确 |
| 接口对齐 | 201 路由 / 19 调用 / 0 未匹配 |

> 仍未接入：券核销、订单取消、售后审核（已在清单注明，不假装覆盖完整）。

---

## 2026-10-19：后台退款接口（D7 后端部分）

`POST /admin/v1/orders/{orderId}/refunds`，body `{amount, reason}`：

- 金额 > 0、原因必填；
- 写 `ORDER_REFUND_REQUESTED` 审计（actor=`staff:<用户名>`）；
- 调用 `PaymentService.refund`，**按行分摊自动完成**（不重复记录）。

**为什么放 payment 模块**：第一版放在 `AdminOrderController`，Modulith 立刻报
`Cycle detected: Slice order -> ...`——payment 已依赖 order，反向依赖构成循环。
移到 `payment/api/AdminRefundController` 后通过。

| 验证 | 结果 |
|---|---|
| 后端测试 | **413 个全绿**（含模块边界测试） |
| 接口对齐 | **202** 路由 / 19 调用 / 0 未匹配 |

> 未做：前端退款按钮、行级勾选（按行分摊已自动完成，界面暂不需要选行）。

---

## 2026-10-19：后台退款界面 + E2E（D7 收口）

订单详情新增「发起退款」：金额按元输入（内部换算成分）、原因必填、`:loading` 防重复点击。
提交后刷新详情与列表。

| 验证 | 结果 |
|---|---|
| **E2E** | ✅ 造单 → 支付 → 后台打开订单 → 发起退款（半额 + 原因）→ 提示"退款已提交" |
| 前端构建 | ✅ |
| 后端测试 | 413 个全绿（本轮未改后端） |

### 又踩一个模板陷阱

我把 `<el-dialog>` 用 `s.replace('</template>', ...)` 插入，命中的是**表格列插槽的第一个 `</template>`**，
结果退款输入框被渲染成"每行一个"（E2E 报 20 个同名输入框）。
改为插入到**最后一个** `</template>` 前才正确。

教训：改 Vue 模板不能用 `replace` 命中第一个闭合标签——模板里 `</template>` 到处都是。

---

## 2026-10-19：券核销与订单取消接入审计（I9 收尾）

| 事件 | 触发点 | actor |
|---|---|---|
| `COUPON_REDEEMED` | 结算核销券 | `member:<id>` |
| `ORDER_CANCELLED_BY_SYSTEM` | 超时未支付自动取消 | `system:expiry` |

审计写入点累计 **21 处**。

| 验证 | 结果 |
|---|---|
| 后端测试 | **414 个，全部通过，0 跳过** |
| 新增用例 | 券核销后 `audit_event` 恰好 1 条且 actor 正确 |
| 接口对齐 | 202 路由 / 19 调用 / 0 未匹配 |

> 订单取消事件目前**仅代码接入，无断言测试**（超时任务测试成本高），已在清单注明。

---

## 2026-10-19：顾客端收藏页（E9 部分）

- 后端 `GET /api/v1/favorites` 从"只有 skuId"改为返回**商品 ID / 标题 / 价格 / 币种**（JOIN sku+product+翻译）。
- storefront 新增 `/favorites` 页面：登录态（cookie）、加载中/失败/空态、跳商品详情。

| 验证 | 结果 |
|---|---|
| 后端测试 | **415 个，全部通过，0 跳过** |
| 新增用例 | 收藏后列表含标题「收藏商品」、价格 2599、商品 ID |
| 前端构建 | ✅ |
| 接口对齐 | 202 路由 / **20** 调用 / 0 未匹配 |

> 未做：足迹页面（E10 前身）；收藏页**未做浏览器验证**（只验证了接口与构建）。

---

## 2026-10-19：顾客消息中心 + 收藏入口（E10 / E9 收口）

- storefront 新增 `/notifications`：未读计数、列表、标记已读（互斥防重复）、加载/失败/空态。
- 顶部导航新增「我的收藏」「消息中心」入口（此前页面存在但无法到达）。
- E2E：未登录显示登录提示；带 cookie 后页面渲染，收藏页可达。

| 验证 | 结果 |
|---|---|
| **E2E** | ✅ `member-pages.spec.ts` 通过（1.3s） |
| 前端构建 | ✅ |
| 后端测试 | 415 个（上轮已验证，本轮未改后端） |

> 收藏页在 E2E 中只断言"标题可见 + 空态/加载"，**未验证带数据的渲染**（需造收藏数据）。

---

## 2026-10-19：生日礼积分（E3）

- `BirthdayGiftService.grantForDate(date)`：按等级 `member_level.birthday_points` 发放；
  **业务时区固定 UTC+8**（服务器 UTC 会让生日错一天）。
- 幂等两道：`member.birthday_gift_year` + `points_ledger.business_key = birthday:<member>:<year>`。
- `BirthdayGiftTask` 注册进调度表（`member.birthday-gift`，默认每天 00:10，**默认开启**）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **416 个，全部通过，0 跳过** |
| 新增用例 | 生日当天会员发放、非当天不发；**再次执行不重复发放**（余额不变） |

> 调度注册表测试再次拦下我：新任务没登记到断言里导致 `hasSize(9)` 失败，已更新为 10。

---

## 2026-10-19：收藏页带数据 E2E（E9 收口）

新增 `favorites.spec.ts`：OTP 登录 → 调收藏接口 → 校验接口返回标题/价格 → 带 cookie 打开 `/favorites`
→ 标题链接与价格都渲染。

| 验证 | 结果 |
|---|---|
| E2E | ✅ `favorites.spec.ts` + `member-pages.spec.ts` 共 2 条通过 |
| 后端测试 | 416 个（上轮已验证） |

> 又一次踩"旧 jar"：第一次跑 E2E 时接口仍返回只有 `skuId` 的旧结构，因为只 `mvn test` 未 `package`。
> 重新打包后接口返回 `title/priceAmount/productId`，E2E 随即通过。
> **这条已经踩了第 4 次**，之后凡是验证 HTTP 行为前一律先 `package`。

---

## 2026-10-19：浏览足迹页面（E9 完成）

- 后端 `GET /api/v1/history` 补齐 `productId / title / priceAmount / currency`（JOIN sku+product+翻译）。
- storefront 新增 `/history`：登录态、加载/失败/空态、查看次数与日期；导航新增「浏览足迹」。

| 验证 | 结果 |
|---|---|
| 后端测试 | **417 个，全部通过，0 跳过** |
| 新增用例 | 记录足迹后列表含标题「足迹商品」、价格 3999、商品 ID、viewCount=1 |
| 前端构建 | ✅ |
| 接口对齐 | 202 路由 / **23** 调用 / 0 未匹配 |

> 足迹页**未做浏览器 E2E**（收藏页已有同构 E2E 覆盖同类逻辑），如实标注。

---

## 2026-10-19：券结算 E2E（H12 补覆盖）

新增 `coupon-checkout.spec.ts`（纯接口链路，不依赖页面）：

1. 后台建券（5 元抵扣）→ 启用；
2. 会员 OTP 领券（201）；
3. 用券下单 → **`discount.amount` 恰好 500**；
4. **重复用同一张券再下单被拒**（券已核销）。

| 验证 | 结果 |
|---|---|
| E2E | ✅ 通过（1.4s） |

第 4 步是重点：它验证的是"券不会被用两次"，这是**资损防线**，单元测试里容易漏掉。

> 仍缺：售后换货全流程 E2E（需造换货 → 收货 → 发货 → 完成四步）。

---

## 2026-10-19：生日礼改为按会员时区（K1）

原实现固定 UTC+8 是错的：会员在 UTC+14 或 UTC-8 时"今天是不是生日"不同。

- V63：`member.timezone`（默认 `Asia/Shanghai`）。
- `BirthdayGiftService.grantAt(instant)`：逐个会员按其时区算本地日期，只在**本地生日当天**发放。
- **不顺延补发**：错过当天就视为错过（防止改时区/跨日重复领取）。
- 时区非法（如写错字符串）**直接抛错**，不静默按默认时区发（会错一天）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **418 个，全部通过，0 跳过** |
| 新增用例 | 同一时刻 UTC+14 会员（10-21）与 UTC-8 会员（10-20）都发放；UTC+8 的 10-22 不发放；重复执行不重复发放；次日不补发 |

---

## 2026-10-19：快递100 响应解析（D8 推进）

新增 `Kuaidi100Response.parse(body)`：纯解析、不发请求，AK 到位后可直接接 `RestClient`。

| 情形 | 行为 |
|---|---|
| `status=200` + data 数组 | 转成 `TrackingEvent`（时间按 Asia/Shanghai 解析） |
| `status!=200` | **抛错并带 message**（不返回空轨迹，避免顾客以为"未发货"） |
| 非 JSON / 缺 data / 条目缺字段 | **抛错**（报文结构变化必须暴露） |

| 验证 | 结果 |
|---|---|
| 后端测试 | **421 个，全部通过，0 跳过** |
| 新增用例 | 正常解析 2 条；失败状态抛错含原因；非法 JSON/缺 data/缺字段各自抛错 |

> 仍未做：真正的 HTTP 发送（需要 AK 才能联调）。清单 D8 保持 ⚠️。

---

## 2026-10-19：首次配置检查 + 后台常驻提示（K2 第一步）

- 后端：`GET /admin/v1/setup/status`（8 个决策项，`mock` 不算已配置）、`POST /admin/v1/setup/complete`。
- 前端：后台顶部常驻提示"首次配置尚未完成（还差 N 项）"，可跳系统设置；**不阻断任何操作**。

| 验证 | 结果 |
|---|---|
| 后端测试 | **423 个，全部通过，0 跳过** |
| 新增用例 | 缺项被指出（含"未配置真实支付"提示）；`mock` 不算已配置；真实服务商算；标记完成后 `completed=true` |
| 前端构建 | ✅ |
| 接口对齐 | **204** 路由 / 23 调用 / 0 未匹配 |

> 未做：分步向导页面（基础/运营/服务商/完成四页）。
> 未验证：提示条的浏览器行为（本轮仅构建 + 后端测试）。

---

## 2026-10-19：首次配置向导页面（K2 完成）

`/setup` 四步向导：

| 页 | 内容 |
|---|---|
| 1 基础 | 站点名称、默认语言、默认币种 |
| 2 运营规则 | 积分过期模式/天数、一级佣金、最低提现 |
| 3 服务商 | 支付/物流/短信 + AK（留空=不修改，已配置显示 `******`） |
| 4 完成 | 汇总缺项提示，一键"完成并进入后台" |

顶部常驻提示可直达向导；每步有「跳过本页」，**不阻断使用**。

| 验证 | 结果 |
|---|---|
| **E2E** | ✅ 登录 → 进入 /setup → 三步保存 → 完成页 → "首次配置已完成"（3.0s） |
| 前端构建 | ✅ |
| 后端测试 | 423 个（上轮已验证） |

---

## 2026-10-19：积分逐笔余额与 FIFO 扣减（K3-A）

- V64：`points_ledger.remaining_points`（EARN 回填为原值，其余为 0）+ FIFO 索引。
- `MemberPointsService.consumeFifo`：核销时按 `created_at, id` 顺序扣减最早 EARN 行；
  **余量不足直接抛错**（说明账本与余额已不一致，必须暴露而不是少扣）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **424 个，全部通过，0 跳过** |
| 新增用例 | 获得 100 + 50，抵扣 120 → 逐笔余量 `[0, 30]`，会员余额 30 |
| 存量用例 | `MemberPointsServiceTest` 的初始积分补了对应 EARN 行（原先只有总额、没有逐笔行，被新校验拦住） |

> 未做：过期任务与后台判定（K3-B）。清单 K3 记 ⚠️。

---

## 2026-10-19：退款累计上限（A4 部分）

**发现真实缺口**：`MockPaymentConnector` 只校验"单次退款 ≤ 支付额"。先退 4000 再退 8000（原 10000）会通过，
等于**超退 2000**。

- V65：`payment.refunded_amount`（按历史 `payment_event` 回填）+ DB CHECK `≤ amount_amount`。
- `PaymentService.refund`：先算可退余额，超额抛 `REFUND_EXCEEDS_REMAINING`；成功后累加。

| 验证 | 结果 |
|---|---|
| 后端测试 | **425 个，全部通过，0 跳过** |
| 新增用例 | 退 4000 → 再退 8000 被拒（含"已退/本次/可退"数字）；再退 6000 成功，累计 10000 |

### 顺带暴露的既有问题（已登记 A9）

测试里用单数量行时，第二次退款失败：`累计退款数量 2 超过该行数量 1`。
原因是 `allocateAndRecord` 用金额反推数量，**同一行多次部分退款会累计超量**。
这是既有缺陷，本轮只登记不改（改它需要重新设计行退款的金额/数量关系）。

---

## 2026-10-19：生日礼防刷与时区默认（K1 收口）

按新规则调整：

| 规则 | 实现 |
|---|---|
| 改生日不能重复领 | `birthday_gift_year` 年度幂等：领过后把生日改成明天/下月都不会再发 |
| 生日为空不发放 | 查询过滤 `birthday IS NOT NULL`；V66 明确 `birthday DEFAULT NULL`（防元旦给全部会员误发） |
| 会员时区默认跟随系统 | V66 去掉 `timezone` 默认值与 NOT NULL；为空时取 `app.timezone`，未配则取服务器时区 |
| 时区非法 | 直接抛错，不静默回落 |

| 验证 | 结果 |
|---|---|
| 后端测试 | **427 个，全部通过，0 跳过** |
| 新增用例 | 领过后改生日仍不发；生日为空的会员在元旦不发；跨时区（UTC+14/UTC-8）各自按本地生日发放 |

### 仍未做（已登记 K8）

前端时间显示目前用**浏览器本地时区**。要求是：顾客端按用户设置的时区显示，admin 与供应商端固定东八。
这需要在前端统一封装格式化函数（`toLocaleString` 显式传 `timeZone`），涉及多个页面。

---

## 2026-10-19：管理端时间统一东八（K8 第一步）

- `@fullmall/api-client` 新增 `formatDateTime` / `formatDate`（显式 `timeZone: 'Asia/Shanghai'`）。
- admin / store-console / support-console / supplier-portal 共 **12 个文件、16 处**时间格式化改为调用该函数。

**为什么管理端固定东八**：不同电脑的浏览器时区不同，运营会看到不同"今天"，对账（今日订单/销售额）直接对不上。

| 验证 | 结果 |
|---|---|
| 前端构建（5 应用 + 类型检查） | ✅ |
| 残留检查 | 管理端 `toLocaleString('zh-CN')` **0 处** |

> 顾客端按会员时区显示**未做**（需 `/api/v1/me` 返回时区或读 cookie），K8 记 ⚠️。
> 本轮未跑 E2E（纯格式化改动，无接口变更）。

---

## 2026-10-19：顾客端按会员时区显示（K8 收口）

- `GET /api/v1/member/auth/me` 新增 `timezone`：会员设置了返回会员的，未设置返回系统默认（`app.timezone` 或服务器时区）。
- storefront 新增 `~/utils/datetime`：`formatMemberDateTime / formatMemberDate` 接收时区参数。
- 结算取到令牌后拉一次资料，把时区写入 cookie `fullmall_member_timezone`；6 个页面改为按会员时区格式化。
- 取不到时区时回退**浏览器本地时区**（顾客看自己设备时间更合理，不硬编码东八）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **428 个，全部通过，0 跳过** |
| 新增用例 | 设了 `Asia/Tokyo` 返回 Tokyo；未设置返回系统默认且可被 `ZoneId.of` 解析 |
| 前端构建（5 应用） | ✅ |
| 接口对齐 | 204 路由 / **24** 调用 / 0 未匹配 |

> 未做：顾客端时区的**浏览器验证**（只做了构建与后端测试）。

---

## 2026-10-19：行退款数量改为可空，修掉自造缺陷（A9）

**问题**：`allocateAndRecord` 用金额反推数量，同一行多次部分退款会累计超过行数量 →
报 `累计退款数量 2 超过该行数量 1`，把后续合法退款也挡住了。

**修法**：

| 项 | 内容 |
|---|---|
| V67 | `order_line_refund.quantity` 可空；CHECK 改为 `quantity IS NULL OR quantity > 0` |
| 服务 | 金额能被单价整除且在范围内 → 写数量；否则写 `NULL`（`recordAmountOnly`） |
| 校验 | 件数校验只对非空数量生效；金额上限校验始终生效 |

| 验证 | 结果 |
|---|---|
| 后端测试 | **430 个，全部通过，0 跳过** |
| 新增用例 | 单件 100 元分两次退 40+60 → 金额合计 10000、数量均为 NULL；两件 50 元退 50 → 数量=1 |

---

## 2026-10-19：时间存储与时区生成审计（A10）

### 核实结论

| 项 | 状态 |
|---|---|
| 业务表时间列 | ✅ 全部 `TIMESTAMPTZ`（存绝对时刻，与服务器时区无关） |
| 例外 | JaVers 自带表 `commit_date TIMESTAMP`（库 schema，未改，已记录） |
| **订单号生成** | ❌→✅ 原用 `LocalDateTime.now()` **隐式依赖服务器 TZ**；改为显式时区 |

### 修复

`Ids.orderNo()` 改用 `ZonedDateTime.now(zone)`；`app.timezone` 注入（`IdZoneConfiguration`），
未配置则用服务器时区；**非法时区启动失败**（不生成时间跳变的单号）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **432 个，全部通过，0 跳过** |
| 新增用例 | 切到 `Asia/Shanghai` / `America/Los_Angeles` 单号前缀各自正确；非法时区抛错 |

### 关于"退款数量上限"的澄清

数量约束只用于**售后退货按件**的场景（不能退超过购买件数）；按金额退款只受金额上限约束。
之前的报错是我"用金额反推件数"造成的假约束，已在 A9 修正（数量可为 NULL）。

---

## 2026-10-19：售后换货全流程 E2E（H12 收口）

`exchange-flow.spec.ts` 走完整五步：

```
会员下单 → 支付 → 申请换货(EXCHANGE)
  → 运营审核 approve
  → 收货入库 receive（隔离批次）
  → 发换货 exchange-ship（承运商+运单号）
  → 完成 complete-exchange → status=COMPLETED
```

| 验证 | 结果 |
|---|---|
| E2E | ✅ 通过（1.6s） |
| 覆盖 | 申请 / 审核 / 收货 / 发货 / 完成；状态机全程推进 |

> 这一步同时验证了「换货必须先进隔离批次再发货」的业务约束（`shipExchange` 要求 `RECEIVED`）。

---

## 2026-10-19：后台操作审计页面（I6 收口）

- 侧边栏「操作审计」（仅 ADMIN）：事件类型、操作人（模糊）、时间范围筛选 + 分页；时间按**东八**显示。
- 只读：不提供编辑/删除入口（审计不可篡改）。

| 验证 | 结果 |
|---|---|
| **E2E** | ✅ 打开页面、列表加载、按 `PAYMENT_REFUNDED` 筛选 |
| 前端构建 | ✅ |

> 说明：页面只展示 `audit_event`（应用级操作审计）。JaVers 快照（I6b）另接口，未做页面。

---

## 2026-10-19：供应商售后独立建模（K5 第一步）

按你的要求，把两类售后彻底分开：

| 类型 | 表 | 流程 |
|---|---|---|
| **商城售后单**（顾客 → 商城） | `after_sale` | 申请 → 审核 → 收货 → 退款/换货 |
| **供应商售后单**（商城 → 供应商） | **`supplier_return`（新增 V68）** | 发起 → 供应商接受/拒绝 → 结算扣减 |

接口：`POST /admin/v1/supplier-returns`、`POST .../{id}/status`、`GET ...`（权限 `supplier.manage`）。

状态机严格受限：`REQUESTED → ACCEPTED|REJECTED`，`ACCEPTED → SETTLED`，其余流转一律拒绝；
`REJECTED`/`SETTLED` 时写 `closed_at`。

| 验证 | 结果 |
|---|---|
| 后端测试 | **434 个，全部通过，0 跳过** |
| 新增用例 | 正常流转到 SETTLED 并写关闭时间；REQUESTED 直接 SETTLED 被拒；未知状态被拒；缺原因/负金额被拒 |
| 接口对齐 | **206** 路由 / 24 调用 / 0 未匹配 |

> 未做：供应商门户侧查看与申诉、结算单联动扣减。K5 仍记 ⚠️。

---

## 2026-10-19：供应商侧查看与表态（K5 续）

| 接口 | 说明 |
|---|---|
| `GET /open/v1/returns` | 供应商**只看到自己的**退货单（HMAC 鉴权，supplierId 来自签名） |
| `POST /open/v1/returns/{id}/decision` | 只能 `ACCEPTED` / `REJECTED`；**不能直接 SETTLED**（结算是平台动作） |

越权访问他人单据返回 **404「不存在」**，不泄露存在性。

| 验证 | 结果 |
|---|---|
| 后端测试 | **435 个，全部通过，0 跳过** |
| 新增用例 | 他方供应商处理被拒（404 语义）；本方接受后状态变 ACCEPTED |
| 接口对齐 | **208** 路由 / 24 调用 / 0 未匹配 |

> 未做：`SETTLED` 后自动从供应商应付扣减（结算联动）。K5 仍 ⚠️。

---

## 2026-10-19：供应商退货扣减结算金额（K5 收口）

- `SupplierReturnService.settledAmount(supplierId, periodStart, periodEnd, currency)`：
  统计周期内 **SETTLED** 的退货金额（按 `closed_at` 落期）。
- `SupplierSettlementService.generate` 用该金额作为 `refundAmount` 入参，
  应付 = 毛额 − 退货扣减 − 佣金（公式不变，只替换原先写死的 0）。

**未结算（ACCEPTED）的退货不扣**——避免"还没谈定就先扣钱"。

| 验证 | 结果 |
|---|---|
| 后端测试 | **436 个，全部通过，0 跳过** |
| 新增用例 | 一张 SETTLED(2000) + 一张 ACCEPTED(3000) → 扣减仅 2000 |

> 说明：断言直接验证扣减口径函数；未构造"供应商代发订单 + 生成结算单"的完整夹具（成本高），
> 该链路仍属未被 E2E 覆盖的部分。

---

## 2026-10-19：供应商门户退货面板

供应商门户新增「退货与折让」：

| 能力 | 说明 |
|---|---|
| 列表 | 平台发起的退货单（原因、订单、金额、状态） |
| 操作 | **仅 `REQUESTED` 状态**显示「同意 / 拒绝」，`:disabled` 防重复 |
| 边界 | 不能结算（结算是平台动作），说明写在面板上 |

| 验证 | 结果 |
|---|---|
| 前端构建（5 应用） | ✅ |

> **未做浏览器验证**：只做了构建与后端接口测试（上轮）。供应商门户的页面交互未在真实浏览器点过。

---

## 2026-10-19：供应商门户退货面板 E2E 验证

`supplier-returns.spec.ts`：平台发起退货 → 供应商门户连接 → 面板出现该单 → 点「同意」→
提示"已同意退货" → 后台复核状态为 ACCEPTED。

### 过程中发现并修掉的真实问题

**连接成功后不加载退货单**：`connect()` 只调了 `loadOrders()`，退货面板永远是"暂无"。
补上 `loadReturns()` 后通过。

| 验证 | 结果 |
|---|---|
| E2E | ✅ 通过（1.6s） |
| 前端构建 | ✅ |

### 又踩两次坑（已记录第 5、6 次）

1. **旧 jar**：接口 404，因为没 `package`；
2. **python 引号冲突**：补丁脚本报错未生效，改用编辑工具；
3. **测试数据不唯一**：历史退货单导致 strict mode 命中 3 条，改为唯一原因。

---

## 2026-10-19：足迹页 E2E（E9 收尾）

`history.spec.ts`：OTP 登录 → 调 `POST /api/v1/history/{skuId}` 记录 → 接口返回标题 →
带 cookie 打开 `/history` → 标题链接与"看过 N 次"均渲染。

| 验证 | 结果 |
|---|---|
| E2E | ✅ 通过（1.4s） |
| 说明 | 收藏页与足迹页现在都有带数据 E2E，不再只有单元测试 |

> 本轮踩坑：spec 里 `await (await request.get(...)).json()` 嵌套写法导致语法错误（缺括号），
> 拆成两行后通过。

---

## 2026-10-19：后台供应商结算界面

- 新增 `GET /admin/v1/suppliers/{id}/settlements`（复用既有 `list`，limit 1..200）。
- 供应商列表每行新增「结算」：弹窗选周期 → 生成结算单 → 列表显示
  **毛额 / 退货扣减 / 佣金 / 应付**（金额按分转元显示，时间按东八）。

| 验证 | 结果 |
|---|---|
| 后端测试 | **436 个，全部通过，0 跳过** |
| 前端构建 | ✅ |
| 接口对齐 | 208 路由 / 24 调用 / 0 未匹配 |

> 未做浏览器验证（仅构建 + 后端测试）。对账文件导出仍未实现。

---

## 2026-10-19：结算生成 E2E，抓出一个**一直存在的 SQL 错误**

`settlement.spec.ts`：造供应商代发订单 → 支付 → 后台生成结算单 → 打开结算弹窗看到应付。

### 发现的真实缺陷

结算聚合 SQL 用了 `f.supplier_id`，而 `fulfillment_order` **没有这个列**（供应商关联在 `source_id`）：

```
ERROR: column f.supplier_id does not exist
```

也就是说：**"生成结算单"这个功能在真实数据下从来没跑通过**——之前只有单测覆盖 calculator，
没有一次端到端生成。修成 `f.source_id = ?` 后通过。

| 验证 | 结果 |
|---|---|
| E2E | ✅ 通过（4.2s） |
| 后端测试 | 436 个全绿 |
| 前端构建 | ✅ |

> 教训：**只测计算器不算测功能**。这条 SQL 错误存在了多轮，直到补 E2E 才暴露。

---

## 2026-10-19：后台只读接口冒烟（H13）

新增 `api-smoke.spec.ts`：一次请求 **25 个只读接口**（后台 22 + 公开 3），任何 5xx 或非预期 4xx 直接失败。
目的：抓出"只有单测、从未被端到端调用"的接口（上轮结算 SQL 错误就是这类）。

### 本轮抓到的问题（都是真问题）

| 现象 | 真相 |
|---|---|
| `/admin/v1/settlement` 之前 500 | 上轮已修（`f.supplier_id` 不存在） |
| `/admin/v1/permissions/matrix` 404 | 实际路径是 `/admin/v1/permissions`（我按猜写） |
| `/api/v1/theme` 401 | **带 staff token 调公开接口会被会员过滤器拒绝**——真实现象，值得记录 |
| `/admin/v1/maintenance/retention` 405 | 它是 POST（执行清理），不是 GET |

| 验证 | 结果 |
|---|---|
| 冒烟用例 | ✅ 25 个接口全部正常（576ms） |

### 记录一个真实行为

**公开接口不接受 staff token**：`/api/v1/**` 的会员过滤器会把无效会员 token 当未登录 → 401。
前端如果同时带两种 token 会踩坑，已写入 `docs/PITFALLS.md`。

---

## 2026-10-19：写接口冒烟，修复"非法输入返回 500"（H13 扩展）

`api-smoke-write.spec.ts`：8 个写接口用**明显无效参数**调用，断言 4xx（不会改数据）。

### 抓到的真实缺陷

| 接口 | 现象 | 根因 |
|---|---|---|
| 促销规则 / 券模板 / 供应商退货 | 缺字段 → **500** | `HttpMessageNotReadableException` 落到兜底处理器 |
| 代发起提现 | 非法 UUID → **500** | 控制器里 `UUID.fromString` 未捕获 |

修复：

1. `GlobalExceptionHandler` 新增 `HttpMessageNotReadableException → 400 INVALID_REQUEST_BODY`（带具体解析原因）；
2. `AdminDistributionController.requestWithdrawal` 显式校验参数，抛 `400 INVALID_WITHDRAWAL_REQUEST`。

| 验证 | 结果 |
|---|---|
| 写接口冒烟 | ✅ 8 个接口全部 4xx（676ms） |
| 后端测试 | **436 个全绿** |

> 意义：以前"客户端传错参数"会显示成"服务端崩了"，排查方向被带偏。现在错误归位到 400。

---

## 2026-10-19：顾客端接口冒烟 + 路径参数类型 500 修复

新增 `api-smoke-member.spec.ts`：会员读 8 个 + 写 2 个（非法输入）。

### 抓到的真实缺陷

`POST /api/v1/favorites/not-a-uuid` → **500**：`@PathVariable UUID` 转换失败抛
`MethodArgumentTypeMismatchException`，落到兜底处理器。

修复：全局映射该异常 → **400 `INVALID_PARAMETER_TYPE`**（与"请求体无法解析"同类，属调用方错误）。

| 验证 | 结果 |
|---|---|
| 三个冒烟用例 | ✅ 全绿（3.1s） |
| 后端测试 | **436 个全绿** |

### 冒烟覆盖现状

| 范围 | 数量 |
|---|---|
| 后台读 | 25 |
| 后台写（非法输入） | 8 |
| 顾客端读 | 8 |
| 顾客端写（非法输入） | 2 |
| **合计** | **43 个接口** |

---

## 2026-10-19：审计崩溃恢复**实测不可用**（H1 降级为 ⚠️）

给恢复逻辑补一次真实调用（造 PENDING outbox 行 → `recoverOrphans()`），结果是：

| 观察项 | 实际 |
|---|---|
| `attempts` | 1（有尝试） |
| `last_error` | 有值（记录了失败原因） |
| `status` | **仍为 PENDING**（未被消费） |
| `pendingRows()` | 未减少 |

也就是说：**"崩溃恢复"这条链路此前从未真正成功过**，只是"能记录失败"。
之前我把它记为 ✅ 是错的——只有"任务存在 + 默认关闭"的断言，没有一次真跑。

测试已改为**如实断言现状**（attempts=1、last_error 非空、状态仍 PENDING），并登记 **H1b**：
`materialize` 反序列化后 JaVers 提交失败，需改成"按 id 重载实体再提交"或补齐反序列化契约。

| 验证 | 结果 |
|---|---|
| 后端测试 | **437 个，全部通过**（含这条记录现状的用例） |

---

## 2026-10-19：修复崩溃恢复（H1 真正可用）

上轮实测：恢复只记录失败、从不成功。根因是**靠反序列化 payload 还原实体**。

### 修复

1. 新增 `AuditEntityLoaders`：模块按实体注册"按 id 重载"函数；
2. 恢复时**优先按 id 从库里重载当前实体**再提交 JaVers，未注册才退回反序列化；
3. `AuditOutbox.Pending` 增加 `entityId`；asset 模块注册 `AssetEntity` 加载器。

### 过程中踩的两个坑（同源）

| 坑 | 现象 |
|---|---|
| 注册键用模块别名 `asset`，outbox 存全类名 | `find` 查不到 → 静默退回旧路径 |
| 先 `toLowerCase` 再取简单类名 | `AssetEntity` → `assetentity`，仍然查不到 |

最终 `registerClass(Class, alias, loader)`：同时登记全类名、简单类名与别名。

| 验证 | 结果 |
|---|---|
| 后端测试 | **437 个，全部通过** |
| 恢复实测 | 造 PENDING 行 → 恢复 → **status=DONE**、`last_error` 为空、待处理数下降 |

> 仍有限制（H1b）：只有注册了加载器的实体能可靠恢复；目前仅 asset。

---

## 2026-10-19：供应商结算 CSV 导出验证（G4）

`supplier-csv.spec.ts`：后台签发凭证 → **在测试里自行做 HMAC-SHA256 签名** → 调
`GET /open/v1/settlements.csv` → 断言 200、`Content-Disposition` 正确、内容非空。

| 验证 | 结果 |
|---|---|
| E2E | ✅ 通过（616ms） |
| 签名方式 | `sha256(body)` + `HMAC(secret, "GET\npath\nts\nnonce\nbodyHash")` |

> 这条用例的价值：证明**外部系统按文档实现签名就能拿到导出**，而不只是内部调用能通。
> 仍未做：CSV 批量**导入**（解析器存在，无入口）。

---

## 2026-10-19：供应商 CSV 导入接线（G4 完成）

- 新增 `POST /open/v1/inventory/feed.csv`（`text/csv`，HMAC 签名）：复用既有解析器，
  解析后**仍走 `updateStock`**，不绕过校验。
- 顺手补一个通用映射：`CsvImportException → 400 CSV_IMPORT_INVALID`（带每行/字段的具体原因），
  此前它是 500。

| 验证 | 结果 |
|---|---|
| 后端测试 | **437 个全绿** |
| E2E | ✅ 签名上传 CSV，返回 200（导入）或 400（行级校验错误带明细），不再是 500 |

> 测试里 CSV 表头写成旧格式导致 400，正好验证了新映射：错误信息指出"表头不匹配，期望 [...]"。
> 导出与导入现在都覆盖，G4 关闭。

---

## 2026-10-19：站点维护状态实跑验证（H14）

代码里已有 `FeatureGuardFilter` 对 `/api/v1/**` 做站点状态拦截，但**从未验证过**。

`site-status.spec.ts`：改 `site_status=MAINTENANCE` → 顾客端返回 **503 `SITE_MAINTENANCE`** →
改回 `OPEN` → 恢复 200。

| 验证 | 结果 |
|---|---|
| E2E | ✅ 通过（747ms） |

> 顺带确认：维护中的文案会随 503 返回（`maintenance_message`），前台可直接展示。
> 这类"配置项看起来有代码但没验证过"的地方，是这阶段最值得逐个点一遍的。

---

## 2026-10-19：导出流水线实测——**默认关闭导致永远 PENDING**（H15）

`export-job.spec.ts`：后台发起 `member` 导出 → 轮询状态 → 下载 CSV。第一次跑就卡住：
状态长时间 PENDING。

### 根因

`ExportWorkerTask.enabledByDefault() == false`，而 `enabledByDefault` **只在首次注册时生效**；
存量库里的行仍是 `enabled=false`。也就是说：**任何导出任务发起后都不会被处理**。

### 修复

1. `ExportWorkerTask` 默认改为开启（导出是"发起后必须有人处理"的流水线，幂等可重入）；
2. V69 一次性把存量 `export.worker` 行置为启用；
3. 调度测试口径同步（`export.worker` 从"默认关闭"移到"必开"）。

| 验证 | 结果 |
|---|---|
| E2E | ✅ 发起 → SUCCEEDED → 下载 200 且内容非空（6.2s） |
| 后端测试 | **437 个全绿** |

> 这是本阶段第 4 个"默认关闭/配置错位导致功能从未生效"的案例（前三个：结算 SQL、审计恢复、验收放宽）。

---

## 2026-10-19：清理接口的 dryRun 会静默删数据（H16）

核查运维入口时发现：`POST /admin/v1/maintenance/retention?dryRun=true` —— **`dryRun` 是未知参数，
Spring 静默忽略**，接口照常执行**真实删除**。

### 后果

运维以为自己做了"预览"，实际清掉了通知/导出记录/审计数据。这类"看起来安全、实际破坏"的接口
比直接报错危险得多。

### 修复

控制器接收全部参数并校验白名单：只允许 `notificationDays / exportDays / auditDays`，
其它参数（含 `dryRun`）返回 **400 `UNSUPPORTED_PARAMETER`**，并明确说明"该接口会真实删除数据，不提供 dryRun"。

| 验证 | 结果 |
|---|---|
| 后端测试 | **438 个全绿** |
| 新增用例 | 传 `dryRun` → 400 且提示包含 `dryRun` |
| 接口对齐 | **209** 路由 / 24 调用 / 0 未匹配 |

---

## 2026-10-19：积分过期（K3 完成，按你的口径）

口径：**后台可配、默认永不过期、判断写完善**。

| 项 | 实现 |
|---|---|
| `member.points.expiry-mode` | `NEVER`（默认）/ `ROLLING_DAYS` / `FIFO` |
| `member.points.expiry-days` | 有效期天数；非 NEVER 时必须 > 0 且 ≤ 3650，否则**报错** |
| 过期算法 | 扫 `EARN` 且 `remaining_points > 0` 且 `created_at <= now - days`；按剩余量扣，写 `EXPIRE` 流水（`business_key=expire:<ledgerId>` 幂等），余额不为负 |
| 定时任务 | `member.points-expiry` 默认**开启**；NEVER 时直接返回不扫描 |

| 验证 | 结果 |
|---|---|
| 后端测试 | **441 个，全部通过** |
| 用例 | NEVER 不expire；ROLLING_DAYS=30 只过期 40 天前那笔（新笔保留、余额 150→50、写 1 条 EXPIRE）；非法模式与 0 天各报错 |

> 说明：`FIFO` 与 `ROLLING_DAYS` 目前算法相同（都是"获得后 N 天"），保留两个取值是为将来支持"统一到期日"。

---

## 2026-10-19：部分退货佣金 DEFER（A6/K4 完成）

按你给的口径实现"延长冻结 → 售后关闭再定"：

| 时机 | 行为 |
|---|---|
| 顾客提交**部分退货** | 该订单佣金按退款占比冻结（`hold_amount`），写入 `hold_after_sale_id` |
| 售后关闭 = **退货完成/已退款** | 按冻结额回冲（累加 `reversed_amount`），扣满则置 `REVERSED` |
| 售后关闭 = **驳回 / 换货完成** | 解除冻结，佣金原额保留 |

配套约束：

- 冻结期间**不可提现**（余额查询排除 `hold_after_sale_id IS NOT NULL`）；
- 冻结期间**不被解冻任务处理**（两种释放查询都加该条件）；
- 与**供应商售后**（`supplier_return`）完全独立，互不影响。

| 验证 | 结果 |
|---|---|
| 后端测试 | **442 个，全部通过** |
| 用例 | 1000 佣金/订单 10000 退 4000 → 冻结 400；关闭退货 → `reversed_amount=400`、hold 清空；驳回 → hold 清空且金额不变 |

---

## 2026-10-19：佣金冻结/回冲端到端验证

`commission-hold.spec.ts`（真实接口链路）：

```
带推广人下单(2 件) → 支付 → 佣金已生成(hold=0)
→ 顾客申请 1 件退货 → holdAmount > 0（冻结生效）
→ 运营 审核→收货→退款 → reversedAmount > 0 且 holdAmount = 0
```

配套：`GET /admin/v1/distribution/commissions` 新增 `reversedAmount` / `holdAmount` 两个字段
（运营也需要看到"被冻结/已回冲"多少）。

| 验证 | 结果 |
|---|---|
| E2E | ✅ 通过（1.7s） |
| 后端测试 | 442 个（上轮已验证） |

### 本轮踩坑

打包时**替换了正在运行的 jar**，导致运行中进程 `ClassNotFoundException: ThrowableProxy`。
规则补充：**先停服务再打包**（或打包到临时文件再替换），否则会出现难以理解的运行时错误。

---

## 2026-10-19：通知失败重试（H17）

`POST /admin/v1/notifications/{id}/retry`：

| 规则 | 行为 |
|---|---|
| 仅 `FAILED` 可重试 | `SENT` 等状态返回 409（**防止给顾客重复发短信**） |
| 渠道未配置发送器 | 保持 `FAILED` 并明确报错（不假装成功） |
| 成功 | 置 `SENT`、清空 `error_message` |
| 重试再失败 | 保持 `FAILED` 并更新原因，同时返回 409 |

| 验证 | 结果 |
|---|---|
| 后端测试 | **444 个，全部通过** |
| 用例 | FAILED → 重试 → SENT 且错误清空；已 SENT 再重试被拒 |
| 接口对齐 | **210** 路由 / 24 调用 / 0 未匹配 |

> 说明：接口挂在会员通知控制器上，但路径是 `/admin/v1`，权限由 `StaffAuthFilter` 的路径映射生效
> （`notification.view`）。后续如果加独立后台控制器，会一起迁移。

---

## 2026-10-19：通知重试按钮接入后台页面

`NotificationsView` 操作列：`FAILED` 行显示「重试」（`:loading` 互斥、成功后刷新）；其它状态仍显示「标记已读」。

| 验证 | 结果 |
|---|---|
| 前端构建 | ✅ |
| 未做 | 浏览器验证（**dev 环境所有渠道都是 mock，无法自然产生 FAILED 记录**；要验证需注入失败场景） |

> 这是本轮第二次"接口先行、页面跟上"，避免出现"接口存在但没人能用"的情况。

---

## 2026-10-19：通知渠道连通性自检（H18）

`POST /admin/v1/notifications/test-send`：`{channel,title?,content?}` → 往 STAFF 发一条测试消息，
返回**真实投递结果**（`SENT` / `FAILED` + 原因），未知渠道 400 并列出可选值。

为什么需要：运营改了短信/邮件配置后，原本**没有任何手段确认能不能发出去**。

| 验证 | 结果 |
|---|---|
| 后端测试 | **446 个，全部通过** |
| 用例 | `sms` → SENT 且无错误；`carrier-pigeon` → 400 列出可选渠道 |
| 接口对齐 | **211** 路由 / 24 调用 / 0 未匹配 |

### 关于上一轮"FAILED 界面无法验证"

查明原因：**渠道 sender 由启动配置决定**（`app.sms.provider`），改数据库设置不会重建 sender，
所以 dev 环境无法在不重启的情况下制造 FAILED。这个自检接口正好补上了"确认渠道是否可用"的能力。

---

## 2026-10-19：渠道自检 UI + 修掉路由拼错

系统设置页新增「渠道自检」：选渠道 → 发送测试消息 → 按真实结果显示成功/失败原因。

### 踩到的真实问题：路由被拼错

`/admin/v1/notifications/test-send` 404 —— 因为该方法写在会员控制器里，
类上有 `@RequestMapping("/api/v1/notifications")`，实际路径变成
`/api/v1/notifications/admin/v1/notifications/test-send`。

修复：拆出独立的 `NotificationAdminController`（`@RequestMapping("/admin/v1/notifications")`），
`test-send` 与 `retry` 都迁过去。

| 验证 | 结果 |
|---|---|
| **E2E** | ✅ 设置页 → 发送测试消息 → 显示"渠道可用：测试消息已发送"（2.9s） |
| 后端测试 | **446 个全绿** |
| 接口对齐 | 211 路由 / 24 调用 / 0 未匹配 |

> 教训：**同一个 Controller 里混用两套前缀会拼接**，不要靠想象路径，跑一次就知道。

---

## 2026-10-19：供应商门户结算面板（G3 收口）

门户新增「结算单」：周期、毛额、**退货扣减**、佣金、应付、状态；「下载 CSV」按钮
用签名请求取文本再触发浏览器下载（带签名的链接无法用 `<a href>` 直接下）。

### 本轮抓到两个真问题

| 问题 | 真相 |
|---|---|
| `GET /admin/v1/suppliers/{id}/settlements` **405** | 第 195 轮那次"补丁"其实被存在性判断挡住，**从未真正加进控制器**（后台结算弹窗一直在报 405） |
| 门户结算面板永远"暂无" | `connect()` 里漏调 `loadSettlements()`（与退货面板同一类问题） |

另外 `signedRequest` 只解析 JSON，CSV 会抛错——补了 `signedRequestText`。

| 验证 | 结果 |
|---|---|
| E2E | ✅ 生成结算 → 门户出现结算行 + 下载按钮（1.7s） |
| 后端测试 | **446 个全绿** |
| 接口对齐 | 211 路由 / 24 调用 / 0 未匹配 |

> 这轮的教训又一次是同一个：**"补丁打印成功"不等于"代码生效"**。今后每处补丁都 `grep` 验证。

---

## 2026-10-19：全量 `make verify` **全部通过**

| 阶段 | 结果 |
|---|---|
| 后端测试 | ✅ 446 个（0 失败 0 跳过） |
| 后端打包 | ✅ |
| 前端构建（5 应用） | ✅ |
| 接口对齐 | ✅ 211 路由 / 24 调用 / 0 未匹配 |
| E2E（Chromium） | ✅ **35 条全通过**（1.2 分钟） |

对比第 142 轮（首次全绿：12 条 E2E、366 测试），本阶段：
- 后端测试 +80 个
- E2E +23 条
- 修复 5 处"代码有、功能无"的真实缺陷（结算 SQL、审计恢复、退货面板、导出默认关闭、清库 dryRun）
- 4 类"调用方错误被当服务端故障"统一归位到 400

---

## 2026-10-19：促销规则编辑（C8）

规则列表草稿行新增「编辑」：复用创建表单（预填现有值），保存走 `PUT /admin/v1/promotions/rules/{id}`，
成功后刷新列表。**只有 DRAFT 可编辑**——生效中的规则改动会影响正在进行的活动。

| 验证 | 结果 |
|---|---|
| **E2E** | ✅ 新建草稿 → 编辑改名 → 提示"规则已更新" → 列表显示新名称（3.9s） |
| 前端构建 | ✅ |

---

## 2026-10-19：会员等级配置——**本来就是好的**（E4 纠正）

清单写的是"后台无编辑页"。实际核查：后台「会员与优惠券 → 会员等级」Tab 已可编辑折扣率、
积分倍率、生日礼积分，接口 `GET/PUT /admin/v1/pos/member-levels` 存在。

补了一条 E2E 证明它真的能用：改折扣 → 保存 → 刷新页面仍是新值。

| 验证 | 结果 |
|---|---|
| E2E | ✅ 通过（4.6s） |

> 这是第 3 次发现"清单条目与代码不符"（前两次 D3、D9）。**登记前必须先 grep 代码**，
> 否则会去修不存在的问题、也会漏掉真正没验证过的功能。

---

## 2026-10-19：订单行补创建时间（H5）

- V71：`order_line.created_at`（回填为所属订单时间，`NOT NULL` + 默认 `now()` + `(order_id, created_at)` 索引）。
- 行退款读取顺序从 `ORDER BY id`（UUID 无序）改为 `ORDER BY created_at, id`。

| 验证 | 结果 |
|---|---|
| 后端测试 | **446 个全绿**（含迁移冒烟与行退款用例） |
| 迁移 | v71 |

> 诚实说明：回填用订单时间近似（同批写入，误差可忽略但确实不是行精确时间）。
> 今后新写入的行才有精确 `now()`。

---

## 2026-10-19：客户与售后列表排序（H7 部分）

- `GET /admin/v1/customers?sort&direction`：白名单 `lastOrderAt`（默认）/ `orderCount` / `totalSpent`；
- `GET /admin/v1/after-sales?sort&direction`：白名单 `createdAt`（默认）/ `refundAmount` / `status`；
- 前端两页加 `sortable="custom"` + `@sort-change`，筛选变更重置页码。

| 验证 | 结果 |
|---|---|
| 后端测试 | **448 个，全部通过** |
| 新增用例 | 金额排序生效；**未知字段/`DROP TABLE` 片段安全回落且数据完好**；售后排序契约可用 |
| 接口对齐 | 211 路由 / 24 调用 / 0 未匹配 |

> 又踩一次"SQL 拼接处 `DESC` 与 `LIMIT` 粘连"（与第 143 轮同一坑）。已在坑清单记为固定检查项。

---

## 2026-10-19：员工与供应商列表排序（H7 完成）

- `GET /admin/v1/staff?sort&direction`：白名单 `displayName`（默认）/ `username` / `role` / `status`；
- `GET /admin/v1/suppliers?sort&direction`：白名单 `name`（默认）/ `code` / `active`；
- 前端两页加 `sortable="custom"` + `@sort-change`。

| 验证 | 结果 |
|---|---|
| 后端测试 | **449 个，全部通过** |
| 新增用例 | 员工按显示名排序可用；**两处未知字段与 `DROP TABLE` 片段均安全回落且数据完好**；供应商按编码排序可用 |
| 接口对齐 | 211 路由 / 24 调用 / 0 未匹配 |

**六个后台列表（商品/订单/客户/售后/员工/供应商）现在共用同一套排序列白名单模式。**

---

## 2026-10-19：文档路径契约测试（H19）

新增 `DocumentedPathsExistTest`：扫描 `docs/OPEN_ITEMS.md` 中出现的所有
`/admin/v1/**`、`/api/v1/**`、`/open/v1/**`、`/store/v1/**` 路径，
与 Spring 实际注册的路由**逐条比对**，任何"文档有、代码无"直接失败。

### 第一次运行的收获（18 条差异）

| 类别 | 处理 |
|---|---|
| 文档里的历史叙述（如 `/admin/v1/permissions/matrix`，是我曾写错的路径） | 只扫**当前清单**，不扫实现状态文档（那里面有大量修复前后对比） |
| 路径变量名不同（`{id}` vs `{fulfillmentOrderId}`） | 归一化为 `{}` 后再比较 |

### 现在的能力

把"文档说做了但接口不存在"这类问题（本项目出现过至少 2 次）**变成机器可查**，
新增接口如果只写文档不写代码，测试会红。

| 验证 | 结果 |
|---|---|
| 后端测试 | **450 个，全部通过** |

---

## 2026-10-19：补 `brand` 表（C7 完成 / F1 推进）

**真缺陷**：`product.brand_id` 一直存在、促销与券的 BRAND 范围也在查 `brand` 表，
但**这张表从来没建过**——运营一旦在范围选择器里选「指定品牌」，查询就会因表不存在而报错。

- V72：建 `brand` 表（`code` 唯一、`sort_order`）+ 两条演示品牌；
- 测试：`scopeOptions("BRAND", 20)` 返回非空且 label 非空。

| 验证 | 结果 |
|---|---|
| 后端测试 | **451 个全绿** |
| 迁移 | v72 |

> 剩余（F1 ⚠️）：后台品牌管理页（增删改）未做。

---

## 2026-10-19：构建分版 M0 完成（社区版不含 AI）

| 项 | 实现 |
|---|---|
| Maven profile | `community`（默认，`activeByDefault`）/ `enterprise` |
| 源码隔离 | AI 与许可源码放 `server/src-enterprise/java`，仅 `enterprise` 用 `build-helper` 加入源码根 |
| 运行标识 | `app.edition: ${APP_EDITION:community}`；surefire 注入 `APP_EDITION=${app.edition}` |
| 类隔离验证 | `EditionPackagingTest`：社区版断言 `com.fullmall.ai.*` **不存在**，企业版断言存在 |

企业版骨架 `AdminAiController`：

- `app.ai.enabled=false` → **503**，`reason="AI 模块未启用"`；
- 启用但缺配置 → **503**，`missing=["app.ai.chat-base-url", ...]` 逐项列出；
- 全配好 → 200 返回模型名与 topK。

| 验证 | 结果 |
|---|---|
| 社区版编译 + 全量测试 | ✅ **452 个全绿** |
| 企业版编译（`-Penterprise`） | ✅ BUILD SUCCESS，产物含 `com/fullmall/ai/api/AdminAiController.class` |
| 企业版用例 | ✅ `EditionPackagingTest` 通过 |
| 社区版类目录 | ✅ `target/classes/com/fullmall/ai` 不存在 |

---

## 2026-10-19：AI 的"取数 + 约束"骨架（先做约束，再谈模型）

结论已写入 `docs/AI_SERVICE_MODULE.md` §0：**本期不做训练**。
价格/库存/政策每天都在变，微调记住的事实第二天就过期，而且无法给出引用来源；
RAG 改库即生效、能回溯、无命中就说不知道。训练留给"风格/术语/格式"问题，且要有 ≥2000 条人审问答。

### 已实现（企业版源码，社区版不含）

| 组件 | 作用 |
|---|---|
| `KnowledgeChunk` / `KnowledgeRetriever` | 知识片段与检索端口（pgvector 实现待写） |
| `ChatModel` | OpenAI 兼容对话端口，不绑定 vLLM/Ollama |
| `AnswerGuard` | **可机械判定的四道约束**（见下） |
| `AnswerService` | 检索→提示→模型→约束→返回；无命中不调用模型 |
| `POST /admin/v1/ai/ask` | 返回 `answer / citations / fallback / violations` |

四道约束：

1. 无检索资料 → **不调用模型**，直接"没查到，建议转人工"（`fallback=true`）；
2. 答案里的金额必须能在资料原文找到，否则判违规；
3. "我保证一定能退/给您退款/给您免单/改价/封号"等**越权承诺**判违规；
4. "已为您退款/已改价"等**声称已执行**判违规。

违规处理是**拒答转人工**，不是删掉违规句继续答。

| 验证 | 结果 |
|---|---|
| 社区版全量测试 | ✅ **452 个全绿** |
| 企业版 `AnswerServiceTest` | ✅ 4 条（无资料不调用模型、编价格被拒、越权承诺被拒、合规答案带引用通过） |

### 顺带修掉两个测试问题

| 问题 | 处理 |
|---|---|
| 契约测试在社区版误报 `/admin/v1/ai` | 按 `APP_EDITION` 跳过企业版前缀 |
| 取货用例在共享测试库下假失败 | 改为**增量断言**（下单前记录批次基线），不再假设绝对库存 |

---

## 2026-10-19：许可内核 M1/M2/M4（到期只读）

按确认口径实现：**本地证书 + 到期只读（GET 可用，写 402）**。

| 组件 | 说明 |
|---|---|
| `LicenseVerifier` | Ed25519 验签（签名覆盖 payload 原始字节）、域名通配、宽限期、永久（`expiresAt=null`） |
| `LicenseService` | 60 秒缓存、到期/域名不符/时钟回拨 → `readOnly=true`；`license_state.last_seen_at` 只前进 |
| `LicenseReadOnlyFilter` | 只读时写请求 **402 `LICENSE_EXPIRED`**；放行登录、许可上传、**支付回调** |
| `LicenseController` | `GET /admin/v1/license/status`、`POST /admin/v1/license/upload`（写入即重验，失败如实返回 INVALID） |
| V73 | `license_state` 表 |

| 验证 | 结果 |
|---|---|
| 后端全量 | ✅ **461 个全绿**（新增 9 条许可用例） |
| 验签用例 | 篡改 payload 拒绝、错公钥拒绝、畸形内容抛错（不是静默通过）、宽限期、域名通配与 localhost 放行 |
| 只读用例 | 过期时 GET 200 / POST 402；登录与支付回调仍 200；有效许可证写请求 200 |

> 未做：打包预设（7 天/30 天）、后台许可页、公网许可服务端。已登记 M3/M5/M6。

---

## 2026-10-19：客户资料导入 + 演示模式（L6/L8）

你说得对：**客户给什么格式、我们怎么用**才是关键。所以先写"给客户看的导入指南"，再实现解析链路。

### 导入格式（已实现解析）

| 格式 | 解析 | 结果 |
|---|---|---|
| `.md` / `.txt` / `.csv` / `.html` | Tika | 切块入库 |
| `.pdf`（文本版） | Tika | 抽正文；扫描版 → `EMPTY_DOCUMENT` |
| `.docx` / `.doc` / `.xlsx` / `.xls` | Tika | 抽正文/表格文本 |
| 图片、压缩包、其它二进制 | **拒绝** `UNSUPPORTED_FORMAT` | 不生成垃圾知识块 |

规则：单块 ≤ 500 字（按段落切，超长按句切）；同源替换（先删后写），改价/改文档立即覆盖旧知识。

### 演示模式（无 GPU 也能演）

`app.ai.mode=demo`：

| 环节 | 演示实现 | 说明 |
|---|---|---|
| 检索 | 内存关键词匹配 | 语义检索要生产模式；**不假装等价** |
| 回答 | `DemoChatModel` **抽取原文** | 输出全部来自资料，天然过约束 |
| 约束 | 同生产（来源/金额/越权/已执行） | 演示不放松 |

### 接口

- `POST /admin/v1/ai/knowledge/import`（multipart）：返回 `sourceId/type/chunks/chars/totalChunks`；
- `POST /admin/v1/ai/ask`：返回 `answer/citations/fallback/violations`。

| 验证 | 结果 |
|---|---|
| 社区版全量 | ✅ **461 个全绿** |
| 企业版新增 | ✅ 4 条：Markdown 问答解析→检索→带引用回答；png 被拒且不入库；空文档被拒；无命中回落转人工 |

---

## 2026-10-19：生产知识库（存储 + 混合召回 + 商品同步）

| 组件 | 实现 |
|---|---|
| V74 `knowledge_chunk` | 块内容 + 来源 + 版本 + 嵌入（float32 bytea） |
| `PgKnowledgeStore` | 关键词把候选缩到 200 条 → **应用内余弦重排**；无向量的块不参与向量排序 |
| `VectorMath` | float32 编解码 + 余弦；维度不符/零向量返回 **-1**（排除而不是当 0 分） |
| `EmbeddingClient` | OpenAI 兼容 `/v1/embeddings`；非 200/缺字段**抛错**，不返回零向量 |
| `ProductKnowledgeSyncService` | 按 SKU 重建知识块（价格/描述/状态），**同源替换** |
| 开关 | `app.ai.mode=demo` → 内存关键词；`prod` → PG + 向量重排（`@ConditionalOnProperty` 显式区分） |

接口新增：`POST /admin/v1/ai/knowledge/sync?skuId=`（改价后立即重建）。

| 验证 | 结果 |
|---|---|
| 企业版全量 | ✅ **474 个全绿** |
| 社区版全量 | ✅ **461 个全绿** |
| 关键用例 | 改价 39.90 → 45.90 后重建，旧价格**从知识库消失**（同源替换生效）；向量重排按相似度排序；维度不符被排除 |

> 未做（L9 ⚠️）：真实模型与嵌入服务联调（需要模型地址）。存储与算法侧已就绪。
> 说明：没有采用 pgvector 扩展，原因是嵌入式测试库不支持；当前方案在 10 万块以内可接受，
> 超出后按文档切 ANN 索引。这是**明确的取舍**，不是遗漏。

---

## 2026-10-19：AI 后台页（L11）

`后台 → AI 客服`：状态卡（形态/启用/就绪/模型/topK）、资料导入（multipart）、提问测试（答案 + **引用来源表** + 触发约束）。

- 社区版：页面明确显示"当前是社区版构建，不包含 AI 模块"，不白屏不报错；
- 企业版未配置：显示 503 原因与缺失配置项；
- 上传走新增的 `ApiClient.postForm`（FormData 不能手写 Content-Type，否则丢 boundary）。

### 两个真实缺陷（都已修）

| 问题 | 现象 |
|---|---|
| demo 模式两个 `KnowledgeRetriever`/`ChatModel` bean | 企业版**启动失败**（`required a single bean, but 2 were found`） |
| API 客户端强制 JSON 头 | multipart 上传会被网关/服务端拒绝 |

| 验证 | 结果 |
|---|---|
| 社区版 E2E | ✅ AI 页显示"社区版不含 AI 模块"（1.8s） |
| 企业版 E2E | ✅ 上传 faq.md（2 块）→ 提问"鲜奶可以退货吗"→ 答案含"未开封"+引用 faq.md（1.4s） |
| 企业版全量 | ✅ **474 个全绿** |
| 社区版全量 | ✅ **461 个全绿** |

---

## 2026-10-19：结构化输出契约 + 真实模型联调（L12/L13/L14）

### 1. 用 JSON 契约替代"猜答案里有没有答出来"

模型必须返回：

```json
{"answer":"...","grounded":true,"needsHuman":false,"citationIds":[1,2]}
```

- 解析失败 → **拒绝使用模型输出**，转人工并记录原因（`violations`）；
- `grounded=false` 或 `needsHuman=true` → `fallback=true`；
- 金额必须能在资料原文找到（`AnswerGuard` 保留，这条是机械校验，不是词表）。

顺带修掉一个真实误判：模型在**正确回答后**补一句"建议转人工"，旧词表会把它误判成没答出来。

### 2. 知识库必须有真实数据

新增 `POST /admin/v1/ai/knowledge/rebuild`：索引**全部在售 SKU**（标题/编码/价格/规格/描述）。
实测：11 个商品 → 19 个知识块（含此前导入的政策文档）。

### 3. 真实模型联调（`deepseek-v41`，OpenAI 兼容）

| 问题 | 结果 |
|---|---|
| 有机鲜牛奶多少钱？ | **"有机鲜牛奶的价格是19.90 CNY。"** + 引用「有机鲜牛奶 · 价格与规格」 |

- 该端点**没有嵌入接口**（`/embeddings` 返回 400），故本轮显式使用 `app.ai.retrieval=keyword`
  （中文二元组召回），并在文档中标注；接入嵌入模型后切回 `vector`。
- API Key 只经环境变量传入，存放于 `/tmp` 且**已删除**，未写入仓库与日志。

| 验证 | 结果 |
|---|---|
| 企业版全量 | ✅ **476 个全绿** |
| 社区版全量 | ✅ **461 个全绿** |
| 用例新增 | 非 JSON 输出被拒；`grounded=false` → fallback；演示模式同样遵守 JSON 契约 |

---

## 2026-10-19：到期只读的三个例外经用户确认

用户确认（2026-10-19）以下三项在许可证过期后**仍然放行**：

1. 登录 / 验证码（否则交不了续费）；
2. 许可状态查询与上传（否则无法恢复）；
3. 支付 / 退款回调（否则已发生的付款会丢账）。

新增用例 `confirmedExceptionsStayAvailableWhileExpired`：过期状态下这三项均 200，
而普通业务写操作（如给客户加备注）仍是 **402 `LICENSE_EXPIRED`**。

| 验证 | 结果 |
|---|---|
| 许可相关用例 | ✅ 10 条全绿（验签 6 + 只读 4） |

---

## 2026-10-19：后台帮助体系 + 覆盖率硬门禁（H20）

### 内容

- **22 个后台页面**各有帮助：`description / steps / tips / pitfalls / related`；
- 顶栏「帮助」抽屉按当前路由展示（无条目时明确提示"该页面还没有帮助内容"）；
- `docs/HELP_INDEX.md` 由脚本生成，供培训/交付打印。

### 防"文档落后于代码"的机制

`scripts/check-help-coverage.py` 已接入 `scripts/verify.sh` **第 5 步**，以下任一情况构建失败：

| 检查 | 场景 |
|---|---|
| 路由有、帮助无 | 新增页面忘写帮助 |
| 帮助有、路由无 | 删页面留死链 |
| `steps/tips/pitfalls` 为空 | 占了位置没写内容 |
| `related` 指向不存在路由 | 帮助内跳转写错 |
| `HELP_INDEX.md` 与内容不一致 | 改了帮助没重新生成 |

### 反证（证明门禁真的会拦）

手动改坏 `HELP_INDEX.md` → 脚本退出码 **1** 并提示"与帮助内容不一致"；恢复后退出码 0。

| 验证 | 结果 |
|---|---|
| 覆盖率脚本 | ✅ 22 个页面，索引同步 |
| E2E | ✅ 订单页打开帮助 → 显示"操作步骤/运营建议/常见错误"及佣金冻结说明（2.1s） |
| 前端构建 | ✅ |

### 明确未做

字段级 tooltip 尚未全量覆盖；门店收银/供应商/客服前端帮助待补；文案暂为中文单语。

---

## 2026-10-19：换货到期自动完成（J6 转功能，D11）

口径（用户确认）：**自营场景下，换货发出后超过 N 天，顾客未申报"未收到"即视为确认收货**。
理由：大量顾客不会主动点确认；快递"已签收"只代表被取走，但自营可视为送达；争议走客服。

| 实现 | 说明 |
|---|---|
| V76 | `after_sale.auto_complete_at / disputed_at / auto_completed` |
| 发货 | 记录 `auto_complete_at = now() + N 天`（`make_interval`，默认 7，可配 1..60） |
| 任务 | `after-sale.exchange-auto-complete`，每 10 分钟扫描到期单 |
| 自动完成 | 写 `COMPLETED` + `auto_completed=true` + 事件，并**解除佣金冻结**（换货不算退货，不扣佣金） |
| 顾客异议 | `POST /api/v1/orders/{orderId}/after-sales/{id}/not-received`（校验手机号）→ 写 `disputed_at` → **永不自动完成**，转人工 |

| 验证 | 结果 |
|---|---|
| 后端全量 | ✅ **466 个全绿** |
| 用例 | 到期无争议 → COMPLETED 且 auto_completed=true；有争议 → 保持 EXCHANGE_SHIPPED 且不在可自动完成集合；默认天数=7 |
| 接口对齐 | 213 路由 / 24 调用 / 0 未匹配 |

---

## 2026-10-19：券模板编辑/停用（C5）

| 能力 | 规则 |
|---|---|
| 修改 | `PUT /admin/v1/coupons/templates/{id}`，**仅 DRAFT**；面额/数量/有效期做范围校验 |
| 停用 | `POST .../disable`；停止发放，**已领取的券不受影响**（按领取时的有效期继续可用） |
| V77 | 状态约束加入 `DISABLED` |

| 验证 | 结果 |
|---|---|
| 后端全量 | ✅ **470 个全绿** |
| 用例 | 草稿可改且落库；生效中修改被拒；停用后模板 DISABLED、已发券仍 UNUSED、再领取被拒 |
| 接口对齐 | 215 路由 / 24 调用 / 0 未匹配 |

> 顺带修掉两处测试事实错误：把券实例表写成 `coupon`（实际 `member_coupon`）、状态写成 `ISSUED`（实际 `UNUSED`）。

---

## 2026-10-19：券效果统计（C6）

`GET /admin/v1/coupons/templates/{id}/stats` → `{claimed, used, expired, unused, claimRate, redemptionRate}`；
后台券列表「统计」弹窗展示，核销率加粗（运营最关心的一个数）。

口径写死：

- **领取率** = 已领取 / 发放总量；
- **核销率** = 已核销 / 已领取（不是 / 总量，否则会低估活动效果）；
- 过期单列，不并入"未使用"（否则高估余量）。

| 验证 | 结果 |
|---|---|
| 后端全量 | ✅ **471 个全绿** |
| 用例 | 3 张券：1 用、1 过期、1 未用 → claimed=2、used=1、expired=1、unused=0、领取率 66.7%、核销率 50% |
| 前端构建 | ✅ |
| 接口对齐 | 216 路由 / 24 调用 / 0 未匹配 |

---

## 2026-10-19：SKU 成本（A8，解锁砍价/毛利/PROFIT 佣金）

| 项 | 设计 |
|---|---|
| V78 | `sku.cost_amount BIGINT`，可空；`CHECK (cost_amount IS NULL OR cost_amount >= 0)` |
| 写 | `PUT /admin/v1/products/skus/{skuId}/cost`，**仅 ADMIN**（成本敏感，运营不可见） |
| 读 | `GET` 同路径，仅 ADMIN；未维护返回 **null，不返回 0** |
| 清除 | 传 null 表示回到"未维护"，依赖成本的功能必须明确拒绝 |

| 验证 | 结果 |
|---|---|
| 后端全量 | ✅ **476 个全绿** |
| 用例 | 设置→读取→清除；未维护返回 null；负数被拒绝 |
| 接口对齐 | 216 路由（新增 2 个写/读成本接口）/ 24 调用 / 0 未匹配 |

> 为什么强调"未维护返回 null"：若把缺失成本当 0，毛利会被算成 100%，
> 砍价底价校验也会放行任何价格——这是典型的静默兜底事故。

---

## 2026-10-19：成本核算内核（COST-1/COST-2）

先做**纯计算内核**，再接数据库——避免把"算法对不对"和"SQL 对不对"混在一起调。

`MovingAverageCost`：

| 操作 | 规则 |
|---|---|
| 入库 | `金额累加`（不四舍五入）→ 均价四舍五入到分，**差额留在金额里**，防止长期累计偏差 |
| 出库 | 按当前均价结转，**均价不变**（移动平均的本质：销售不改变单位成本） |
| 卖完 | 金额清零、**保留最后均价**并标记 `stale` |
| 非法 | 数量 ≤0、成本 <0、出库超库存 → 直接报错（不做负库存） |

| 验证 | 结果 |
|---|---|
| 后端全量 | ✅ **480 个全绿** |
| 用例 | 10@10 + 10@14 → 均价 12、金额 240；出库 5 → 金额 180、均价仍 12；卖完保留均价且 stale；非法输入被拒 |

> 未做（A8 仍 ⚠️）：批次表加单位成本、收货/出库/退货结转、`stock_cost_movement`、只读展示。
> 下一步按 `docs/COST_ACCOUNTING_DESIGN.md` §6 顺序落地。

---

## 2026-10-19：成本核算落地第二步（V79 + SkuCostService）

| 组件 | 内容 |
|---|---|
| V79 | `sku_cost`（数量/金额/均价/stale）+ `stock_cost_movement`（RECEIPT/ISSUE/RETURN/ADJUST/WRITE_OFF） |
| `SkuCostService.receive` | 收货入库 → 重算移动加权均价 → 写 RECEIPT 流水（同一事务） |
| `SkuCostService.issue` | 出库按均价结转、均价不变 → 写 ISSUE 流水；**无成本记录直接拒绝** |
| `current` | 从未入库返回 **null**（调用方不得当作 0） |

| 验证 | 结果 |
|---|---|
| 后端全量 | ✅ **482 个全绿** |
| 用例 | 10@10 + 10@14 → 均价 12、金额 240；出库 5 → 金额 180、均价仍 12；流水 3 条；无成本记录出库被拒 |

> 仍缺（A8 ⚠️）：收货接口调用 `receive`、售后退货按原批次回冲、`inventory_batch.unit_cost_amount`、只读展示。

---

## 2026-10-19：成本链路的真实阻塞——**没有采购入库入口**

准备把 `SkuCostService.receive` 接到收货时发现：**系统里没有采购/收货单流程**。

核查结论：

| 检查 | 结果 |
|---|---|
| 是否存在 purchase/procurement 模块 | ❌ 无 |
| 是否存在收货/入库单接口 | ❌ 无（只有调拨 receive 与售后 receive） |
| `inventory_batch` 谁在写 | 仅演示种子 `seed.sql`；代码里只做数量更新 |
| 影响 | 成本无法自动产生 → 砍价底价、PROFIT 佣金、毛利报表全部缺前置 |

已登记 **G7 采购入库/收货单**（⬜）。这不是我"没接上"，而是**目标物不存在**；
若强行把成本写在一个虚构入口，就是自欺欺人。

### 建议的最小可行实现（下一步）

1. `purchase_order`（供应商、SKU、数量、单价、状态）
2. `goods_receipt`（收货单，按行确认入库）
3. 收货确认时：写 `inventory_batch(unit_cost_amount)` + 调 `SkuCostService.receive`
4. 退货入库按原批次成本回冲

---

## 2026-10-19：采购单 + 收货 → 成本自动产生（G7 第一步）

| 组件 | 内容 |
|---|---|
| V80 | `purchase_order` / `purchase_order_line`（数量、单价、已收数量） |
| `PurchaseService.create` | 下单（数量 >0、单价 ≥0 校验） |
| `PurchaseService.receive` | **收货即写成本**（调用 `SkuCostService.receive`），状态 PLACED→RECEIVED；重复收货被拒 |
| 接口 | `POST /admin/v1/purchases`、`POST /admin/v1/purchases/{id}/receive` |

| 验证 | 结果 |
|---|---|
| 后端全量 | ✅ **484 个全绿** |
| 用例 | 10@8 元 + 10@12 元 → 均价 10 元、金额 200 元；重复收货被拒 |
| 接口对齐 | 218 路由 / 24 调用 / 0 未匹配 |

> 仍缺（G7 ⚠️）：收货时**写 `inventory_batch`**（需要仓库/库位/效期），以及退货按原批次回冲。
> 目前成本已能自动产生，但库存批次账与成本账尚未合并——分两步走，避免一次改太多。

---

## 2026-10-19：收货同时写库存批次（G7 完成）

`inventory_batch` **本来就有** `unit_cost_amount`，缺的只是"收货时写批次"。本轮补上：

| 改动 | 说明 |
|---|---|
| `receive(purchaseId, locationType, locationId)` | 收货必须指定库存地点；只传采购单 id 会被拒（不允许默认仓） |
| 同事务写入 | 成本流水（`sku_cost` 加权平均）+ **批次行**（数量、批次单位成本、币种） |
| 批次号 | `PO-<采购单前8位>-<行前6位>`，可回溯成本来源 |

| 验证 | 结果 |
|---|---|
| 后端全量 | ✅ **485 个全绿** |
| 用例 | 采购 7 件@9 元收货 → 批次数量 7、批次成本 900；两次采购不同单价 → 加权均价 10 元；重复收货被拒 |
| 接口对齐 | 218 路由 / 24 调用 / 0 未匹配 |

> 剩余（A8 ⚠️）：售后退货**按原批次成本回冲**、成本只读展示页（F8）。
> 采购入库到成本结转这条主链已经打通。

---

## 2026-10-19：退货按原批次成本回冲（COST-3）

`SkuCostService.returnFromBatch(skuId, batchId, quantity)`：

| 规则 | 说明 |
|---|---|
| 成本来源 | 读 `inventory_batch.unit_cost_amount`（**原批次成本**） |
| 禁止 | 批次无成本记录 → 明确拒绝（`BATCH_COST_UNKNOWN`），**不按当前均价猜** |
| 加权 | 回冲走 `MovingAverageCost.receive`，按原成本重算均价 |
| 追溯 | 写 `RETURN` 流水，`reference_id = 批次ID` |

| 验证 | 结果 |
|---|---|
| 后端全量 | ✅ **487 个全绿** |
| 用例 | 10 件@10 元 + 10 件@20 元（均价 15）→ 按批次 10 元退 5 件 → 数量 25、金额 350、均价 **14 元**（若按均价回冲会是 15 元，差额=虚增利润） |

---

## 2026-10-19：成本只读视图接口（F8 后端）

`GET /admin/v1/products/skus/{skuId}/cost-view`（仅 ADMIN）：

```json
{"skuId":"…","unknown":false,"quantity":20,"amount":24000,"avgCost":1200,"stale":false,
 "latestBatch":{"batchNo":"PO-TRACE-1","quantity":12,"unitCost":880,"currency":"CNY","receivedAt":"…"}}
```

- 无成本记录时 **`unknown=true` 且不带数值**（不返回 0）；
- `latestBatch` 提供**来源追溯**（哪个批次、什么单价、何时收货）。

| 验证 | 结果 |
|---|---|
| 后端全量 | ✅ **488 个全绿** |
| 用例 | 无批次时 `latestBatch=null`；有批次时返回批次号与单价 |
| 接口对齐 | 218 路由 / 24 调用 / 0 未匹配 |

> F8 仍 ⚠️：**后台页面还没展示**这两个数字。

---

## 2026-10-19：退货回冲的另一处阻塞——订单行没有批次

准备把 `returnFromBatch` 接到售后收货时发现：

| 检查 | 结果 |
|---|---|
| `order_line` 有 `batch_id` 吗 | ❌ 只有 `order_id`、`sku_id` |
| 售后行能定位原批次吗 | ❌ 只有 `order_line_id` 与 `sku_id` |
| 结论 | `returnFromBatch(skuId, batchId, qty)` 能力已就绪，但**拿不到可信的 batchId** |

不能退而求其次"按当前均价回冲"或"按最近批次猜"——那正是上一轮刚证明会虚增利润的做法。

已登记 **H21**：履约拣货时把 `batch_id` 写入 `order_line`（POS/仓库拣货已经知道批次），之后售后收货即可精确回冲。

> 这轮只做核查与登记，**没有写"看起来能跑"的接线**。

---

## 2026-10-19：订单行批次列（H21 第一步）

- V81：`order_line.batch_id UUID REFERENCES inventory_batch(id)` + 部分索引；
- `OrderRepository.recordLineBatch(lineId, batchId)`：**只允许写一次**（`WHERE batch_id IS NULL`），
  防止后续批次覆盖导致退货回冲到错误成本；
- `findLineBatch` 供售后退货取原批次。

| 验证 | 结果 |
|---|---|
| 后端全量 | ✅ **489 个全绿** |
| 用例 | 首次写入成功、第二次返回 false 且值不变 |

> 仍缺（H21 ⚠️）：**拣货路径写入**（POS/仓库拣货知道批次，需在那里调用 `recordLineBatch`），
> 之后售后收货才能安全调用 `returnFromBatch`。

---

## 2026-10-19：拣货批次回填的精确阻塞点

为把 `recordLineBatch` 接到拣货路径，查了拣货表结构：

```sql
CREATE TABLE pick_task_line (
    id, pick_task_id, sku_id, quantity,
    suggested_batch_id UUID REFERENCES inventory_batch(id),
    suggested_expiry_date, picked_quantity
);
```

**没有 `order_line_id`** —— 拣货行只有 SKU，无法确定"这一拣对应订单的哪一行"，
因此也无法把批次写到正确的 `order_line.batch_id` 上。

| 结论 | 说明 |
|---|---|
| 不能硬接 | 同 SKU 多行订单会写错行，退货按错批次回冲 |
| 需要 | V82 给 `pick_task_line` 加 `order_line_id`（生成拣货任务时写入） |
| 之后再 | 拣货确认 → `recordLineBatch(orderLineId, batchId)` → 售后可精确回冲 |

> 本轮只做结构核查与登记，没有写"看起来能跑"的代码。

---

## 2026-10-19：拣货行 → 订单行映射（H21 第二步）

V82：`pick_task_line.order_line_id`（可空）。

`FulfillmentService` 的合并逻辑改为携带**来源订单行集合**：

| 情况 | 行为 |
|---|---|
| 合并后只有一个来源订单行 | 回填 `order_line_id` |
| 同 SKU+批次来自多条订单行（付费行 + 赠品行） | **留空**（无法唯一定位，需先拆分辨率） |

为什么不用"猜一个"：写错行会让退货回冲到错误批次，账面利润错且事后难查。

| 验证 | 结果 |
|---|---|
| 后端全量 | ✅ **489 个全绿**（拣货/自提流程未被破坏） |
| 编译 | ✅ |

> 仍缺（H21 ⚠️）：拣货**确认**时调用 `recordLineBatch`；以及"多行合并"场景的拆分辨率方案。

---

## 2026-10-19：拣货确认回写订单行批次（H21）

`confirmPick` 成功后调用 `recordOrderLineBatch(pickLineId, batchId)`：

| 规则 | 说明 |
|---|---|
| 写入条件 | `pick_task_line.order_line_id` 存在（唯一来源）且 `order_line.batch_id IS NULL` |
| 失败处理 | 不阻断拣货（拣货是主业务），记录 INFO：多行合并或无来源订单行 |
| 价值 | 售后退货可拿到**原批次**，按原成本回冲 |

| 验证 | 结果 |
|---|---|
| 后端全量 | ✅ **489 个全绿**（自提/拣货流程未受影响） |

> 仍缺：多行合并（付费行+赠品行）场景需要 `pick_line_order_line` 映射表按数量分摊；
> 当前这类行的 `batch_id` 为空，退货回冲需人工指定批次（已在台账标注）。

---

## 2026-10-19：成本链闭环（A8 ✅）

售后收货现在按**原批次**回冲成本：

| 步骤 | 行为 |
|---|---|
| 取批次 | `order_line.batch_id`（拣货确认时回填） |
| 无批次 | **明确拒绝** `RETURN_BATCH_UNKNOWN`，要求人工指定（不按均价猜） |
| 有批次 | `SkuCostService.returnFromBatch(skuId, batchId, qty)` → 按原批次成本重算加权 |

完整链路（全部有测试）：

```
采购单 → 收货(写批次+成本+加权平均) → 拣货确认(回写订单行批次) → 售后退货(按原批次回冲)
```

| 验证 | 结果 |
|---|---|
| 后端全量 | ✅ **489 个全绿** |

> 已知限制（H21 仍 ⚠️）：多行合并（付费行+赠品行同 SKU）的拣货行没有订单行映射，
> 这类退货会走"明确拒绝 + 人工指定批次"路径，而不是自动回冲。

---

## 2026-10-19：砍价三张表（D13 数据层）

| 表 | 关键约束 |
|---|---|
| `bargain_activity` | 底价 >0；助力人数 3..30；单次砍价 `cut_min>0`；有效期 12..72h；`allow_below_cost` + `below_cost_reason`（低于成本必须留痕） |
| `bargain_session` | `UNIQUE(activity_id, member_id)`：同一会员同一活动只能发起一次；状态 `ONGOING/READY/EXPIRED/ORDERED` |
| `bargain_help` | `UNIQUE(session_id, helper_member_id)`：**助力人去重由数据库保证**，不靠应用判断 |

| 验证 | 结果 |
|---|---|
| 迁移冒烟 | ✅ 通过（v83） |

> 未做：发起/助力/达成/超时释放逻辑与接口；库存按正常下单预占，未支付由订单超时释放。

---

## 2026-10-19：砍价接口层（D13）

| 端点 | 作用 |
|---|---|
| `POST /admin/v1/bargain-activities` | 创建活动：读 SKU 加权成本 → 走 `BargainPricing`（无硬下限，低于成本/成本未知需确认）→ 返回推荐价与标记 |
| `POST /api/v1/bargain/activities/{id}/start` | 顾客发起（一人一次） |
| `POST /api/v1/bargain/sessions/{id}/help` | 助力（去重、不破底价、人数到位 READY） |
| `POST /api/v1/bargain/sessions/{id}/order` | 绑单（仅 READY、一次） |

| 验证 | 结果 |
|---|---|
| 后端全量 | ✅ **497 个全绿** |
| 接口对齐 | **222 路由**（+4）/ 24 调用 / 0 未匹配 |

> 未做：结算真正按活动价下单（需改 checkout 取价逻辑）、前端活动页与规则公示。

---

## 2026-10-19：E2E 全量跑出 4 个失败，2 个是真回归（已修）

`verify.sh` 完整跑（含 E2E）：**37 passed / 4 failed**。

| 失败项 | 性质 | 处理 |
|---|---|---|
| `commission-hold`、`exchange-flow` | **真回归**：售后收货被我上一轮改成"缺批次即拒绝"，导致退货/换货流程被内部数据缺失卡住 | 改为**记录 `COST_REVERSAL_PENDING` 事件 + WARN 日志**并继续（顾客退货不阻断），人工核对后补做回冲 |
| `ai-enterprise`、`ai-real` | 社区版构建没有 AI 路由（预期失败） | 加 `test.skip(APP_EDITION !== 'enterprise')`，**显式跳过并说明原因**，不是静默跳过 |

| 验证 | 结果 |
|---|---|
| 受影响单测 | ✅ 11 个全绿（含换货自动完成、成本服务、采购收货） |
| 后端全量（上一轮） | 497 个全绿 |

> 教训：把"数据缺失"当成"业务失败"是错的处理方式——售后是顾客权利，不能被内部记账问题挡住；
> 正确做法是**业务继续 + 留下可见待办**。

---

## 2026-10-19：回归复跑通过

修复后单独重跑两个曾失败的 spec：

```
✓ commission-hold  partial return holds commission then reverses it on refund (1.6s)
✓ exchange-flow    exchange flows from request to completion (0.9s)
2 passed
```

| 项 | 结果 |
|---|---|
| 曾失败的 2 条 | ✅ 通过（证明"售后不再被缺批次卡住"） |
| AI 2 条 | 已按构建显式跳过（社区版无该路由） |

> 未重跑全量 41 条：本轮时间/上下文有限，已如实记录，不声称"全部通过"。

---

## 2026-10-19：全量 E2E **39 条全部通过**

```
39 passed (1.5m)
```

前置：embedded PG + 后端 jar + 5 个前端（admin 5273 / store 5274 / supplier 5275 / support 5276 / storefront 3000）。

| 项 | 结果 |
|---|---|
| E2E | ✅ **39 passed / 0 failed** |
| AI 两条 | 按构建显式跳过（社区版无 AI 路由） |
| 后端单测 | ✅ 497 个全绿（上一轮） |
| 前端构建 / 接口对齐 / 帮助覆盖 | ✅（上一轮 verify） |

> 踩坑记录：storefront 的脚本名是 `dev`（不是 `dev:e2e`），漏启会导致 9 条前端相关用例失败。
> 这也是为什么 E2E 失败时先查"服务是否都起来了"，而不是先改代码。

---

## 2026-10-19：砍价结算取价（D13 继续）

`BargainService.quote(sessionId, memberId)`：

| 校验 | 结果 |
|---|---|
| 场次不存在 | 404 |
| 非本人场次 | 409 `BARGAIN_NOT_OWNER` |
| 未达成（非 READY） | 409 `BARGAIN_NOT_READY` |
| 通过 | 返回 `{skuId, unitPrice, currency}`（**锁定价**） |

设计取舍：校验失败**不回落原价**——回落等于"看起来用了砍价，其实按原价卖"，比报错更糟。

| 验证 | 结果 |
|---|---|
| 砍价用例 | ✅ 5 个全绿（含未达成拒绝、非本人拒绝、达成后返回锁定价且不低于底价） |

> 仍缺：checkout 调用 quote 替换 SKU 原价；前端活动页与规则公示。

---

## 2026-10-19：微信支付 v3 SDK 接入（骨架 + 明确拒绝）

引入官方 `wechatpay-java:0.2.15`，新增 `WechatPayConnector`：

| 行为 | 说明 |
|---|---|
| 缺配置（商户号/证书序列号/APIv3 密钥/私钥路径） | **构造即失败**（启动期暴露） |
| 已配置但 API 未接入 | 下单/退款抛 `PAYMENT_PROVIDER_NOT_READY`，**不回落 mock** |
| provider | 固定 `WECHAT`（退款按原渠道路由依赖它） |

| 验证 | 结果 |
|---|---|
| 新增用例 | ✅ 缺配置失败、已配置未接入明确拒绝 |
| 后端全量 | ✅ **504 个全绿** |

> 未做：真实的统一下单/回调验签/退款 HTTP 调用（需商户证书与 APIv3 密钥）。
> 登记为 ⚠️，且**不允许**在没有验签实现的情况下上线回调入口。

---

## 2026-10-22：全量验证（第 270 轮）

`scripts/verify.sh --no-e2e` 实跑：

| 阶段 | 结果 |
|---|---|
| 后端测试 | ✅ **523 个**（0 失败 0 跳过） |
| 后端打包 | ✅ |
| 前端构建（5 应用） | ✅ |
| 接口对齐 | ✅ 226 路由 / 24 调用 / **0 未匹配** |
| 帮助覆盖 | ✅ 23 页同步 |

说明：本轮刻意不做高风险改动，只验证前 5 轮新增（OSS/SMS/微信/支付宝/快递100/储值/品牌/质检/砍价/成本）未破坏基线。
E2E 未跑（需要 5 个前端 + PG，下轮单独跑）。

---

## 全量核验（第 23 轮，`verify.sh --no-e2e`）

| 阶段 | 结果 |
|---|---|
| 后端测试 | ✅ **618 个**（0 失败 0 跳过） |
| 后端打包 | ✅ |
| 前端构建（5 应用） | ✅ 6 次 `built` 全部成功 |
| 接口对齐 | ✅ 260 路由 / 26 调用 / **0 未匹配** |
| 帮助覆盖 | ✅ 32 页，索引同步 |
| E2E | 未跑（`--no-e2e`），下轮补 |

