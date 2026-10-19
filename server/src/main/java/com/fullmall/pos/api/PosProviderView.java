package com.fullmall.pos.api;

public record PosProviderView(
        String code,
        String label,
        boolean requiresMember,
        boolean mock
) {
}
