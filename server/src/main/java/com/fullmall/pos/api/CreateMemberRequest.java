package com.fullmall.pos.api;

import jakarta.validation.constraints.NotBlank;

public record CreateMemberRequest(
        @NotBlank String name,
        @NotBlank String phone,
        String tier,
        java.time.LocalDate birthday
) {
}
