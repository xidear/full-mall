package com.fullmall.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

/**
 * Boot 4 移除了 `TestRestTemplate`，这里用 `RestClient` 提供项目现有测试所需的最小等价 API。
 *
 * <p>两个关键点：
 * <ul>
 *   <li>用 {@code exchange} 而不是 {@code retrieve}：**4xx/5xx 必须原样返回**，
 *       否则断言状态码的负向测试会直接抛异常；</li>
 *   <li>显式注册 Jackson 2 转换器：Boot 4 默认 Jackson 3，`JsonNode` 需要 Jackson 2 的 converter。</li>
 * </ul>
 */
public class TestHttp {

    private final RestClient client;
    private final String baseUrl;

    public TestHttp(int port) {
        this.client = RestClient.builder()
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    // 字符串转换器必须在 Jackson 之前，否则 application/json 的纯文本响应会被 Jackson 抢走
                    converters.add(0, new org.springframework.http.converter.StringHttpMessageConverter(
                            java.nio.charset.StandardCharsets.UTF_8));
                    converters.add(1, new MappingJackson2HttpMessageConverter(new ObjectMapper()));
                })
                .build();
        this.baseUrl = "http://localhost:" + port;
    }

    public <T> ResponseEntity<T> getForEntity(String path, Class<T> responseType) {
        return exchange(path, HttpMethod.GET, HttpEntity.EMPTY, responseType);
    }

    public <T> ResponseEntity<T> postForEntity(String path, Object body, Class<T> responseType) {
        return exchange(path, HttpMethod.POST, new HttpEntity<>(body), responseType);
    }

    public <T> ResponseEntity<T> exchange(String path, HttpMethod method,
                                          HttpEntity<?> requestEntity, Class<T> responseType) {
        RestClient.RequestBodySpec spec = client.method(method).uri(baseUrl + path);
        if (requestEntity != null && requestEntity.getHeaders() != null) {
            requestEntity.getHeaders().forEach((name, values) ->
                    values.forEach(value -> spec.header(name, value)));
        }
        Object body = requestEntity == null ? null : requestEntity.getBody();
        RestClient.RequestBodySpec withBody = body == null ? spec : spec.body(body);

        return withBody.exchange((request, response) -> {
            ResponseEntity.BodyBuilder builder = ResponseEntity.status(response.getStatusCode());
            response.getHeaders().forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
            return builder.body(response.bodyTo(responseType));
        });
    }
}