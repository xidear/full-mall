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

    public CatalogService(CatalogRepository repository) {
        this.repository = repository;
    }

    public ProductPage listProducts(String locale, String keyword, UUID categoryId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = safePage * safeSize;
        List<Product> items = repository.findActive(locale, keyword, categoryId, safeSize, offset);
        long total = repository.countActive(locale, keyword, categoryId);
        return new ProductPage(items, safePage, safeSize, total);
    }

    public ProductPage listAllProducts(String locale, String keyword, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = safePage * safeSize;
        List<Product> items = repository.findAll(locale, keyword, safeSize, offset);
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
}