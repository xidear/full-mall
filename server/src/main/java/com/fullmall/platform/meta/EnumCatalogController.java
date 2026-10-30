package com.fullmall.platform.meta;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 枚举字典下发：前端不再各页面写死中文映射（AGENTS §13）。 */
@RestController
@RequestMapping("/admin/v1/meta")
public class EnumCatalogController {

    @GetMapping("/enums")
    public Map<String, List<EnumCatalog.Option>> enums() {
        return EnumCatalog.all();
    }
}
