package com.fullmall.distribution.app;

import com.fullmall.catalog.app.CatalogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * 分销报表：按推广员汇总佣金（运营对账用）。
 *
 * <p>口径说明：
 * <ul>
 *   <li>金额单位是分，直接返回整数，避免前端二次换算出错；</li>
 *   <li>`payrollAmount` 是员工工资通道（不计入可提现）；`walletAmount` 是会员钱包通道；</li>
 *   <li>`availableAmount` 已扣掉 `consumed_amount`（提现消耗），因此是**真实可提**余额。</li>
 * </ul>
 */
@Service
public class DistributionReportService {

    private final JdbcTemplate jdbcTemplate;

    public DistributionReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record Row(UUID promoterId, String promoterType,
                      long orderAmount, long commissionAmount,
                      long frozenAmount, long availableAmount, long withdrawnAmount,
                      long payrollAmount, long walletAmount, long orderCount) {
    }

    public record Report(LocalDate from, LocalDate to, List<Row> rows, Row totals) {
    }

    public Report report(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "REPORT_RANGE_REQUIRED", "必须指定统计起止日期");
        }
        if (to.isBefore(from)) {
            throw com.fullmall.platform.error.ApiException.badRequest(
                    "REPORT_RANGE_INVALID", "结束日期不能早于开始日期");
        }
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Row> rows = jdbcTemplate.query("""
                SELECT promoter_staff_id, promoter_type,
                       SUM(order_amount) AS order_amount,
                       SUM(commission_amount) AS commission_amount,
                       SUM(CASE WHEN status = 'FROZEN' THEN commission_amount ELSE 0 END) AS frozen_amount,
                       SUM(CASE WHEN status = 'AVAILABLE'
                                THEN commission_amount - consumed_amount ELSE 0 END) AS available_amount,
                       SUM(CASE WHEN status = 'WITHDRAWN' THEN commission_amount ELSE 0 END) AS withdrawn_amount,
                       SUM(CASE WHEN payee_channel = 'PAYROLL' THEN commission_amount ELSE 0 END) AS payroll_amount,
                       SUM(CASE WHEN payee_channel = 'WALLET' THEN commission_amount ELSE 0 END) AS wallet_amount,
                       COUNT(DISTINCT order_id) AS order_count
                  FROM distribution_commission
                 WHERE created_at >= ? AND created_at < ?
                 GROUP BY promoter_staff_id, promoter_type
                 ORDER BY commission_amount DESC
                """, (rs, rowNum) -> new Row(
                rs.getObject("promoter_staff_id", UUID.class),
                rs.getString("promoter_type"),
                rs.getLong("order_amount"),
                rs.getLong("commission_amount"),
                rs.getLong("frozen_amount"),
                rs.getLong("available_amount"),
                rs.getLong("withdrawn_amount"),
                rs.getLong("payroll_amount"),
                rs.getLong("wallet_amount"),
                rs.getLong("order_count")), java.sql.Timestamp.from(start), java.sql.Timestamp.from(end));

        Row totals = rows.stream().reduce(
                new Row(null, null, 0, 0, 0, 0, 0, 0, 0, 0),
                (a, b) -> new Row(null, null,
                        a.orderAmount() + b.orderAmount(), a.commissionAmount() + b.commissionAmount(),
                        a.frozenAmount() + b.frozenAmount(), a.availableAmount() + b.availableAmount(),
                        a.withdrawnAmount() + b.withdrawnAmount(), a.payrollAmount() + b.payrollAmount(),
                        a.walletAmount() + b.walletAmount(), a.orderCount() + b.orderCount()));
        return new Report(from, to, rows, totals);
    }
}
