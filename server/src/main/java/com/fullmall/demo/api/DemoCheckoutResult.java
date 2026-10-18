package com.fullmall.demo.api;

import java.util.Map;

public record DemoCheckoutResult(
        String scenario,
        boolean readOnly,
        Map<String, Object> order,
        String notice
) {
}