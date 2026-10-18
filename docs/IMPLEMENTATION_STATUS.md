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
