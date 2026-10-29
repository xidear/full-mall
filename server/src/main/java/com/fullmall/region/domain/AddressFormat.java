package com.fullmall.region.domain;

/**
 * 国别地址格式（ADR-0013 §8.1）。这是"该国地址长什么样"的唯一来源：
 * 必填哪些字段、什么顺序、邮编正则、一级行政区叫什么、是否下拉。
 */
public record AddressFormat(String countryCode, String required, String fmt, String zip, String zipex,
                           String stateNameType, boolean hasAdminList, boolean adminOptional,
                           String upper) {

    /** 该字段是否必填（国家字段永远必填，故不在 require 串里）。 */
    public boolean requires(String field) {
        return required != null && required.contains(field);
    }

    /**
     * 是否有"市"这一级。libaddressinput 的 fmt 里出现 %C 才表示该国地址包含 locality；
     * 没有 %C 的国家（如新加坡）渲染城市输入框就是在问一个不存在的问题。
     */
    public boolean hasLocality() {
        return fmt != null && fmt.contains("%C");
    }

    /** 是否有"区/依赖 locality"这一级。 */
    public boolean hasDependentLocality() {
        return fmt != null && fmt.contains("%D");
    }
}
