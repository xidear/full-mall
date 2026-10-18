# 第三方集成与 Mock 策略

## 原则

- 所有外部服务通过 SPI/适配器接入，业务代码不直接依赖厂商 SDK。
- 开源默认使用 Mock；真实适配器由配置启用，缺配置时启动校验失败。
- 不在仓库提交任何 AccessKey、Secret、商户号、证书。
- 所有发送/上传记录落库，失败可重试、可审计。
- 真实厂商 SDK 必须固定版本、记录许可证、CI 做依赖与 License 扫描。

## 已实现：通知渠道 Mock

`notification` 模块已经实现：

- 事件：支付成功、备货完成、订单发货、订单完成。
- 渠道：站内通知、短信、邮件、微信、APP 推送。
- 每个渠道通过 `NotificationChannelSender` 接口适配。
- 当前全部为 `MockNotificationChannelSender`，只写发送记录和脱敏日志，不产生外部副作用。
- 运营后台「通知中心」可查看事件、接收方、渠道、发送状态、失败原因，并标记已读。
- 记录表：`notification`，包含状态（PENDING/SENT/FAILED）、失败原因、已读时间、发送时间。

后续接真实渠道时，只需新增实现类并配置开关，不改业务事件代码。

## 规划：短信

接口建议：

```java
public interface SmsSender {
    String provider();
    void send(String phone, String templateCode, Map<String, String> params);
}
```

适配器：

- 阿里云短信：官方 SDK，配置 AccessKeyId/AccessKeySecret/SignName/TemplateCode/RegionId。
- 腾讯云短信：官方 SDK，配置 SecretId/SecretKey/SdkAppId/SignName/TemplateId/Region。
- 开发/演示：`MockSmsSender`，仅记录脱敏手机号和模板参数。

配置键建议：

```text
SMS_PROVIDER=mock|aliyun|tencent
SMS_ALIYUN_ACCESS_KEY_ID=
SMS_ALIYUN_ACCESS_KEY_SECRET=
SMS_ALIYUN_SIGN_NAME=
SMS_TENCENT_SECRET_ID=
SMS_TENCENT_SECRET_KEY=
SMS_TENCENT_SDK_APP_ID=
SMS_TENCENT_SIGN_NAME=
```

要求：模板 ID 与 locale 绑定；发送频率限制；验证码类短信单独限流；失败重试与黑名单。

## 规划：对象存储

接口建议：

```java
public interface StorageProvider {
    String provider();
    StoredObject upload(String key, InputStream content, String contentType);
    void delete(String key);
    String publicUrl(String key);
}
```

适配器：

- 本地/S3 兼容：开发默认 SeaweedFS/MinIO。
- 阿里云 OSS：官方 Java SDK，配置 endpoint/bucket/AccessKeyId/AccessKeySecret；可选 STS 临时凭证。
- 七牛云：官方 Java SDK，配置 AccessKey/SecretKey/bucket/domain/region。
- Mock：`MockStorageProvider`，返回本地占位 URL。

配置键建议：

```text
STORAGE_PROVIDER=local|s3|aliyun-oss|qiniu|mock
STORAGE_S3_ENDPOINT=
STORAGE_S3_ACCESS_KEY=
STORAGE_S3_SECRET_KEY=
STORAGE_S3_BUCKET=
STORAGE_ALIYUN_OSS_ENDPOINT=
STORAGE_ALIYUN_OSS_BUCKET=
STORAGE_ALIYUN_OSS_ACCESS_KEY_ID=
STORAGE_ALIYUN_OSS_ACCESS_KEY_SECRET=
STORAGE_QINIU_ACCESS_KEY=
STORAGE_QINIU_SECRET_KEY=
STORAGE_QINIU_BUCKET=
STORAGE_QINIU_DOMAIN=
```

上传流程：前端拿直传凭证或走后端上传；服务端校验 MIME/大小/扩展名；图片压缩与缩略图；病毒扫描可选；CDN 域名；私有 bucket 签名 URL。

## 已实现：支付/物流 Mock

- `PaymentConnector` SPI：Mock 默认；真实微信/支付宝/银联适配器待接入。
- 物流：门店/仓库拣货任务与 Mock 运单号；真实承运商电子面单待接入。
- 开业版本不配置真实支付，客户自行配置凭证并编译正式站。

## 失败与安全

- 外部调用必须有超时、重试上限、熔断/降级策略；失败写记录，不吞异常。
- 凭证只从环境变量/密钥管理读取；日志脱敏手机号、邮箱、地址。
- Webhook 必须验签、幂等、防重放；供应商 HMAC 使用时间戳 + nonce。
- Mock 模式禁止访问外网；生产模式配置缺失时启动失败。

## 下一步

1. 抽象 `SmsSender`、`StorageProvider` 接口并接入通知模块。
2. 实现阿里云/腾讯云短信适配器 + sandbox/mock。
3. 实现阿里云 OSS/七牛云上传适配器 + 后台媒体库。
4. 后台「系统设置」增加 provider 配置、连通性测试、发送/上传记录。
5. CI 增加厂商 SDK 固定版本、License 扫描和 Mock 端到端测试。