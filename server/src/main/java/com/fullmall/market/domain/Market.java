package com.fullmall.market.domain;

/**
 * 市场（开通地区）。
 *
 * <p>ADR-0014：市场是独立实体，**不由国家推导**——大陆与香港同属"中国"，但关税区、币种、
 * 法律主体、支付体系、地址体系全都不同，按 country_code 一刀切会把两者等同。
 *
 * <p>语言不在这里：一个市场可以有多种语言（见 {@code market_locale}），
 * 界面语言还是用户的独立偏好。
 */
public record Market(
        String code,
        String name,
        String currency,
        String countryCode,
        String taxJurisdiction,
        /** NULL = 尚未配置（迁移只能给默认市场行补骨架，含税口径属运营决策）；新建/编辑时必须填写。 */
        Boolean priceIncludesTax,
        String timezone,
        String legalEntityName,
        boolean active,
        int sort) {
}
