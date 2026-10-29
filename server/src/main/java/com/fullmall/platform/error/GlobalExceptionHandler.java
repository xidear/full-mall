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

/**
 * 全局异常 → HTTP 状态 + 稳定错误码 的唯一出口（MVC 层）。
 *
 * <p>口径（AGENTS.md §10）：message 面向用户、不泄露实现，所以兜底固定 {@code Internal server error}，
 * 不要拼异常文本（会把 SQL、类名送到顾客浏览器）；原始原因只进日志，管理端凭 requestId 反查。
 *
 * <p>边界：① 只覆盖进入 MVC 之后的异常，过滤器阶段自己手写 JSON；② 有 {@code Exception} 兜底，
 * 故 Spring 的 {@code ResponseStatusException}（如 ManagerAuthController 的 401）也会变成 500。
 */
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

    /** 业务异常直达：status 与 code 取自 {@link ApiException}，不再改写——否则同一错误会出现两种码。 */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.status())
                .body(new ErrorResponse(exception.code(), exception.getMessage(), requestId(request), List.of()));
    }

    /** 乐观锁失败 → **409**：让用户刷新重取，不要自动重试覆盖对方的修改（AGENTS.md §10.3）。 */
    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            org.springframework.dao.OptimisticLockingFailureException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("VERSION_CONFLICT",
                        "数据已被其他操作修改，请刷新后重试", requestId(request), List.of()));
    }

    /** Bean Validation 失败 → **400**。details 必须是 "字段名: 消息"，字段名不能抹掉，否则前端无法定位输入框。 */
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

    /** CSV 导入的行级校验失败 → **400**，并把每行问题带回。 */
    @ExceptionHandler(com.fullmall.platform.importing.CsvImportException.class)
    public ResponseEntity<ErrorResponse> onCsvImport(
            com.fullmall.platform.importing.CsvImportException error,
            HttpServletRequest request) {
        List<String> details = error.errors().stream()
                .map(row -> "第 " + row.rowNumber() + " 行/" + row.field() + "：" + row.reason())
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "CSV_IMPORT_INVALID", error.getMessage(), requestId(request), details));
    }

    /**
     * 方法不支持（路径存在、方法用错）→ **405**，不是 500。
     *
     * <p>这是后台编辑页 500 的直接原因：前端 GET /admin/v1/products/{id}，后端只实现了 PUT，
     * Spring 抛 HttpRequestMethodNotSupportedException 落到兜底处理器变成 "Internal server error"，
     * 于是"接口没实现"被显示成"服务端崩了"。
     */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> onMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException error,
            HttpServletRequest request) {
        String supported = error.getSupportedHttpMethods() == null ? ""
                : error.getSupportedHttpMethods().stream()
                        .map(Object::toString).sorted()
                        .collect(java.util.stream.Collectors.joining("/"));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(new ErrorResponse(
                "METHOD_NOT_ALLOWED",
                "接口存在但不支持 " + error.getMethod() + " 方法: " + request.getRequestURI()
                        + (supported.isEmpty() ? "" : "（可用方法：" + supported + "）"),
                requestId(request), List.of()));
    }

    /**
     * 上传文件超过 multipart 解析层上限 → **413**，并给出可行动信息。
     *
     * <p>为什么必须单独处理：未配置 multipart 上限时 Spring Boot 默认单文件 1MB，超过会在进入
     * Controller 之前抛这个异常，落到兜底处理器变成 "Internal server error"，于是"图片传不上去"
     * 看起来像服务端崩了。业务侧体积上限（图片/视频）由 MediaUploadValidator 判定，
     * 这里只负责解析层天花板，两者报同一个错误码 ASSET_TOO_LARGE 但文案不同。
     */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> onUploadTooLarge(
            org.springframework.web.multipart.MaxUploadSizeExceededException error,
            HttpServletRequest request) {
        long ceiling = com.fullmall.platform.validation.ValidationRules.UPLOAD_MULTIPART_CEILING_BYTES;
        long reported = error.getMaxUploadSize();
        long limit = reported > 0 ? reported : ceiling;
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new ErrorResponse(
                "ASSET_TOO_LARGE",
                "文件超过单文件上限 " + limit + " 字节（≈" + (limit / 1024 / 1024)
                        + "MB），请压缩后重试；上传大小上限可在后台「系统设置 → 素材上传」调整",
                requestId(request), List.of()));
    }

    /** Content-Type 不支持 → **415**（例如把表单当 JSON 提交）。 */
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> onMediaTypeNotSupported(
            org.springframework.web.HttpMediaTypeNotSupportedException error,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(new ErrorResponse(
                "UNSUPPORTED_MEDIA_TYPE",
                "不支持的 Content-Type: " + error.getContentType(), requestId(request), List.of()));
    }

    /** 缺少必填查询参数 → **400**（不是 500）。 */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> onMissingParameter(
            org.springframework.web.bind.MissingServletRequestParameterException error,
            HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "MISSING_PARAMETER",
                "缺少必填参数 " + error.getParameterName() + "（" + error.getParameterType() + "）",
                requestId(request), List.of()));
    }

    /** 参数级校验（@Validated 在方法参数上）→ **400**。 */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> onConstraintViolation(
            jakarta.validation.ConstraintViolationException error, HttpServletRequest request) {
        List<String> details = error.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "VALIDATION_FAILED", "参数校验失败", requestId(request), details));
    }

    /**
     * 兜底 → **500**。日志里的 requestId + 路径 + 堆栈是唯一留存原始原因的地方（AGENTS.md §10.5）；
     * 新增可预期异常要补专门 handler，否则都会被记成"服务故障"。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        String requestId = requestId(request);
        log.error("Unhandled request failure requestId={} path={}", requestId, request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Internal server error", requestId, List.of()));
    }

    /** 复用网关的 {@code X-Request-Id}，缺失才生成：用户报障靠它串日志；跨域读取需 WebConfig 暴露该头。 */
    private String requestId(HttpServletRequest request) {
        String header = request.getHeader("X-Request-Id");
        return header == null || header.isBlank() ? UUID.randomUUID().toString() : header;
    }
}