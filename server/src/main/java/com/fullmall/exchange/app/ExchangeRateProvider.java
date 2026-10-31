package com.fullmall.exchange.app;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 自动汇率源：一次返回「1 单位 base = x 单位 quote」的汇率表。
 *
 * <p>只做直接汇率（provider 返回什么就用什么），不在此处做三角换算。
 */
public interface ExchangeRateProvider {

    String code();

    Map<String, BigDecimal> rates(String baseCurrency);
}