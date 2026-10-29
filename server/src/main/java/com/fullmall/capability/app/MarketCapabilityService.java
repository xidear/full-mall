package com.fullmall.capability.app;

import com.fullmall.market.app.MarketService;
import com.fullmall.market.domain.Market;
import com.fullmall.platform.error.ApiException;
import com.fullmall.region.app.RegionService;
import com.fullmall.shipping.app.ShippingRateService;
import com.fullmall.tax.app.TaxService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 市场能力门禁（"这个地区到底能不能做生意"）。
 *
 * <p>为什么要一处统一判断：能力分散在各模块（地址格式在 region、运费在 shipping、税在 tax、
 * 支付/物流开关在 market_integration），每个模块只知道自己那一半。结果就是
 * "市场配了、运费配了、但目的地根本没开通"这类组合错误，要在下单那一刻才炸，甚至不炸。
 *
 * <p>门禁按 **市场 × 目的地** 求值，四类阻断原因分别有稳定错误码：
 * {@code MARKET_INACTIVE} / {@code DESTINATION_NOT_DELIVERABLE} /
 * {@code ADDRESS_FORMAT_MISSING} / {@code SHIPPING_RULE_MISSING}。
 *
 * <p>税率**不阻断**：不少辖区确实没有可计税种（或属低值免税额内），把它做成硬门禁会让正常订单下不了。
 * 缺税率由 {@link TaxService} 打 WARN、报关单导出列出，属"可见的缺口"而不是"静默免掉"。
 */
@Service
public class MarketCapabilityService {

    private final MarketService markets;
    private final RegionService regions;
    private final ShippingRateService shipping;
    private final TaxService taxes;
    /** 通道启用关系统一走这个服务，别在这里再写一份 market_integration 的 SQL。 */
    private final com.fullmall.market.app.MarketIntegrationService integrations;
    private final JdbcTemplate jdbcTemplate;

    public MarketCapabilityService(MarketService markets, RegionService regions,
                                  ShippingRateService shipping, TaxService taxes,
                                  com.fullmall.market.app.MarketIntegrationService integrations,
                                  JdbcTemplate jdbcTemplate) {
        this.markets = markets;
        this.regions = regions;
        this.shipping = shipping;
        this.taxes = taxes;
        this.integrations = integrations;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 一个 市场 × 目的地 的能力快照，供门禁与后台/前台展示共用。 */
    public record Capabilities(String marketCode, String destinationCountry,
                              boolean marketActive, boolean deliverable, boolean addressFormat,
                              boolean shippingRule, boolean taxRule, boolean paymentEnabled,
                              boolean logisticsEnabled, List<String> blockers) {

        public boolean canCheckout() {
            return blockers.isEmpty();
        }
    }

    public Capabilities evaluate(String marketCode, String destinationCountry) {
        Market market = markets.require(marketCode);
        String destination = destinationCountry == null || destinationCountry.isBlank()
                ? null : destinationCountry.trim().toUpperCase(Locale.ROOT);

        boolean addressFormat = destination != null && regions.hasFormat(destination);
        boolean shippingRule = destination != null && shipping.hasRule(market.code(), destination);
        boolean taxRule = taxes.hasRule(market.code(), destination);
        boolean paymentEnabled = integrations.enabledProvider(market.code(), "payment").isPresent();
        boolean logisticsEnabled = integrations.enabledProvider(market.code(), "logistics").isPresent();
        boolean deliverable = deliverable(market, destination);

        List<String> blockers = new ArrayList<>();
        if (!market.active()) {
            blockers.add("MARKET_INACTIVE");
        }
        if (destination == null) {
            // 没有目的地就谈不上"能不能送达"：这是地址缺字段，不是国家没开通
            blockers.add("DESTINATION_REQUIRED");
        } else {
            if (!deliverable) {
                blockers.add("DESTINATION_NOT_DELIVERABLE");
            }
            if (!addressFormat) {
                blockers.add("ADDRESS_FORMAT_MISSING");
            }
            if (!shippingRule) {
                blockers.add("SHIPPING_RULE_MISSING");
            }
        }
        return new Capabilities(market.code(), destination, market.active(), deliverable,
                addressFormat, shippingRule, taxRule, paymentEnabled, logisticsEnabled, blockers);
    }

    /** 下单前置门禁：任一项不满足就带着稳定错误码失败，不进入定价/扣库存。 */
    public Capabilities requireCheckoutCapable(String marketCode, String destinationCountry) {
        Capabilities capabilities = evaluate(marketCode, destinationCountry);
        if (capabilities.canCheckout()) {
            return capabilities;
        }
        String first = capabilities.blockers().get(0);
        String message = switch (first) {
            case "MARKET_INACTIVE" -> "该市场已停用，暂不接单";
            case "DESTINATION_REQUIRED" -> "缺少收货国家，无法判断能否配送";
            case "DESTINATION_NOT_DELIVERABLE" -> "暂不支持配送到该国家/地区";
            case "ADDRESS_FORMAT_MISSING" -> "该国地址格式未配置，暂不能下单";
            case "SHIPPING_RULE_MISSING" -> "该市场到该目的地没有配置运费规则";
            default -> "当前市场/" + marketCode + " 目的地 " + destinationCountry + " 暂不可下单";
        };
        throw ApiException.conflict(first, message + "（" + String.join(",", capabilities.blockers()) + "）");
    }

    /**
     * 目的地可达性。
     *
     * <p>默认口径：**只有市场所属国家可达**。跨境要显式在 market_destination 里加一行
     * （deliverable=TRUE）——"没配"绝不等于"全球都能送"，那等于白送运费。
     */
    private boolean deliverable(Market market, String destination) {
        if (destination == null) {
            return false;
        }
        List<Boolean> rows = jdbcTemplate.query("""
                SELECT deliverable FROM market_destination
                 WHERE market_code = ? AND destination_country = ?
                """, (rs, rowNum) -> rs.getBoolean("deliverable"), market.code(), destination);
        if (!rows.isEmpty()) {
            return rows.get(0);
        }
        return destination.equals(market.countryCode());
    }


}
