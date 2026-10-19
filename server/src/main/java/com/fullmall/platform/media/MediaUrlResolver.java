package com.fullmall.platform.media;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 媒体地址规范化：**快照与对外输出一律存绝对 http(s) URL，绝不存相对路径**。
 *
 * <p>原因（用户 2026-10-19 明确）：
 * <ol>
 *   <li>域名会变；</li>
 *   <li>小程序/客户端要求 http(s):// 绝对地址；</li>
 *   <li>存储商与存储方案会整体迁移（测试期本地 → 上线云存储）。</li>
 * </ol>
 *
 * <p>规则：
 * <ul>
 *   <li>已是 http(s):// 绝对地址 → 原样保留（历史快照指向当时的存储/CDN）；</li>
 *   <li>协议相对 `//cdn/x.jpg` → 补 `https:`；</li>
 *   <li>相对路径 → 拼接配置的公共基址（`app.media.public-base-url`）；</li>
 *   <li>含 `../` 或空白 → 拒绝（防止路径穿越与脏数据进快照）；</li>
 *   <li>null/空 → 返回 null（由调用方决定是否允许无图）。</li>
 * </ul>
 */
@Component
public class MediaUrlResolver {

    private final String publicBaseUrl;

    public MediaUrlResolver(@Value("${app.media.public-base-url:}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
    }

    public String absolute(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();

        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        if (value.startsWith("//")) {
            return "https:" + value;
        }
        if (value.contains("..")) {
            throw new IllegalArgumentException("媒体路径不允许包含 '..'： " + value);
        }
        if (publicBaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "未配置 app.media.public-base-url，无法把相对路径转为绝对地址: " + value);
        }
        String base = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        String path = value.startsWith("/") ? value : "/" + value;
        return base + path;
    }
}