package com.fullmall.platform.error;

import java.util.List;

public record ErrorResponse(String code, String message, String requestId, List<String> details) {
}