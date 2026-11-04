package com.fullmall.platform.channel.sender;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;

/**
 * 腾讯云短信（官方 SDK tencentcloud-sdk-java-sms）。
 *
 * <p>SendStatus 里每一条都要是 Ok：只检查顶层 Response 会漏掉「提交成功但单条被拒」，
 * 那正是「以为发了、用户没收到」的典型现场。
 */
public class TencentSmsChannel implements SmsChannel {

    private final SmsClient client;
    private final String sdkAppId;
    private final String signName;
    private final String templateId;

    public TencentSmsChannel(ChannelSettings settings) {
        String secretId = settings.required("secret-id", "SecretId");
        String secretKey = settings.required("secret-key", "SecretKey");
        this.sdkAppId = settings.required("sdk-app-id", "SDK AppID");
        this.signName = settings.required("sign-name", "短信签名");
        this.templateId = settings.required("template-id", "模板 ID");
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint(settings.optional("endpoint", "sms.tencentcloudapi.com"));
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        this.client = new SmsClient(new Credential(secretId, secretKey),
                settings.optional("region", "ap-guangzhou"), clientProfile);
    }

    @Override
    public void send(String phone, String text) {
        SendSmsRequest request = new SendSmsRequest();
        request.setSmsSdkAppId(sdkAppId);
        request.setSignName(signName);
        request.setTemplateId(templateId);
        request.setPhoneNumberSet(new String[]{"+86" + phone});
        request.setTemplateParamSet(new String[]{text == null ? "" : text});
        try {
            var response = client.SendSms(request);
            if (response.getSendStatusSet() == null || response.getSendStatusSet().length == 0) {
                throw new IllegalStateException("腾讯云短信没有返回发送状态，无法确认是否送达");
            }
            for (var status : response.getSendStatusSet()) {
                if (!"Ok".equalsIgnoreCase(status.getCode())) {
                    throw new IllegalStateException("腾讯云短信返回失败：" + status.getCode()
                            + " " + status.getMessage());
                }
            }
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("发送短信失败：" + error.getMessage(), error);
        }
    }
}
