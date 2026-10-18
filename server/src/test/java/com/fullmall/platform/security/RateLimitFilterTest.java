package com.fullmall.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitFilterTest {

    @Test
    void blocksWritesOverLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 2, 60, new ObjectMapper());

        assertPasses(filter, "POST");
        assertPasses(filter, "POST");
        assertBlocked(filter, "POST");
    }

    @Test
    void doesNotLimitReads() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 1, 60, new ObjectMapper());
        assertPasses(filter, "GET");
        assertPasses(filter, "GET");
    }

    private void assertPasses(RateLimitFilter filter, String method) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/checkout");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
    }

    private void assertBlocked(RateLimitFilter filter, String method) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/checkout");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains("RATE_LIMITED"));
        assertNull(chain.getRequest());
    }
}