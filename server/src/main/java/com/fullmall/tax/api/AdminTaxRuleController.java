package com.fullmall.tax.api;

import com.fullmall.tax.app.TaxService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** 税率规则管理（批次 E）。 */
@RestController
public class AdminTaxRuleController {

    private final TaxService taxes;

    public AdminTaxRuleController(TaxService taxes) {
        this.taxes = taxes;
    }

    public record RuleBody(@NotBlank String jurisdiction, @NotBlank String name,
                           @Min(0) int rateBps, boolean priceIncludesTax, boolean active) {
    }

    @GetMapping("/admin/v1/tax-rules")
    public List<TaxService.RuleRow> list() {
        return taxes.list();
    }

    /** 税务辖区候选：来自已建的税率规则（没有固定枚举，避免页面瞎填）。 */
    @GetMapping("/admin/v1/tax-rules/jurisdictions")
    public List<String> jurisdictions() {
        return taxes.jurisdictions();
    }

    // 两个路径分开声明：契约检查脚本按注解逐个解析，数组形式会漏掉 /{id}（会被当成前端多调）
    @PutMapping("/admin/v1/tax-rules")
    public List<TaxService.RuleRow> create(@RequestBody RuleBody body) {
        return upsert(UUID.randomUUID().toString(), body);
    }

    @PutMapping("/admin/v1/tax-rules/{id}")
    public List<TaxService.RuleRow> update(@PathVariable String id, @RequestBody RuleBody body) {
        return upsert(id, body);
    }

    private List<TaxService.RuleRow> upsert(String target, RuleBody body) {
        taxes.upsert(target, body.jurisdiction(), body.name(), body.rateBps(),
                body.priceIncludesTax(), body.active());
        return taxes.list();
    }

    @DeleteMapping("/admin/v1/tax-rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        taxes.delete(id);
    }
}
