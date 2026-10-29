package com.fullmall.platform.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 兜底映射：框架级异常不能变成 500。
 *
 * <p>回归背景：后台编辑页 GET /admin/v1/products/{id} 返回 500 "Internal server error"，
 * 真实原因是后端没有实现该 GET，Spring 抛 HttpRequestMethodNotSupportedException 落到兜底处理器。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void methodNotSupportedBecomes405WithAllowedMethods() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/admin/v1/products/aaaa0000-0000-0000-0000-0000000000c1");

        ResponseEntity<ErrorResponse> response = handler.onMethodNotSupported(
                new HttpRequestMethodNotSupportedException("GET", List.of("PUT", "PATCH")), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("METHOD_NOT_ALLOWED");
        assertThat(response.getBody().message()).contains("GET").contains("/admin/v1/products");
    }

    @Test
    void missingParameterBecomes400() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ResponseEntity<ErrorResponse> response = handler.onMissingParameter(
                new org.springframework.web.bind.MissingServletRequestParameterException("type", "String"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("MISSING_PARAMETER");
        assertThat(response.getBody().message()).contains("type");
    }
}
