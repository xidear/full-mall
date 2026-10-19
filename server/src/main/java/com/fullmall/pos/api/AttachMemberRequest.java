package com.fullmall.pos.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttachMemberRequest(@NotNull UUID memberId) {
}
