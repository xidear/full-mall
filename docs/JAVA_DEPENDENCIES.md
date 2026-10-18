# Java 依赖管理与三方包松散集成

## Composer 对应物

| PHP | Java |
|---|---|
| Composer | Maven（主流）或 Gradle |
| `composer.json` | `pom.xml`（Maven）或 `build.gradle` |
| Packagist | Maven Central |
| `composer require` | 在 `pom.xml` 增加 `<dependency>` |
| `composer.lock` | `pom.xml` + 固定版本 + `mvn dependency:tree`；Maven 没有完全等价的 lock，可用 BOM/版本锁定/SBOM |
| `vendor/` | `~/.m2/repository` |
| autoload | classpath + `ServiceLoader` / Spring 容器 |
| PSR 接口 | 自定义 SPI 接口 |

本项目已经使用 Maven。常用命令：

```bash
mvn dependency:tree
mvn versions:display-dependency-updates
mvn dependency:analyze
mvn test
mvn -Pprod package
```

建议生成 Maven Wrapper（`mvn wrapper:wrapper`），保证客户机器不依赖全局 Maven 版本。

## 松散集成方式

### 1. 独立 adapter module + SPI

```text
server/
  payment-api/            # 只放接口、DTO、异常，不依赖任何厂商 SDK
  payment-mock/           # 开源默认
  payment-wechat/         # 微信支付 SDK
  payment-alipay/         # 支付宝 SDK（含碰一碰云 API）
  payment-unionpay/       # 银联/云闪付
  payment-pos-vendor-x/   # 收银机/聚合支付厂商
  app/                    # 业务，只依赖 payment-api
```

核心接口示例：

```java
public interface PosPaymentProvider {
    String provider();
    PosPaymentIntent create(PosPaymentRequest request);
    PosPaymentResult query(String providerOrderNo);
    void cancel(String providerOrderNo, String reason);
    PosRefundResult refund(PosRefundRequest request);
    void handleWebhook(String payload, String signature);
}
```

业务层只依赖 `PosPaymentProvider`，不接触微信/支付宝/银联的 DTO。每个 adapter 负责：

- SDK 调用与签名；
- 把厂商状态翻译成内部状态；
- 把厂商错误码翻译成内部错误码；
- webhook 验签、幂等、重放；
- 对账文件解析。

### 2. Spring 条件装配

```java
@Component
@ConditionalOnProperty(name = "pos.payment.provider", havingValue = "alipay")
public class AlipayPosProvider implements PosPaymentProvider { ... }
```

`application.yml`：

```yaml
pos:
  payment:
    provider: mock
```

开源默认 `mock`，客户配置真实 provider 后编译/启动。不要把厂商 SDK 直接写进订单/库存领域。

### 3. 动态插入/热插拔

- 普通场景：Maven 多模块 + 配置选择即可，不需要 OSGi。
- 需要客户自行放入 jar：用 `ServiceLoader` 或 Spring `ApplicationContext` 扫描，定义插件目录和版本兼容规则；必须限制来源、签名、classloader 隔离。
- 需要真正隔离/灰度：把每个厂商 adapter 做成独立服务（Docker），通过内部 HTTP/消息通信；这比 OSGi 更容易运维。
- 不建议为了“插件化”引入 OSGi/复杂类加载器；先把 SPI 和 adapter module 做好。

## 支付/收银厂商 SDK 选型

| 能力 | 推荐接入方式 | 说明 |
|---|---|---|
| 微信支付 | 微信支付 Java SDK / REST v3 | 官方 SDK，证书/密钥管理 |
| 支付宝 | 支付宝 Java SDK / REST | 官方 SDK；碰一碰需要终端/ISV 资质 |
| 银联/云闪付 | 银联开放平台 SDK | 商户资质、终端号 |
| 聚合扫码/收银 | 收钱吧、拉卡拉、富友等厂商云 API | 一个 adapter 支持多种扫码/刷卡 |
| 支付宝碰一碰 | 认证 POS 终端 + 支付宝 ISV 云 API | 不是纯后端接口，需要硬件/资质 |
| 现金 | 内部账本，不接三方 | 现金收付、钱箱、交接班 |
| 会员卡/储值 | 内部 ledger | 不依赖支付厂商 |
| 积分/优惠券 | 内部 ledger | 核销/锁定/回滚必须幂等 |

## 依赖治理

- 固定版本，禁止 `LATEST`/`RELEASE` 动态版本。
- 用 BOM/`dependencyManagement` 统一版本；adapter module 只暴露最小依赖。
- 厂商 SDK 用 `optional` 或独立 module，避免传递给不需要的部署。
- CI：`mvn dependency:tree`、License/SBOM 扫描、Trivy/OWASP Dependency-Check。
- 每个 adapter 有 mock 和 sandbox 测试；Mock 不访问外网。
- 密钥从环境变量/密钥管理读取；日志脱敏；webhook 验签/防重放。
- 真实 SDK 的许可证、终端认证、商户资质单独记录。

## POS 终端与后端边界

- 后端负责：销售单、组合支付记录、积分/券 ledger、现金账、班次、对账、报表。
- 终端/收银机负责：扫码枪、NFC/碰一碰、钱箱、打印、网络中断本地队列。
- 若终端是 Android，可用 Kotlin/Java 或厂商 SDK；若终端是厂商封闭系统，按厂商云 API 接入。
- 后端绝不能假设“碰一碰”只是生成一个二维码；它依赖认证终端和支付网络。

## 结论

- Java 的 Composer 是 **Maven**，包来自 Maven Central；多模块 Maven 工程就是我们的“包生态”。
- 三方支付采用 **SPI + adapter module + 配置开关 + Mock**，不让 SDK 污染核心领域。
- 需要热插拔时优先独立 adapter 服务，不要盲目上 OSGi。
- POS/碰一碰的硬件和资质问题必须在计划里单列，不能只写代码接口。