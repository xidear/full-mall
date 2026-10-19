# Full Mall 项目记忆与跨机器开发交接

> 更新时间：2026-10-19
> 用途：换机器、换会话或换开发者时，先读本文，避免重新踩环境、模块边界、种子数据和并发一致性的坑。
> 权威顺序：`AGENTS.md` → 代码与迁移 → 本文 → 其他设计文档。若本文与代码冲突，以代码为准并更新本文。

## 1. 项目定位

Full Mall 是一个**源码开放（source-available，非 OSI 开源）**的跨境商城，许可证为 **PolyForm Noncommercial License 1.0.0**，禁止商业使用。对外描述请用“源码开放”，不要宣传为“开源”。

目标范围：

- 后端 API、PC 商城、移动 APP、小程序、供应商端/API、客服工作台。
- 多规格 SPU/SKU、库存与简易 ERP、批次/效期/FEFO、多仓/门店/供应商虚拟仓。
- 自营仓发货、门店发货、供应商代发、门店自提、按履约源拆单。
- 多语言/多地区长期规划：首发中国大陆（zh-CN、CNY、微信/支付宝/银联 Mock），后续港澳台（zh-HK/zh-TW、HKD/MOP/TWD、当地支付/物流/保税仓）。
- Mock 支付/物流/短信/邮件/发票；真实适配器可配置编译，但仓库不包含任何商户号、密钥、证书。
- 公开演示站严格只读：GET + `/mock/**`，POST/PUT/DELETE 返回 405，无外网副作用。
- 企业版预留 edition/license/entitlement 与签名更新机制，核心不依赖企业模块。

已确定的产品原则：

- 管理后台默认不允许手填 JSON、逗号分隔字符串或裸 ID；必须用选择器、变体矩阵、规则构建器、日期选择器、开关、扫码。
- 金额存整数最小货币单位（分），前台按市场币种格式化；禁止浮点。
- 库存不能错：不可变库存流水、预占 TTL、原子扣减、FEFO、防超卖。
- 后台可配置优先于硬编码；用户可见文本必须是完整标签，不显示原始 key。
- 借鉴成熟产品策略，但 clean-room，不复制 CRMEB 等代码/UI/文案/表结构。

## 2. 不可违反的工作规则（来自 `AGENTS.md`）

1. **禁止静默兜底**：错误必须抛出或在明确边界处理，不能用默认值、空值、跳过、无限重试、`|| true`、`2>/dev/null` 掩盖失败。
2. 每次执行动作前，先输出一行 `[禁止静默兜底]`。
3. 行动前探测并记录操作系统类别/版本、Shell 类别/版本；不得凭记忆假设。
4. 开源优先：先查标准库、官方 SDK、成熟开源方案；自研必须说明调研过什么、为何不用、维护成本。
5. 执行有副作用操作前评估灾难性后果：是否不可逆、是否影响生产/他人数据、是否需要提权、是否泄露密钥、回滚方案是否验证过。
6. 声称完成前必须实际运行测试/构建/命令并检查退出码；报告区分“已做并验证 / 已做未验证 / 未做”。

## 3. 仓库、发布与许可证

仓库名均为 `full-mall`：

| 远端 | 地址 | 用途 |
|---|---|---|
| GitHub | `https://github.com/xidear/full-mall.git` | **唯一公开主仓库**，开发提交推这里 |
| Gitea | `https://company.binnarui.com:8148/xidear/full-mall.git` | 私有仓库，直接推送 |
| Gitee | `https://gitee.com/xidear/full-mall.git` | 公开镜像，仅由 GitHub Actions 强制同步，**不要手动推送** |

- 本地 `main` 跟踪 `github/main`；推送 GitHub 使用 `git -c http.version=HTTP/1.1 push github main`（HTTP/2 会间歇超时）。
- Gitee 由 `.github/workflows/mirror-gitee.yml` 在推送 `main`/tag 后同步，依赖 Actions secret `GITEE_TOKEN`；GitHub CI 失败不会阻止镜像复制，但应在 GitHub 修。
- 当前环境没有 `gh` CLI，无法直接查 Actions 运行状态。
- 许可证：PolyForm Noncommercial 1.0.0 + `Required Notice: Copyright (c) 2026 xidear and Full Mall contributors.` GitHub 会显示 SPDX `NOASSERTION`，这是正常的。
- 不要把 `.env`、真实商户号、证书、私钥、token 写入仓库或 `.git/config`。

## 4. 技术栈与架构

- Java 21、Spring Boot 3.4.5、Spring Modulith 1.3.5、Spring Security、Spring WebSocket。
- PostgreSQL（目标 17；本地测试容器 16.15）、Valkey、RabbitMQ、OpenSearch、SeaweedFS。
- Nuxt 3、Vue 3、Element Plus、uni-app、Vite。
- Docker Compose、Dev Container；Maven（不使用 OSGi，第三方适配器走 SPI）。

### 4.1 后端模块

`server/src/main/java/com/fullmall` 下按业务分包：

`catalog`、`inventory`、`store`、`order`、`fulfillment`、`payment`、`analytics`、`legal`、`support`、`demo`、`seed`、`platform`、`staff`、`permission`、`aftersale`、`warehouse`、`supplier`、`distribution`、`promotion`、`theme`、`setting`。

Modulith 规则：

- `ModulithStructureTest` 调用 `ApplicationModules.verify()`；新增跨模块依赖必须通过命名接口暴露。
- `com.fullmall.platform` 是 `@ApplicationModule(type = OPEN)`，放 `Money`、`FulfillmentType`、`ApiException`、`Ids`、配置等共享类型。
- 其他模块用 `package-info.java` 暴露 `<mod>-domain` / `<mod>-service` / `<mod>-api` / `<mod>-events`；跨模块直接访问 `infra` 会被拒绝。
- 例子：`pos` 用 `pos-api`/`pos-domain`/`pos-service`；`staff` 用 `staff-service`/`staff-domain`；`promotion` 用 `promotion-service`/`promotion-events`；`payment.app` 已加 `payment-service` 供退款监听器使用。
- `PaymentService` 调 `OrderService`，不能直接改订单仓储；退款监听器放在 payment 模块，避免 order→promotion→payment→order 的模块环。
- 新增定时任务如果访问新表，必须加 `initialDelayString`，否则 H2/test 下可能在 Flyway 完成前触发。

### 4.2 Profile

- `api`、`worker`（无 Web）、`ws`（8081）、`dev`、`demo`、`prod`、`test`。
- Web-only Bean（SecurityConfig、WebConfig、WS、DemoReadOnlyFilter）加 `@ConditionalOnWebApplication(SERVLET)`。
- `DemoReadOnlyFilter` 仅 `demo` profile：所有非 GET/HEAD/OPTIONS 返回 405 `DEMO_READ_ONLY`。
- dev/demo 默认允许访问；prod 对 `/admin`、`/store`、`/supplier`、`/open` fail-closed。
- `app.store.auth-required`、`app.support.auth-required`、`app.pos.require-manager-approval` 在 prod 为 true，dev 默认 false。

### 4.3 金额与库存不变量

- `Money = record Money(long amount, String currency)`，最小货币单位；`Currency.getInstance` 校验；无浮点。
- `inventory_batch` 以 SKU + 地点 + 批次 + 效期为原子库存；`quantity/reserved_quantity` 有 CHECK。
- `stock_move` 不可变，只能追加冲销流水。
- `reservation` 有 TTL；FEFO 分配是纯函数，按效期（空值最后）、收货时间排序；预占使用原子 UPDATE，失败明确报错。
- 过期/隔离/冻结批次不可售；退款回补只允许回到可售批次，否则 409 转人工处理。
- 取消未支付订单会释放预占和促销名额；支付与取消都用条件状态更新，竞态只有一方成功。

## 5. 数据库迁移（当前 Flyway v21）

| 版本 | 内容 |
|---|---|
| V1 | 核心：market/product/sku/warehouse/store/supplier/inventory/reservation/order/fulfillment/legal 等 |
| V2 | 幂等记录 |
| V3 | 自提多次核销 |
| V4 | 员工认证 |
| V5 | 主题配置 |
| V6 | 通知 |
| V7 | 系统设置 |
| V8 | POS：会员、积分/储值 ledger、优惠券、班次、销售、行、批次分配、tender、现金流水、退款、对账；含演示会员/优惠券（**污染 prod，待拆**） |
| V9 | POS 部分退款：退款行/批次分配、累计退款、积分扣回 |
| V10 | `pos_sale.status` 加宽到 32 |
| V11 | 供应商 API：AES-GCM 凭证、库存 feed、供应商虚拟 SKU |
| V12 | 仓储 ERP：收货、盘点、调拨 |
| V13 | RBAC：permission、role_permission |
| V14 | 线上售后 |
| V15 | `payment.status` 加宽到 32 |
| V16 | 会员等级、生日、成长值、生日礼、成长 ledger |
| V17 | 分销：推荐绑定、订单推荐人、佣金 |
| V18 | 客服工作台：会话字段、消息 metadata、转单 |
| V19 | 员工 PIN、员工-门店绑定 |
| V20 | 促销：秒杀/买赠/拼团、参团、订单行 `promotion_id`/`line_type` |
| V21 | `promotion_redemption.released_at`，支持取消订单回退促销名额 |

重要教训：

- 任何状态列如果可能超过 16 字符（如 `PARTIALLY_REFUNDED`），直接建 `VARCHAR(32)`，否则运行期 500。
- 新表迁移不要在迁移里塞演示业务数据；V8 的演示会员/券是已知问题。
- Flyway 校验和会阻止修改已应用迁移；补字段用新迁移。

## 6. 已实现功能面

### 6.1 商品、库存、履约

- SPU/SKU/选项/属性、类目、翻译、上下架、图片。
- 订单结算、Mock 支付、订单状态机、地址快照、法律 clickwrap。
- 按履约源拆单：仓库/门店/供应商/自提；一个履约组对应 `fulfillment_order` + `pick_task`。
- FEFO 拣货、扫码校验、自提提货码/手机号/身份证核验、超时释放。
- 供应商代发接单/发货/拒单；拒单释放对应订单行库存。

### 6.2 POS 收银

- 班次：开班、备用金、现金收/付、关班、应有/实盘/差异、差异原因。
- 销售：商品扫码搜索、会员、优惠券、积分抵扣、储值、现金+微信/支付宝/银行卡组合支付、找零。
- `PosCalculator` 纯函数：优惠券上限、应收、积分、`allocateNet` 最大余数法保证行净额精确求和、退款按比例分摊。
- 退款：按整单或商品行部分退款；跨原 tender 分摊；积分按比例扣回；批次回补；状态 `COMPLETED → PARTIALLY_REFUNDED → REFUNDED`。
- 会员：等级折扣、积分倍率、成长值自动升级、生日礼；**目前仅在 POS 绑定会员时生效，线上匿名结算不生效**。
- 锁屏/店长授权：员工 PIN；prod 退款默认要求店长/运营 PIN；`POST /store/v1/pos/auth/manager`。
- 报表：日结、支付方式汇总、现金对账、支付对账、当日销售。

### 6.3 供应商

- `/open/v1` HMAC-SHA256：method + path/query + timestamp + nonce + SHA256(body)；±300s；nonce 防重放；AES-GCM 加密 secret。
- 供应商门户：订单列表、接单、发货、拒单、库存 feed。
- 供应商数据范围由 HMAC 凭证的 supplier id 隐式限定。
- 缺口：结算单/CSV/Webhook、采购审批、质检、退货对账。

### 6.4 仓储 ERP

- 收货入库（批次/效期/成本）、盘点（系统/实盘/差异）、调拨（FEFO 源批次/在途）、补货建议。
- 后台“仓储作业”可视化操作。

### 6.5 RBAC / 售后 / 数据权限

- 权限字典 + role_permission；ADMIN 隐式全权限；接口级强制校验；30s 缓存。
- 线上售后：退货/换货、审核、拒单、收货隔离批次、Mock 原路退款、事件记录。
- `StoreAccessService` 是门店数据范围唯一入口：
  - STORE 角色必须命中 `staff_store`；未绑定失败关闭 `STORE_SCOPE_EMPTY`。
  - 未登录且 `app.store.auth-required=true` → 401。
  - ADMIN/OPERATOR 不受限。
- POS 与门店作业的 storeId、班次、销售、拣货任务、自提查询/核销均校验归属。
- `/store/v1/my-stores` 只返回绑定门店；后台 `/admin/v1/stores` 返回全部门店。
- 客服只能看/操作分配给自己的会话；供应商看自身；主管跨组视图/导出范围仍缺。

### 6.6 会员、分销、客服

- 会员等级：discount_rate 1–100、points_multiplier、birthday_points、min_growth；成长 ledger；生日礼每年一次。
- 分销五种绑定：`ORDER / REGISTER / VISIT / ONE_TIME / PERMANENT`；访问绑定 30 天有效；支付后按实付 5% 产生佣金（四舍五入）。
- 客服工作台：领取、转单、文本/商品/帮助链接消息、移动端卡片；商品卡片带 `?ref=<agentId>&mode=...`，支付后佣金归客服。
- 实测：2990 分实付 → 150 分佣金。
- 客服 WebSocket 仍是 echo stub，工作台用轮询；AI/RAG/SLA/质检/附件/已读回执/顾客侧聊天入口未做。

### 6.7 促销（V20/V21）

- 秒杀、买赠、拼团；后台可视化创建/结束；商城 `/promotions` 与商品标签。
- 定价：`PromotionService.prepare(items, phone, groupBuyId)` 在下单前计算；订单小计保留原价，`discount = 原价小计 − 促销小计`；赠品行 0 元 `lineType=GIFT`，仍预占真实库存。
- 秒杀售罄明确 `PROMOTION_SOLD_OUT`，不回退原价。
- 并发：活动库存原子扣减；每人限购对 promotion+phone 加 advisory lock；拼团人数对团 id 加锁。
- 拼团：支付后标记成员已付；已付人数 ≥ 成团人数 → `SUCCESS`；过期 `FAILED` 并逐单退款，未退款订单每分钟重试。
- 未支付订单超时：`PENDING_PAYMENT → CANCELLED`，释放库存/促销名额，删除未支付团成员；取消后支付 409。
- 缺口：满减/满折、叠加/互斥/最优价、砍价、渠道隔离；POS 不自动应用线上促销。

## 7. 前端与测试

### 7.1 应用

| 目录 | 说明 | 本地端口 |
|---|---|---|
| `web/apps/storefront` | Nuxt 3 商城，SSR | 3000 |
| `web/apps/admin` | Vue 3 + Element Plus 运营后台 | 5173 / 本地可用 5273 |
| `web/apps/store-console` | 门店作业 + POS | 5174 / 5274 |
| `web/apps/supplier-portal` | 供应商门户 | 5175 / 5275 |
| `web/apps/support-console` | 客服工作台 | 5176 / 5276 |
| `web/apps/mobile` | uni-app 多端 | — |
| `web/packages/api-client` | 共享 TS 类型与 API client | — |
| `web/packages/theme` | 主题预设/CSS 变量 | — |
| `web/packages/i18n` | 多语言 | — |
| `web/packages/ui` | 共享 UI | — |

构建：

```bash
cd web
npm --workspace @fullmall/storefront run build
npm --workspace @fullmall/admin run build
npm --workspace @fullmall/store-console run build
npm --workspace @fullmall/supplier-portal run build
npm --workspace @fullmall/support-console run build
npx playwright test
```

### 7.2 已知前端/E2E 陷阱

- Nuxt 水合前点击是静默 no-op。E2E 必须等待：
  ```js
  await page.waitForFunction(() => Boolean(document.querySelector('#__nuxt')?.__vue_app__))
  ```
- 依赖客户端状态的跳转用页面内链接（如 `.cart-link`），不要直接 `page.goto`。
- Element Plus `el-input-number` 不会刷新已有 input 的 `aria-disabled`；条件变化时用 `:key` 重新挂载。
- 门店工作台两个 tab 用 `v-show` 保留 DOM；POS tab 首次访问时懒挂载。完全卸载会破坏自提 E2E。
- 门店端共用 `src/api.ts` 单例 client，token 取自 `localStorage['fullmall.staff.token']`。
- Element Plus 打包 >1MB 的 chunk 警告是已知现象，不是构建失败。
- `useCart` 在 add/update/remove/clear 中显式 persist，没有 deep watch。
- 管理端 tsconfig 需要 `"types": ["vite/client","node"]`。
- 移动 uni-app 依赖仍用 `latest`，未固定版本。

### 7.3 测试基线

- 后端最近结果：38 个测试，36 通过，2 个 Testcontainers 用例在无 Docker socket 的 Maven 容器中跳过。
- Playwright 最近结果：**10/10 通过**（admin、pickup、POS、promotions、storefront、supplier、support）。
- `ServerContextTest` 使用 H2、Flyway off；`ModulithStructureTest` 校验模块边界。
- Mockito 在 JDK 21 容器中 self-attach 受限，已用 `server/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` = `mock-maker-subclass` 修复。

## 8. 本机与跨机器开发环境

### 8.1 当前验证环境（2026-10-19）

- Windows 10.0.26300 x64；Git Bash/MINGW64 5.3.9；WSL Ubuntu 2 可用。
- 宿主机 JDK 17；项目 target Java 21，因此构建/测试/运行用容器。
- 宿主机没有 `mvn`；使用缓存的 `maven:3.9.9-eclipse-temurin-21` 镜像。
- Node 22.22.3、npm 12.0.2；Python 3.11.15（命令是 `python`，`python3` 是 Store alias stub）。
- 浏览器只有 Microsoft Edge；Playwright 用 `msedge` channel。
- `gh`、`psql`、`jq` 未安装。
- **Maven Central 可直连**：`repo1.maven.org` 从 Windows/WSL/Maven 容器直连与走 `192.168.1.2:7893` 均 200；全新本地仓库下载构件成功。**默认不要加 `-o`。**
- Windows `curl` 使用 schannel，可能报 `CRYPT_E_REVOCATION_OFFLINE`，加 `--ssl-no-revoke`；WSL/容器 curl 不需要。
- Docker Hub 出网曾不稳定；优先使用缓存镜像，不要改用来路不明的镜像源。
- GitHub push 使用 HTTP/1.1；Gitea 直推；Gitee 不手动推送。

### 8.2 后端构建/测试/打包

```bash
cd /d/works/full-mall
MSYS_NO_PATHCONV=1 wsl bash -lc "cd /mnt/d/works/full-mall && \
  docker run --rm \
    -v /mnt/d/works/full-mall:/workspace \
    -v /mnt/c/Users/admin/.m2:/root/.m2 \
    -w /workspace/server \
    maven:3.9.9-eclipse-temurin-21 \
    mvn -B -U test"
```

- `MSYS_NO_PATHCONV=1` 必须；否则 Docker 收到 Windows 路径。
- 不加 `-o`；Maven Central 已验证在线可用。
- 打包用 `mvn -B -U -DskipTests clean package`。
- 运行中的 API 容器把 jar 以只读 bind mount 到 `/app/app.jar`；**打包前先 `docker stop fullmall-api-pos`**，打包后 `docker start`。运行中覆盖 jar 会报 `device or resource busy`，替换后可能报 `Invalid or corrupt jarfile`，需要 `clean package` + 重建容器。

### 8.3 本地 API / PostgreSQL

最小 PostgreSQL 容器（首次创建）：

```bash
docker run -d --name fullmall-postgres-test \
  -p 55432:5432 \
  -e POSTGRES_DB=fullmall \
  -e POSTGRES_USER=fullmall \
  -e POSTGRES_PASSWORD=change-me-local \
  postgres:16-alpine
```

当前本地 API 容器（如果同名容器已存在，先 `docker rm -f fullmall-api-pos`；打包前先 `docker stop`）：

```bash
docker run -d --name fullmall-api-pos --network host \
  -v /mnt/d/works/full-mall/server/target/fullmall-server-0.1.0-SNAPSHOT.jar:/app/app.jar:ro \
  -e SERVER_PORT=8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:55432/fullmall \
  -e SPRING_DATASOURCE_USERNAME=fullmall \
  -e SPRING_DATASOURCE_PASSWORD=change-me-local \
  eclipse-temurin:21-jre java -jar /app/app.jar
```

- PostgreSQL 容器：`fullmall-postgres-test`，镜像 `postgres:16-alpine`，宿主机端口 **55432**，库 `fullmall`，用户 `fullmall`，密码 `change-me-local`（仅本地演示）。
- API：`http://localhost:8080`，健康检查 `/actuator/health`。
- 演示账号（仅 dev/demo，生产必须改）：
  - `admin / Admin@123456` 超级管理员
  - `operator / Operator@123456` 运营
  - `store / Store@123456` 门店
  - 登录需要算术验证码：`GET /admin/v1/auth/captcha` 返回 `question: "a + b = ?"`，答案是和。
- `app.store.auth-required=false` 时门店端可不登录；prod 为 true。
- 开发环境 RabbitMQ health 关闭；`/actuator/health` 仍 200。

### 8.4 Compose

```bash
cd /d/works/full-mall
MSYS_NO_PATHCONV=1 wsl bash -lc "cd /mnt/d/works/full-mall && docker compose config -q"
```

- `compose.yaml` 是完整栈；`compose.lite.yaml` 是低内存模式。
- 应用端口：API 8080、WS 8081、商城 3000、后台 5173、门店 5174、供应商 5175、客服 5176。
- 生产目标 PostgreSQL 17；本地测试用 16 替代。

### 8.5 API 调用注意事项

- Git Bash `curl -d` 传中文 JSON 会乱码或解析失败；用 Python 构造 UTF-8：
  ```python
  import json, urllib.request
  data = json.dumps(payload, ensure_ascii=False).encode('utf-8')
  req = urllib.request.Request(url, data=data, method='POST')
  req.add_header('Content-Type', 'application/json')
  ```
- 读 API JSON 用 Python，不依赖终端编码。
- Windows 下 Write 工具写 `/tmp/...` 会落到 `D:\tmp\...`；Git Bash 的 `/tmp` 是 `C:\Users\admin\AppData\Local\Temp`。跨工具写临时脚本时注意路径。

## 9. 种子、测试数据与 fixture 现状

### 9.1 当前布局

- `server/src/main/resources/db/demo/seed.sql`：商品/SKU/门店/仓库/批次/供应商/法律文档；`DemoDataSeeder` 仅在 dev/demo 且 product 表为空时执行。
- `db/demo/staff.sql`：演示员工账号。
- `V8__pos.sql`：**在迁移里插入演示会员与优惠券**，因此 prod 也会得到演示数据，是已知 P0 污染。
- `db/demo/seed.sql` 末尾新增演示促销 `91000000-…`：`DEMO-FLASH-MUG`、`DEMO-GIFT-MILK`、`DEMO-GROUP-MUG`。长期数据库因 product 表非空会跳过整个 seed，需手工插入。
- 测试/ E2E 中硬编码 UUID：
  - 牛奶 SKU `bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb`
  - 马克杯 SKU `dddddddd-dddd-dddd-dddd-dddddddddddd`
  - 上海静安店 `22222222-2222-2222-2222-222222222222`
  - 北京朝阳店 `44444444-4444-4444-4444-444444444444`
  - 仓库 `11111111-1111-1111-1111-111111111111`
  - 供应商 `33333333-3333-3333-3333-333333333333`
- `DemoController` 也重硬编码同类 UUID，但不读 DB。
- 共享演示库的券/积分是一次性的；E2E/API 验证要用新券或会员 `M0002`（李静），不要断言绝对值。

### 9.2 建议的 fixture 方向（尚未实施，用户未最终确认）

1. 四层拆分：Flyway 只做 schema；reference data（市场/币种/系统设置）用幂等 `R__`/`db/reference`；demo 数据放 `db/demo`；测试数据放 `src/test/resources/db/test` + `application-test.yml`。
2. 拆 `seed.sql` 为 catalog/store/inventory/pos 等域文件。
3. 测试用业务键（`sku_code=MILK-1L`、门店名、会员手机号）通过 fixture helper 解析，只有深链页面保留固定 ID。
4. 一份 YAML fixture manifest 供 Java 测试、Playwright、DemoController 共用，并加一致性测试校验 key 对应 ID 存在。
5. 集成测试断言增量（前后差值），不要断言 `points == 500` 这类绝对值。
6. `app.mode=prod` + demo seeder 同时启用时应启动失败，而不是静默跳过。

## 10. 第三方依赖与 POS 集成原则

- Java 的 Composer 类比是 **Maven**（Maven Central；多模块 Maven ≈ composer 包；Gradle 是替代，不要混用）。
- 不引入 OSGi。业务代码只依赖 SPI：
  - 线上支付：`PaymentConnector`。
  - POS：`PosPaymentProvider`（`create/query/cancel/refund`），实现 `MockPosPaymentProvider`。
- 微信/支付宝/银联/聚合收单 SDK 只放在 adapter 模块，用 `@ConditionalOnProperty` 或独立服务隔离；领域层不出现厂商 DTO。
- 支付宝碰一碰需要认证终端 + NFC + ISV/商户资质；后端只做 intent/query/webhook/refund/reconcile。
- 离线扫码/银行卡取决于收单机构；禁止“假装成功”；离线队列必须幂等、有限、设备绑定、可人工复核。
- 真实支付适配器、后台真实凭证、对账文件仍未实现；当前只有 Mock。

## 11. 技术栈选择原则

用户明确要求：**不要根据本机已安装的工具选技术栈**，要按项目长期需求选择（事务正确性、多端复用、外部集成、团队构成、许可证、运维能力）。本地工具只用于验证，不作为选型依据。

## 12. 当前仍缺的功能（诚实清单）

1. 线上会员：会员登录、线上等级折扣、线上成长值/生日礼。
2. 促销：满减/满折、叠加/互斥/最优价、砍价、渠道隔离；POS 应用线上促销。
3. 真实支付：微信/支付宝/银联/聚合收单适配器、后台凭证、webhook 验签、对账文件；当前拼团失败退款也依赖 Mock。
4. 客服实时化：真实 WebSocket、AI Bot/RAG、附件/图片、已读回执、会话关闭、SLA/质检、顾客侧聊天入口。
5. 数据权限细化：客服主管跨组、导出脱敏、供应商结算范围、操作日志覆盖率。
6. 供应商/ERP：结算单、CSV、Webhook、采购审批、质检、退货对账。
7. 门店：店长改价/作废授权、交接班审核、员工排班；线下扫码/碰一碰真实接入。
8. 多语言/多币种：en/es/fr/ar + RTL、港澳台币种/支付/物流。
9. 企业版更新机制、许可证/entitlement、完整运维文档。
10. 性能/并发压测、完整安全测试、生产部署与回滚演练。

## 13. 新机器 / 新会话上手清单

1. 克隆 GitHub 主仓库，切到 `main`，确认 `git log` 有 `666ca5b` / `35a0b68` 之后的提交。
2. 安装 Docker（或 WSL Docker）、Node 22、Python 3.11、Edge；Java 21 可通过 Maven 容器，不必装宿主机 JDK 21。
3. `cp .env.example .env`，按需改本地密码；不要提交 `.env`。
4. 启动核心依赖（Postgres/Valkey/RabbitMQ 等），或按本文的 `fullmall-postgres-test` + API 容器方式最小启动。
5. 后端：`mvn -B -U test`（在线，不加 `-o`）；打包后先 stop API 容器再启动。
6. 前端：`cd web && npm install`，分别启动 3000/5273/5274/5275/5276。
7. 跑 `npx playwright test`，当前基线 10/10。
8. 推送：`git -c http.version=HTTP/1.1 push github main` + `git push gitea main`；Gitee 等 Actions。
9. 修改前先读 `AGENTS.md`、`docs/PROMOTIONS_AND_DATA_SCOPE.md`、`docs/IMPLEMENTATION_STATUS.md`、`docs/MARKET_PARITY.md`。

## 14. Agent 技能（本机全局，非仓库内容）

2026-10-18 在 `C:\Users\admin\.zcode\skills\` 全局安装 21 个技能，来源与 commit 记录在 `~/.zcode/skills/THIRD_PARTY_SKILLS.md`；换机器不会自动带走。

- mattpocock/skills（MIT）：`diagnosing-bugs`、`tdd`、`code-review`、`implement`、`implement-spec`、`codebase-design`、`improve-codebase-architecture`、`prototype`、`domain-modeling`、`grill-with-docs`、`to-spec`、`to-tickets`、`research`、`pr`、`setup-matt-pocock-skills`、`grill-me`、`grilling`。
- anthropics/skills（Apache-2.0）：`frontend-design`、`webapp-testing`（Python Playwright，用 `python`）。
- Leonxlnx/taste-skill（MIT）：安装名 `design-taste-frontend`（需要 Node 22+、Chrome/Edge）。
- Vercel Web Interface Guidelines（MIT）：`web-design-guidelines` 运行时抓取规则。

已拒绝：REA/逆向/渗透类、Vercel React 最佳实践（项目是 Vue/Nuxt）、Agent Browser、Find Skills、MotionSites、Impact-Cable、女娲、huashu-design。

推荐用法：bug → `diagnosing-bugs`；需求 → `grill-me`/`to-spec`/`to-tickets`/`implement` + `tdd`；评审 → `code-review`；UI → `frontend-design`/`design-taste-frontend` + `webapp-testing`/`web-design-guidelines`。

## 15. 相关文档

- `AGENTS.md`：强制工作规则。
- `docs/DEVELOPMENT_PLAN.md`：32 周计划与范围。
- `docs/IMPLEMENTATION_STATUS.md`：逐轮实施记录与验证计数。
- `docs/MARKET_PARITY.md`：与成熟商城的功能对照。
- `docs/PROMOTIONS_AND_DATA_SCOPE.md`：促销与数据权限设计。
- `docs/POS_DESIGN.md`：POS 组合支付/班次/对账/离线设计。
- `docs/JAVA_DEPENDENCIES.md`：Java 第三方依赖与 SPI 策略。
- `docs/erd.md`、`docs/openapi.yaml`：数据模型与接口。
- `docs/ENVIRONMENT.md`：早期环境记录（部分内容已被本文 2026-10-19 的 Maven Central 结论更新）。
