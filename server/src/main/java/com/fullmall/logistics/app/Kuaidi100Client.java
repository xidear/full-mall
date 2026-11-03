package com.fullmall.logistics.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.stream.Collectors;

/**
 * 快递100 查询客户端（真实 HTTP）。
 *
 * <p>只在配置了 customer/key 时可用；未配置时**明确拒绝**而不是返回空轨迹
 * （空轨迹会让客服误以为"包裹没动"，比报错更危险）。
 *
 * <p>用 JDK 自带 `HttpClient`：该接口是极简单的表单 POST，引 SDK 反而多一层依赖。
 */
@Component
public class Kuaidi100Client {

    private final String customer;
    private final String key;
    private final String endpoint;
    private final HttpClient http = com.fullmall.platform.http.HttpClients.shared();

    public Kuaidi100Client(
            @Value("${app.logistics.kuaidi100.customer:}") String customer,
            @Value("${app.logistics.kuaidi100.key:}") String key,
            @Value("${app.logistics.kuaidi100.endpoint:https://poll.kuaidi100.com/poll/query.do}")
            String endpoint) {
        this.customer = customer;
        this.key = key;
        this.endpoint = endpoint;
    }

    public boolean configured() {
        return customer != null && !customer.isBlank() && key != null && !key.isBlank();
    }

    /** 用环境变量里的凭据查询；后台配置优先的场景请用 {@link #query(String, String, String, String)}。 */
    public String query(String carrierCode, String trackingNo) {
        if (!configured()) {
            throw com.fullmall.platform.error.ApiException.conflict("LOGISTICS_NOT_CONFIGURED",
                    "快递100 未配置（app.logistics.kuaidi100.customer/key），无法查询真实轨迹");
        }
        return query(customer, key, carrierCode, trackingNo);
    }

    /**
     * 用调用方给定的凭据查询：凭据的唯一来源是后台配置（集成/系统设置），
     * 环境变量只是迁移期兼容，因此凭据必须能由上层传进来。
     */
    public String query(String customer, String key, String carrierCode, String trackingNo) {
        if (customer == null || customer.isBlank() || key == null || key.isBlank()) {
            throw com.fullmall.platform.error.ApiException.conflict("LOGISTICS_NOT_CONFIGURED",
                    "快递100 customer/key 不能为空，无法查询真实轨迹");
        }
        var form = Kuaidi100Query.build(customer, key, carrierCode, trackingNo);
        String body = form.asMap().entrySet().stream()
                .map(entry -> entry.getKey() + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw com.fullmall.platform.error.ApiException.conflict("LOGISTICS_HTTP_ERROR",
                        "快递100 返回 " + response.statusCode() + "：" + response.body());
            }
            return response.body();
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw com.fullmall.platform.error.ApiException.conflict("LOGISTICS_QUERY_FAILED",
                    "查询快递100 失败：" + error.getMessage());
        }
    }
}
