package com.fullmall.platform.channel;

import com.fullmall.platform.channel.sender.AliyunSmsChannel;
import com.fullmall.platform.channel.sender.ChannelSettings;
import com.fullmall.platform.channel.sender.ImWebhookChannel;
import com.fullmall.platform.channel.sender.SmtpEmailChannel;
import com.fullmall.platform.channel.sender.TencentSmsChannel;
import com.fullmall.platform.channel.sender.WechatTemplateChannel;
import com.fullmall.platform.error.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 通道发送入口：把「业务动作 + 收件人 + 内容」翻译成「哪家供应商 + 哪套凭证 + 哪个实现」。
 *
 * <p>业务代码只认识通道（SMS/EMAIL/WECHAT）和用途（OTP_LOGIN/ORDER_EVENT/ALERT_FATAL），
 * 不认识供应商；换供应商、同时接两家、按用途分流都只改后台数据（ADR-0018）。
 *
 * <p>演示模式（app.mode=demo）下没有配置也不发外网：打 WARN 并当作已投递，
 * 演示环境因此不依赖任何真实凭据；生产模式未配置则明确失败，绝不伪装成功。
 */
@Service
public class ChannelSendService {

    private static final Logger log = LoggerFactory.getLogger(ChannelSendService.class);

    /** 通知渠道 → 集成能力。IN_APP 不在其中：站内消息落库即送达，不走外部通道。 */
    private static final Map<String, String> CHANNEL_CAPABILITIES = Map.of(
            "SMS", "sms", "EMAIL", "email", "WECHAT", "wechat");

    /** 后台「渠道自检」可选的渠道。 */
    public static final List<String> SUPPORTED_CHANNELS = List.of("IN_APP", "SMS", "EMAIL", "WECHAT");

    private final IntegrationRouteService routes;
    private final boolean demo;
    private final boolean production;

    public ChannelSendService(IntegrationRouteService routes, @Value("${app.mode:dev}") String mode) {
        this.routes = routes;
        String normalized = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        this.demo = "demo".equals(normalized);
        this.production = "prod".equals(normalized);
    }

    public record Result(String capability, String usageKey, String provider, boolean mock) {
    }

    /** 通知渠道对应的能力标识；未知渠道返回 null（由调用方报稳定错误码）。 */
    public static String capabilityOf(String channel) {
        return channel == null ? null : CHANNEL_CAPABILITIES.get(channel.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * 事件类型 → 通信用途。集中一处映射：新增事件类型若没登记会显式失败（CHANNEL_EVENT_UNMAPPED），
     * 而不是悄悄落到默认用途上，把告警当订单通知发出去。
     */
    public static String usageFor(String capability, String eventType) {
        String event = eventType == null ? "" : eventType.trim().toUpperCase(Locale.ROOT);
        if (capability == null) {
            throw ApiException.badRequest("CHANNEL_CAPABILITY_UNSUPPORTED", "缺少通信能力");
        }
        if ("alert_im".equals(capability)) {
            return event.startsWith("ALERT_RECOVER") ? "ALERT_RECOVERY" : "ALERT_FATAL";
        }
        if (event.contains("OTP")) {
            return "OTP_LOGIN";
        }
        if (event.startsWith("ALERT")) {
            return "ALERT_FATAL";
        }
        if (event.startsWith("ORDER_") || event.startsWith("PAYMENT_") || event.startsWith("REFUND_")
                || event.startsWith("PICKUP_") || event.startsWith("STOCK_") || event.startsWith("NEAR_")
                || "CHANNEL_TEST".equals(event)) {
            return "ORDER_EVENT";
        }
        throw ApiException.badRequest("CHANNEL_EVENT_UNMAPPED",
                "事件类型「" + eventType + "」没有对应的通信用途：请在 ChannelSendService.usageFor 登记");
    }

    /** 按渠道发送（供通知模块用）：渠道 → 能力 → 用途 → 路由。 */
    public Result sendChannel(String channel, String eventType, String recipient,
                              String subject, String content) {
        String capability = capabilityOf(channel);
        if (capability == null) {
            throw ApiException.badRequest("NOTIFICATION_CHANNEL_INVALID",
                    "未知渠道 " + channel + "，可选：" + SUPPORTED_CHANNELS);
        }
        return send(capability, usageFor(capability, eventType), recipient, subject, content);
    }

    public Result send(String capability, String usageKey, String recipient,
                        String subject, String content) {
        var resolved = routes.find(capability, usageKey);
        if (resolved.isEmpty()) {
            if (!demo) {
                // 生产：明确失败并告诉运营去哪配；不猜测「第一个启用的供应商」。
                throw routes.missing(capability, usageKey);
            }
            log.warn("[MOCK] 演示模式 {} / {} 未绑定通道，消息不发外网：recipient={} subject={}",
                    capability, usageKey, mask(recipient), subject);
            return new Result(capability, usageKey, "mock", true);
        }
        var target = resolved.get();
        if (demo && !"mock".equals(target.provider())) {
            // 演示模式不联网：绑了真实供应商时，既不能真发（违反演示口径），也不能假装成功（AGENTS §1），
            // 只能明确拒绝并把改法写清楚。e2e 实测过：放行会真的打到阿里云（假凭据拿到 404，接口报 500）。
            throw ApiException.conflict("CHANNEL_PROVIDER_DEMO_FORBIDDEN",
                    "演示模式不允许「" + capability + " / " + usageKey + "」走真实供应商 " + target.provider()
                            + "：请改绑 mock，或切到生产模式并配置真实凭据");
        }
        if ("mock".equals(target.provider())) {
            // 生产环境绑定 mock 等于「假装发成功」：明确拒绝，让配置错误当场暴露（AGENTS §1）。
            if (production) {
                throw ApiException.conflict("CHANNEL_MOCK_FORBIDDEN",
                        "生产环境不允许把「" + capability + " / " + usageKey
                                + "」绑定到 mock 供应商：请改绑真实供应商");
            }
            log.warn("[MOCK] {} / {} 显式绑定 mock 供应商，消息不发外网：recipient={}",
                    capability, usageKey, mask(recipient));
            return new Result(capability, usageKey, "mock", true);
        }
        dispatch(target, recipient, subject, content);
        return new Result(target.capability(), target.usageKey(), target.provider(), false);
    }

    private void dispatch(IntegrationRouteService.Resolved target, String recipient,
                           String subject, String content) {
        ChannelSettings settings = new ChannelSettings(target.config());
        String capability = target.capability();
        String provider = target.provider();
        switch (capability) {
            case "sms" -> {
                switch (provider) {
                    case "aliyun" -> new AliyunSmsChannel(settings)
                            .send(requireRecipient(recipient, capability), content);
                    case "tencent" -> new TencentSmsChannel(settings)
                            .send(requireRecipient(recipient, capability), content);
                    default -> throw unsupported(capability, provider);
                }
            }
            case "email" -> new SmtpEmailChannel(provider, settings)
                    .send(requireRecipient(recipient, capability), subject, content);
            case "wechat" -> new WechatTemplateChannel(settings)
                    .send(requireRecipient(recipient, capability), content);
            case "alert_im" -> new ImWebhookChannel(provider, settings).send(content);
            default -> throw unsupported(capability, provider);
        }
    }

    private String requireRecipient(String recipient, String capability) {
        if (recipient == null || recipient.isBlank()) {
            throw ApiException.badRequest("CHANNEL_RECIPIENT_REQUIRED",
                    "能力「" + capability + "」发送必须指定收件人");
        }
        return recipient.trim();
    }

    private IllegalStateException unsupported(String capability, String provider) {
        return new IllegalStateException("能力「" + capability + "」没有供应商「" + provider + "」的发送实现");
    }

    private String mask(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}
