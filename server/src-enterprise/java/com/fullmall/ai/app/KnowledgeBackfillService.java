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

    /**
     * 增量同步：找出"商品或 SKU 的 updated_at 晚于上次同步"的 SKU 重建知识。
     *
     * <p>为什么不用事件：改价、改图、改规格、改标题、上下架分散在多个入口，
     * 逐个埋点必然漏一个；`updated_at` 水位能覆盖**所有**写入路径（包括脚本/SQL 改的）。
     * 代价是延迟（默认 30 秒巡检一次），对客服场景足够。
     */
    public BackfillResult syncChanged() {
        List<UUID> changed = jdbc.query("""
                SELECT s.id
                  FROM sku s
                  JOIN product p ON p.id = s.product_id
                  LEFT JOIN knowledge_sync_state st ON st.sku_id = s.id
                 WHERE p.status = 'ACTIVE'
                   AND GREATEST(s.updated_at, p.updated_at, p.created_at)
                       > COALESCE(st.synced_at, TIMESTAMPTZ '1970-01-01')
                 ORDER BY s.updated_at
                 LIMIT 200
                """, (rs, rowNum) -> rs.getObject("id", UUID.class));

        int chunks = 0;
        for (UUID skuId : changed) {
            chunks += productSync.sync(skuId).chunks();
            jdbc.update("""
                    INSERT INTO knowledge_sync_state (sku_id, synced_at, updated_at)
                    VALUES (?, now(), now())
                    ON CONFLICT (sku_id) DO UPDATE SET synced_at = now(), updated_at = now()
                    """, skuId);
        }
        return new BackfillResult(changed.size(), chunks, store.size());
    }

    public boolean vectorMode() {
        return properties.isVectorRetrieval();
    }
}
