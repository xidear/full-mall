package com.fullmall.staff.domain;

import java.time.Instant;

public record StaffSessionUser(StaffUser user, Instant expiresAt) {
}