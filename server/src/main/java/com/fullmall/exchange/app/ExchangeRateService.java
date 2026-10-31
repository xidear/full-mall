package com.fullmall.exchange.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.money.CurrencyPrecision;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * 汇率（阶段 1）：手工维护 + 精确换算。
 *
 * <p>rate 的含义固定为「1 单位 base = rate 单位 quote」。换算全程 BigDecimal：
 * 先转主单位，再乘汇率，再按目标币种小数位 HALF_UP；禁止 double。
 *
 * <p>没有直接汇率时只允许**反向**推导（quote→base 取倒数），不做多跳三角换算——
 * 三角换算会引入两次舍入，且中间币种的选择没有业务依据。找不到就明确失败。
 */
@Service
public class ExchangeRateService {

    private static final MathContext INVERSE_CONTEXT = MathContext.DECIMAL128;

    private final JdbcTemplate jdbc;

    public ExchangeRateService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record RateView(UUID id, String baseCurrency, String quoteCurrency, BigDecimal rate,
                           String source, Instant effectiveAt, String createdBy, Instant createdAt) {
    }

    public record Conversion(long amount, String fromCurrency, long convertedAmount,
                             String toCurrency, BigDecimal rate, Instant effectiveAt,
                             boolean inverse, long fromMinorUnits, long toMinorUnits) {
    }

    public List<RateView> list(int limit) {
        int safe = Math.min(Math.max(limit, 1), 500);
        return jdbc.query("""
                SELECT id, base_currency, quote_currency, rate, source, effective_at,
                       created_by, created_at
                  FROM exchange_rate
                 ORDER BY effective_at DESC, base_currency, quote_currency
                 LIMIT ?
                """, (rs, rowNum) -> new RateView(
                rs.getObject("id", UUID.class), rs.getString("base_currency"),
                rs.getString("quote_currency"), rs.getBigDecimal("rate"),
                rs.getString("source"), rs.getTimestamp("effective_at").toInstant(),
                rs.getString("created_by"), rs.getTimestamp("created_at").toInstant()), safe);
    }

    /**
     * 新增一条汇率。允许同一对币种保留历史（effective_at 不同），
     * 同一时刻重复录入返回 409，而不是悄悄覆盖。
     */
    @Transactional
    public RateView upsert(String baseCurrency, String quoteCurrency, BigDecimal rate,
                           String source, Instant effectiveAt, String actor) {
        String base = currency(baseCurrency, "base");
        String quote = currency(quoteCurrency, "quote");
        if (base.equals(quote)) {
            throw ApiException.badRequest("EXCHANGE_RATE_SAME_CURRENCY", "源币种与目标币种不能相同");
        }
        if (rate == null || rate.signum() <= 0) {
            throw ApiException.badRequest("EXCHANGE_RATE_INVALID", "汇率必须大于 0");
        }
        if (actor == null || actor.isBlank()) {
            throw ApiException.badRequest("ACTOR_REQUIRED", "必须记录操作人");
        }
        Instant at = effectiveAt == null ? Instant.now() : effectiveAt;
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("""
                    INSERT INTO exchange_rate (id, base_currency, quote_currency, rate, source,
                                               effective_at, created_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, id, base, quote, rate, blank(source) ? "MANUAL" : source.trim(),
                    Timestamp.from(at), actor);
        } catch (org.springframework.dao.DuplicateKeyException exception) {
            throw ApiException.conflict("EXCHANGE_RATE_EXISTS",
                    "同一对币种在该生效时刻已有汇率：" + base + "->" + quote + " @" + at);
        }
        return find(id).orElseThrow(() -> new IllegalStateException("汇率写入后读取失败：" + id));
    }

    private Optional<RateView> find(UUID id) {
        return jdbc.query("""
                SELECT id, base_currency, quote_currency, rate, source, effective_at,
                       created_by, created_at
                  FROM exchange_rate WHERE id = ?
                """, (rs, rowNum) -> new RateView(
                rs.getObject("id", UUID.class), rs.getString("base_currency"),
                rs.getString("quote_currency"), rs.getBigDecimal("rate"),
                rs.getString("source"), rs.getTimestamp("effective_at").toInstant(),
                rs.getString("created_by"), rs.getTimestamp("created_at").toInstant()),
                id).stream().findFirst();
    }

    @Transactional
    public void delete(UUID id, String actor) {
        if (actor == null || actor.isBlank()) {
            throw ApiException.badRequest("ACTOR_REQUIRED", "必须记录操作人");
        }
        int rows = jdbc.update("DELETE FROM exchange_rate WHERE id = ?", id);
        if (rows != 1) {
            throw ApiException.notFound("EXCHANGE_RATE_NOT_FOUND", "汇率不存在：" + id);
        }
    }

    /** 精确换算：金额是最小单位整数，结果也是最小单位整数。 */
    public Conversion convert(long amountMinor, String fromCurrency, String toCurrency, Instant at) {
        String from = currency(fromCurrency, "from");
        String to = currency(toCurrency, "to");
        if (from.equals(to)) {
            return new Conversion(amountMinor, from, amountMinor, to, BigDecimal.ONE,
                    at == null ? Instant.now() : at, false,
                    CurrencyPrecision.minorUnits(from), CurrencyPrecision.minorUnits(to));
        }
        Instant when = at == null ? Instant.now() : at;
        Optional<RateView> direct = latest(from, to, when);
        boolean inverse = false;
        RateView rate = direct.orElse(null);
        if (rate == null) {
            rate = latest(to, from, when).orElse(null);
            inverse = rate != null;
        }
        if (rate == null) {
            throw ApiException.conflict("EXCHANGE_RATE_NOT_FOUND",
                    "没有可用汇率：" + from + " -> " + to + " @" + when);
        }
        BigDecimal effectiveRate = inverse
                ? BigDecimal.ONE.divide(rate.rate(), INVERSE_CONTEXT)
                : rate.rate();
        int fromUnits = CurrencyPrecision.minorUnits(from);
        int toUnits = CurrencyPrecision.minorUnits(to);
        long converted = BigDecimal.valueOf(amountMinor)
                .movePointLeft(fromUnits)
                .multiply(effectiveRate)
                .movePointRight(toUnits)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        return new Conversion(amountMinor, from, converted, to,
                effectiveRate.stripTrailingZeros(), rate.effectiveAt(), inverse, fromUnits, toUnits);
    }

    private Optional<RateView> latest(String base, String quote, Instant at) {
        return jdbc.query("""
                SELECT id, base_currency, quote_currency, rate, source, effective_at,
                       created_by, created_at
                  FROM exchange_rate
                 WHERE base_currency = ? AND quote_currency = ? AND effective_at <= ?
                 ORDER BY effective_at DESC
                 LIMIT 1
                """, (rs, rowNum) -> new RateView(
                rs.getObject("id", UUID.class), rs.getString("base_currency"),
                rs.getString("quote_currency"), rs.getBigDecimal("rate"),
                rs.getString("source"), rs.getTimestamp("effective_at").toInstant(),
                rs.getString("created_by"), rs.getTimestamp("created_at").toInstant()),
                base, quote, Timestamp.from(at)).stream().findFirst();
    }

    private static String currency(String value, String field) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("EXCHANGE_RATE_FIELDS_REQUIRED", field + " 币种不能为空");
        }
        String code = value.trim().toUpperCase(Locale.ROOT);
        CurrencyPrecision.minorUnits(code); // 顺带校验 ISO-4217
        return code;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}