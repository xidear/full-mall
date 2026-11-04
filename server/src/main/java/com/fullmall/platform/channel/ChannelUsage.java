package com.fullmall.platform.channel;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 通道「用途」清单：说明哪个业务动作用哪个通道（ADR-0018）。
 *
 * <p>它是 {@code integration_route} 的候选项来源：后台的勾选矩阵、服务端的绑定校验都读这里，
 * 前端不得再写一份用途列表（AGENTS §13）。新增用途必须同时在这里登记，否则绑定会被拒绝。
 */
public final class ChannelUsage {

    /**
     * @param recipientRequired 路由是否必须填收件人：告警类要固定的值班手机号/邮箱，
     *                          订单通知的收件人来自业务数据（下单手机号），群机器人发到群里也不需要。
     */
    public record Usage(String capability, String key, String label, String description,
                         boolean recipientRequired) {
    }

    private static final List<Usage> ALL = List.of(
            new Usage("sms", "OTP_LOGIN", "登录验证码", "会员登录、注册、找回密码的短信验证码", false),
            new Usage("sms", "ORDER_EVENT", "订单通知", "下单、支付、发货、提货等订单状态短信", false),
            new Usage("sms", "ALERT_FATAL", "故障告警", "FATAL 级内部告警，发到值班手机", true),
            new Usage("email", "OTP_LOGIN", "登录验证码", "邮箱验证码登录", false),
            new Usage("email", "ORDER_EVENT", "订单通知", "订单状态邮件", false),
            new Usage("email", "ALERT_FATAL", "故障告警", "FATAL 级内部告警邮件", true),
            new Usage("wechat", "ORDER_EVENT", "订单通知", "公众号模板消息", false),
            new Usage("alert_im", "ALERT_FATAL", "故障告警", "FATAL/HIGH 立即推送到群机器人", false),
            new Usage("alert_im", "ALERT_RECOVERY", "恢复通知", "告警恢复时推送一条", false));

    private ChannelUsage() {
    }

    public static List<Usage> all() {
        return ALL;
    }

    public static List<Usage> of(String capability) {
        String normalized = normalize(capability);
        return ALL.stream().filter(item -> item.capability().equals(normalized)).toList();
    }

    public static Optional<Usage> find(String capability, String key) {
        String normalizedCapability = normalize(capability);
        String normalizedKey = key == null ? "" : key.trim();
        return ALL.stream()
                .filter(item -> item.capability().equals(normalizedCapability)
                        && item.key().equals(normalizedKey))
                .findFirst();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
