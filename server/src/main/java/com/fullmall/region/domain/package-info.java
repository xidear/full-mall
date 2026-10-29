/**
 * 区划与地址格式的**值类型**（RegionNode / AddressFormat）。
 *
 * <p>为什么显式暴露：address 模块要按国别判定必填字段，需要读 {@code AddressFormat}；
 * 这是纯数据载体，不含行为与仓储，暴露它不会把 region 的内部实现泄露出去。
 */
@org.springframework.modulith.NamedInterface("region-model")
package com.fullmall.region.domain;
