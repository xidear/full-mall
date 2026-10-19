package com.fullmall.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.feature.app.FeatureFlagService;
import com.fullmall.setting.app.SystemSettingService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeatureGuardFilterTest {

    private final FeatureFlagService flags = mock(FeatureFlagService.class);
    private final SystemSettingService settings = mock(SystemSettingService.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final FeatureGuardFilter filter = new FeatureGuardFilter(flags, settings, new ObjectMapper());

    private MockHttpServletResponse run(String method, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    private void enableAllFlags() {
        when(flags.isEnabled(anyString())).thenReturn(true);
        when(settings.siteStatus()).thenReturn(SystemSettingService.SITE_STATUS_OPEN);
    }

    @Test
    void blocksSupplierApiWhenSupplierModuleDisabled() throws Exception {
        when(flags.isEnabled(FeatureFlagService.SUPPLIER_ENABLED)).thenReturn(false);

        MockHttpServletResponse response = run("GET", "/open/v1/orders");

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("SUPPLIER_DISABLED");
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void allowsSupplierApiWhenEnabled() throws Exception {
        enableAllFlags();

        MockHttpServletResponse response = run("GET", "/open/v1/orders");

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blocksPosSaleWhenSaleFlagDisabled() throws Exception {
        enableAllFlags();
        when(flags.isEnabled(FeatureFlagService.POS_SALE_ENABLED)).thenReturn(false);

        MockHttpServletResponse response = run("POST", "/store/v1/pos/sales");

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("POS_SALE_DISABLED");
    }

    @Test
    void blocksPosPickupWhenPickupFlagDisabled() throws Exception {
        enableAllFlags();
        when(flags.isEnabled(FeatureFlagService.POS_PICKUP_ENABLED)).thenReturn(false);

        MockHttpServletResponse response = run("POST", "/store/v1/pickups/654321/verify");

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("POS_PICKUP_DISABLED");
    }

    @Test
    void closesCustomerFrontWhenSiteClosed() throws Exception {
        enableAllFlags();
        when(settings.siteStatus()).thenReturn(SystemSettingService.SITE_STATUS_CLOSED);
        when(settings.maintenanceMessage()).thenReturn("系统维护中，请稍后再试。");

        MockHttpServletResponse response = run("GET", "/api/v1/products");

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("SITE_CLOSED");
        assertThat(response.getContentAsString()).contains("系统维护中");
    }

    @Test
    void keepsPaymentCallbacksAndSiteInfoOpenWhenSiteClosed() throws Exception {
        enableAllFlags();
        when(settings.siteStatus()).thenReturn(SystemSettingService.SITE_STATUS_CLOSED);

        assertThat(run("POST", "/api/v1/payments/mock/callback").getStatus()).isEqualTo(200);
        assertThat(run("GET", "/api/v1/site/settings").getStatus()).isEqualTo(200);
    }

    @Test
    void keepsAdminAndHealthOpenWhenSiteClosed() throws Exception {
        enableAllFlags();
        when(settings.siteStatus()).thenReturn(SystemSettingService.SITE_STATUS_CLOSED);

        assertThat(run("GET", "/admin/v1/feature-flags").getStatus()).isEqualTo(200);
        assertThat(run("GET", "/actuator/health").getStatus()).isEqualTo(200);
        assertThat(run("GET", "/store/v1/pickups/654321").getStatus()).isEqualTo(200);
    }

    @Test
    void maintenanceBlocksFrontWithMaintenanceCode() throws Exception {
        enableAllFlags();
        when(settings.siteStatus()).thenReturn(SystemSettingService.SITE_STATUS_MAINTENANCE);
        when(settings.maintenanceMessage()).thenReturn("维护中");

        MockHttpServletResponse response = run("GET", "/api/v1/products");

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("SITE_MAINTENANCE");
    }
}