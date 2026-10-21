package com.fullmall.notification.channel;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.teaopenapi.models.Config;
import com.fullmall.notification.domain.Notification;

/**
 * 阿里云短信（官方 SDK `dysmsapi20170525`）。
 *
 * <p>配置缺失时**构造即失败**，避免"以为发了、其实没发"。
 * 短信模板必须使用**已审核通过的模板**（`templateCode`），内容作为模板参数 `content` 传入；
 * 模板不匹配时阿里云会返回错误，我们按失败处理并把错误写回通知记录。
 */
public class AliyunSmsSender implements NotificationChannelSender {

    private final Client client;
    private final String signName;
    private final String templateCode;

    public AliyunSmsSender(String accessKeyId, String accessKeySecret, String endpoint,
                           String signName, String templateCode) {
        if (accessKeyId == null || accessKeyId.isBlank()
                || accessKeySecret == null || accessKeySecret.isBlank()
                || signName == null || signName.isBlank()
                || templateCode == null || templateCode.isBlank()) {
            throw new IllegalStateException(
                    "app.sms.provider=aliyun 时必须配置 access-key-id/access-key-secret/sign-name/template-code");
        }
        this.signName = signName;
        this.templateCode = templateCode;
        try {
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret);
            config.endpoint = endpoint == null || endpoint.isBlank()
                    ? "dysmsapi.aliyuncs.com" : endpoint;
            this.client = new Client(config);
        } catch (Exception error) {
            throw new IllegalStateException("初始化阿里云短信客户端失败：" + error.getMessage(), error);
        }
    }

    @Override
    public String channel() {
        return "SMS";
    }

    @Override
    public void send(Notification notification) {
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(notification.recipientId())
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam("{\"content\":\"" + escape(notification.content()) + "\"}");
            var response = client.sendSms(request);
            String code = response.getBody() == null ? null : response.getBody().getCode();
            if (!"OK".equalsIgnoreCase(code)) {
                throw new IllegalStateException("阿里云短信返回失败：" + code + " "
                        + (response.getBody() == null ? "" : response.getBody().getMessage()));
            }
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("发送短信失败：" + error.getMessage(), error);
        }
    }

    private String escape(String text) {
        return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
