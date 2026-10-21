package com.fullmall.ai.app;

import com.fullmall.ai.config.AiProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 全量重建商品知识：把在售 SKU 的标题、编码、价格、规格、描述写入知识库。
 *
 * <p>为什么必须有它：只导入几份政策文档，问商品问题时当然查不到——那证明不了方案可用。
 * 这个服务让"真实商品数据"进入检索范围，才能验证"AI 能不能回答真实问题"。
 */
@Service
public class KnowledgeBackfillService {

    private final JdbcTemplate jdbc;
    private final KnowledgeStore store;
    private final ProductKnowledgeSyncService productSync;
    private final AiProperties properties;

    public KnowledgeBackfillService(JdbcTemplate jdbc, KnowledgeStore store,
                                    ProductKnowledgeSyncService productSync, AiProperties properties) {
        this.jdbc = jdbc;
        this.store = store;
        this.productSync = productSync;
        this.properties = properties;
    }

    public record BackfillResult(int products, int chunks, int totalChunks) {
    }

    /** 重建所有 ACTIVE 商品；单个 SKU 失败会中断并报错（不静默跳过）。 */
    public BackfillResult rebuildProducts() {
        List<UUID> skuIds = jdbc.query("""
                SELECT s.id FROM sku s JOIN product p ON p.id = s.product_id
                 WHERE p.status = 'ACTIVE'
                 ORDER BY s.created_at
                """, (rs, rowNum) -> rs.getObject("id", UUID.class));

        int chunks = 0;
        for (UUID skuId : skuIds) {
            chunks += productSync.sync(skuId).chunks();
        }
        return new BackfillResult(skuIds.size(), chunks, store.size());
    }

    public boolean vectorMode() {
        return properties.isVectorRetrieval();
    }
}
