package com.fullmall.catalog.api;

import com.fullmall.catalog.domain.InventoryTrackingLevel;
import com.fullmall.catalog.domain.ProductStatus;
import com.fullmall.catalog.domain.ShelfLifePolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import com.fullmall.platform.validation.ValidationRules;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 新建商品请求体。约束值一律引用 {@code ValidationRules}（唯一来源 docs/validation.schema.json），
 * 不在注解里写裸数字；校验通过只代表格式合法，编码重复/类目存在等判断在 CatalogService。
 */
public record ProductUpsertRequest(
        @NotBlank String title,
        String description,
        UUID categoryId,
        ProductStatus status,
        @NotNull InventoryTrackingLevel inventoryTrackingLevel,
        boolean shelfLifeManaged,
        @Valid @NotEmpty List<SkuUpsert> skus
) {

    /**
     * 随商品一次性创建的 SKU。priceAmount/compareAtAmount 单位是分（整数，避免舍入进账），
     * compareAtAmount 只作划线价、不参与应付；options 会回填规格关联表（真相来源），imageUrl 原样入库。
     */
    public record SkuUpsert(
            @NotBlank @Pattern(regexp = ValidationRules.SKU_CODE_PATTERN,
                    message = "SKU 编码只能包含字母、数字、点、下划线、短横线，最长 64 位") String skuCode,
            String barcode,
            @Min(ValidationRules.PRICE_MIN) long priceAmount,
            @NotBlank @Size(min = 3, max = 3) String currency,
            Long compareAtAmount,
            Map<String, String> options,
            String imageUrl,
            ShelfLifePolicy shelfLifePolicy
    ) {
    }
}