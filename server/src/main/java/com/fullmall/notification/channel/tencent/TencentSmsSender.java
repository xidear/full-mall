package com.fullmall.notification.channel.tencent;

import com.fullmall.notification.channel.NotificationChannelSender;
import com.fullmall.notification.domain.Notification;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;

/**
 * 腾讯云短信适配器（官方 SDK 3.2.x）。
 *
 * <p>**可剥离设计**：本类与依赖都只存在于 `notification.channel.tencent` 包；
 * 领域与业务只依赖 `NotificationChannelSender` 端口，不认识腾讯云类型。
 * 删除本包 + 依赖 + 若干配置项即可完全移除通道，不影响其它模块编译。
 *
 * <p>失败语义：缺配置构造即失败；接口返回码非 Ok 抛错，不返回"发送成功"。
 */
public class TencentSmsSender implements NotificationChannelSender {

    private final SmsClient client;
    private final String sdkAppId;
    private final String signName;
    private final String templateId;

    public TencentSmsSender(String secretId, String secretKey, String region,
                            String sdkAppId, String signName, String templateId) {
        if (isBlank(secretId) || isBlank(secretKey) || isBlank(sdkAppId)
                || isBlank(signName) || isBlank(templateId)) {
            throw new IllegalStateException(
                    "app.sms.provider=tencent 时必须配置 secret-id/secret-key/sdk-app-id/sign-name/template-id");
        }
        Credential credential = new Credential(secretId, secretKey);
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("sms.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        this.client = new SmsClient(credential, isBlank(region) ? "ap-guangzhou" : region, clientProfile);
        this.sdkAppId = sdkAppId;
        this.signName = signName;
        this.templateId = templateId;
    }

    @Override
    public String channel() {
        return "SMS";
    }

    @Override
    public void send(Notification notification) {
        SendSmsRequest request = new SendSmsRequest();
        request.setSmsSdkAppId(sdkAppId);
        request.setSignName(signName);
        request.setTemplateId(templateId);
        request.setPhoneNumberSet(new String[]{"+86" + notification.recipientId()});
        request.setTemplateParamSet(new String[]{notification.content()});
        try {
            SendSmsResponse response = client.SendSms(request);
            SendStatus[] statuses = response.getSendStatusSet();
            if (statuses == null || statuses.length == 0) {
                throw new IllegalStateException("腾讯云短信未返回发送状态：" + response.getRequestId());
            }
            for (SendStatus status : statuses) {
                if (!"Ok".equalsIgnoreCase(status.getCode())) {
                    throw new IllegalStateException("腾讯云短信发送失败：" + status.getCode()
                            + " " + status.getMessage() + "（requestId=" + response.getRequestId() + "）");
                }
            }
        } catch (Exception error) {
            throw new IllegalStateException("调用腾讯云短信失败：" + error.getMessage(), error);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
