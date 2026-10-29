# ADR-0013: 国家与行政区划数据的来源与治理

- 状态：Accepted
- 日期：2026-10-29
- 相关：ADR-0003（中国市场优先）、ADR-0005（第三方配置入库）、ADR-0009（clean-room）、§9 验证一致性、§12 依赖与部署可交付

## 背景

当前收货地址是**三个自由文本输入框**，没有任何区划字典：

| 事实 | 证据 |
|---|---|
| `member_address` 的 region_code/province/city/district/postal_code 全为可空自由文本，无 FK、无 CHECK | V42__member_address.sql:7-12 |
| 结算地址整体塞进 `orders.address_json JSONB`，无结构约束 | V1__core.sql:168 |
| 后端只校验 recipient/phone/detail，省市区一个字节都不校验 | AddressService.java:174-184 |
| `region_code` **只写不读**：list()/get() 的 SELECT 里没有它，写进去就再也取不出来 | AddressService.java:63-68 vs :144、:163 |
| 全库 135 个迁移中不存在 region/area/division/country 表 | 对 db/migration 的 CREATE TABLE 检索，0 命中 |
| `market` 表（CN/HK/MO/TW）是币种+locale+法律主体维度，不是行政区划，且只有 4 行演示种子 | V1__core.sql:1-8、db/demo/seed.sql:1-14 |
| storefront 结算页只有一个「详细地址」输入，且没有任何页面调用已实现的 /api/v1/addresses | storefront/pages/checkout.vue（ship-address）；对 addresses/地址簿 检索 0 命中 |
| mobile 是三个手填输入 + 页面内写死「请填写省市区」 | mobile/src/pages/checkout/checkout.vue:58,216-218 |
| admin 无地址/区域相关配置页 | 对 admin/src 检索 province/district/regionCode，0 命中 |
| 地址/邮编未进 validation.schema.json | docs/validation.schema.json 中只有 phone 等地区无关规则 |
| **`member_address` 与 `orders` 都没有 country 列**：全栈无「国家」概念，无法解释「上海市」属于哪个国家，也无法按国别切换地址格式 | 对 db/migration 检索 country，0 命中；对 server/src/main/java 检索 country，0 命中；前端仅命中 i18n 文案 |
| **可开通市场写死白名单**，新增一个国家必须改代码发版 | `SystemSettingService.java:92` `MARKET_VALUES = Set.of("CN","HK","MO","TW")`、`:311-314` 校验并抛 `INVALID_DEFAULT_MARKET` |
| **运费是无条件常量 0**，没有区域/国别差异可言 | `OrderService.java:67` `FLAT_SHIPPING_FEE = 0L`、`:234-236` 非自提即取该常量 |
| 站点只有单一 `default_market`（一个值），不是「已开通市场集合」 | `SystemSettingService.java:79` DEFAULT_MARKET_KEY；`market` 表虽可多行，但设置项只允许一个生效值 |

后果不是「体验差」，而是**地址不可治理**：无法按区域算运费、无法做电子面单与自提路由、无法规范化国际地址，脏数据一旦入库不可逆。

## 决策

### 1. 唯一权威来源 = 构建期离线打包的标准数据集，运行期零外网依赖

用户给的三个选项里，「纯手填」和「纯三方服务」都不采用：

- 纯手填：地址不可治理（见背景）。
- 纯三方服务当唯一来源：违反 §12「不得假设构建机能出网」「不得要求客户手工装东西」，内网/离线交付时地址选择整块挂掉，还有按量计费与限流。

做法：区划数据以**已提交的规范化快照**随构建产物入库，由 Flyway 灌入 region 表（Flyway 是生产 schema 唯一迁移源，§1.3）。npm 数据包只在「年度刷新区划」这一次性步骤里需要，离线交付机器不需要。

### 2. region 表（新）

> ⚠️ **层级不是固定的「省/市/区」三级**。全球行政层级深度是 1～4 级且各地叫法不同（见 §8），所以 `level` 按**语义位**命名，直接复用 libaddressinput 的字段语义（S/C/D，见 §调研「按国别地址格式」），不要自造第二套词汇。

```text
code            VARCHAR(16) PRIMARY KEY   -- ISO 3166-1 alpha-2（国家）或 ISO 3166-2 / GB/T 2260 区划代码
parent_code     VARCHAR(16) REFERENCES region(code)
level           VARCHAR(16) NOT NULL      -- COUNTRY / ADMIN(=S 省州郡) / LOCALITY(=C 市) / DEPENDENT(=D 区/邮编前缀区)
depth           SMALLINT      NOT NULL    -- 1..4：该国这条链上的实际层级，前端据此决定是否渲染该级输入
country_code    CHAR(2)       NOT NULL    -- 国家锚点；按国裁剪与运费区域前缀匹配都靠它
names           JSONB         NOT NULL    -- { "zh-CN": "上海市", "en": "Shanghai" }，locale 口径同 ui_translation
name_variants   JSONB                     -- 简称/旧名/拉丁名，用于搜索与「历史名仍可命中」
pinyin          VARCHAR(128)
active          BOOLEAN NOT NULL DEFAULT TRUE
source_id       VARCHAR(32) NOT NULL      -- provenance
source_version  VARCHAR(32) NOT NULL      -- 数据快照版本/抓取日期
CHECK (level = 'COUNTRY' OR parent_code IS NOT NULL)
```

`level` 取值用 CHECK 约束成枚举（§9.2：枚举是数据库职责）。**`phone_hint` 挪到 `address_format`/`market`**，不放在区划行上——号码前缀是国家属性，不是区县属性。

关键约束：**区划会变（撤县设区、新设市）**。刷新时旧节点置 `active = FALSE`，**禁止删除** —— 历史订单与地址簿必须还能解释自己存的那个 code。

### 3. 地址写入 = 规范化文本 + 可选 code 锚点，不强 FK

`member_address` / `CheckoutRequest.Address` 保留 province/city/district 原文本，同时把 `region_code` 修成真正的锚点：

- 命中字典时，最细一级 code 必须是 region 里存在的 code；应用层校验并返回稳定错误码 `INVALID_REGION_CODE`。
- 未命中（海外新区划、字典过时）时允许 code 为空、走原文本 —— **手填兜底是功能，不是失败**。
- 不做 DB 层 FK 强约束：字典不保证覆盖全球每一级，强 FK 会把合法地址拒掉。
- `region_code` 现状只写不读的问题必须一并修掉：读路径补上 code，或明确改名废弃；不允许保留孤儿字段。

### 4. 交互 = 表单形状由后端下发，前端不猜

storefront / mobile / admin 三端统一走后端字典与格式接口，**不在前端塞第二套数据，也不在前端写死「省市区」三个框**。前端先取该国 `address_format`，再按 `required` / `fmt` 动态渲染字段与 label（见 §8）：

```text
GET /api/v1/regions/countries                     → 可选国家（active 的 COUNTRY 节点）
GET /api/v1/regions/formats?country=US            → 该国的必填集、各级 label、邮编正则、是否下拉
GET /api/v1/regions?country=CN&parent=11&depth=2  → 下一级区划（仅当 has_admin_list 为真才调用）
```

名称按 Accept-Language 取 `names` 对应 locale；**匹配与存储只用 code，名称仅展示**（HK 的中英 `sub_keys` 完全不同，见 §8）。区划未命中时允许留空 code 走原文本（§3）。

### 5. 三方地址服务 = 可选增强，永不阻塞下单

复用 ADR-0005 的 `integration_config`，新增 `capability = 'address'`（provider：google / amap / cainiao / 空）。

- 未配置：静态字典 + 手填，**功能完整可用**，对顾客无任何报错。
- 已配置：地址联想与规范化失败时静默回落到手填（用户不感知），但真实原因必须进日志/管理端（§10.2「对用户模糊，对系统精确」）。
- **不纳入** `ProductionSafetyGuard` 的强制项：支付/短信/物流缺配置可以失败退出，地址校验不行 —— 它不是交易必要条件。这是与现有生产自检策略的**有意差异**，实现时不要顺手加进去。

### 6. 明确否决「默认空表、用户填一个新增一个」

把**权威字典**变成**用户输入的影子表**，否决理由：

1. 字典失去标准性：上海市 / 上海 / 上海直辖市 / 沪 会变成四个「合法」节点，运费区域与面单反而更难匹配。
2. 可被写入侧污染：地址簿接口是顾客端可调用面，等于把主数据开放给未认证输入。
3. 全球范围维护不完：到村（居）级是 66 万+ 量级（见调研「商业数据集」），GB/T 2260 本身每年调整，靠增量手填永远追不上。

**保留一个不污染字典的出口**：新增 `region_gap_report(code_hint, name_raw, locale, count, status)` 或在 admin 侧做「字典缺失上报」列表，运营定期核对后把确实存在的新区划**补进数据来源快照**（走 §1 的刷新流程），而不是自动成为正式字典项。上报是待办，不是事实。

### 7. 校验规则单一来源（§9 硬性）

先改 `docs/validation.schema.json`（新增 address 组：区划 code 格式、按国家的邮编正则、detail 长度），再 `npm run generate:validation` 生成前端常量，Java 侧只引用 `ValidationRules`，最后补 `ValidationRulesContractTest`。

mobile 页面里手写的「请填写省市区」是**页面级校验副本**，属 §9.1 禁止项，收进 schema 时必须一起清掉，并全仓搜索旧字面量确认无残留。

### 8. 跨境：地址表单由「国家」驱动，不是「省市区」驱动（核心）

**「国外用户没有省市区怎么办」——正解是他们本来就没有这三样。** 「省/市/区」是中国大陆的心智模型，写死成三个输入框对多数国家都错。按 libaddressinput（Google 开源、驱动 Android/Chromium 地址表单，Apache-2.0，pushed 2026-05-15、633★）定义的**语义位**建模即可，其字段缩写：

```text
N 收件人 · O 公司 · A 街道地址行 · D 依赖 locality（市内区/郊区）
C 城市/locality · S 一级行政区（省/州/郡/岛） · Z 邮编 · X 分拣码
```

**已在本次取到并验证的证据（libaddressinput 官方 wiki 原文，逐字核对）：**

- 字段语义缩写 `N/O/A/D/C/S/Z/X` 的定义（上方代码块）。
- **美国**：`require = "ACSZ"`、`fmt` 为姓名/公司/地址行/城市 州 邮编 的顺序、邮编正则与 Java 正则兼容；且**州级数据自带邮编前缀正则**（`data/US/CA` 给出加州邮编前缀集合）⇒ 邮编可按行政区二次收窄。
- **层级可达 4 级**：wiki 原文举例 `data/BR/AC/Acrelândia`（国家/州/市）。
- **根西岛**（Guernsey）`fmt` 里没有 `%S`：**根本没有行政区这一级**，且国家名是模板中的字面量 ⇒「省必填」在该国无法成立。
- **`data/ZZ` 兜底键**：某国缺字段时回退到 ZZ ⇒ 我们必须实现同一兜底，禁止拿中国模板凑数。
- **同一行政区的 sub_keys 随语言变化，连顺序都变**（实测：`data/HK` 声明 `lang=zh-Hant` 却返回**英文名** `Kowloon~Hong Kong Island~New Territories`；`data/HK--en` 为 `Hong Kong Island~Kowloon~New Territories`，**顺序不同**）⇒ 区划必须**按语言显式抓取**，且**匹配只能用 code，不能用名称或顺序**。注意这与官方 wiki 示例文字不一致 —— **以实测为准，不要照抄文档示例**。

**已实测验证（2026-10-29 经代理直连 Google Address Data Service，共抓 23 个 key）**

| 国家/地区 | `require` | `fmt` 模板 | 邮编正则 | 一级行政区 | 名称类型 / 备注 |
|---|---|---|---|---|
| CN | ACSZ | `%Z%n%S%C%D%n%A%n%O%n%N` | `\d{6}` | 34 | 省→市→区，**严格三级**（北京市→16 个区） |
| HK | **AS** | `%S%n%C%n%A%n%O%n%N` | **无邮编** | 3 | `area`；C 出现在 fmt 但不在 require |
| MO | **A**（仅地址行） | `%A%n%O%n%N` | **无邮编** | 0 | **最简形状**：只要求一条地址行 |
| TW | ACSZ | `%Z%n%S%C%n%A%n%O%n%N` | `\d{3}(\d{2,3})?` | 22 | `county` |
| US | ACSZ | `%N%n%O%n%A%n%C, %S %Z` | `(\d{5})(?:[ \-](\d{4}))?` | **62**（含属地/军邮） | `state`；**州级自带邮编前缀**：CA=`9[0-5]|96[01]`、NY=`1[0-4]|06390|00501|00544`、TX=`7[5-9]|885|73301|73344` |
| JP | ASZ | `〒%Z%n%S%n%A%n%O%n%N` | `\d{3}-?\d{4}` | 47 | `prefecture`；**邮编在模板最前且含 〒 字面量**；東京都自带前缀 `1[0-8]|19[0-8]|20` |
| KR | ACSZ | `%S %C%D%n%A%n%O%n%N%n%Z` | `\d{5}` | 17 | `do_si`；**邮编放最后** |
| SG | **AZ** | `%N%n%O%n%A%nSINGAPORE %Z` | `\d{6}` | 0 | **无省市**，国家名是模板字面量 |
| AE | **AS** | `%N%n%O%n%A%n%S` | **无邮编** | 7 | `emirate` |
| MY | ACZ | `%N%n%O%n%A%n%D%n%Z %C%n%S` | `\d{5}` | 16 | **邮编在州之前** |
| TH | **(无 require → 回退 ZZ)** | `%N%n%O%n%A%n%D %C%n%S %Z` | `\d{5}` | 77 | |
| PH | **(无 require → 回退 ZZ)** | `%N%n%O%n%A%n%D, %C%n%Z %S` | `\d{4}` | 82 | |
| ID | AS | `%N%n%O%n%A%n%C%n%S %Z` | `\d{5}` | 34 | 有邮编但**不要求填** |
| VN | AS | `%N%n%O%n%A%n%C%n%S %Z` | `\d{5}\d?` | 63 | 同上 |
| DE | ACZ | `%N%n%O%n%A%n%Z %C` | `\d{5}` | 0 | **无行政区级** |
| FR | ACZ | `%O%n%N%n%A%n%Z %C` | `\d{2} ?\d{3}` | 0 | **无行政区级**，姓名顺序是「公司在前」 |
| GB | ACZ | `%N%n%O%n%A%n%C%n%Z` | 超长正则（含 BFPO 军邮） | 0 | **无行政区级** |
| AU | ACSZ | `%O%n%N%n%A%n%C %S %Z` | `\d{4}` | 9 | `state` |
| CA | ACSZ | `%N%n%O%n%A%n%C %S %Z` | 加拿大字母数字正则 | 13 | `upper=ACNOSZ`（含姓名） |
| BR | **ASCZ** | `%O%n%N%n%A%n%D%n%C-%S%n%Z` | `\d{5}-?\d{3}` | 27 | `state`；**4 级且 D 在 C 之前** |
| IN | ACSZ | `%N%n%O%n%A%n%T%n%F%n%L%n%C %Z%n%S` | `\d{6}` | 36 | **模板含 `%T`/`%F`/`%L` 三个九字段以外的占位符** |
| ZZ（兜底） | **AC** | `%N%n%O%n%A%n%C` | **无邮编** | 0 | `province`（缺省值） |

**实现时必须吸收的 7 个坑（全部实测撞到，不是推演）**

1. **CN 的省级列表含「澳门」**（34 项里含澳门）→ 直接建省表会让澳门既是「中国的一个省」又是 `market=MO`，必须显式排除或标注归属。
2. **HK 默认请求声明 `lang=zh-Hant` 却返回英文名**，且 `HK--en` 的 `sub_keys` **顺序不同** ⇒ 按语言显式抓取，**只用 code 匹配**。
3. **IN 的 fmt 含 `%T`/`%F`/`%L`**，不在 N/O/A/D/C/S/Z/X 九字段内 ⇒ 模板解析器**必须对未知占位符显式失败**，不能当字面量渲染（否则用户会看到「%T」）。
4. **TH / PH 没有 `require`** ⇒ 必须回退 `data/ZZ`，而 ZZ 是 `AC`（地址行 + 城市）、**无邮编** ⇒ 「未收录国家」也不能省掉城市，更不能强加邮编。
5. **MO 只需一条地址行**（`require=A`）⇒ 校验必须有「只填地址行」的合法路径。
6. **US 一级行政区是 62 项**（不是「50 州」）⇒ 下拉数据必须来自数据集，不能按常识写死。
7. **US/JP 的州与都道府县自带邮编前缀正则，且 JSON 有 `sub_zips` 字段** ⇒ 区域收窄校验可实现，但必须把 `sub_zips` 一起抓。

**结论（实测证伪「固定三级」）**：这 23 个 key 的 `require` 就有 6 种组合（`A` / `AC` / `AS` / `AZ` / `ACZ` / `ACSZ` / `ASCZ`），形状从「只填一条地址行」(MO) 到「4 级 + 邮编在中间」(BR) 全有。把 province/city/district 三个框铺到全球必然出错。

#### 8.1 `address_format` 表 = 国别表单的唯一来源

```text
country_code     CHAR(2) PRIMARY KEY REFERENCES region(code)   -- ISO 3166-1
required         VARCHAR(16)  NOT NULL   -- libaddressinput 风格字段缩写串；US 已验证为 ACSZ，其余由快照生成，禁止手写国别样例
upper            VARCHAR(16)            -- 哪些字段自动大写；取自快照，不凭常识填
fmt              VARCHAR(64)  NOT NULL  -- 打印顺序模板（%N %O %A %C %S %Z 组合）；US 已验证，其余由快照生成
zip              VARCHAR(64)            -- 该国邮编 Java 正则（进 validation schema 的唯一值源）
zipex            VARCHAR(128)           -- 示例值，直接做 input placeholder 与错误提示
admin_area_name  VARCHAR(32)            -- 该级本地叫法（wiki 原文示例 state / island，缺省 province）→ 用作 label，不再一律写「省」
locality_name    VARCHAR(32)
dependent_name   VARCHAR(32)
has_admin_list   BOOLEAN NOT NULL       -- 快照里该国有 sub_keys（US 州清单已验证）→ 下拉；否则文本
admin_optional   BOOLEAN NOT NULL       -- 该级是否可留空（根西岛已验证：fmt 中无 %S，即无行政区级）
locale_names     JSONB                  -- 各级标签的多语言文案 key（复用 ui_translation，不在前端写死）
```

> **注意 has_admin_list**：只有 S 在快照里带 `sub_keys` 时才做下拉（US 的州清单已由 wiki 证实为封闭枚举）。多数国家的市（C）**不是封闭枚举**且随时改名，所以正确形状是：**S 下拉（有枚举才下拉）+ C/D 文本输入 + 可选地址联想（§5 的 provider 增强）**，不是固定三级 cascader。中国大陆是我们唯一「三级都有标准 code」的场景，它是特例，不是通用形状。

#### 8.2 「开通哪个地区就必须有哪个地区的省市区数据吗」——**分层回答**

**是：开通一个市场必须有该国家的**「格式规则 + 一级行政区清单（若该国是封闭枚举）」**；不是：不必须逐级补齐到市/区。**

三档最小开通集（写进文档，作为验收口径）：

| 档 | 内容 | 要求 |
|---|---|---|
| **必须（开通前置）** | `address_format` 该国 1 行 + ISO 3166-1 国家节点 | 缺则**不允许开通该市场**；启动/自检明确报错「国家 X 无地址格式规则」，禁止静默按中国模板跑 |
| **应该（该国是封闭枚举时）** | 一级行政区清单（US 51、JP 47、AE 7、TH 77 府、KR 17） | 有 enum 的国家必须补齐，否则退化成文本输入（可用但易脏） |
| **可以不做（按国家取舍）** | 市/区/邮编前缀逐级树（DE 市镇、FR commune、JP 市町村、BR 市镇 5000+） | **不做全量树**：数据量大、变更频繁，用「文本 + 地址联想（§5 provider）」替代；邮编正则 + 国家即可 |

所以不是「按国别分别设置一套完整区划树」，也不是「所有国家共用一套」，而是：**格式与一级枚举按国配置，深层区划按国按需**。

#### 8.3 运费/税必须与国别打通（当前是硬缺口）

- `FLAT_SHIPPING_FEE = 0L` 常量 + `MARKET_VALUES` 写死白名单 = **加一个国家要改代码发版、运费无差异**。
- 本 ADR 只规定：`region` 提供 `level/parent_code/country_code` 以供运费区域**前缀匹配**（如 `9[0-5]` 加州邮编前缀 → 美西费率），`member_address` 必须补 `country_code`（NOT NULL，默认取站点 `default_market`），否则无法回答「这个地址在哪个国家」，运费与报关都无从谈起。
- 运费模板、税费、报关字段属独立 ADR，本轮不做（避免范围蔓延）。

#### 8.4 已开通市场从常量改为数据

`MARKET_VALUES = Set.of("CN","HK","MO","TW")` 属 §12 反模式（人工步骤 + 改代码才能扩容）。改为：`market.active` 决定可开通集合，`default_market` 校验改为「必须在 active 市场内」，新增国家 = 后台配置 + 该区划/格式快照迁移。

⚠️ **市场本身的结构性问题（币种/语言/通道全绑在一行、订单无 market 维度）见 [ADR-0014](0014-market-locale-destination.md)**：本节只讨论「开通一个国家要准备哪些地址数据」，不重复讨论三轴建模。

#### 8.5 libaddressinput 的离线注意（§12 硬性）

其**元数据不在仓库里**，是从 `chromium-i18n.appspot.com/ssl-address/data/<CC>` 在线取。

> **取数可行性（实测）**：该域名直连不通，但**经 HTTP 代理可达**（2026-10-29 用 `http(s)_proxy=http://192.168.1.15:7897` 成功抓取 23 个 key，产物共 138KB）。所以「抓快照」这一步是可执行的；只是**客户运行期不得依赖它**（内网更不可达）。`python3` + `urllib` 即可，无需额外依赖。

因此：

- 采用其**词汇与模型**（Apache-2.0 的 Java 代码可用于校验，`com.google.i18n.phonenumbers` 风格的 `AddressMetadata`/`ValidationRuleSupplier`）。注意：**若用其默认 supplier，运行期会需要外网**，违反离线交付。
- 本项目做法：**把元数据一次性抓取为自有快照**（构建/运维期），落成 `address_format` 表；运行期完全不访问 Google 服务。
- 校验逻辑若要省事可用其 Java 库（Apache-2.0，干净）读我们自己的快照；不自研正则匹配器。

## 实现状态（2026-10-29）

| 项 | 状态 |
|---|---|
| V143 `region` + `address_format` 表 | **已实施**，全新库 146 个迁移重放 + 种子通过 |
| `scripts/fetch-address-metadata.py`（经代理抓 ADS → SQL 快照） | **已实施**；已生成 9 国格式 + 741 个区划节点（V144），V146 补中文国名 |
| 接口 `/api/v1/regions/countries`、`/regions/formats`、`/regions` | **已实施**；`RegionHttpTest` 5 项通过（含"国家列表不能为空"与"未知国家 404"回归） |
| 按国别必填校验（`AddressService.requireCountryFormat`） | **已实施**：缺格式直接拒绝、按 `required` 校验 S/C/Z、按 `zip` 校验邮编 |
| storefront 动态地址表单（`AddressForm.vue` + `useMarketCountry`） | **已实施**；Playwright 2/2 通过（日本 47 项都道府县、新加坡无省市、中国三级 + 真实下单） |
| mobile 动态地址表单（国家 picker + 形状驱动字段） | **已实施**；mobile-h5 e2e 通过 |
| 前端去掉写死的市场白名单（`useSite` 不再维护 CN/HK/MO/TW） | **已实施** |
| 深层区划（中国市级以上的区县全量、其他国家市镇级） | **未做**：目前 CN 到市级（471 个 LOCALITY 节点），区县靠文本 + `%D` 输入框 |
| 下游计价：`sku_price` 按市场覆盖价 + `shipping_rate` 市场×目的地×重量阶梯 + SKU 补 HS/原产地/净重 | **已实施**（批次 D，V147）；`MultiMarketPricingTest` 7 项通过 |
| 税与报关（批次 E，V148）：`tax_rule` + `order_tax_line` + 报关单导出 | **已实施**；`TaxAndCustomsTest` 5 项通过 |

### 实施中修掉的三个真 bug（都属"静默兜底"类）

1. **国家列表恒为空**：查询国家时 `countryCode` 为 null，SQL 里 `country_code = ?` 恒不成立，且基串残留占位符导致参数缺失 → 500。已由 `RegionHttpTest.countryListIsNotEmpty` 守护。
2. **前端拿错国家码**：`loadFormat` 里用 `props.modelValue.countryCode`（父组件下一轮才更新）→ 下级区划请求国家为空 → 下拉永远空。
3. **默认国家没写进表单**：mobile 只设了 picker 索引没设 `form.countryCode` → 提交被"请选择国家/地区"拦下。

## 调研结论（候选、许可证、活跃度）

数据取自 2026-10-29 对 npm registry、api.npmjs.org、api.github.com、Maven Central 的实测，并用 `npm pack` + `tar` **解包核对随包 LICENSE 原文**，非记忆。下载量为 last-month，窗口 2026-09-28 ~ 2026-10-27。

### 许可证性质与「fork 能否解决冲突」

| 许可证 | 传染性 | 对闭源/商用交付的实际义务 |
|---|---|---|
| MIT / BSD / Apache-2.0 | **无传染**（permissive） | 保留版权声明与许可证文本；不要求开源，不要求同名许可 |
| WTFPL | **无传染**，且比 MIT 更松（非 OSI 认证、无担保条款） | 第 0 条即「你就 DO WHAT THE FUCK YOU WANT TO」，零义务 |
| GPL / AGPL | **有传染**（copyleft） | 链接进核心即要求衍生作品同许可开源；ADR-0009 已禁止 |
| ODbL-1.0 | **share-alike**（数据库版 copyleft，Open Data Commons） | 内部使用可以；对外再分发衍生数据集须署名 + 以 ODbL 开放 |

**结论：fork 既不能、也不需要解决 `china-division` 的许可证冲突。**

1. **不需要**：实测 npm 发布者 `_npmUser = modood <modood@qq.com>`，与仓库 owner 一致 ⇒ 授予 WTFPL 的正是**版权人本人**，README 徽章也自标 WTFPL。因此 `package.json` 里 `"license": "MIT"` 是**元数据写错**，不是授权缺口。按最保守口径取 WTFPL 即可合法使用，而 WTFPL 覆盖 MIT 所允许的一切（闭源商用、修改、再分发），没有任何"权限"需要靠 fork 争取。
2. **不能**：fork 不改变版权归属，**只有版权人能重新授权**。我们不是 modood，fork 后自贴 MIT = 对第三方作不实授权声明，合规姿势比现状更差；同时违反 §12「禁止使用来路不明的包；引入依赖需固定版本并记录来源」。
3. **真正的解法是本 ADR §1 的结构**：不把该 npm 包作为运行期依赖分发。构建期一次性取数据 → 抽成规范化快照（事实数据）提交进仓库 → Flyway 灌库。交付物是我们自己的数据表，快照内附 `source_id` / `source_version` 与许可证说明，SBOM 中不会出现 `china-division`，也不会出现 world-countries。
4. 区划数据本身是**政府公开事实数据**（国家统计局 / 国家地名信息库 / GB/T 2260），受保护的是汇编表达而非事实；保留来源与抓取日期是为了可追溯与年度 diff，不是因为我们缺授权。

### 体积实测（避免"数据包很大"的误判）

| 需要的内容 | 文件 | 实测字节 |
|---|---|---|
| 省市区三级（我们只需要这些） | china-division `dist/pca-code.json` | **136,996** |
| 同上（CSV 形态） | `provinces.csv + cities.csv + areas.csv` | 532 + 7,580 + 85,403 |
| province-city-china 三级 | `province.csv + city.csv + area.csv` | 779 + 8,564 + 76,525（`area.json` 2,847 条区县） |
| 村级（**不需要**，才是体积大头） | china-division `dist/villages.json` | 84,894,055 |

三级字典约 **140KB**：灌库、随仓库提交、年度 diff 成本都很低。「不采用五级全量」的理由（村级 66 万+ 量级且非必要层级）依然成立，但与包大小无关。

### 中国大陆区划

| 候选 | 版本 | npm 许可证 | 最近发布 | 上月下载 | 仓库状态 | 结论 |
|---|---|---|---|---|---|---|
| china-division | 2.7.0 | package.json 写 **MIT**，但 **npm tarball 内随包的 LICENSE 原文实测为 WTFPL**（详见 §许可证性质与 fork 问题） | 2023-09-13 | 46,354 | modood/Administrative-divisions-of-China：20,989 stars、pushed 2025-12-27；npm 发布者 `_npmUser = modood <modood@qq.com>` = 版权人本人；README 徽章自标 WTFPL | 数据最全（五级），但**数据快照截止 2023-06-30**，比 province-city-china 老约 3 年。冲突性质是元数据写错，**按最保守口径取 WTFPL 即可合法使用，无需 fork** |
| province-city-china | 9.0.0 | package.json MIT，仓库根 LICENSE 原文实测 MIT（Copyright (c) 2019 wabg），**但 npm tarball 内不含 LICENSE 文件** | 2026-10-11 | 13,184 | uiwjs/province-city-china：3,069 stars、pushed 2026-10-11；经 GitHub Actions **trustedPublisher（OIDC）** 发布，maintainers = wcjiang / uiwjs | **采用为默认来源**：README 记录数据更新至 **2026-10-09**，并已改用「国家地名信息库」官方接口，比 china-division 新约 3 年。tarball 缺 LICENSE 文本的缺口，由我们在生成的快照里自行附许可证与来源记录来消除 |
| area-data | 5.0.6 | MIT | 2018-04-18 | 4,200 | dwqs/area-data：480 stars，仓库**已 archived**（2022-03-21） | **淘汰**：停更 8 年且归档 |
| china-area-data | 5.0.1 | MIT | 2020-07-01 | 16,824 | registry 无 repository 字段 | **淘汰**：停更 6 年 |

### 国家与全球

| 候选 | 版本 | 许可证 | 最近发布 | 上月下载 | 仓库 | 结论 |
|---|---|---|---|---|---|---|
| world-countries | 5.1.0 | npm 元数据 **ODbL-1.0**（share-alike）；**其 LICENSE 文件原文本次未取到**（raw.githubusercontent 三次超时） | 2025-02-26 | 1,132,855 | mledoze/countries：6,274 stars、pushed 2026-10-22 | **降为备选**。ODbL 是本议题里唯一带「类传染」（share-alike）义务的许可：内部使用可以，但对外再分发衍生数据集须署名 + 以 ODbL 开放。闭源商业交付应避开这一不确定性 |
| i18n-iso-countries | 7.14.0 | **MIT（已验证 npm 元数据）** | 2026（活跃） | 10,933,125 | michaelwittig/node-i18n-iso-countries | **改为国家层默认来源**：只做 ISO 3166-1 多语言名，MIT 无 share-alike，直接绕开 world-countries 的 ODbL 问题 |
| GeoNames dump | 每日 | 搜索结果为 CC-BY 4.0（署名可商用） | 每日 | — | download.geonames.org/export/dump/ | 备选：需要市级以下/邮编/经纬度全量时用。**本次未能抓取官方许可证正文页**，采用前必须人工确认，不得凭搜索摘要写进依赖登记 |
| 高德行政区划 API / 菜鸟国际地址库 | — | 商业/开放平台条款 | — | — | — | 只做 §5 的可选 provider，不作为数据来源。高德 web 服务免费额度据第三方转述为 1000 次/日、2 QPS（**未从官方页核实**） |
| 商业区划数据集（qqzeng 等） | — | 商业授权（三级省市区基础版 ¥68；乡镇 4.2 万+、村级 66 万+） | — | — | — | **不采购**：省市区三级用国标开源数据已足够；村级不是收货地址字典的必要层级 |

### Java 生态（关键结论）

Maven Central 检索 `administrative division china`、`china area`、`country code java library`：**0 命中**。

- 即 **Java 侧没有现成的中国行政区划数据集 artifact**，不存在「加个 Maven 依赖就完事」的成熟方案（符合 §2 的调研义务，但结论是「没有」）。
- `com.neovisionaries:nv-i18n:1.29` 在 Maven Central 存在，可作为国家/语言码枚举的辅助；**其 POM 许可证块本次未取到**（Maven Central 直连取包失败、本机无 curl），不写成结论。
- 落地路径因此是「**npm 数据包 → 一次性生成规范化快照（JSON/CSV）→ Flyway 灌库**」，Java 不引区划数据依赖。区划刷新是数据运维，不是构建依赖变更。

## 后果

- 新增：区划 + 地址格式快照生成脚本（Node，构建期/运维期一次性）、1 组 Flyway 迁移（`region`、`address_format`、`member_address.country_code`）、只读字典/格式接口、三端**动态地址表单**改造（不是加一个 cascader 那么简单：字段集、label、必填、邮编正则都随国家变）。
- 必须补 `validation.schema.json` 的 address 规则 + 生成文件 + `ValidationRules.java` + 契约测试（§9.3 流程一次走完）。
- 每年一次「区划刷新」运维动作（国家统计局一般在当年 9 月前后调整县及以上代码，据设管司暂行规定转述，**未从官方页核实**）；刷新 = 重跑生成脚本 + 一条新迁移，旧节点置 active=FALSE 不删。
- 顺带暴露两个既有缺口，需同轮或紧随处理：storefront 完全没接已实现的会员地址簿；`region_code` 孤儿字段。
- 运费按区域本 ADR 只预留结构（`level`/`parent_code`/`country_code` 支持前缀匹配）。`FLAT_SHIPPING_FEE = 0L`（[OrderService.java:67](../server/src/main/java/com/fullmall/order/app/OrderService.java#L67)）与 `MARKET_VALUES` 写死白名单（[SystemSettingService.java:92](../server/src/main/java/com/fullmall/setting/app/SystemSettingService.java#L92)）是跨境的硬阻塞，见 §8.3/§8.4；运费模板、税费、报关另立 ADR。

## 验证状态（已做并验证 / 已做未验证 / 未做）

| 项 | 状态 |
|---|---|
| 现状盘点（表结构、校验、三端、OpenAPI、validation schema、无 region/country 表、无字典接口） | **已做并验证**（逐项文件+行号，见背景表） |
| npm 版本/许可证/发布日期/上月下载量 | **已做并验证**（registry.npmjs.org + api.npmjs.org 实测） |
| GitHub stars/pushed/archived | **已做并验证**（api.github.com 实测，含 dwqs/area-data 已 archived） |
| Maven Central 无中国区划 artifact | **已做并验证**（solrsearch 实测 0 命中） |
| china-division 许可证冲突 | **已做并验证（已升级为强证据）**：上游仓库 `LICENSE` 原文 = WTFPL；**`npm pack` 解出的 tarball 内 `package/LICENSE` 原文也是 WTFPL**，而同包 `package.json` 写 `"license": "MIT"` ⇒ 确证是元数据写错而非嗅探误判 |
| 冲突方是否版权人本人 | **已做并验证**：`_npmUser = maintainers = modood <modood@qq.com>`，与 GitHub 仓库 owner 一致 ⇒ WTFPL 由权利人本人授予，使用无需再授权 |
| province-city-china tarball 缺 LICENSE 文件 | **已做并验证**：`tar -xzf` 后 `package/LICENSE` 不存在；仓库根 LICENSE 原文为 MIT（Copyright (c) 2019 wabg） |
| province-city-china 经 OIDC 受信发布 | **已做并验证**（`_npmUser.trustedPublisher.id = github`） |
| 三级数据实测体积（137KB 级） | **已做并验证**（`tar -tzvf` 逐文件字节数） |
| libaddressinput：Apache-2.0、633★、pushed 2026-05-15；字段语义、US `require=ACSZ` 与 fmt、层级可达 4 级、根西岛无 `%S`、`data/ZZ` 兜底、HK 中英 `sub_keys` 不同 | **已做并验证**（api.github.com + 官方 wiki 原文取到） |
| libaddressinput 的元数据**不在仓库内**，须在线取 ADS | **已做并验证**（README 原文 + 仓库 contents 无 metadata 目录）⇒ 必须转成自有快照，否则运行期违反 §12 |
| 各国 `address_format` 具体取值（SG/AE/JP/TH/DE/FR/GB 的层级与邮编） | **未做**：ADS 在本会话网络下 15 国全部取数失败；§8.1 已禁止手写样例，须实施期抓取生成 |
| 全栈无 `country` 列、`MARKET_VALUES` 写死、运费常量 0 | **已做并验证**（grep 0 命中 + 具体行号，见背景表） |
| 数据年份对比（china-division 2023-06-30 vs province-city-china 2026-10-09） | **已做并验证**（各自 tarball 内 README 原文） |
| 高德免费额度 1000 次/日、2 QPS | **已做未验证**（第三方转述，官方页本次取不到） |
| Google Address Validation 定价与免费层 | **已做未验证**（仅取到区域化定价页片段，官方 usage-limits 页抓取失败） |
| GeoNames = CC-BY 4.0 | **已做未验证**（搜索摘要 + 二手资料，官方页未取到正文） |
| nv-i18n 许可证 | **已做未验证**（POM 许可证块未取到） |
| ~~WTFPL 与 SBOM 扫描口径冲突需要 Legal 裁决~~ | **已消解**：授权来自版权人本人（WTFPL 无义务），且我们不把该包作为依赖分发（只落自己的快照）。SBOM 侧只需在生成脚本注释里写明 provenance |
| ODbL（world-countries）对再分发的影响 | **不再阻塞**：国家层改用已验证 MIT 的 i18n-iso-countries，world-countries 降为备选。若日后启用（需要旗帜/多语言全量时），必须先完成 Legal 复核 |
| GeoNames CC-BY 4.0 署名条目 | 若启用需人工确认许可并加署名；当前不引入，故不阻塞 |
| 任何代码/迁移/接口实现 | **未做**（本 ADR 只定方案） |

## 替代方案

- **纯手填（现状）**：地址不可治理，脏数据不可逆；否决。
- **自维护全球区划表 + 用户填一个新增一个**：见 §6；否决。
- **三方地址 API 作为唯一来源**：违反 §12 离线交付要求，且有费用/限流/封禁风险；降级为可选增强。
- **整包 china-division 五级数据**：村级不是收货字典的必要层级（三级实测仅约 140KB，村级 villages.json 85MB 才是大头）。只用三级，或直接用 province-city-china。
- **前端内置 JSON 字典**：违反 §9.1 单一来源（前端一套后端一套），storefront SSR 与 mobile 各存一份必然漂移；否决。
- **全球统一「省/市/区」三级表单（现状的延伸）**：已被 libaddressinput 的 US / 根西岛 / 巴西三种不同形状证伪（§8）——对多数国家要么问不存在的问题，要么把必填做成选填；否决。
- **每个开通国家都灌完整区划树**：德/法/日/巴的市镇级为万级且频繁改名，年度 diff 与存储成本不可控；改为 §8.2 的「格式 + 一级枚举必须、深层按需」。
