package com.fullmall.staff.api;

import com.fullmall.staff.domain.StaffUser;

public record LoginResponse(String token, StaffUser user) {
}