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
