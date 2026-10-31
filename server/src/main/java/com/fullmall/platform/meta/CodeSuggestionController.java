package com.fullmall.platform.meta;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.Map;

/**
 * 业务编码建议：给运营一个不会撞车的默认编码。
 *
 * <p>为什么是后端生成：编码唯一性由数据库约束保证，前端随机拼出来的码撞了只能等保存时报错。
 * 这里按表实测占用情况，生成后仍由唯一约束兜底（生成后仍可能被并发抢占，保存时明确报 409）。
 *
 * <p>不做"自动覆盖用户输入"：只在用户点"生成"时给出建议值，编码是可读业务标识，不能悄悄改。
 */
@RestController
@RequestMapping("/admin/v1/codes")
public class CodeSuggestionController {

    private static final Map<String, String[]> TYPES = Map.of(
            "BRAND", new String[]{"brand", "code", "BRAND-"},
            "COUPON", new String[]{"coupon_template", "code", "CPN-"},
            "PROMOTION", new String[]{"promotion", "code", "PROMO-"},
            "STORE", new String[]{"store", "code", "STORE-"});

    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();

    public CodeSuggestionController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/suggest")
    public Map<String, String> suggest(@RequestParam String type) {
        String[] spec = TYPES.get(type == null ? "" : type.trim().toUpperCase(java.util.Locale.ROOT));
        if (spec == null) {
            throw com.fullmall.platform.error.ApiException.badRequest("CODE_TYPE_UNSUPPORTED",
                    "不支持的编码类型：" + type + "，可选 " + TYPES.keySet());
        }
        String table = spec[0];
        String column = spec[1];
        String prefix = spec[2];
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = prefix + Long.toString(System.currentTimeMillis() % 100000_000L, 36).toUpperCase(java.util.Locale.ROOT)
                    + "-" + (random.nextInt(36 * 36) + 36 * 36);
            Integer exists = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, candidate);
            if (exists != null && exists == 0) {
                return Map.of("code", candidate);
            }
        }
        throw com.fullmall.platform.error.ApiException.serviceUnavailable("CODE_SUGGEST_FAILED",
                "连续 20 次都生成到已占用编码，请检查编码规则或稍后重试");
    }
}
