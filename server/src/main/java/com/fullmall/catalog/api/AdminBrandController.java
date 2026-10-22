package com.fullmall.catalog.api;

import com.fullmall.catalog.app.BrandService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 品牌管理（后台）。 */
@RestController
@RequestMapping("/admin/v1/brands")
public class AdminBrandController {

    private final BrandService brands;

    public AdminBrandController(BrandService brands) {
        this.brands = brands;
    }

    public record BrandRequest(String code, String name, int sortOrder) {
    }

    @GetMapping
    public List<BrandService.Brand> list() {
        return brands.list();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Map<String, UUID> create(@RequestBody BrandRequest body) {
        return Map.of("brandId",
                brands.create(body.code(), body.name(), body.sortOrder()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public void update(@PathVariable UUID id, @RequestBody BrandRequest body) {
        brands.update(id, body.name(), body.sortOrder());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        brands.delete(id);
    }
}
