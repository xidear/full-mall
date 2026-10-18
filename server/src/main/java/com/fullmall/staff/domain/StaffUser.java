package com.fullmall.staff.domain;

import java.util.UUID;

public record StaffUser(
        UUID id,
        String username,
        String displayName,
        String role,
        String status
) {
}