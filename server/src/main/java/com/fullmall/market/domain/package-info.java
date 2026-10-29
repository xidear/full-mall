/**
 * 市场的**值类型**（{@code Market}）。
 *
 * <p>显式暴露的理由同 region.domain：它是纯数据载体，setting 模块要读市场属性（是否启用、
 * 币种、税务辖区）来校验 default_market 与站点设置，暴露它不会泄露 market 的内部实现。
 */
@org.springframework.modulith.NamedInterface("market-model")
package com.fullmall.market.domain;
