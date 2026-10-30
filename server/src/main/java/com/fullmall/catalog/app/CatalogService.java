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
import com.fullmall.platform.media.MediaUrlResolver;
import com.fullmall.platform.money.Money;
import com.fullmall.platform.util.Ids;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 商品读写领域服务：后台维护、前台浏览、订单侧取 SKU 都走这里。
 * 金额一律是最小货币单位的 long，本类不换算不舍入；并发正确性靠数据库条件更新与唯一约束，不靠应用层锁。
 */
@Service
public class CatalogService {

    private final CatalogRepository repository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private final SkuOptionsMigrationService optionsMigration;

    /** 对外输出统一把媒体地址转成绝对 http(s) URL；域名/存储切换只改配置。 */
    private final MediaUrlResolver mediaUrls;

    /** 富文本入库前清洗：前端编辑器的限制可被绕过，服务端清洗才是权威。 */
    private final com.fullmall.platform.html.RichTextSanitizer richText;
    private final com.fullmall.asset.app.RichTextMediaGuard richTextMedia;
    private final com.fullmall.asset.app.AssetLocaleVariantService assetVariants;

    public CatalogService(CatalogRepository repository,
                          org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
                          SkuOptionsMigrationService optionsMigration,
                          MediaUrlResolver mediaUrls,
                          com.fullmall.platform.html.RichTextSanitizer richText,
                          com.fullmall.asset.app.RichTextMediaGuard richTextMedia,
                          com.fullmall.asset.app.AssetLocaleVariantService assetVariants) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.optionsMigration = optionsMigration;
        this.mediaUrls = mediaUrls;
        this.richText = richText;
        this.richTextMedia = richTextMedia;
        this.assetVariants = assetVariants;
    }

    // 只改“出参投影”，不动库里的值：库里存相对路径或既有绝对地址，图片域名/CDN 迁移时
    // 只需改 MediaUrlResolver 的配置，不必回灌全表；订单快照才在创建时固化绝对地址。
    private Product withAbsoluteMedia(Product product) {
        return new Product(
                product.id(),
                product.title(),
                product.description(),
                product.categoryId(),
                product.status(),
                product.inventoryTrackingLevel(),
                product.shelfLifeManaged(),
                product.skus().stream().map(this::withAbsoluteMedia).toList());
    }

    private Sku withAbsoluteMedia(Sku sku) {
        return new Sku(
                sku.id(),
                sku.productId(),
                sku.skuCode(),
                sku.barcode(),
                sku.price(),
                sku.compareAtPrice(),
                sku.options(),
                mediaUrls.absolute(sku.imageUrl()),
                sku.shelfLifePolicy());
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
                        sortKey, direction)
                .stream().map(this::withAbsoluteMedia).toList();
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
        List<Product> items = repository.findAll(locale, keyword, safeSize, offset, sortKey, direction)
                .stream().map(this::withAbsoluteMedia).toList();
        long total = repository.countAll(locale, keyword);
        return new ProductPage(items, safePage, safeSize, total);
    }

    /** 单商品上下架。先查再更新是为了把“不存在”暴露成 404：只靠 UPDATE 影响行数，0 行会被误当成改成功。 */
    @Transactional
    public void updateStatus(UUID productId, ProductStatus status) {
        getProduct(productId, "zh-CN");
        repository.updateStatus(productId, status);
    }

    /**
     * 按 id 取商品并叠加译文。译文只认 {@code status = 'PUBLISHED'}，其余情况回落源文——前台不会出现机翻草稿。
     * 商品不存在抛 404 {@code PRODUCT_NOT_FOUND}，永不返回 null。
     */
    public Product getProduct(UUID productId, String locale) {
        Product product = repository.findProduct(productId, locale);
        if (product == null) {
            throw ApiException.notFound("PRODUCT_NOT_FOUND", "Product not found: " + productId);
        }
        return withAbsoluteMedia(product);
    }

    /** 按 id 取 SKU；不存在直接抛 404 {@code SKU_NOT_FOUND}，不返回 null（下单/改价都依赖这个约定）。 */
    public Sku getSku(UUID skuId) {
        Sku sku = repository.findSku(skuId);
        if (sku == null) {
            throw ApiException.notFound("SKU_NOT_FOUND", "SKU not found: " + skuId);
        }
        return withAbsoluteMedia(sku);
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

    /**
     * 新建商品 + 首批 SKU + 回填规格关联表，三步在同一事务：失败全部回滚，不会留下没有 SKU 的孤儿商品。
     * 返回回读后的商品（带 id 与默认值），不用请求体——请求体里没有这些字段。
     */
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
                    // 商品图保持“相对路径或既有绝对地址”原样入库，输出时再按当前域名/CDN 投影；
                    // 订单快照才在创建时固化绝对地址。
                    skuRequest.imageUrl(),
                    skuRequest.shelfLifePolicy() == null ? ShelfLifePolicy.none() : skuRequest.shelfLifePolicy());
            repository.insertSku(sku);
        }

        // 规格写入关联表（真相来源），JSON 由投影重建
        optionsMigration.backfillProduct(productId);
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

    /**
     * 商品各 SKU 可用量（前台用于禁用缺货规格组合）。
     *
     * <p>只算批次现存数量，不减去已下单未出库——预占在拣货时以批次扣减体现，
     * 因此这里给的是"可售上限"，下单仍会二次校验（防超卖以扣减为准）。
     */
    public java.util.List<java.util.Map<String, Object>> availability(UUID productId) {
        return jdbcTemplate.query("""
                SELECT s.id AS sku_id, s.sku_code,
                       COALESCE(SUM(b.quantity), 0) AS available
                  FROM sku s
                  LEFT JOIN inventory_batch b ON b.sku_id = s.id
                 WHERE s.product_id = ?
                 GROUP BY s.id, s.sku_code
                 ORDER BY s.sku_code
                """, (rs, rowNum) -> {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("skuId", rs.getObject("sku_id", UUID.class));
            row.put("skuCode", rs.getString("sku_code"));
            row.put("available", rs.getInt("available"));
            return row;
        }, productId);
    }

    /** 富文本详情（原始内容），后台编辑页回显用。 */
    public String detailHtml(UUID productId) {
        var rows = jdbcTemplate.query("SELECT detail_html FROM product WHERE id = ?",
                (rs, rowNum) -> rs.getString("detail_html"), productId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 前台详情 HTML：优先该语言已发布的译文详情，否则回退默认语言（草稿/待审不能出现在商城）。 */
    public String detailHtmlFor(UUID productId, String locale) {
        String html = null;
        if (locale != null && !locale.isBlank()) {
            var translated = jdbcTemplate.query("""
                    SELECT detail_html FROM product_translation
                     WHERE product_id = ? AND locale = ? AND status = 'PUBLISHED'
                       AND detail_html IS NOT NULL AND detail_html <> ''
                    """, (rs, rowNum) -> rs.getString("detail_html"), productId, locale);
            if (!translated.isEmpty() && translated.get(0) != null) {
                html = translated.get(0);
            }
        }
        if (html == null) {
            html = detailHtml(productId);
        }
        // 图片按语言替换：详情里存的是默认语言图（带中文文案），英文页要换成英文图
        return localizeImages(html, locale);
    }

    /** 把详情里“有该语言变体”的图片换成本地化版本。放在读取侧而非保存侧：后补的变体也要生效。 */
    private String localizeImages(String html, String locale) {
        if (html == null || html.isBlank() || locale == null || locale.isBlank()) {
            return html;
        }
        return DetailImages.localize(html, assetVariants.urlMap(locale));
    }

    /**
     * 编辑商品基础信息（标题/描述/类目/富文本详情）。SKU 单独维护。
     *
     * @param detailHtml 富文本详情；保存前用 {@link com.fullmall.platform.html.RichTextSanitizer}
     *                   白名单清洗（null 表示"不修改"，空串表示"清空详情"）
     */
    @Transactional
    public void updateBasics(UUID productId, String title, String description, UUID categoryId,
                            String detailHtml) {
        if (title == null || title.isBlank()) {
            throw ApiException.badRequest("PRODUCT_TITLE_REQUIRED", "商品标题不能为空");
        }
        String cleaned = detailHtml == null ? null : richText.sanitize(detailHtml);
        if (cleaned != null) {
            // 清洗只管"没有 XSS"，这里再管"素材必须在库"：外站图/base64 一律拒绝保存（见 RichTextMediaGuard）
            richTextMedia.requireLibraryMedia(cleaned);
        }
        int updated = jdbcTemplate.update("""
                UPDATE product SET title = ?, description = ?, category_id = ?,
                                   detail_html = COALESCE(?, detail_html)
                 WHERE id = ?
                """, title.trim(), description, categoryId, cleaned, productId);
        if (updated != 1) {
            throw ApiException.notFound("PRODUCT_NOT_FOUND", "商品不存在：" + productId);
        }
    }
}
