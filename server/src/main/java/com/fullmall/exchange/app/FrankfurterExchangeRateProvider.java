package com.fullmall.exchange.app;

import com.fullmall.platform.json.Json;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Frankfurter（ECB 参考汇率，免费、无 API Key）。
 *
 * <p>接口：`GET {base}/latest?from=CNY`，返回 `rates: {USD: 0.1389, ...}`。
 * 只使用返回的直接汇率，不做三角换算。失败抛异常，由上层保留上次/手工汇率。
 */
public class FrankfurterExchangeRateProvider implements ExchangeRateProvider {

    // Frankfurter 已迁到 api.frankfurter.dev/v1，且旧域名会 301；显式跟随重定向
    private final java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder()
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String endpoint;

    public FrankfurterExchangeRateProvider(String endpoint) {
        this.endpoint = (endpoint == null || endpoint.isBlank())
                ? "https://api.frankfurter.dev/v1" : endpoint.trim();
    }

    @Override
    public String code() {
        return "frankfurter";
    }

    @Override
    public Map<String, BigDecimal> rates(String baseCurrency) {
        String url = endpoint + "/latest?from=" + baseCurrency;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Frankfurter HTTP " + response.statusCode());
            }
            var rates = Json.read(response.body()).path("rates");
            if (!rates.isObject() || rates.isEmpty()) {
                throw new IllegalStateException("Frankfurter 返回 rates 为空");
            }
            Map<String, BigDecimal> result = new LinkedHashMap<>();
            rates.fields().forEachRemaining(entry -> {
                BigDecimal rate = entry.getValue().decimalValue();
                if (rate.signum() > 0) {
                    result.put(entry.getKey().toUpperCase(java.util.Locale.ROOT), rate);
                }
            });
            if (result.isEmpty()) {
                throw new IllegalStateException("Frankfurter 没有有效正数汇率");
            }
            return result;
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("拉取 Frankfurter 汇率失败：" + error.getMessage(), error);
        }
    }
}