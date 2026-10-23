package com.fullmall.platform.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * 全局 JSON 入口（避免每个类各自 `new ObjectMapper()`）。
 *
 * <p>线程安全：ObjectMapper 配置完成后可安全共享。
 * 统一配置：未知字段忽略（对外部响应更宽容，但**解析失败仍抛错**）、不输出时间戳数组。
 */
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private Json() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception error) {
            throw new IllegalStateException("JSON 序列化失败：" + error.getMessage(), error);
        }
    }

    public static JsonNode read(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception error) {
            throw new IllegalStateException("JSON 解析失败：" + error.getMessage(), error);
        }
    }

    public static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception error) {
            throw new IllegalStateException("JSON 解析失败：" + error.getMessage(), error);
        }
    }
}
