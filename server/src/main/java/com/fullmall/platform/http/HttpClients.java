package com.fullmall.platform.http;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 统一 HTTP 客户端（10 处适配器共用，避免每个适配器各自建连接池、超时不一致）。
 *
 * <p>默认：连接超时 5 秒；**不跟随重定向**（支付/Webhook 场景跟着跳转很危险）；
 * 复用同一实例可获得连接复用与统一行为。
 */
public final class HttpClients {

    private static final HttpClient SHARED = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private HttpClients() {
    }

    public static HttpClient shared() {
        return SHARED;
    }
}
