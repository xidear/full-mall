package com.fullmall.supplier.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullmall.supplier.app.SupplierSecretCipher;
import com.fullmall.supplier.infra.SupplierRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SupplierHmacFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SupplierHmacFilter.class);

    public static final String SUPPLIER_ID_ATTRIBUTE = "supplierId";
    public static final String ACCESS_KEY_HEADER = "X-Supplier-Key";
    public static final String TIMESTAMP_HEADER = "X-Supplier-Timestamp";
    public static final String NONCE_HEADER = "X-Supplier-Nonce";
    public static final String SIGNATURE_HEADER = "X-Supplier-Signature";

    private final SupplierRepository repository;
    private final SupplierSecretCipher cipher;
    private final ObjectMapper objectMapper;
    private final long timestampSkewSeconds;
    private final SupplierNonceStore nonceStore;
    private final SupplierRateLimiter rateLimiter;
    private final com.fullmall.supplier.app.SupplierTokenService tokenService;
    private final int rateLimitMaxRequests;
    private final long rateLimitWindowSeconds;

    public SupplierHmacFilter(SupplierRepository repository,
                              SupplierSecretCipher cipher,
                              ObjectMapper objectMapper,
                              @Value("${app.supplier.timestamp-skew-seconds:300}") long timestampSkewSeconds,
                              SupplierNonceStore nonceStore,
                              SupplierRateLimiter rateLimiter,
                              com.fullmall.supplier.app.SupplierTokenService tokenService,
                              @Value("${app.supplier.rate-limit.max-requests:120}") int rateLimitMaxRequests,
                              @Value("${app.supplier.rate-limit.window-seconds:60}") long rateLimitWindowSeconds) {
        this.repository = repository;
        this.cipher = cipher;
        this.objectMapper = objectMapper;
        this.timestampSkewSeconds = timestampSkewSeconds;
        this.nonceStore = nonceStore;
        this.rateLimiter = rateLimiter;
        this.tokenService = tokenService;
        this.rateLimitMaxRequests = rateLimitMaxRequests;
        this.rateLimitWindowSeconds = rateLimitWindowSeconds;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 预检请求不带签名头（浏览器不会给 OPTIONS 加自定义头），必须放过去交给 MVC 的 CORS 处理，
        // 否则分端口部署时供应门户发不出任何 /open/** 请求：预检被这里拦成 401 且没有 Allow-Origin（实测现象）。
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        return !request.getRequestURI().startsWith("/open/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        byte[] body = request.getInputStream().readAllBytes();
        CachedBodyRequest cachedRequest = new CachedBodyRequest(request, body);

        // 认证路径二：Bearer Token（SUP-030）。HMAC 头存在时仍走 HMAC，保持向后兼容。
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length()).trim();
            java.util.Optional<UUID> supplierId = tokenService.verify(token);
            if (supplierId.isEmpty()) {
                unauthorized(response, "SUPPLIER_TOKEN_INVALID", "供应商 token 无效、已过期或已吊销");
                return;
            }
            SupplierRateLimiter.Decision tokenDecision = rateLimiter.check("token:" + supplierId.get(),
                    rateLimitMaxRequests, java.time.Duration.ofSeconds(rateLimitWindowSeconds), Instant.now());
            if (!tokenDecision.allowed()) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.setHeader("Retry-After", String.valueOf(tokenDecision.retryAfterSeconds()));
                objectMapper.writeValue(response.getWriter(), java.util.Map.of(
                        "code", "SUPPLIER_RATE_LIMITED",
                        "message", "请求过于频繁，请 " + tokenDecision.retryAfterSeconds() + " 秒后重试",
                        "requestId", UUID.randomUUID().toString()));
                return;
            }
            request.setAttribute(SUPPLIER_ID_ATTRIBUTE, supplierId.get());
            SecurityContext tokenContext = SecurityContextHolder.createEmptyContext();
            tokenContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                    "supplier-token:" + supplierId.get(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_SUPPLIER"))));
            SecurityContextHolder.setContext(tokenContext);
            try {
                filterChain.doFilter(cachedRequest, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
            return;
        }

        String accessKey = request.getHeader(ACCESS_KEY_HEADER);
        String timestampValue = request.getHeader(TIMESTAMP_HEADER);
        String nonce = request.getHeader(NONCE_HEADER);
        String signature = request.getHeader(SIGNATURE_HEADER);
        if (isBlank(accessKey) || isBlank(timestampValue) || isBlank(nonce) || isBlank(signature)) {
            unauthorized(response, "SUPPLIER_SIGNATURE_REQUIRED",
                    "缺少供应商签名头：X-Supplier-Key/Timestamp/Nonce/Signature");
            return;
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampValue.trim());
        } catch (NumberFormatException exception) {
            unauthorized(response, "SUPPLIER_TIMESTAMP_INVALID", "X-Supplier-Timestamp 必须是 Unix 秒");
            return;
        }
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - timestamp) > timestampSkewSeconds) {
            unauthorized(response, "SUPPLIER_TIMESTAMP_EXPIRED",
                    "请求时间戳超出允许范围 ±" + timestampSkewSeconds + " 秒");
            return;
        }
        if (nonce.length() > 128) {
            unauthorized(response, "SUPPLIER_NONCE_INVALID", "X-Supplier-Nonce 不能超过 128 个字符");
            return;
        }
        if (!nonceStore.tryRegister(accessKey.trim(), nonce, Instant.now(), timestampSkewSeconds)) {
            conflict(response, "SUPPLIER_NONCE_REPLAYED", "Nonce 已使用，请重新生成");
            return;
        }

        SupplierRepository.CredentialRow credential = repository.findCredential(accessKey.trim());
        if (credential == null || !credential.active()) {
            unauthorized(response, "SUPPLIER_CREDENTIAL_INVALID", "供应商 Access Key 无效或已停用");
            return;
        }
        // SUP-030：IP 白名单（多段 CIDR）。配置错误会抛错而不是悄悄放行。
        SupplierIpAllowlist allowlist = SupplierIpAllowlist.parse(credential.ipAllowlist());
        if (allowlist.isConfigured() && !allowlist.allows(request.getRemoteAddr())) {
            unauthorized(response, "SUPPLIER_IP_NOT_ALLOWED",
                    "调用来源 IP 不在白名单内: " + request.getRemoteAddr());
            return;
        }
        String secret;
        try {
            secret = cipher.decrypt(credential.secretEncrypted());
        } catch (IllegalStateException exception) {
            unauthorized(response, "SUPPLIER_CREDENTIAL_UNREADABLE", "供应商凭证无法解密，请轮换凭证");
            return;
        }

        String signedPath = request.getRequestURI()
                + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
        String canonical = request.getMethod() + "\n"
                + signedPath + "\n"
                + timestampValue.trim() + "\n"
                + nonce + "\n"
                + sha256Hex(body);
        String expected = hmacHex(secret, canonical);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            log.warn("Supplier signature mismatch canonical={}", canonical.replace("\n", "|"));
            unauthorized(response, "SUPPLIER_SIGNATURE_INVALID", "供应商签名校验失败");
            return;
        }

        // SUP-033：验签通过后按 access key 限流，超限 429 + Retry-After
        SupplierRateLimiter.Decision decision = rateLimiter.check(accessKey.trim(), rateLimitMaxRequests,
                java.time.Duration.ofSeconds(rateLimitWindowSeconds), Instant.now());
        if (!decision.allowed()) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            objectMapper.writeValue(response.getWriter(), java.util.Map.of(
                    "code", "SUPPLIER_RATE_LIMITED",
                    "message", "请求过于频繁，请 " + decision.retryAfterSeconds() + " 秒后重试",
                    "requestId", UUID.randomUUID().toString()));
            return;
        }

        request.setAttribute(SUPPLIER_ID_ATTRIBUTE, credential.supplierId());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                "supplier:" + accessKey, null, List.of(new SimpleGrantedAuthority("ROLE_SUPPLIER"))));
        SecurityContextHolder.setContext(context);
        try {
            filterChain.doFilter(cachedRequest, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String sha256Hex(byte[] body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String hmacHex(String secret, String canonical) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void unauthorized(HttpServletResponse response, String code, String message) throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED, code, message);
    }

    private void conflict(HttpServletResponse response, String code, String message) throws IOException {
        write(response, HttpServletResponse.SC_CONFLICT, code, message);
    }

    private void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "code", code,
                "message", message,
                "requestId", UUID.randomUUID().toString()));
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException("Async read is not supported");
                }

                @Override
                public int read() {
                    return input.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}