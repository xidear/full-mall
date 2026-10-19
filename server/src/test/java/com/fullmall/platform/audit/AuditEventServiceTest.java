package com.fullmall.platform.audit;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditEventServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final AuditEventService service = new AuditEventService(jdbcTemplate);

    @Test
    void rejectsEventWithoutActorInsteadOfRecordingAnonymous() {
        assertThatThrownBy(() -> service.record(new AuditEventService.AuditEvent(
                "LOGIN_SUCCESS", "  ", "staff_user", "1", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("操作人");
    }

    @Test
    void rejectsEventWithoutType() {
        assertThatThrownBy(() -> service.record(new AuditEventService.AuditEvent(
                null, "admin", "feature_flag", "x", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("类型");
    }

    @Test
    void failsLoudlyWhenInsertAffectsNoRow() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(0);

        assertThatThrownBy(() -> service.record(new AuditEventService.AuditEvent(
                "LOGIN_SUCCESS", "admin", "staff_user", "1", "role=ADMIN", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("影响行数=0");
    }

    @Test
    void recordsEventAndTruncatesLongDetail() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        String longDetail = "x".repeat(2000);
        service.record(new AuditEventService.AuditEvent(
                "POS_REFUND", "cashier", "pos_sale", "sale-1", longDetail, "req-9"));

        org.mockito.ArgumentCaptor<Object> id = org.mockito.ArgumentCaptor.forClass(Object.class);
        org.mockito.ArgumentCaptor<Object> type = org.mockito.ArgumentCaptor.forClass(Object.class);
        org.mockito.ArgumentCaptor<Object> actor = org.mockito.ArgumentCaptor.forClass(Object.class);
        org.mockito.ArgumentCaptor<Object> resource = org.mockito.ArgumentCaptor.forClass(Object.class);
        org.mockito.ArgumentCaptor<Object> resourceId = org.mockito.ArgumentCaptor.forClass(Object.class);
        org.mockito.ArgumentCaptor<Object> detail = org.mockito.ArgumentCaptor.forClass(Object.class);
        org.mockito.ArgumentCaptor<Object> requestId = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate).update(anyString(), id.capture(), type.capture(), actor.capture(),
                resource.capture(), resourceId.capture(), detail.capture(), requestId.capture());
        assertThat(type.getValue()).isEqualTo("POS_REFUND");
        assertThat(actor.getValue()).isEqualTo("cashier");
        assertThat(String.valueOf(detail.getValue())).hasSize(1024).endsWith("...");
        assertThat(requestId.getValue()).isEqualTo("req-9");
    }

    @Test
    void queriesByType() {
        when(jdbcTemplate.query(anyString(), org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<AuditEventService.AuditEvent>>any(), eq("LOGIN_SUCCESS")))
                .thenReturn(List.of());
        assertThat(service.findByType("LOGIN_SUCCESS")).isEmpty();
    }
}