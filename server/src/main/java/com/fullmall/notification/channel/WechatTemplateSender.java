package com.fullmall.notification.channel;

import com.fullmall.notification.domain.Notification;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;

import java.util.List;

/**
 * 微信模板消息（**WxJava** 三方 SDK）。
 *
 * <p>为什么用三方 SDK：微信官方没有 Java SDK，但 WxJava 是事实标准（access_token 刷新、
 * 错误码、重试都由 SDK 处理），比手写 HTTP 更稳。这类"官方无 SDK、生态有成熟实现"的情况
 * 按项目规则应优先用成熟实现。
 *
 * <p>失败语义：缺配置启动即失败；SDK 抛错原样上抛，不伪装成功。
 */
public class WechatTemplateSender implements NotificationChannelSender {

    private final WxMpService wxMpService;
    private final String templateId;

    public WechatTemplateSender(String appId, String appSecret, String templateId) {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()
                || templateId == null || templateId.isBlank()) {
            throw new IllegalStateException(
                    "app.notification.wechat 需配置 app-id/app-secret/template-id");
        }
        WxMpDefaultConfigImpl config = new WxMpDefaultConfigImpl();
        config.setAppId(appId);
        config.setSecret(appSecret);
        WxMpService service = new WxMpServiceImpl();
        service.setWxMpConfigStorage(config);
        this.wxMpService = service;
        this.templateId = templateId;
    }

    @Override
    public String channel() {
        return "WECHAT";
    }

    @Override
    public void send(Notification notification) {
        WxMpTemplateMessage message = WxMpTemplateMessage.builder()
                .toUser(notification.recipientId())
                .templateId(templateId)
                .data(List.of(new WxMpTemplateData("content", notification.content())))
                .build();
        try {
            wxMpService.getTemplateMsgService().sendTemplateMsg(message);
        } catch (Exception error) {
            throw new IllegalStateException("发送微信模板消息失败：" + error.getMessage(), error);
        }
    }
}
