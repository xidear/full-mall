package com.fullmall.region.domain;

import java.util.Map;

/**
 * 区划节点（ADR-0013 §2）。
 *
 * <p>{@code level} 是**语义位**而不是"省/市/区"：全球层级深度 1–4 且叫法不同
 * （state / prefecture / emirate / county），固定三级已被实测证伪。
 *
 * <p>{@code names} 是 locale → 名称；**匹配与存储只用 code**，名称仅展示
 * （同一地区在不同语言下 sub_keys 连顺序都不同）。
 */
public record RegionNode(String code, String parentCode, String level, int depth,
                        String countryCode, Map<String, String> names, boolean active) {
}
