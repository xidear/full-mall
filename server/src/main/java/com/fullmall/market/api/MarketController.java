package com.fullmall.market.api;

import com.fullmall.market.app.MarketService;
import com.fullmall.market.domain.Market;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 市场配置接口（ADR-0014）。
 *
 * <p>管理员看到设置（ICP/客服电话等）与语言候选；公开接口只暴露下单所需的三轴信息，
 * 不返回市场级设置——那些是运营配置，不是顾客需要的内容。
 */
@RestController
public class MarketController {

    private final MarketService markets;

    public MarketController(MarketService markets) {
        this.markets = markets;
    }

    /** 管理端视图：含语言候选与市场级设置。 */
    public record MarketView(String code, String name, String currency, String countryCode,
                             String taxJurisdiction, Boolean priceIncludesTax, String timezone,
                             String legalEntityName, boolean active, int sort,
                             List<String> locales, Map<String, String> settings) {
    }

    /** 公开视图：只给前端渲染所需，不含运营配置。 */
    public record PublicMarketView(String code, String name, String currency, String countryCode,
                                  Boolean priceIncludesTax, String timezone, List<String> locales,
                                  String primaryLocale) {
    }

    public record MarketBody(@NotBlank String name, @NotBlank String currency, String countryCode,
                             @NotBlank String taxJurisdiction, Boolean priceIncludesTax,
                             @NotBlank String timezone, String legalEntityName,
                             boolean active, int sort) {
    }

    public record LocalesBody(List<String> locales, String primaryLocale) {
    }

    @GetMapping("/admin/v1/markets")
    public List<MarketView> list(@RequestParam(defaultValue = "false") boolean onlyActive) {
        return markets.list(onlyActive).stream().map(this::toView).toList();
    }

    @PutMapping("/admin/v1/markets/{code}")
    public MarketView save(@PathVariable String code, @Valid @RequestBody MarketBody body) {
        Market saved = markets.save(new Market(code, body.name(), body.currency(), body.countryCode(),
                body.taxJurisdiction(), body.priceIncludesTax(), body.timezone(),
                body.legalEntityName(), body.active(), body.sort()));
        return toView(saved);
    }

    @PutMapping("/admin/v1/markets/{code}/locales")
    public MarketView setLocales(@PathVariable String code, @RequestBody LocalesBody body) {
        markets.setLocales(code, body.locales(), body.primaryLocale());
        return toView(markets.require(code));
    }

    @PutMapping("/admin/v1/markets/{code}/settings")
    public MarketView putSettings(@PathVariable String code, @RequestBody Map<String, String> settings) {
        markets.putSettings(code, settings);
        return toView(markets.require(code));
    }

    /** 公开：前台按此渲染"可切换的市场/币种"，不返回设置项。 */
    @GetMapping("/api/v1/markets")
    public List<PublicMarketView> publicList() {
        return markets.list(true).stream()
                .map(market -> {
                    List<String> locales = markets.locales(market.code());
                    return new PublicMarketView(market.code(), market.name(), market.currency(),
                            market.countryCode(), market.priceIncludesTax(), market.timezone(),
                            locales, locales.isEmpty() ? null : locales.get(0));
                })
                .toList();
    }

    private MarketView toView(Market market) {
        return new MarketView(market.code(), market.name(), market.currency(), market.countryCode(),
                market.taxJurisdiction(), market.priceIncludesTax(), market.timezone(),
                market.legalEntityName(), market.active(), market.sort(),
                markets.locales(market.code()), markets.settings(market.code()));
    }
}
