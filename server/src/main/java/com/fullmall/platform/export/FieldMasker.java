package com.fullmall.platform.export;

/**
 * 字段脱敏工具（SEC-002）。
 *
 * <p>这里只做**字符串掩码**，不是字段权限框架：哪些字段可以出现在导出里，由按角色定义的
 * DTO/投影决定（CR-020、CR-032），禁止用掩码代替“不输出敏感字段”。
 *
 * <p>null 保持 null，不做空串兜底；长度不足以掩码时明确返回全掩码，而不是原样输出。
 */
public final class FieldMasker {

    private FieldMasker() {
    }

    /** 手机号：保留前 3 与后 4，如 13800000001 → 138****0001。 */
    public static String phone(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() < 7) {
            return "*".repeat(trimmed.length());
        }
        String head = trimmed.substring(0, 3);
        String tail = trimmed.substring(trimmed.length() - 4);
        return head + "*".repeat(Math.max(4, trimmed.length() - 7)) + tail;
    }

    /** 邮箱：保留首字符与域名，如 zhangsan@example.com → z***@example.com。 */
    public static String email(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        int at = trimmed.indexOf('@');
        if (at <= 0 || at == trimmed.length() - 1) {
            return "*".repeat(trimmed.length());
        }
        return trimmed.charAt(0) + "***" + trimmed.substring(at);
    }

    /** 身份证/证件号：保留前 6 与后 4，中间掩码。 */
    public static String idCard(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 10) {
            return "*".repeat(trimmed.length());
        }
        String head = trimmed.substring(0, 6);
        String tail = trimmed.substring(trimmed.length() - 4);
        return head + "*".repeat(trimmed.length() - 10) + tail;
    }

    /** 姓名：保留首字符，如 张三 → 张*。 */
    public static String name(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return trimmed.charAt(0) + "*".repeat(trimmed.length() - 1);
    }
}