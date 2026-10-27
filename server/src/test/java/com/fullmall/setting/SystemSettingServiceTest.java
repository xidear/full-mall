package com.fullmall.setting;

import com.fullmall.platform.error.ApiException;
import com.fullmall.setting.app.SystemSettingService;
import com.fullmall.setting.infra.SystemSettingRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemSettingServiceTest {

    private static final Instant START = Instant.parse("2026-10-19T00:00:00Z");

    private final SystemSettingRepository repository = mock(SystemSettingRepository.class);
    private final MutableClock clock = new MutableClock(START);
    private final SystemSettingService service = new SystemSettingService(repository, clock, 30);

    private static Map<String, String> settings(String status) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("site_name", "Full Mall");
        if (status != null) {
            map.put(SystemSettingService.SITE_STATUS_KEY, status);
        }
        map.put(SystemSettingService.MAINTENANCE_MESSAGE_KEY, "系统维护中，请稍后再试。");
        map.put(SystemSettingService.DEFAULT_LOCALE_KEY, "zh-CN");
        return map;
    }

    @Test
    void siteStatusCachesWithinTtl() {
        when(repository.findAll()).thenReturn(settings("OPEN"));

        assertThat(service.siteStatus()).isEqualTo("OPEN");
        assertThat(service.siteStatus()).isEqualTo("OPEN");
        verify(repository, times(1)).findAll();

        when(repository.findAll()).thenReturn(settings("CLOSED"));
        clock.advanceSeconds(31);
        assertThat(service.siteStatus()).isEqualTo("CLOSED");
        verify(repository, times(2)).findAll();
    }

    @Test
    void missingSiteStatusFailsInsteadOfAssumingOpen() {
        when(repository.findAll()).thenReturn(settings(null));

        assertThatThrownBy(service::siteStatus)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("site_status");
    }

    @Test
    void rejectsUnknownSiteStatusValueOnRead() {
        when(repository.findAll()).thenReturn(settings("DOWN"));

        assertThatThrownBy(service::siteStatus)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("取值非法");
    }

    @Test
    void rejectsUnknownSiteStatusValueOnSave() {
        assertThatThrownBy(() -> service.save(Map.of(SystemSettingService.SITE_STATUS_KEY, "DOWN")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("OPEN / MAINTENANCE / CLOSED");
    }

    @Test
    void saveInvalidatesSiteStatusCache() {
        when(repository.findAll()).thenReturn(settings("OPEN"));
        assertThat(service.siteStatus()).isEqualTo("OPEN");

        when(repository.findAll()).thenReturn(settings("CLOSED"));
        service.save(Map.of(SystemSettingService.SITE_STATUS_KEY, "CLOSED"));

        assertThat(service.siteStatus()).isEqualTo("CLOSED");
    }

    @Test
    void normalizesSiteDomainHostAndPort() {
        when(repository.findAll()).thenReturn(new LinkedHashMap<>(settings("OPEN")));

        service.save(Map.of(SystemSettingService.SITE_DOMAIN_KEY, " Company.BinNarui.COM:443 "));

        verify(repository).upsert(SystemSettingService.SITE_DOMAIN_KEY, "company.binnarui.com:443");
    }

    @Test
    void rejectsSiteDomainWithSchemeOrPath() {
        assertThatThrownBy(() -> service.save(
                Map.of(SystemSettingService.SITE_DOMAIN_KEY, "https://company.binnarui.com")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("host[:port]");

        assertThatThrownBy(() -> service.save(
                Map.of(SystemSettingService.SITE_DOMAIN_KEY, "company.binnarui.com/mall")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("host[:port]");
    }

    @Test
    void rejectsSiteDomainWithInvalidPort() {
        assertThatThrownBy(() -> service.save(
                Map.of(SystemSettingService.SITE_DOMAIN_KEY, "company.binnarui.com:70000")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("1-65535");
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advanceSeconds(long seconds) {
            this.instant = this.instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}