package com.fullmall.platform.secret;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 第三方服务的候选清单：能力 → 服务商 → 配置字段。
 *
 * <p>为什么需要它：服务商名和配置键原本是自由文本，前端只能给输入框。运营因此无从知道
 * "这个能力可以填哪家、这家要填哪些键"，填错一个字就是一条永不生效的死配置（服务端在
 * 保存时只校验能力，不校验服务商）。这里把候选收敛成一份清单，前端据此渲染下拉与字段，
 * 服务端据此拒绝未知服务商/未知键。
 *
 * <p>字段的 secret 标记是加密的唯一依据：命中此标记（或键名含 secret/key/token/password/private）
 * 才会加密且永不回显。新增服务商时必须在这里登记，否则保存会被拒绝。
 */
public final class IntegrationCatalog {

    /** 一个可填的配置字段。hint 直接显示在输入框下方，告诉运营"填什么"。 */
    public record Field(String key, String label, boolean secret, boolean required, String hint) {
        static Field text(String key, String label, String hint) {
            return new Field(key, label, false, true, hint);
        }

        static Field optional(String key, String label, String hint) {
            return new Field(key, label, false, false, hint);
        }

        static Field secret(String key, String label, String hint) {
            return new Field(key, label, true, true, hint);
        }

        static Field optionalSecret(String key, String label, String hint) {
            return new Field(key, label, true, false, hint);
        }
    }

    public record Provider(String code, String label, List<Field> fields) {
    }

    public record Capability(String code, String label, String description, List<Provider> providers) {
    }

    private static final List<Capability> ALL = List.of(
            new Capability("sms", "短信", "验证码、通知短信", List.of(
                    new Provider("aliyun", "阿里云短信", List.of(
                            Field.secret("access-key-id", "AccessKey ID", "控制台 RAM 用户的 AccessKey ID"),
                            Field.secret("access-key-secret", "AccessKey Secret", "与 AccessKey ID 成对；留空表示沿用旧值"),
                            Field.text("sign-name", "短信签名", "控制台已审核通过的签名，例如「示例商城」"),
                            Field.optional("template-code", "默认模板 CODE", "验证码模板，例如 SMS_123456789"))),
                    new Provider("tencent", "腾讯云短信", List.of(
                            Field.secret("secret-id", "SecretId", "访问管理 → API 密钥"),
                            Field.secret("secret-key", "SecretKey", "与 SecretId 成对；留空表示沿用旧值"),
                            Field.text("sdk-app-id", "SDK AppID", "短信控制台的应用 ID，纯数字"),
                            Field.text("sign-name", "短信签名", "已审核通过的签名内容"))))),

            new Capability("payment", "支付", "在线支付与退款回调", List.of(
                    new Provider("wechat", "微信支付", List.of(
                            Field.text("app-id", "AppID", "公众号或小程序的 AppID"),
                            Field.text("mch-id", "商户号", "微信支付商户号，纯数字"),
                            Field.secret("api-v3-key", "APIv3 密钥", "商户平台设置的 32 位密钥"),
                            Field.optionalSecret("cert-serial", "证书序列号", "可留空；回调验签需要时再填"))),
                    new Provider("alipay", "支付宝", List.of(
                            Field.text("app-id", "APPID", "开放平台应用的 APPID"),
                            Field.secret("private-key", "应用私钥", "PKCS8 格式，勿带换行"),
                            Field.secret("alipay-public-key", "支付宝公钥", "开放平台下载的公钥"),
                            Field.optional("gateway", "网关地址", "默认 https://openapi.alipay.com/gateway.do"))))),

            new Capability("storage", "对象存储", "素材与视频的存放位置", List.of(
                    new Provider("local", "本机磁盘", List.of(
                            new Field("directory", "存储目录", false, true, "必填：磁盘上的存储根目录（如 /data/assets），需与静态资源映射一致；未配置时上传会直接失败"))),
                    new Provider("qiniu", "七牛云", List.of(
                            Field.secret("access-key", "AccessKey", "七牛个人中心的密钥管理"),
                            Field.secret("secret-key", "SecretKey", "与 AccessKey 成对；留空表示沿用旧值"),
                            Field.text("bucket", "Bucket", "存储空间名称"),
                            Field.text("domain", "访问域名", "绑定到 Bucket 的域名，含 https://"))),
                    new Provider("cos", "腾讯云 COS", List.of(
                            Field.secret("secret-id", "SecretId", "访问管理 → API 密钥"),
                            Field.secret("secret-key", "SecretKey", "与 SecretId 成对；留空表示沿用旧值"),
                            Field.text("bucket", "存储桶", "形如 example-1250000000"),
                            Field.text("region", "地域", "形如 ap-guangzhou"))))),

            new Capability("export", "导出文件存储", "导出中心的 CSV 存放位置（含顾客手机号，禁止公开访问）", List.of(
                    new Provider("local", "本机磁盘", List.of(
                            new Field("directory", "存储目录", false, true, "必填：磁盘上的导出目录（如 /data/fullmall-exports），不要放在静态资源目录下"),
                            new Field("retention-days", "保留天数", false, false, "过期文件自动清理；留空表示不自动清理"))))),

            new Capability("moderation", "内容审核", "商品图与评价图的合规检查", List.of(
                    new Provider("tencent", "腾讯云内容安全", List.of(
                            Field.secret("secret-id", "SecretId", "访问管理 → API 密钥"),
                            Field.secret("secret-key", "SecretKey", "与 SecretId 成对"),
                            Field.optional("region", "地域", "默认 ap-guangzhou"))),
                    new Provider("aliyun", "阿里云内容安全", List.of(
                            Field.secret("access-key", "AccessKey ID", "RAM 用户 AccessKey"),
                            Field.secret("secret-key", "AccessKey Secret", "与 AccessKey ID 成对"),
                            Field.optional("region", "地域", "默认 cn-shanghai"))))),

            new Capability("logistics", "物流查询", "运单轨迹查询与推送", List.of(
                    new Provider("kuaidi100", "快递100", List.of(
                            Field.text("customer", "客户编号", "快递100 企业版 customer"),
                            Field.secret("key", "授权 Key", "快递100 企业版 key"))))),

            new Capability("ai", "AI 模型", "文本、图像、多模态供应商", List.of(
                    new Provider("openai", "OpenAI 兼容", List.of(
                            Field.text("base-url", "接口地址", "例如 https://api.openai.com/v1"),
                            Field.secret("api-key", "API Key", "留空表示沿用旧值"),
                            Field.text("text-model", "文本模型", "例如 gpt-4o-mini"),
                            Field.optional("image-model", "图像模型", "需要生图时填写"))),
                    new Provider("dashscope", "阿里云百炼", List.of(
                            Field.text("base-url", "接口地址", "默认 https://dashscope.aliyuncs.com/compatible-mode/v1"),
                            Field.secret("api-key", "API Key", "百炼控制台创建"),
                            Field.text("text-model", "文本模型", "例如 qwen-max"),
                            Field.optional("image-model", "图像模型", "例如 qwen-vl-max"))),
                    new Provider("ollama", "本机 Ollama", List.of(
                            Field.text("base-url", "接口地址", "默认 http://127.0.0.1:41289"),
                            Field.text("text-model", "文本模型", "例如 qwen2.5:3b"),
                            Field.optional("image-model", "图像模型", "例如 moondream"))))));

    private IntegrationCatalog() {
    }

    public static List<Capability> capabilities() {
        return ALL;
    }

    public static Set<String> capabilityCodes() {
        return ALL.stream().map(Capability::code).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /** 该能力允许的服务商对象（供设置页判断"哪些真的能用"）。 */
    public static List<Provider> providers(String capability) {
        String normalized = capability == null ? "" : capability.trim().toLowerCase(java.util.Locale.ROOT);
        return ALL.stream()
                .filter(item -> item.code().equals(normalized))
                .findFirst()
                .map(Capability::providers)
                .orElse(List.of());
    }

    /** 该能力允许的服务商（带中文名），用于错误提示与文档。 */
    public static String providersOf(String capability) {
        String normalized = capability == null ? "" : capability.trim().toLowerCase(java.util.Locale.ROOT);
        return ALL.stream()
                .filter(item -> item.code().equals(normalized))
                .flatMap(item -> item.providers().stream())
                .map(item -> item.code() + "(" + item.label() + ")")
                .collect(java.util.stream.Collectors.joining("/"));
    }

    public static Optional<Provider> provider(String capability, String providerCode) {
        if (capability == null || providerCode == null) {
            return Optional.empty();
        }
        String normalizedCapability = capability.trim().toLowerCase(java.util.Locale.ROOT);
        String normalizedProvider = providerCode.trim().toLowerCase(java.util.Locale.ROOT);
        return ALL.stream()
                .filter(item -> item.code().equals(normalizedCapability))
                .flatMap(item -> item.providers().stream())
                .filter(item -> item.code().equals(normalizedProvider))
                .findFirst();
    }
}
