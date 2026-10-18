# Full Mall

源码开放（非商业许可）的跨境商城。首发中国大陆，支持多仓、门店、供应商代发、门店自提、批次/效期与 Mock 支付；后续扩展香港、澳门、台湾地区。

> 当前仓库处于 Phase 0 地基阶段。计划与决策见 [`docs/DEVELOPMENT_PLAN.md`](docs/DEVELOPMENT_PLAN.md) 和 [`docs/adr/`](docs/adr/)。

## 技术栈

- 后端：Java 21、Spring Boot 3、Spring Modulith、PostgreSQL、Valkey、RabbitMQ
- 搜索：OpenSearch（轻量开发可用 Meilisearch）
- 存储：SeaweedFS（S3 兼容）
- 前端：Nuxt 3、Vue 3、uni-app
- 开发环境：Docker Compose、Dev Container

## 快速开始

前置：Docker Desktop、JDK 21、Node 22、npm（或使用 Dev Container）。

```bash
cp .env.example .env
docker compose --profile core --profile app --profile tools up -d
```

- 商城：http://localhost:3000
- 管理后台：http://localhost:5173
- 门店端：http://localhost:5174
- 供应商端：http://localhost:5175
- 客服工作台：http://localhost:5176
- API：http://localhost:8080
- OpenAPI：http://localhost:8080/swagger-ui.html
- Mailpit：http://localhost:8025
- RabbitMQ：http://localhost:15672
- OpenSearch Dashboards：http://localhost:5601
- pgAdmin：http://localhost:5050
- Grafana（可选）：`docker compose --profile obs up -d`，http://localhost:3001

轻量模式（低内存）：

```bash
docker compose -f compose.lite.yaml up -d
```

## 构建模式

| 模式 | 用途 | 行为 |
|---|---|---|
| `dev` | 本地开发 | Mock 支付/物流/短信，允许写入 |
| `demo` | 公开演示站 | 只读 + GET `/mock/**`，禁止 POST/PUT/DELETE，无外网副作用 |
| `prod` | 正式站 | 必须配置真实适配器，启动校验 |

## 运营后台

运营后台是**商城运营人员**的工作台，不是供应商端：

- 登录/退出/修改密码、验证码、失败锁定、角色菜单（超级管理员/运营人员）。
- 首页：今日订单/销售额、待付款、待发货、待自提、低库存、临期、客户数、待办跳转。
- 商品管理：全状态列表、新建、上架/下架。
- 订单管理：按状态筛选、订单详情、履约单。
- 客户管理：按手机号聚合订单、订单数、累计消费、最近下单。
- 员工与权限：员工列表、角色、状态；角色菜单过滤。
- 库存与批次：仓库/门店/供应商库存、临期过滤。
- 通知中心：支付/备货/发货/完成事件，站内/短信/邮件/微信/APP Mock 渠道，发送记录与已读。
- 主题配置：PC 商城/微信小程序分别配置，图形化调整并保存到服务器。
- 系统设置：站点名称、公司主体、ICP备案号、公网安备、客服电话/邮箱、站点状态/维护文案、默认语言/币种、存储/短信/支付/物流适配器。
- 门店工作台：首次按门店拉取任务，之后通过 `/ws/store` 推送任务变更；断线自动回退轮询。
- 演示模式：门店工作台显示试用提货码 `654321`，可拉取演示顾客/订单/商品信息，但不会执行真实核销。
- 商城商品使用本地 SVG 图片；后台商品列表显示缩略图。金额后端存分，前台按当地币种格式化显示。
- 自提门店根据浏览器定位排序并显示距离。

本地演示账号（仅 `dev`/`demo`，生产必须修改）：

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `Admin@123456` | 超级管理员 |
| `operator` | `Operator@123456` | 运营人员 |
| `store` | `Store@123456` | 门店人员（当前不进后台） |

供应商端是独立应用，包含 API 推送说明；客服端是独立 WS 工作台。

## 多主题

- PC 商城预设：`mall`（通用零售）、`industrial`（机械设备/工业品）、`fresh`、`minimal`。
- 正式配置在运营后台「主题配置」中保存到服务器；PC 商城和小程序分别生效。
- 开发预览可用 `THEME=mall`、`NUXT_PUBLIC_THEME=industrial` 或 `?theme=industrial`；**前台不再放模板切换控件**。
- 小程序复用同一套 `@fullmall/theme` 预设，并可在后台图形化调整颜色、圆角、字号、卡片风格、商品布局和 Banner 文案。
- 主题只影响表现层，不改变价格、库存、履约和权限规则。

```bash
# 正式镜像（需要先配置 .env）
make build-prod

# 演示镜像
make build-demo
```

开源仓库不包含任何真实商户号、密钥或证书。客户配置自己的支付/物流参数后重新编译即可。

## 目录

```text
server/                 Spring Boot 应用（api/worker/ws profiles）
web/apps/storefront/    Nuxt 3 商城
web/apps/admin/         运营后台
web/apps/store-console/ 门店工作台
web/apps/supplier-portal/ 供应商后台
web/apps/support-console/ 客服工作台
web/apps/mobile/        uni-app 多端
web/packages/           共享 API client、i18n、设计令牌
docs/                   计划、ADR、ERD、OpenAPI、参考登记
deploy/                 部署脚本与 Helm（后续）
```

## 测试

```bash
# 后端单测/结构测试
cd server && mvn test

# 页面 E2E（需要 API、商城、Admin 5273、门店 5274 已启动）
cd web && npx playwright test
```

环境要求、WSL Docker 代理和缺失镜像见 [`docs/ENVIRONMENT.md`](docs/ENVIRONMENT.md)。

## 文档

- [开发计划](docs/DEVELOPMENT_PLAN.md)
- [实施状态](docs/IMPLEMENTATION_STATUS.md)
- [市场主流商城差距清单](docs/MARKET_PARITY.md)
- [POS 收银与组合支付设计](docs/POS_DESIGN.md)
- [Java 依赖管理与三方包集成](docs/JAVA_DEPENDENCIES.md)
- [第三方集成与 Mock](docs/INTEGRATIONS.md)
- [环境与缺失软件](docs/ENVIRONMENT.md)
- [架构决策](docs/adr/README.md)
- [核心 ERD](docs/erd.md)
- [OpenAPI 契约](docs/openapi.yaml)
- [参考项目与 clean-room 规则](docs/references/README.md)
- [功能对照矩阵](docs/parity-matrix.md)

## 规则

- 不在仓库提交真实密钥、证书、商户号、个人信息。
- 演示站必须保持只读；任何写接口都必须在 demo profile 被拦截。
- 业务数据变更必须可审计；库存流水不可变；金额禁止浮点。
- 参考项目只做 clean-room 借鉴，不复制代码/UI/文案。

## 仓库与镜像

- GitHub 主仓库：https://github.com/xidear/full-mall
- Gitee 自动镜像：https://gitee.com/xidear/full-mall

开发提交只推送到 GitHub `main`；GitHub Actions（`.github/workflows/mirror-gitee.yml`）会在每次推送后自动同步到 Gitee。不要直接在 Gitee 上提交，镜像同步会覆盖。

## 许可证

本项目采用 [PolyForm Noncommercial License 1.0.0](LICENSE)（SPDX：`PolyForm-Noncommercial-1.0.0`）：

- 允许个人学习、研究、实验、业余项目，以及教育、慈善、公共研究等非商业用途；
- 禁止任何商业用途，包括商业部署、收费服务、商业产品集成和生产经营使用；
- 商业授权请联系仓库维护者。

注意：由于限制商业使用，本项目属于 **source-available（源码开放）**，不是 OSI 定义的开源软件。