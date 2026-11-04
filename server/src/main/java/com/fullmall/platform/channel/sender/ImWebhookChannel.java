package com.fullmall.platform.channel.sender;

import com.fullmall.platform.http.HttpClients;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 群机器人 Webhook（企微 / 飞书 / 钉钉）。
 *
 * <p>为什么直接用 JDK HttpClient 而不引 SDK：三家协议都是「一次 HTTPS POST + 一个 JSON」，
 * 官方没有 Java SDK、社区 SDK 各自只覆盖一家；按 AGENTS §2 的优先顺序，标准库够用时不引重依赖。
 *
 * <p>三条契约差异（写错会静默丢消息）：企微 {@code msgtype=markdown} 且成功判定 {@code errcode=0}；
 * 钉钉 {@code msgtype=text}，开启「加签」后 URL 必须追加 timestamp+sign，否则 310000；
 * 飞书 {@code msg_type=text} 且成功判定 {@code code=0}（旧版本返回 {@code StatusCode}）。
 * 所以只判 HTTP 200 不够：HTTP 200 + 业务错误码是这三家的常态。
 */
public class ImWebhookChannel implements ImChannel {

    public enum Vendor { WECOM, FEISHU, DINGTALK }

    private final Vendor vendor;
    private final String webhookUrl;
    private final String secret;
    private final String mention;

    public ImWebhookChannel(String provider, ChannelSettings settings) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "wecom" -> this.vendor = Vendor.WECOM;
            case "feishu" -> this.vendor = Vendor.FEISHU;
            case "dingtalk" -> this.vendor = Vendor.DINGTALK;
            default -> throw new IllegalStateException("不支持的告警群机器人供应商：" + provider);
        }
        this.webhookUrl = settings.required("webhook-url", "Webhook 地址");
        this.secret = settings.optional("secret");
        this.mention = settings.optional("mention");
        if (!webhookUrl.startsWith("https://") && !webhookUrl.startsWith("http://")) {
            throw new IllegalStateException("Webhook 地址必须以 http(s):// 开头，实际=" + webhookUrl);
        }
    }

    @Override
    public void send(String text) {
        String body = payload(text == null ? "" : text);
        HttpRequest request = HttpRequest.newBuilder(URI.create(targetUrl()))
                .header("Content-Type", "application/json; charset=UTF-8")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response;
        try {
            response = HttpClients.shared().send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("发送群机器人消息被中断：" + vendor, error);
        } catch (Exception error) {
            throw new IllegalStateException("发送群机器人消息失败：" + vendor + "，" + error.getMessage(), error);
        }
        requireAccepted(response.statusCode(), response.body());
    }

    private void requireAccepted(int status, String body) {
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("群机器人返回 HTTP " + status + "：" + truncate(body));
        }
        String errorCode = errorCodeOf(body);
        if (errorCode != null) {
            throw new IllegalStateException("群机器人返回业务错误 " + errorCode + "：" + truncate(body));
        }
    }

    /** 只认成功码字段；结构变了就当失败，不允许猜成成功。 */
    private String errorCodeOf(String body) {
        if (body == null || body.isBlank()) {
            return "EMPTY_RESPONSE";
        }
        String key = switch (vendor) {
            case WECOM, DINGTALK -> "errcode";
            case FEISHU -> "code";
        };
        Integer code = intField(body, key);
        if (code == null && vendor == Vendor.FEISHU) {
            code = intField(body, "StatusCode");
        }
        if (code == null) {
            return "UNRECOGNIZED_RESPONSE";
        }
        return code == 0 ? null : String.valueOf(code);
    }

    private Integer intField(String body, String key) {
        String needle = "\"" + key + "\"";
        int at = body.indexOf(needle);
        if (at < 0) {
            return null;
        }
        int colon = body.indexOf(':', at + needle.length());
        if (colon < 0) {
            return null;
        }
        int index = colon + 1;
        while (index < body.length() && Character.isWhitespace(body.charAt(index))) {
            index++;
        }
        int cursor = index;
        if (cursor < body.length() && (body.charAt(cursor) == '-' || Character.isDigit(body.charAt(cursor)))) {
            cursor++;
            while (cursor < body.length() && Character.isDigit(body.charAt(cursor))) {
                cursor++;
            }
        }
        if (cursor == index) {
            return null;
        }
        try {
            return Integer.valueOf(body.substring(index, cursor));
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private String targetUrl() {
        if (vendor != Vendor.DINGTALK || secret == null) {
            return webhookUrl;
        }
        long timestamp = System.currentTimeMillis();
        String separator = webhookUrl.contains("?") ? "&" : "?";
        return webhookUrl + separator + "timestamp=" + timestamp + "&sign=" + sign(timestamp, secret);
    }

    /** 钉钉加签：HmacSHA256(secret, timestamp + 换行 + secret) → Base64 → URL 编码。 */
    private String sign(long timestamp, String secretValue) {
        try {
            String toSign = timestamp + "\n" + secretValue;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretValue.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(toSign.getBytes(StandardCharsets.UTF_8));
            return URLEncoder.encode(Base64.getEncoder().encodeToString(digest), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | InvalidKeyException error) {
            throw new IllegalStateException("钉钉加签失败：" + error.getMessage(), error);
        }
    }

    private String payload(String text) {
        String content = mention == null ? text : text + " " + mentionText();
        String escaped = escapeJson(content);
        return switch (vendor) {
            case WECOM -> "{\"msgtype\":\"markdown\",\"markdown\":{\"content\":\"" + escaped + "\"}}";
            case DINGTALK -> "{\"msgtype\":\"text\",\"text\":{\"content\":\"" + escaped + "\"}}";
            case FEISHU -> "{\"msg_type\":\"text\",\"content\":{\"text\":\"" + escaped + "\"}}";
        };
    }

    private String mentionText() {
        StringBuilder builder = new StringBuilder();
        for (String phone : mention.split(",")) {
            if (!phone.isBlank()) {
                builder.append("<@").append(phone.trim()).append("> ");
            }
        }
        return builder.toString().trim();
    }

    private String escapeJson(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch == '"') {
                builder.append("\\\"");
            } else if (ch == '\\') {
                builder.append("\\\\");
            } else if (ch == '\n') {
                builder.append("\\n");
            } else if (ch == '\r') {
                builder.append("\\r");
            } else if (ch == '\t') {
                builder.append("\\t");
            } else if (ch < 0x20) {
                builder.append(String.format("\\u%04x", (int) ch));
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 300 ? body : body.substring(0, 300) + "...";
    }
}
