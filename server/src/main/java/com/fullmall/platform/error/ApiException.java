package com.fullmall.platform.error;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public static ApiException tooManyRequests(String code, String message) {
        return new ApiException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, code, message);
    }

    public static ApiException serviceUnavailable(String code, String message) {
        return new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, code, message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}