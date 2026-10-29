package com.fullmall.order.api;

import com.fullmall.platform.error.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 商业发票 / 报关要素导出（批次 E）。
 *
 * <p>缺 HS 编码、原产地或净重的行**不隐藏、不填 0**，而是在 {@code warnings} 里逐行列出——
 * 报关单缺项会被海关退单，静默补 0 等于制造一张看起来完整、其实错误的单子。
 */
@RestController
public class CustomsInvoiceController {

    private final JdbcTemplate jdbcTemplate;

    public CustomsInvoiceController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record InvoiceLine(String skuCode, String title, int quantity, String hsCode,
                             String originCountry, long unitValueAmount, String currency,
                             Integer netWeightGrams, Integer grossWeightGrams, long lineTotalAmount) {
    }

    public record Invoice(String orderNo, String currency, String destinationCountry,
                          List<InvoiceLine> lines, long merchandiseTotal, long shippingTotal,
                          long taxTotal, long invoiceTotal, List<String> warnings) {
    }

    @GetMapping("/admin/v1/orders/{orderId}/customs-invoice")
    public Invoice invoice(@PathVariable UUID orderId) {
        record Head(String orderNo, String currency, String destination) {
        }
        List<Head> head = jdbcTemplate.query("""
                SELECT order_no, currency, address_json->>'countryCode' AS destination
                  FROM orders WHERE id = ?
                """, (rs, rowNum) -> new Head(rs.getString("order_no"), rs.getString("currency"),
                rs.getString("destination")), orderId);
        if (head.isEmpty()) {
            throw ApiException.notFound("ORDER_NOT_FOUND", "订单不存在: " + orderId);
        }
        String orderNo = head.get(0).orderNo();
        String currency = head.get(0).currency();
        String destination = head.get(0).destination();

        List<String> warnings = new ArrayList<>();
        List<InvoiceLine> lines = jdbcTemplate.query("""
                SELECT ol.sku_code_snapshot, ol.title_snapshot, ol.quantity, ol.unit_price_amount,
                       s.hs_code, s.origin_country, s.net_weight_grams, s.gross_weight_grams
                  FROM order_line ol
                  LEFT JOIN sku s ON s.id = ol.sku_id
                 WHERE ol.order_id = ?
                 ORDER BY ol.title_snapshot
                """, (rs, rowNum) -> {
                    String hsCode = rs.getString("hs_code");
                    String origin = rs.getString("origin_country");
                    Integer net = rs.getObject("net_weight_grams") == null
                            ? null : rs.getInt("net_weight_grams");
                    Integer gross = rs.getObject("gross_weight_grams") == null
                            ? null : rs.getInt("gross_weight_grams");
                    String skuCode = rs.getString("sku_code_snapshot");
                    if (hsCode == null || hsCode.isBlank()) {
                        warnings.add(skuCode + " 缺少 HS 编码");
                    }
                    if (origin == null || origin.isBlank()) {
                        warnings.add(skuCode + " 缺少原产地");
                    }
                    if (net == null || net <= 0) {
                        warnings.add(skuCode + " 缺少净重");
                    }
                    long unit = rs.getLong("unit_price_amount");
                    int quantity = rs.getInt("quantity");
                    return new InvoiceLine(skuCode, rs.getString("title_snapshot"), quantity, hsCode,
                            origin, unit, currency, net, gross, unit * quantity);
                }, orderId);

        long merchandise = lines.stream().mapToLong(InvoiceLine::lineTotalAmount).sum();
        List<Long> shipping = jdbcTemplate.queryForList(
                "SELECT shipping_fee_amount FROM orders WHERE id = ?", Long.class, orderId);
        long shippingTotal = shipping.isEmpty() ? 0 : shipping.get(0);
        List<Long> taxes = jdbcTemplate.queryForList(
                "SELECT COALESCE(SUM(tax_amount), 0) FROM order_tax_line WHERE order_id = ?",
                Long.class, orderId);
        long taxTotal = taxes.isEmpty() ? 0 : taxes.get(0);
        if (taxTotal == 0) {
            warnings.add("该订单没有税行：可能未配置该辖区税率（报关时需人工确认）");
        }
        if (destination == null) {
            warnings.add("订单缺少收货国家：无法确定报关目的地");
        }
        return new Invoice(orderNo, currency, destination, lines, merchandise, shippingTotal, taxTotal,
                merchandise + shippingTotal, warnings);
    }
}
