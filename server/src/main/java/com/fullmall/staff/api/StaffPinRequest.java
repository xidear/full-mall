package com.fullmall.staff.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public final class StaffPinRequest {

    private StaffPinRequest() {
    }

    public record SetPin(@NotBlank String pin) {
    }

    public record BindStore(@NotNull UUID storeId) {
    }

    public record ManagerAuth(@NotBlank String username, @NotBlank String pin) {
    }
}
