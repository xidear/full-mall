package com.fullmall.region.app;

import com.fullmall.platform.error.ApiException;
import com.fullmall.region.domain.AddressFormat;
import com.fullmall.region.domain.RegionNode;
import com.fullmall.region.infra.RegionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * 区划字典与地址格式查询（ADR-0013）。
 *
 * <p>规则：
 * <ul>
 *   <li>没有该国的 {@code address_format} 就**明确报错**，不回退中国模板——
 *       静默套模板会让"日本用户的地址表单"长得像中国表单，错得很隐蔽；</li>
 *   <li>区划节点查不到是**正常情况**（未抓该国的深层区划），调用方据此退化为手填；</li>
 *   <li>名称按 locale 取，取不到回退用户请求的 locale → 数据的默认 locale → code。</li>
 * </ul>
 */
@Service
public class RegionService {

    private final RegionRepository repository;

    public RegionService(RegionRepository repository) {
        this.repository = repository;
    }

    /** 可选国家列表（一级节点）。 */
    public List<RegionNode> countries() {
        return repository.findChildren(null, null, "COUNTRY");
    }

    /** 该国是否配了地址格式（不抛异常）：门禁判断用，异常留给真正的"下单时缺格式"场景。 */
    public boolean hasFormat(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return false;
        }
        return repository.findFormat(countryCode.trim().toUpperCase(Locale.ROOT)).isPresent();
    }

    public AddressFormat requireFormat(String countryCode) {
        String code = normalize(countryCode);
        return repository.findFormat(code).orElseThrow(() -> ApiException.notFound(
                "ADDRESS_FORMAT_MISSING",
                "该国地址格式未配置（未抓取快照），不允许开通该市场: " + code));
    }

    /**
     * 查下级区划。{@code parentCode} 为空表示取该国一级行政区。
     *
     * <p>返回值可能为空列表：这表示"该国没有抓深层区划"，不是错误——
     * 前端据此退化为文本输入（手填兜底是功能，不是失败）。
     */
    public List<RegionNode> children(String countryCode, String parentCode, String level) {
        String country = normalize(countryCode);
        if (parentCode != null && !parentCode.isBlank()) {
            RegionNode parent = repository.findNode(parentCode).orElse(null);
            if (parent == null) {
                throw ApiException.notFound("REGION_NOT_FOUND", "区划不存在: " + parentCode);
            }
            if (!parent.countryCode().equals(country)) {
                throw ApiException.badRequest("REGION_COUNTRY_MISMATCH",
                        "区划 " + parentCode + " 不属于国家 " + country);
            }
        }
        return repository.findChildren(country, parentCode == null || parentCode.isBlank() ? null : parentCode, level);
    }

    /** 按 locale 取展示名；缺该 locale 时回退到请求语言的主语言，再回退 code。 */
    public String label(RegionNode node, String locale) {
        if (node.names() == null || node.names().isEmpty()) {
            return node.code();
        }
        String exact = node.names().get(locale);
        if (exact != null && !exact.isBlank()) {
            return exact;
        }
        // 快照里的 key 可能是 "zh" 也可能 "zh-Hans"/"zh-CN"（不同国家数据不一样），
        // 所以先按基础语言精确匹配，再按前缀匹配，最后才用任一可用名称。
        String primary = locale != null && locale.contains("-")
                ? locale.substring(0, locale.indexOf('-')) : locale;
        if (primary != null) {
            String base = node.names().get(primary);
            if (base != null && !base.isBlank()) {
                return base;
            }
            for (var entry : node.names().entrySet()) {
                if (entry.getKey().startsWith(primary + "-")) {
                    return entry.getValue();
                }
            }
        }
        return node.names().values().iterator().next();
    }

    private String normalize(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            throw ApiException.badRequest("REGION_COUNTRY_REQUIRED", "缺少国家代码");
        }
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }
}
