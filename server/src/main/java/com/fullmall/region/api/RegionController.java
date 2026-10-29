package com.fullmall.region.api;

import com.fullmall.region.app.RegionService;
import com.fullmall.region.domain.AddressFormat;
import com.fullmall.region.domain.RegionNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 区划与地址格式接口（ADR-0013 §4）。前端据此**动态渲染**地址表单，不写死"省市区"三个框。
 */
@RestController
public class RegionController {

    private final RegionService regions;

    public RegionController(RegionService regions) {
        this.regions = regions;
    }

    /** 展示用视图：名称已按 locale 解析，同时保留 code 供提交时回传。 */
    public record RegionView(String code, String parentCode, String level, int depth, String name) {
    }

    /** 表单视图：字段集/顺序/邮编正则/是否下拉，全部来自该国 address_format。 */
    public record FormatView(String countryCode, String required, String fmt, String zip, String zipex,
                            String stateNameType, boolean hasAdminList, boolean adminOptional,
                            boolean hasLocality, boolean hasDependentLocality) {
    }

    @GetMapping("/api/v1/regions/countries")
    public List<RegionView> countries(@RequestParam(defaultValue = "zh-CN") String locale) {
        return regions.countries().stream().map(node -> toView(node, locale)).toList();
    }

    @GetMapping("/api/v1/regions/formats")
    public FormatView format(@RequestParam String country) {
        AddressFormat format = regions.requireFormat(country);
        return new FormatView(format.countryCode(), format.required(), format.fmt(), format.zip(),
                format.zipex(), format.stateNameType(), format.hasAdminList(), format.adminOptional(),
                format.hasLocality(), format.hasDependentLocality());
    }

    /**
     * 下级区划。不给 parent 时返回该国一级行政区；空数组表示该国没有深层区划数据（前端退化为手填）。
     */
    @GetMapping("/api/v1/regions")
    public List<RegionView> children(@RequestParam String country,
                                    @RequestParam(required = false) String parent,
                                    @RequestParam(required = false) String level,
                                    @RequestParam(defaultValue = "zh-CN") String locale) {
        return regions.children(country, parent, level).stream()
                .map(node -> toView(node, locale)).toList();
    }

    private RegionView toView(RegionNode node, String locale) {
        return new RegionView(node.code(), node.parentCode(), node.level(), node.depth(),
                regions.label(node, locale));
    }
}
