package com.fullmall.ai.media;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 视频分镜：镜头（提示词 + 引导图 + 时长）逐个生成，结果回填到镜头。
 *
 * <p>为什么按镜头而非整段：短视频通常 3–6 个镜头，逐镜头可控、可单独重生成；
 * 引导图（首帧）来自素材库实拍图，保证商品外观一致。
 */
@Service
public class AiStoryboardService {

    private final JdbcTemplate jdbc;
    private final AiMediaService media;

    public AiStoryboardService(JdbcTemplate jdbc, AiMediaService media) {
        this.jdbc = jdbc;
        this.media = media;
    }

    public record Shot(UUID id, int sortOrder, String prompt, UUID guideAssetId, int durationSec,
                       UUID jobId, UUID assetId) {
    }

    public record Storyboard(UUID id, String title, UUID productId, UUID skuId, List<Shot> shots) {
    }

    public record ShotInput(String prompt, UUID guideAssetId, Integer durationSec) {
    }

    @Transactional
    public UUID create(String title, UUID productId, UUID skuId, List<ShotInput> shots, String actor) {
        if (title == null || title.isBlank()) {
            throw ApiException.badRequest("STORYBOARD_TITLE_REQUIRED", "分镜标题不能为空");
        }
        if (shots == null || shots.isEmpty()) {
            throw ApiException.badRequest("STORYBOARD_SHOTS_REQUIRED", "至少一个镜头");
        }
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO ai_storyboard (id, title, product_id, sku_id, created_by)
                VALUES (?, ?, ?, ?, ?)
                """, id, title.trim(), productId, skuId, actor);
        int sort = 0;
        for (ShotInput shot : shots) {
            if (shot.prompt() == null || shot.prompt().isBlank()) {
                throw ApiException.badRequest("STORYBOARD_SHOT_PROMPT_REQUIRED",
                        "第 " + (sort + 1) + " 个镜头缺少提示词");
            }
            int duration = shot.durationSec() == null ? 5 : shot.durationSec();
            if (duration < 1 || duration > 30) {
                throw ApiException.badRequest("STORYBOARD_SHOT_DURATION_INVALID",
                        "镜头时长必须在 1–30 秒，实际=" + duration);
            }
            jdbc.update("""
                    INSERT INTO ai_storyboard_shot (id, storyboard_id, sort_order, prompt,
                                                    guide_asset_id, duration_sec)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), id, sort++, shot.prompt().trim(),
                    shot.guideAssetId(), duration);
        }
        return id;
    }

    public Storyboard get(UUID id) {
        var rows = jdbc.query("""
                SELECT id, title, product_id, sku_id FROM ai_storyboard WHERE id = ?
                """, (rs, rowNum) -> new Object[]{
                rs.getObject("id", UUID.class), rs.getString("title"),
                rs.getObject("product_id"), rs.getObject("sku_id")}, id);
        if (rows.isEmpty()) {
            throw ApiException.notFound("STORYBOARD_NOT_FOUND", "分镜不存在：" + id);
        }
        Object[] head = rows.get(0);
        List<Shot> shots = jdbc.query("""
                SELECT id, sort_order, prompt, guide_asset_id, duration_sec, job_id, asset_id
                  FROM ai_storyboard_shot WHERE storyboard_id = ? ORDER BY sort_order
                """, (rs, rowNum) -> new Shot(
                rs.getObject("id", UUID.class), rs.getInt("sort_order"), rs.getString("prompt"),
                rs.getObject("guide_asset_id") == null ? null
                        : rs.getObject("guide_asset_id", UUID.class),
                rs.getInt("duration_sec"),
                rs.getObject("job_id") == null ? null : rs.getObject("job_id", UUID.class),
                rs.getObject("asset_id") == null ? null : rs.getObject("asset_id", UUID.class)),
                id);
        return new Storyboard((UUID) head[0], (String) head[1], (UUID) head[2], (UUID) head[3], shots);
    }

    /** 逐镜头入队视频任务；返回 jobId 列表（顺序与镜头一致）。 */
    @Transactional
    public List<UUID> generate(UUID storyboardId, String providerCode, String actor) {
        Storyboard storyboard = get(storyboardId);
        List<UUID> jobIds = new ArrayList<>();
        for (Shot shot : storyboard.shots()) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("duration", shot.durationSec());
            List<UUID> sources = shot.guideAssetId() == null ? List.of() : List.of(shot.guideAssetId());
            UUID jobId = media.enqueue(new AiMediaService.CreateJob(
                    MediaKind.VIDEO, providerCode, storyboard.productId(), storyboard.skuId(),
                    "VIDEO", shot.prompt(), null, params, sources), actor);
            jdbc.update("UPDATE ai_storyboard_shot SET job_id = ? WHERE id = ?", jobId, shot.id());
            jobIds.add(jobId);
        }
        return jobIds;
    }

    /** 完成任务后把资产回填到镜头（由 worker/前端轮询触发都可）。 */
    @Transactional
    public int syncAssets(UUID storyboardId) {
        int updated = 0;
        for (Shot shot : get(storyboardId).shots()) {
            if (shot.jobId() == null || shot.assetId() != null) {
                continue;
            }
            var assetIds = jdbc.query("""
                    SELECT asset_id FROM ai_gen_job WHERE id = ? AND status = 'SUCCEEDED'
                    """, (rs, rowNum) -> rs.getObject("asset_id", UUID.class), shot.jobId());
            if (!assetIds.isEmpty() && assetIds.get(0) != null) {
                updated += jdbc.update(
                        "UPDATE ai_storyboard_shot SET asset_id = ? WHERE id = ?",
                        assetIds.get(0), shot.id());
            }
        }
        return updated;
    }
}
