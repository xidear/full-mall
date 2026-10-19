package com.fullmall.platform.error;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

/** 未知路径必须返回 404，而不是 500（否则"路径写错"会被当成"服务故障"）。 */
class NotFoundHttpTest {

    @Test
    void unknownPathMapsTo404() {
        var request = new MockHttpServletRequest("GET", "/api/status");
        var exception = new NoResourceFoundException(
                org.springframework.http.HttpMethod.GET, "api/status", null);

        var response = new GlobalExceptionHandler().handleNotFound(exception, request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
    }
}
