package com.fullmall.market.app;

import com.fullmall.market.domain.Market;
import com.fullmall.market.infra.MarketRepository;
import com.fullmall.platform.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 市场配置。
 *
 * <p>校验口径（都取自平台能力，不引数据集）：
 * <ul>
 *   <li>币种：{@link java.util.Currency#getInstance(String)} —— ISO 4217；</li>
 *   <li>国家：{@link Locale#getISOCountries()} —— ISO 3166-1 alpha-2；</li>
 *   <li>时区：{@link ZoneId#of(String)} —— IANA。</li>
 * </ul>
 */
@Service
public class MarketService {

    /** 市场码进 URL 与订单快照，限制成大写字母数字，避免空格/斜杠造成的路径与导出问题。 */
    private static final String CODE_PATTERN = "^[A-Z][A-Z0-9]{1,7}$";
    private static final String LOCALE_PATTERN = "^[a-z]{2,3}(-[A-Za-z0-9]{2,8})?$";
    private static final Set<String> ISO_COUNTRIES = new HashSet<>(Arrays.asList(Locale.getISOCountries()));

    private final MarketRepository repository;

    public MarketService(MarketRepository repository) {
        this.repository = repository;
    }

    public List<Market> list(boolean onlyActive) {
        return repository.findAll(onlyActive);
    }

    /** 市场必须存在，否则报 404：不允许把未知市场当成默认市场继续跑。 */
    public Market require(String code) {
        String normalized = normalizeCode(code);
        return repository.findByCode(normalized)
                .orElseThrow(() -> ApiException.notFound("MARKET_NOT_FOUND", "市场不存在: " + normalized));
    }

    /** 只接受已启用的市场；关闭的市场不能下单，也不能作为默认市场。 */
    public Market requireActive(String code) {
        Market market = require(code);
        if (!market.active()) {
            throw ApiException.conflict("MARKET_INACTIVE", "市场未启用: " + market.code());
        }
        return market;
    }

    public List<String> locales(String code) {
        return repository.locales(require(code).code());
    }

    /**
     * 是否已启用。供"设置校验"这类调用方使用：这里返回布尔而不是抛异常，
     * 因为调用方要自己组织错误文案（列出可选市场），异常会丢失上下文。
     */
    public boolean isActive(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return repository.findByCode(code.trim().toUpperCase(Locale.ROOT))
                .map(Market::active)
                .orElse(false);
    }

    /** 已启用的市场码，用于错误提示与后台下拉（替代原来写死的 CN/HK/MO/TW）。 */
    public List<String> activeCodes() {
        return repository.findAll(true).stream().map(Market::code).toList();
    }

    @Transactional
    public Market save(Market market) {
        String code = normalizeCode(market.code());
        validate(market, code);
        Market toSave = new Market(code, market.name(), market.currency().toUpperCase(Locale.ROOT),
                market.countryCode() == null ? null : market.countryCode().toUpperCase(Locale.ROOT),
                market.taxJurisdiction(), market.priceIncludesTax(), market.timezone(),
                market.legalEntityName(), market.active(), market.sort());
        repository.upsert(toSave);
        return require(code);
    }

    /**
     * 设置语言候选。
     *
     * <p>主语言必须落在候选集合内：否则会出现"市场主语言不是该市场允许的语言"这种自相矛盾的配置，
     * 前端拿不到任何可用文案。
     */
    @Transactional
    public List<String> setLocales(String code, List<String> locales, String primaryLocale) {
        Market market = require(code);
        if (locales == null || locales.isEmpty()) {
            throw ApiException.badRequest("MARKET_LOCALES_REQUIRED", "市场至少要有一个语言");
        }
        List<String> cleaned = new ArrayList<>();
        for (String locale : locales) {
            if (locale == null || !locale.matches(LOCALE_PATTERN)) {
                throw ApiException.badRequest("INVALID_MARKET_LOCALE", "语言格式应为 xx 或 xx-YY，实际=" + locale);
            }
            if (!cleaned.contains(locale)) {
                cleaned.add(locale);
            }
        }
        String primary = primaryLocale == null || primaryLocale.isBlank() ? cleaned.get(0) : primaryLocale;
        if (!cleaned.contains(primary)) {
            throw ApiException.badRequest("PRIMARY_LOCALE_NOT_ALLOWED",
                    "主语言必须在该市场的语言候选内: " + primary + " not in " + cleaned);
        }
        repository.replaceLocales(market.code(), cleaned, primary);
        return repository.locales(market.code());
    }

    /** 单条市场级设置；未配置返回 null（是否回退全局由调用方决定，避免这里悄悄兜底）。 */
    public String scopedSetting(String code, String key) {
        if (code == null || code.isBlank() || key == null || key.isBlank()) {
            return null;
        }
        return repository.setting(code.trim().toUpperCase(Locale.ROOT), key);
    }

    public Map<String, String> settings(String code) {
        return repository.settings(require(code).code());
    }

    @Transactional
    public Map<String, String> putSettings(String code, Map<String, String> settings) {
        Market market = require(code);
        if (settings == null || settings.isEmpty()) {
            return repository.settings(market.code());
        }
        repository.putSettings(market.code(), settings);
        return repository.settings(market.code());
    }

    private void validate(Market market, String code) {
        if (!code.matches(CODE_PATTERN)) {
            throw ApiException.badRequest("INVALID_MARKET_CODE",
                    "市场码应为 2–8 位大写字母数字且以字母开头，实际=" + code);
        }
        if (market.name() == null || market.name().isBlank()) {
            throw ApiException.badRequest("MARKET_NAME_REQUIRED", "市场名称不能为空");
        }
        if (market.currency() == null || market.currency().isBlank()) {
            throw ApiException.badRequest("MARKET_CURRENCY_REQUIRED", "市场币种不能为空");
        }
        try {
            java.util.Currency.getInstance(market.currency().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("INVALID_MARKET_CURRENCY",
                    "币种必须是 ISO-4217 三字母代码，实际=" + market.currency());
        }
        if (market.countryCode() != null && !market.countryCode().isBlank()
                && !ISO_COUNTRIES.contains(market.countryCode().toUpperCase(Locale.ROOT))) {
            throw ApiException.badRequest("INVALID_MARKET_COUNTRY",
                    "国家必须是 ISO 3166-1 alpha-2，实际=" + market.countryCode());
        }
        if (market.timezone() == null || market.timezone().isBlank()) {
            throw ApiException.badRequest("MARKET_TIMEZONE_REQUIRED",
                    "市场必须有 IANA 时区（定时任务与报表按市场时区计算）");
        }
        try {
            ZoneId.of(market.timezone());
        } catch (RuntimeException exception) {
            throw ApiException.badRequest("INVALID_MARKET_TIMEZONE",
                    "时区必须是 IANA 名称（如 Asia/Hong_Kong），实际=" + market.timezone());
        }
        if (market.priceIncludesTax() == null) {
            throw ApiException.badRequest("MARKET_PRICE_TAX_FLAG_REQUIRED",
                    "必须声明标价是否含税（含税价与净价混用会让税额算错）");
        }
        if (market.taxJurisdiction() == null || market.taxJurisdiction().isBlank()) {
            throw ApiException.badRequest("MARKET_TAX_JURISDICTION_REQUIRED",
                    "市场必须声明税务辖区（可为国家码，也可以是 EU-OSS 这类跨国辖区）");
        }
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw ApiException.badRequest("MARKET_CODE_REQUIRED", "缺少市场码");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
