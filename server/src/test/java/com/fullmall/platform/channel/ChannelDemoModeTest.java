package com.fullmall.platform.channel;

import com.fullmall.platform.error.ApiException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 演示模式不联网：绑了真实供应商时必须明确拒绝，既不真发也不假装成功。
 *
 * <p>这条用例来自实测缺陷：演示环境把「短信 / 登录验证码」绑到阿里云后，
 * `POST /admin/v1/channel-routes/test` 真的打到了阿里云（假凭据拿到 404），接口返回 500。
 * 演示口径是「只用官方 mock、不联网」，所以这里用打桩的路由把这条路径卡死。
 */
class ChannelDemoModeTest {

    private static IntegrationRouteService routesWith(String capability, String usageKey, String provider) {
        IntegrationRouteService routes = mock(IntegrationRouteService.class);
        when(routes.find(capability, usageKey)).thenReturn(Optional.of(
                new IntegrationRouteService.Resolved(capability, usageKey, provider, null,
                        Map.of("access-key-id", "fake", "access-key-secret", "fake",
                                "sign-name", "示例商城", "template-code", "SMS_1"))));
        return routes;
    }

    @Test
    void demoModeRefusesRealProviderInsteadOfSending() {
        var channels = new ChannelSendService(routesWith("sms", "OTP_LOGIN", "aliyun"), "demo");

        assertThatThrownBy(() -> channels.send("sms", "OTP_LOGIN", "13800000000", "标题", "内容"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("演示模式不允许");
    }

    @Test
    void demoModeStillSendsThroughExplicitMock() {
        var channels = new ChannelSendService(routesWith("sms", "OTP_LOGIN", "mock"), "demo");

        var result = channels.send("sms", "OTP_LOGIN", "13800000000", "标题", "内容");

        assertThat(result.mock()).isTrue();
        assertThat(result.provider()).isEqualTo("mock");
    }

    @Test
    void demoModeToleratesUnboundUsageWithoutTouchingNetwork() {
        var routes = mock(IntegrationRouteService.class);
        when(routes.find("sms", "OTP_LOGIN")).thenReturn(Optional.empty());
        var channels = new ChannelSendService(routes, "demo");

        var result = channels.send("sms", "OTP_LOGIN", "13800000000", "标题", "内容");

        assertThat(result.mock()).isTrue();
    }

    @Test
    void productionRefusesExplicitMockRoute() {
        var channels = new ChannelSendService(routesWith("sms", "OTP_LOGIN", "mock"), "prod");

        assertThatThrownBy(() -> channels.send("sms", "OTP_LOGIN", "13800000000", "标题", "内容"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("生产环境不允许");
    }
}
