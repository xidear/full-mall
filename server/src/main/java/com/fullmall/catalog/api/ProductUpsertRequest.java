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
 * 新建商品的请求体（api 层 DTO，仅 {@code AdminCatalogController#create} 使用）。
 *
 * <p>约束值一律引用 {@code ValidationRules}，唯一来源是 `docs/validation.schema.json`
 * （AGENTS.md §9）：改规则要先改 schema，再同步常量，不要在注解里写裸数字或正则。
 *
 * <p>Bean Validation 只保证格式与必填；编码重复、币种一致性、类目是否存在等业务判断在
 * {@code CatalogService#createProduct} 中完成，所以“校验通过”不等于“一定建得成功”。
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
     * 随商品一次性创建的 SKU。
     *
     * <p>{@code priceAmount}/{@code compareAtAmount} 是最小货币单位（CNY=分）的整数，
     * 不传小数以免舍入进账；{@code compareAtAmount} 只用于展示划线价，不参与应付金额。
     * {@code currency} 是 ISO-4217 三位码，先由 {@code @Size} 卡长度、再由 {@code Money} 校验合法性。
     *
     * <p>{@code options} 会被回填进规格关联表（真相来源），{@code imageUrl} 按原样入库、
     * 输出时才转绝对地址；{@code shelfLifePolicy} 为 null 时落 {@code ShelfLifePolicy.none()}。
     * {@code skuCode} 格式见 {@code ValidationRules.SKU_CODE_PATTERN}，重复由数据库唯一约束兜底。
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