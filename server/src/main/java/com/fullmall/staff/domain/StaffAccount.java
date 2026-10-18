package com.fullmall.staff.domain;

public record StaffAccount(StaffUser user, String passwordHash) {
}