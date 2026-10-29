package com.fullmall.catalog.api;

import com.fullmall.catalog.app.SkuMatrixTemplateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 规格矩阵模板（后台）。
 *
 * <p>模板是"可复用的规格定义"，不含价格；商品侧通过 apply 用模板生成 SKU 并绑定模板 id，
 * 便于之后核对"这批 SKU 是哪个模板生成的"。
 */
@RestController
@RequestMapping("/admin/v1/sku-matrix-templates")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class SkuMatrixTemplateController {

    private final SkuMatrixTemplateService service;

    public SkuMatrixTemplateController(SkuMatrixTemplateService service) {
        this.service = service;
    }

    public record TemplateRequest(String code, String name, String note,
                                 java.util.LinkedHashMap<String, List<String>> dimensions,
                                 List<java.util.Map<String, String>> disabled) {
    }

    public record ApplyRequest(UUID productId, String prefix, long priceAmount, String currency) {
    }

    @GetMapping
    public List<SkuMatrixTemplateService.TemplateView> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public SkuMatrixTemplateService.TemplateView get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SkuMatrixTemplateService.TemplateView create(@RequestBody TemplateRequest body) {
        return service.create(input(body));
    }

    @PutMapping("/{id}")
    public SkuMatrixTemplateService.TemplateView update(@PathVariable UUID id,
                                                       @RequestBody TemplateRequest body) {
        return service.update(id, input(body));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    /** 用模板给商品生成 SKU（价格在这里输入，模板不存价格）。 */
    @PostMapping("/{id}/apply")
    public SkuMatrixTemplateService.ApplyResult apply(@PathVariable UUID id,
                                                      @RequestBody ApplyRequest body) {
        if (body == null || body.productId() == null) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "SKU_MATRIX_PRODUCT_REQUIRED", "必须指定要应用模板的商品");
        }
        return service.applyToProduct(body.productId(), id, body.prefix(), body.priceAmount(),
                body.currency());
    }

    private SkuMatrixTemplateService.TemplateInput input(TemplateRequest body) {
        if (body == null) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "SKU_MATRIX_TEMPLATE_REQUIRED", "模板内容不能为空");
        }
        return new SkuMatrixTemplateService.TemplateInput(body.code(), body.name(), body.note(),
                body.dimensions(), body.disabled());
    }
}
