package com.fullmall.catalog.app;

import com.fullmall.catalog.api.ProductDetailView;
import com.fullmall.catalog.domain.Product;
import com.fullmall.catalog.infra.ProductTranslationRepository;
import com.fullmall.catalog.infra.SkuMatrixTemplateRepository;
import com.fullmall.setting.app.SystemSettingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 后台商品详情聚合：一次查询返回编辑页需要的全部事实。 */
@Service
public class AdminProductDetailService {

    private final CatalogService catalog;
    private final ProductMediaService media;
    private final ProductTranslationRepository translations;
    private final SkuMatrixTemplateRepository matrixTemplates;
    private final SystemSettingService settings;
    private final com.fullmall.platform.media.MediaUrlResolver mediaUrls;

    public AdminProductDetailService(CatalogService catalog, ProductMediaService media,
                                    ProductTranslationRepository translations,
                                    SkuMatrixTemplateRepository matrixTemplates,
                                    SystemSettingService settings,
                                    com.fullmall.platform.media.MediaUrlResolver mediaUrls) {
        this.catalog = catalog;
        this.media = media;
        this.translations = translations;
        this.matrixTemplates = matrixTemplates;
        this.settings = settings;
        this.mediaUrls = mediaUrls;
    }

    public ProductDetailView detail(UUID productId) {
        String defaultLocale = settings.defaultLocale();
        Product product = catalog.getProduct(productId, defaultLocale);
        List<String> enabled = settings.enabledLocales();

        List<ProductTranslationRepository.Row> rows = translations.listByProduct(productId);
        List<ProductDetailView.TranslationEntry> entries = new ArrayList<>();
        for (String locale : enabled) {
            if (locale.equals(defaultLocale)) {
                continue;
            }
            ProductDetailView.TranslationEntry entry = rows.stream()
                    .filter(row -> locale.equals(row.locale()))
                    .findFirst()
                    .map(row -> new ProductDetailView.TranslationEntry(row.locale(), true, row.title(),
                            row.description(), mediaUrls.absolute(row.imageUrl()),
                            row.status() == null ? null : row.status().name()))
                    .orElse(new ProductDetailView.TranslationEntry(locale, false, null, null, null, null));
            entries.add(entry);
        }

        return new ProductDetailView(product.id(), product.title(), product.description(),
                product.categoryId(), product.status().name(),
                product.inventoryTrackingLevel().name(), product.shelfLifeManaged(),
                product.skus(), media.list(productId), entries,
                matrixTemplates.productTemplate(productId).orElse(null),
                defaultLocale, enabled, catalog.detailHtml(productId));
    }
}
