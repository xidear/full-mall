package com.fullmall.platform.channel.sender;

import java.util.Map;

/**
 * 通道配置读取器：缺必填项立刻抛错，错误信息带上字段中文名与配置项 key——
 * 否则运营只知道「发送失败」，不知道该去后台补哪一项。
 */
public record ChannelSettings(Map<String, String> values) {

    public String required(String key, String label) {
        String value = values == null ? null : values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("通道配置缺少必填项：" + label + "（" + key + "）");
        }
        return value.trim();
    }

    public String optional(String key) {
        String value = values == null ? null : values.get(key);
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String optional(String key, String fallback) {
        String value = optional(key);
        return value == null ? fallback : value;
    }
}
