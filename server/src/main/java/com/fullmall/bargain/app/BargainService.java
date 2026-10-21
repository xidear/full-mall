package com.fullmall.bargain.app;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 砍价：发起与助力。
 *
 * <p>约束（用户口径）：
 * <ul>
 *   <li>同一会员同一活动只能发起一次（数据库唯一约束兜底）；</li>
 *   <li>同一助力人对同一场次只能帮一次（唯一约束兜底，应用层只是友好提示）；</li>
 *   <li>砍价额**永不把价格砍到底价以下**，最后一刀自动收敛；</li>
 *   <li>达成（助力人够）后价格锁定为 `READY`，下单时按锁定价；</li>
 *   <li>达成后未支付**不释放库存**，由订单超时任务处理（本次活动不做额外动作）。</li>
 * </ul>
 */
@Service
public class BargainService {

    private final JdbcTemplate jdbc;

    public BargainService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Session(UUID id, UUID activityId, UUID memberId, long currentPrice,
                          int helperCount, int minHelpers, String status) {
    }

    @Transactional
    public Session start(UUID activityId, UUID memberId) {
        var activity = jdbc.query("""
                SELECT a.status, a.floor_price, a.cut_max, a.min_helpers, s.price_amount
                  FROM bargain_activity a JOIN sku s ON s.id = a.sku_id
                 WHERE a.id = ?
                """, rs -> rs.next() ? new Object[]{
                rs.getString("status"), rs.getLong("floor_price"), rs.getLong("cut_max"),
                rs.getInt("min_helpers"), rs.getLong("price_amount")} : null, activityId);
        if (activity == null) {
            throw ApiException.notFound("BARGAIN_ACTIVITY_NOT_FOUND", "砍价活动不存在：" + activityId);
        }
        if (!"ACTIVE".equals(activity[0])) {
            throw ApiException.conflict("BARGAIN_ACTIVITY_INACTIVE", "活动未开始或已结束");
        }
        long floorPrice = (Long) activity[1];
        long originalPrice = (Long) activity[4];
        if (originalPrice <= floorPrice) {
            throw ApiException.conflict("BARGAIN_PRICE_INVALID", "商品价格不高于底价，活动无法进行");
        }
        var existing = jdbc.query("SELECT id FROM bargain_session WHERE activity_id = ? AND member_id = ?",
                (rs, rowNum) -> rs.getObject("id", UUID.class), activityId, memberId);
        if (!existing.isEmpty()) {
            throw ApiException.conflict("BARGAIN_ALREADY_STARTED", "同一活动只能发起一次砍价");
        }
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO bargain_session (id, activity_id, member_id, current_price, helper_count,
                                             status, expires_at)
                VALUES (?, ?, ?, ?, 0, 'ONGOING', now() + make_interval(hours => (
                    SELECT expires_hours FROM bargain_activity WHERE id = ?
                )))
                """, id, activityId, memberId, originalPrice, activityId);
        return find(id);
    }

    /** 助力：锁场次 → 去重 → 计算砍价 → 达成置 READY。 */
    @Transactional
    public Session help(UUID sessionId, UUID helperMemberId) {
        record Row(UUID id, UUID activityId, UUID memberId, long currentPrice, int helperCount,
                   int minHelpers, long floorPrice, long cutMax, String status) {
        }
        var rows = jdbc.query("""
                SELECT bs.id, bs.activity_id, bs.member_id, bs.current_price, bs.helper_count,
                       ba.min_helpers, ba.floor_price, ba.cut_max, bs.status
                  FROM bargain_session bs JOIN bargain_activity ba ON ba.id = bs.activity_id
                 WHERE bs.id = ?
                 FOR UPDATE OF bs
                """, (rs, rowNum) -> new Row(rs.getObject("id", UUID.class),
                rs.getObject("activity_id", UUID.class), rs.getObject("member_id", UUID.class),
                rs.getLong("current_price"), rs.getInt("helper_count"), rs.getInt("min_helpers"),
                rs.getLong("floor_price"), rs.getLong("cut_max"), rs.getString("status")), sessionId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("BARGAIN_SESSION_NOT_FOUND", "砍价场次不存在：" + sessionId);
        }
        Row session = rows.get(0);
        if (!"ONGOING".equals(session.status())) {
            throw ApiException.conflict("BARGAIN_SESSION_CLOSED", "该场次已结束：" + session.status());
        }
        if (session.memberId().equals(helperMemberId)) {
            throw ApiException.conflict("BARGAIN_SELF_HELP", "不能给自己砍价");
        }
        long cut = Math.min(session.cutMax(), session.currentPrice() - session.floorPrice());
        if (cut <= 0) {
            throw ApiException.conflict("BARGAIN_AT_FLOOR", "已经到底价，无法继续砍价");
        }
        long nextPrice = Math.max(session.floorPrice(), session.currentPrice() - cut);
        int nextCount = session.helperCount() + 1;
        boolean ready = nextCount >= session.minHelpers();

        try {
            jdbc.update("""
                    INSERT INTO bargain_help (id, session_id, helper_member_id, cut_amount)
                    VALUES (?, ?, ?, ?)
                    """, UUID.randomUUID(), sessionId, helperMemberId, cut);
        } catch (org.springframework.dao.DuplicateKeyException duplicate) {
            throw ApiException.conflict("BARGAIN_ALREADY_HELPED", "该用户已经帮砍过了");
        }
        jdbc.update("""
                UPDATE bargain_session SET current_price = ?, helper_count = ?, status = ?
                 WHERE id = ?
                """, nextPrice, nextCount, ready ? "READY" : "ONGOING", sessionId);
        return find(sessionId);
    }

    /**
     * 场次绑定订单：只有 `READY`（人数已够）的场次可以下单，且只能绑定一次。
     *
     * <p>这里不改库存：下单走正常订单流程预占，未支付由**订单超时任务**释放
     * （用户口径：达成后未支付不立即释放，等超时）。
     */
    @Transactional
    public Session markOrdered(UUID sessionId, UUID orderId) {
        int updated = jdbc.update("""
                UPDATE bargain_session SET status = 'ORDERED', order_id = ?
                 WHERE id = ? AND status = 'READY'
                """, orderId, sessionId);
        if (updated != 1) {
            throw ApiException.conflict("BARGAIN_NOT_ORDERABLE",
                    "只有人数已够（READY）的砍价场次可以下单，且不能重复下单：" + sessionId);
        }
        return find(sessionId);
    }

    /**
     * 结算取价：仅 `READY`（人数已够）且属于本人的场次可参与结算。
     *
     * <p>返回的是**锁定价格**；下单时必须用它替代 SKU 原价，否则砍价等于白砍。
     * 校验失败一律拒绝，不回落原价（回落等于静默放弃优惠）。
     */
    public record Quote(UUID sessionId, UUID skuId, long unitPrice, String currency) {
    }

    public Quote quote(UUID sessionId, UUID memberId) {
        record Row(UUID memberId, String status, long price, UUID skuId, String currency) {
        }
        var rows = jdbc.query("""
                SELECT bs.member_id, bs.status, bs.current_price, ba.sku_id, ba.currency
                  FROM bargain_session bs JOIN bargain_activity ba ON ba.id = bs.activity_id
                 WHERE bs.id = ?
                """, (rs, rowNum) -> new Row(rs.getObject("member_id", UUID.class),
                rs.getString("status"), rs.getLong("current_price"),
                rs.getObject("sku_id", UUID.class), rs.getString("currency")), sessionId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("BARGAIN_SESSION_NOT_FOUND", "砍价场次不存在：" + sessionId);
        }
        Row row = rows.get(0);
        if (!row.memberId().equals(memberId)) {
            throw ApiException.conflict("BARGAIN_NOT_OWNER", "只能用自己的砍价场次下单");
        }
        if (!"READY".equals(row.status())) {
            throw ApiException.conflict("BARGAIN_NOT_READY",
                    "砍价尚未达成（当前状态 " + row.status() + "），不能按活动价下单");
        }
        return new Quote(sessionId, row.skuId(), row.price(), row.currency());
    }

    public Session find(UUID sessionId) {
        var rows = jdbc.query("""
                SELECT bs.id, bs.activity_id, bs.member_id, bs.current_price, bs.helper_count,
                       ba.min_helpers, bs.status
                  FROM bargain_session bs JOIN bargain_activity ba ON ba.id = bs.activity_id
                 WHERE bs.id = ?
                """, (rs, rowNum) -> new Session(rs.getObject("id", UUID.class),
                rs.getObject("activity_id", UUID.class), rs.getObject("member_id", UUID.class),
                rs.getLong("current_price"), rs.getInt("helper_count"), rs.getInt("min_helpers"),
                rs.getString("status")), sessionId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("BARGAIN_SESSION_NOT_FOUND", "砍价场次不存在：" + sessionId);
        }
        return rows.get(0);
    }
}
