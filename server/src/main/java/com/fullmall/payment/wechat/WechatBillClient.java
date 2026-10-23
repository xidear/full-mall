package com.fullmall.payment.wechat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 微信支付交易账单下载（`GET /v3/bill/tradebill`）。
 *
 * <p>签名串的 URL 部分**必须包含 query**（`/v3/bill/tradebill?bill_date=...&bill_type=ALL`），
 * 这是微信 v3 的规则，写错会导致 401。
 *
 * <p>返回 CSV 文本：首行为表头，字段以 `,` 分隔（微信用反引号包裹字段）。
 * 本类只负责"拿到并拆分"，对账匹配由上层服务做。
 */
public class WechatBillClient {

    private final String merchantId;
    private final String serialNumber;
    private final PrivateKey privateKey;
    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public WechatBillClient(String merchantId, String serialNumber, String privateKeyPem,
                            String apiV3Key, String baseUrl) {
        if (isBlank(merchantId) || isBlank(serialNumber) || isBlank(privateKeyPem)
                || isBlank(apiV3Key) || isBlank(baseUrl)) {
            throw new IllegalStateException(
                    "账单下载需配置 merchant-id/serial-number/private-key/api-v3-key/base-url");
        }
        if (apiV3Key.getBytes(StandardCharsets.UTF_8).length != 32) {
            throw new IllegalStateException("APIv3 密钥必须是 32 字节");
        }
        this.merchantId = merchantId;
        this.serialNumber = serialNumber;
        this.privateKey = WechatV3Signer.privateKeyFromPem(privateKeyPem);
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public record Bill(String billDate, List<String> header, List<List<String>> rows) {
    }

    /** @param billDate yyyy-MM-dd（微信只允许查最近三个月） */
    public Bill download(String billDate) {
        if (isBlank(billDate) || !billDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("账单日期格式必须为 yyyy-MM-dd：" + billDate);
        }
        String path = "/v3/bill/tradebill?bill_date=" + billDate + "&bill_type=ALL";
        long ts = Instant.now().getEpochSecond();
        var signed = WechatV3Signer.sign("GET", path, "", merchantId, serialNumber,
                privateKey, ts, WechatV3Signer.randomNonce());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "text/csv")
                .header("Authorization", signed.authorization())
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("账单下载失败 HTTP " + response.statusCode()
                        + "：" + response.body());
            }
            String csv = response.body();
            if (csv == null || csv.isBlank()) {
                throw new IllegalStateException("账单内容为空：" + billDate);
            }
            List<String> lines = new ArrayList<>(List.of(csv.split("\\r?\\n")));
            List<String> header = splitLine(lines.get(0));
            List<List<String>> rows = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                if (!lines.get(i).isBlank()) {
                    rows.add(splitLine(lines.get(i)));
                }
            }
            return new Bill(billDate, header, rows);
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("账单下载请求失败：" + error.getMessage(), error);
        }
    }

    /** 微信账单字段用反引号包裹，如 `\`订单号\``；这里去掉反引号与首尾空白。 */
    private List<String> splitLine(String line) {
        List<String> cells = new ArrayList<>();
        for (String cell : line.split(",", -1)) {
            String value = cell.trim();
            if (value.length() >= 2 && value.startsWith("`") && value.endsWith("`")) {
                value = value.substring(1, value.length() - 1);
            }
            cells.add(value);
        }
        return cells;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
