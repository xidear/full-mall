package com.fullmall.platform.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** 页深保护：超上限/非法 page 一律 400，正常页放行。 */
class PaginationDepthGuardTest {

    private MockHttpServletResponse run(String page) throws Exception {
        var filter = new PaginationDepthGuardFilter(500);
        var request = new MockHttpServletRequest("GET", "/admin/v1/orders");
        if (page != null) {
            request.setParameter("page", page);
        }
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void normalPagePassesThrough() throws Exception {
        assertThat(run("3").getStatus()).isEqualTo(200);
        assertThat(run(null).getStatus()).isEqualTo(200);
    }

    @Test
    void tooDeepOrInvalidPageIsRejected() throws Exception {
        assertThat(run("501").getStatus()).isEqualTo(400);
        assertThat(run("501").getContentAsString()).contains("PAGE_DEPTH_EXCEEDED");
        assertThat(run("-1").getStatus()).isEqualTo(400);
        assertThat(run("abc").getStatus()).isEqualTo(400);
    }
}
