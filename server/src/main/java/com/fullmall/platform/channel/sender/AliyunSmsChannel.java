package com.fullmall.platform.channel.sender;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.teaopenapi.models.Config;

/**
 * 阿里云短信（官方 SDK dysmsapi20170525）。
 *
 * <p>模板必须提前在控制台审核通过：内容作为模板变量 {@code content} 传入；
 * 返回码非 OK 一律抛错，不返回「发送成功」。
 */
public class AliyunSmsChannel implements SmsChannel {

    private final Client client;
    private final String signName;
    private final String templateCode;

    public AliyunSmsChannel(ChannelSettings settings) {
        String accessKeyId = settings.required("access-key-id", "AccessKey ID");
        String accessKeySecret = settings.required("access-key-secret", "AccessKey Secret");
        this.signName = settings.required("sign-name", "短信签名");
        this.templateCode = settings.required("template-code", "短信模板 CODE");
        try {
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret);
            config.endpoint = settings.optional("endpoint", "dysmsapi.aliyuncs.com");
            this.client = new Client(config);
        } catch (Exception error) {
            throw new IllegalStateException("初始化阿里云短信客户端失败：" + error.getMessage(), error);
        }
    }

    @Override
    public void send(String phone, String text) {
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam("{\"content\":\"" + escape(text) + "\"}");
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

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
