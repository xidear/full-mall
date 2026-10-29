# 前端 / 后端 / 数据库校验分层审计

> 结论先说：**目前不是统一的**。三层各自有校验，但规则来源、边界和严格程度不一致；
> 本轮已修掉几处会直接造成“前端能提交、后端拒绝/静默接受”的漂移，其余列入待办。

## 1. 三层应该各管什么

| 层 | 应该负责 | 不应该负责 |
|---|---|---|
| 数据库 | 永远必须成立的结构不变量：NOT NULL、FK、UNIQUE、枚举 CHECK、正数/范围下限 | 手机号格式、密码强度、邮箱格式等业务/UX 规则 |
| 后端 | **权威业务校验**：DTO Bean Validation + 领域服务；所有写入都必须经过 | 依赖前端已经校验过 |
| 前端 | **快速反馈**：镜像后端规则，减少无效请求；展示后端错误码 | 成为唯一校验层；比后端更宽松或更严格却不说明 |

原则：**后端是权威，数据库是最后防线，前端是体验层**。三层可以重复，但规则值必须来自同一份契约。

## 2. 当前实际状态（审计结果）

| 规则 | 前端 | 后端 | 数据库 | 状态 |
|---|---|---|---|---|
| 手机号 | `^\+?[0-9]{6,20}$`（storefront/mobile） | 曾有三种：会员 6–20 位、地址 6–20 位、结算允许空格/连字符 6–32 位 | `member.phone` 仅 NOT NULL/UNIQUE，无格式 CHECK | 已统一后端规则；DB 仍不校验格式 |
| 结算数量 | 商品/购物车最大 99 | 曾允许 1–1000 | `order_line.quantity > 0` | 已统一为 1–99；DB 只保证正数 |
| 密码 | Admin 前端“至少 8 位” | 曾完全没有长度校验 | 只存 hash，无长度 CHECK | 已补后端 8 位 |
| 员工 PIN | Admin 前端 4–12 位 | 曾只校验最少 4 位，无上限 | 只存 hash | 已补后端上限 12 |
| SKU 价格 | Admin 输入最小 0.01 | `@Positive`（>0）；批量改价 >0 | `price_amount > 0`（V131） | 已统一 |
| 邮箱 | 顾客端不采集邮箱 | `Contact.email` 可选，使用 `@Email` | 可空 | 已统一（可选字段） |
| 语言代码 | 前端只允许启用列表 | 后端有正则；另有 `enabled_locales` | `ui_translation.locale` 为 VARCHAR | **仍靠多处约定** |
| 地址 | 必填详细地址 | `@NotBlank detail`；地址服务再次校验 | NOT NULL | 基本一致 |
| SKU 编码 | Admin 输入提示 | `@Pattern(ValidationRules.SKU_CODE_PATTERN)` | `sku_code` UNIQUE | 已统一 |
| 券模板编码 | Admin 提交前校验 | `ValidationRules.COUPON_CODE_PATTERN` | `code` UNIQUE | 已统一 |
| 状态枚举 | TS union / 字符串 | Java enum | 多处 CHECK | 三层各一份，容易漂移 |
| 媒体 URL | 前端 resolveMediaUrl | `MediaUrlResolver` 校验 http(s) | TEXT | 输出已统一，输入未全部校验 |
| 优惠券面额/折扣 | Admin 输入 min/max | 服务层校验（分散） | CHECK/数量列 | 需逐项对照，尚未统一 |

## 3. 本轮已修的具体漂移

1. **新增 `PhoneNumber` 单一后端规则**
   - `server/src/main/java/com/fullmall/platform/validation/PhoneNumber.java`
   - 会员 OTP、地址、结算 Contact 统一使用同一正则
   - 修复“结算允许 `138-0000-0000`、地址接口拒绝”的不一致

2. **结算数量上限对齐**
   - `CheckoutRequest.Item` 从 `@Max(1000)` 改为 `@Max(99)`
   - 与商品详情、购物车的 99 对齐

3. **密码/PIN 后端补规则**
   - `StaffAuthService.changePassword` 增加最少 8 位
   - `StaffAuthService.setPin` 增加最多 12 位
   - 与 Admin 前端提示一致，避免绕过前端直接调 API 设置弱密码

4. **价格 0 与邮箱格式**
   - 商品 DTO 改为 `@Min(1)`，DB 增加 `sku_price_positive`（V131）
   - `Contact.email` 增加 `@Email`，可选但填了必须合法

5. **SKU / 券编码**
   - 规则加入共享 schema，后端 `@Pattern` / 服务层使用同一常量
   - Admin 在提交前用生成的 `VALIDATION` 常量校验，避免只靠后端报错

## 4. 仍未统一、建议下一步处理

1. **语言/地区枚举**
   - 用后端 `enabled_locales` + 市场配置生成前端选项；不要各写一份。
2. **状态枚举**
   - 以 OpenAPI/Java enum 为源，生成 TS 类型；DB CHECK 与 enum 做测试对齐。
3. **优惠券/促销其余规则**
   - 编码/折扣已纳入；金额、限购、有效期仍需逐项对照，建立完整规则矩阵。
4. **错误码**
   - 后端错误码应稳定；前端按 code 映射人话，不依赖 message 文本。

## 5. 单一契约（本轮已落地最小版本）

采用 **JSON Schema（Draft 2020-12）作为共享规则源**，而不是让前后端各手写一份：

- 契约文件：[validation.schema.json](docs/validation.schema.json)
- 生成前端常量：`npm run generate:validation` → [validation.ts](web/packages/api-client/src/validation.ts)
- 后端常量：[ValidationRules.java](server/src/main/java/com/fullmall/platform/validation/ValidationRules.java)
- 契约测试：`ValidationRulesContractTest` 读取同一 JSON，断言 Java 常量完全一致
- 前端不再手写手机号/数量/密码/PIN 常量，改为从 `@fullmall/api-client` 导入 `VALIDATION`

当前被共享的规则：手机号、密码长度、PIN 长度、结算数量、SKU 价格、联系人姓名长度。

验证命令：

```bash
cd web && npm run generate:validation
cd ../server && mvn -Dtest=ValidationRulesContractTest test
```

**后续升级路径**：把 JSON Schema 挂到 OpenAPI 的 `components.schemas`，用 `openapi-typescript` / `orval` 生成类型与运行时校验（项目技术决策已选 springdoc + OpenAPI Generator）。当前最小版本先解决“规则值漂移”，不引入一整套生成链。

## 6. 目标架构（OpenAPI）

- 后端 Bean Validation 注解 → OpenAPI schema；
- 前端用生成器产出类型与基础校验（或至少生成规则常量）；
- 后端错误码写入 OpenAPI，前端统一映射；
- CI 增加契约测试：同一组边界值分别打前端校验函数和后端 API，结果必须一致。

在此之前，至少做到：

- 每个规则只在一个后端常量/工具类里定义（如 `PhoneNumber.PATTERN`）；
- 前端引用同一组常量（构建期生成或接口下发）；
- DB 只保留结构不变量，业务范围由后端负责；
- 不允许“前端提示 8 位、后端接受 1 位”这种漂移。
