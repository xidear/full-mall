package com.fullmall.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoReadOnlyFilterTest {

    private final DemoReadOnlyFilter filter = new DemoReadOnlyFilter(true, new ObjectMapper());

    @Test
    void blocksWritesInReadOnlyDemo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/checkout");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(405, response.getStatus());
        assertTrue(response.getContentAsString().contains("DEMO_READ_ONLY"));
        assertNull(chain.getRequest());
    }

    @Test
    void allowsGetRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
    }
}