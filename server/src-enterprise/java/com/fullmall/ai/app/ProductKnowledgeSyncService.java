package com.fullmall.ai.app;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 商品知识同步：把 SKU 的最新参数/价格/可售状态重建为知识块。
 *
 * <p>为什么必须"重建"而不是"追加"：价格与库存天天变，追加会让旧价格与新价格同时命中，
 * 模型可能答出过期价格。同源替换是这里唯一安全的做法。
 */
@Service
public class ProductKnowledgeSyncService {

    private final JdbcTemplate jdbc;
    private final KnowledgeStore store;

    public ProductKnowledgeSyncService(JdbcTemplate jdbc, KnowledgeStore store) {
        this.jdbc = jdbc;
        this.store = store;
    }

    public record SyncResult(String skuId, String title, String content, int chunks) {
    }

    @Transactional
    public SyncResult sync(UUID skuId) {
        List<SyncResult> rows = jdbc.query("""
                SELECT s.id,
                       p.title AS title,
                       s.sku_code,
                       p.description,
                       s.price_amount,
                       s.price_currency,
                       p.status
                  FROM sku s
                  JOIN product p ON p.id = s.product_id
                 WHERE s.id = ?
                """, (rs, rowNum) -> {
                    String title = rs.getString("title");
                    String skuCode = rs.getString("sku_code");
                    String description = rs.getString("description");
                    long price = rs.getLong("price_amount");
                    String currency = rs.getString("price_currency");
                    String status = rs.getString("status");
                    StringBuilder content = new StringBuilder()
                            .append("商品：").append(title).append('\n')
                            .append("SKU 编码：").append(skuCode).append('\n')
                            .append("价格：").append(String.format("%.2f", price / 100.0))
                            .append(' ').append(currency).append('\n')
                            .append("状态：").append("ACTIVE".equals(status) ? "在售" : status);
                    if (description != null && !description.isBlank()) {
                        content.append('\n').append("描述：").append(description.strip());
                    }
                    return new SyncResult(rs.getObject("id", UUID.class).toString(),
                            title, content.toString(), 1);
                }, skuId);

        if (rows.isEmpty()) {
            throw com.fullmall.platform.error.ApiException.notFound(
                    "SKU_NOT_FOUND", "SKU 不存在，无法同步知识：" + skuId);
        }
        SyncResult row = rows.get(0);
        List<KnowledgeChunk> chunks = new ArrayList<>();
        chunks.add(new KnowledgeChunk(UUID.randomUUID(), "PRODUCT", row.skuId(),
                row.title() + " · 价格与规格", row.content(), "sync"));
        store.replaceSource("PRODUCT", row.skuId(), chunks);
        return new SyncResult(row.skuId(), row.title(), row.content(), 1);
    }
}
