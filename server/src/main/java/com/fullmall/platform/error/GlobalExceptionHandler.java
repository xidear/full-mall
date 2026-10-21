package com.fullmall.platform.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 不存在的路径 → **404**。
     *
     * <p>Spring Boot 4 对未知路径抛 {@code NoResourceFoundException}（静态资源兜底），
     * 若不显式处理会落到 500 并打完整堆栈，把"路径写错"伪装成"服务故障"，
     * 也让健康探测大量刷错误日志。
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            org.springframework.web.servlet.resource.NoResourceFoundException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", "接口不存在: " + request.getRequestURI(),
                        requestId(request), List.of()));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.status())
                .body(new ErrorResponse(exception.code(), exception.getMessage(), requestId(request), List.of()));
    }

    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            org.springframework.dao.OptimisticLockingFailureException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("VERSION_CONFLICT",
                        "数据已被其他操作修改，请刷新后重试", requestId(request), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_FAILED", "Request validation failed", requestId(request), details));
    }

    /**
     * 请求体无法解析（字段缺失、类型不匹配、非法 UUID）→ **400**，不是 500。
     *
     * <p>为什么重要：这类错误此前落到兜底处理器返回 500，让"客户端传错参数"看起来像"服务端崩了"，
     * 排查时会把时间浪费在错误的方向。
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> onUnreadableBody(
            org.springframework.http.converter.HttpMessageNotReadableException error,
            HttpServletRequest request) {
        String detail = error.getMostSpecificCause().getMessage();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "INVALID_REQUEST_BODY",
                "请求体无法解析（字段缺失或类型错误）：" + detail,
                requestId(request), List.of()));
    }

    /**
     * 路径/查询参数类型不匹配（例如 `{id}` 传了非 UUID）→ **400**。
     *
     * <p>与"请求体无法解析"同类：属于调用方错误，不是服务端故障。
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> onTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException error,
            HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "INVALID_PARAMETER_TYPE",
                "参数 " + error.getName() + " 类型不正确：" + error.getMessage(),
                requestId(request), List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        String requestId = requestId(request);
        log.error("Unhandled request failure requestId={} path={}", requestId, request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Internal server error", requestId, List.of()));
    }

    private String requestId(HttpServletRequest request) {
        String header = request.getHeader("X-Request-Id");
        return header == null || header.isBlank() ? UUID.randomUUID().toString() : header;
    }
}