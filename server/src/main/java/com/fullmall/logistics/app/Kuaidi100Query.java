package com.fullmall.logistics.app;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 快递100 实时查询的请求构造（**不含网络调用**，便于单测）。
 *
 * <p>签名规则（官方文档）：`sign = MD5(param + key + customer).toUpperCase()`，
 * 其中 `param` 是 JSON 字符串 {com, num, resultv2}。
 *
 * <p>为什么把签名单独拆出来：签名错是这类对接最常见的失败原因，且没有 AK 时无法端到端验证；
 * 拆成纯函数后至少有确定性测试兜底。
 */
public final class Kuaidi100Query {

    private Kuaidi100Query() {
    }

    public record Form(String customer, String sign, String param) {
        public Map<String, String> asMap() {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("customer", customer);
            map.put("sign", sign);
            map.put("param", param);
            return map;
        }
    }

    public static Form build(String customer, String key, String carrierCode, String trackingNo) {
        if (customer == null || customer.isBlank()) {
            throw new IllegalArgumentException("快递100 customer 不能为空");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("快递100 key 不能为空");
        }
        if (carrierCode == null || carrierCode.isBlank()) {
            throw new IllegalArgumentException("承运商编码不能为空");
        }
        if (trackingNo == null || trackingNo.isBlank()) {
            throw new IllegalArgumentException("运单号不能为空");
        }
        String param = "{\"com\":\"" + carrierCode.trim().toLowerCase(Locale.ROOT)
                + "\",\"num\":\"" + trackingNo.trim() + "\",\"resultv2\":\"4\"}";
        return new Form(customer.trim(), sign(param, key.trim(), customer.trim()), param);
    }

    static String sign(String param, String key, String customer) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest((param + key + customer).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("MD5 不可用", error);
        }
    }
}