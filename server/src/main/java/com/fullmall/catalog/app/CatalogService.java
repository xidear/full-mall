package com.fullmall.catalog.app;

import com.fullmall.catalog.api.ProductPage;
import com.fullmall.catalog.api.ProductUpsertRequest;
import com.fullmall.catalog.domain.InventoryTrackingLevel;
import com.fullmall.catalog.domain.Product;
import com.fullmall.catalog.domain.ProductStatus;
import com.fullmall.catalog.domain.ShelfLifePolicy;
import com.fullmall.catalog.domain.Sku;
import com.fullmall.catalog.infra.CatalogRepository;
import com.fullmall.platform.error.ApiException;
import com.fullmall.platform.money.Money;
import com.fullmall.platform.util.Ids;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CatalogService {

    private final CatalogRepository repository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public CatalogService(CatalogRepository repository,
                         org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public ProductPage listProducts(String locale, String keyword, UUID categoryId, int page, int size) {
        return listProducts(locale, keyword, categoryId, page, size, "createdAt", "desc");
    }

    /** 顾客端列表：支持服务端排序（白名单在仓储层），并限制单页 1..50 条。 */
    public ProductPage listProducts(String locale, String keyword, UUID categoryId, int page, int size,
                                    String sortKey, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = safePage * safeSize;
        List<Product> items = repository.findActive(locale, keyword, categoryId, safeSize, offset,
                sortKey, direction);
        long total = repository.countActive(locale, keyword, categoryId);
        return new ProductPage(items, safePage, safeSize, total);
    }

    public ProductPage listAllProducts(String locale, String keyword, int page, int size) {
        return listAllProducts(locale, keyword, page, size, "createdAt", "desc");
    }

    public ProductPage listAllProducts(String locale, String keyword, int page, int size,
                                       String sortKey, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = safePage * safeSize;
        List<Product> items = repository.findAll(locale, keyword, safeSize, offset, sortKey, direction);
        long total = repository.countAll(locale, keyword);
        return new ProductPage(items, safePage, safeSize, total);
    }

    @Transactional
    public void updateStatus(UUID productId, ProductStatus status) {
        getProduct(productId, "zh-CN");
        repository.updateStatus(productId, status);
    }

    public Product getProduct(UUID productId, String locale) {
        Product product = repository.findProduct(productId, locale);
        if (product == null) {
            throw ApiException.notFound("PRODUCT_NOT_FOUND", "Product not found: " + productId);
        }
        return product;
    }

    public Sku getSku(UUID skuId) {
        Sku sku = repository.findSku(skuId);
        if (sku == null) {
            throw ApiException.notFound("SKU_NOT_FOUND", "SKU not found: " + skuId);
        }
        return sku;
    }

    /**
     * 维护 SKU 成本。
     *
     * <p>成本是敏感数据（毛利、佣金、砍价底价都依赖它），只允许 ADMIN 调用；
     * 传 null 表示清除（回到"未维护"），此时依赖成本的功能必须明确拒绝。
     */
    @Transactional
    public void updateCost(UUID skuId, Long costAmount) {
        if (costAmount != null && costAmount < 0) {
            throw ApiException.badRequest("SKU_COST_INVALID", "成本不能为负数：" + costAmount);
        }
        int updated = repository.updateCost(skuId, costAmount);
        if (updated != 1) {
            throw ApiException.notFound("SKU_NOT_FOUND", "SKU 不存在：" + skuId);
        }
    }

    /** 读取成本（未维护返回 null，**不返回 0**）。 */
    public Long cost(UUID skuId) {
        Long cost = repository.findCost(skuId);
        if (cost == null && repository.findSku(skuId) == null) {
            throw ApiException.notFound("SKU_NOT_FOUND", "SKU 不存在：" + skuId);
        }
        return cost;
    }

    @Transactional
    public Product createProduct(ProductUpsertRequest request) {
        UUID productId = Ids.newId();
        ProductStatus status = request.status() == null ? ProductStatus.DRAFT : request.status();
        InventoryTrackingLevel trackingLevel = request.inventoryTrackingLevel() == null
                ? InventoryTrackingLevel.VARIANT
                : request.inventoryTrackingLevel();

        Product product = new Product(
                productId,
                request.title(),
                request.description(),
                request.categoryId(),
                status,
                trackingLevel,
                request.shelfLifeManaged(),
                List.of());
        repository.insertProduct(product);

        for (ProductUpsertRequest.SkuUpsert skuRequest : request.skus()) {
            Money compareAt = skuRequest.compareAtAmount() == null
                    ? null
                    : new Money(skuRequest.compareAtAmount(), skuRequest.currency());
            Sku sku = new Sku(
                    Ids.newId(),
                    productId,
                    skuRequest.skuCode(),
                    skuRequest.barcode(),
                    new Money(skuRequest.priceAmount(), skuRequest.currency()),
                    compareAt,
                    skuRequest.options() == null ? Map.of() : skuRequest.options(),
                    skuRequest.imageUrl(),
                    skuRequest.shelfLifePolicy() == null ? ShelfLifePolicy.none() : skuRequest.shelfLifePolicy());
            repository.insertSku(sku);
        }

        return getProduct(productId, "zh-CN");
    }

    /**
     * SKU 批量改价。
     *
     * <p>规则：价格必须 &gt; 0；不存在的 SKU 整批失败（不做部分成功，避免运营以为全改了）；
     * 返回实际更新条数。
     */
    @Transactional
    public int batchUpdatePrices(java.util.Map<UUID, Long> prices) {
        if (prices == null || prices.isEmpty()) {
            throw ApiException.badRequest("BATCH_PRICE_EMPTY", "没有需要更新的 SKU");
        }
        for (var entry : prices.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                throw ApiException.badRequest("BATCH_PRICE_INVALID",
                        "价格必须大于 0：" + entry.getKey());
            }
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sku WHERE id = ?", Integer.class, entry.getKey());
            if (exists == null || exists == 0) {
                throw ApiException.notFound("SKU_NOT_FOUND", "SKU 不存在：" + entry.getKey());
            }
        }
        int updated = 0;
        for (var entry : prices.entrySet()) {
            updated += jdbcTemplate.update("UPDATE sku SET price_amount = ? WHERE id = ?",
                    entry.getValue(), entry.getKey());
        }
        return updated;
    }

    /**
     * 商品批量上下架。
     *
     * <p>与批量改价同样的整批语义：任一商品不存在则整批失败，避免"以为都下架了，其实漏了"。
     */
    @Transactional
    public int batchUpdateStatus(java.util.List<UUID> productIds, ProductStatus status) {
        if (productIds == null || productIds.isEmpty()) {
            throw ApiException.badRequest("BATCH_STATUS_EMPTY", "没有需要更新的商品");
        }
        if (status == null) {
            throw ApiException.badRequest("BATCH_STATUS_REQUIRED", "必须指定目标状态");
        }
        for (UUID productId : productIds) {
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM product WHERE id = ?", Integer.class, productId);
            if (exists == null || exists == 0) {
                throw ApiException.notFound("PRODUCT_NOT_FOUND", "商品不存在：" + productId);
            }
        }
        int updated = 0;
        for (UUID productId : productIds) {
            repository.updateStatus(productId, status);
            updated++;
        }
        return updated;
    }
}