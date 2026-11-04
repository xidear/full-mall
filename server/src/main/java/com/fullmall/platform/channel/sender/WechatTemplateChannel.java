package com.fullmall.platform.channel.sender;

import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;

import java.util.List;

/**
 * 微信公众号模板消息（WxJava：官方无 Java SDK，生态事实标准）。
 *
 * <p>失败语义：缺配置构造即失败；SDK 抛错原样上抛，不伪装成功。
 */
public class WechatTemplateChannel {

    private final WxMpService wxMpService;
    private final String templateId;

    public WechatTemplateChannel(ChannelSettings settings) {
        String appId = settings.required("app-id", "AppID");
        String appSecret = settings.required("app-secret", "AppSecret");
        this.templateId = settings.required("template-id", "模板 ID");
        WxMpDefaultConfigImpl config = new WxMpDefaultConfigImpl();
        config.setAppId(appId);
        config.setSecret(appSecret);
        WxMpService service = new WxMpServiceImpl();
        service.setWxMpConfigStorage(config);
        this.wxMpService = service;
    }

    public void send(String openId, String content) {
        WxMpTemplateMessage message = WxMpTemplateMessage.builder()
                .toUser(openId)
                .templateId(templateId)
                .data(List.of(new WxMpTemplateData("content", content)))
                .build();
        try {
            wxMpService.getTemplateMsgService().sendTemplateMsg(message);
        } catch (Exception error) {
            throw new IllegalStateException("发送微信模板消息失败：" + error.getMessage(), error);
        }
    }
}
