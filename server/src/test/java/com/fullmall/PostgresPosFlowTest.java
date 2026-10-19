package com.fullmall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=true",
                "app.payment.provider=mock",
                "app.demo.read-only=false"
        })
@ActiveProfiles({"test", "dev"})
class PostgresPosFlowTest {

    private static final String SKU_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final String STORE_ID = "22222222-2222-2222-2222-222222222222";
    private static final String MEMBER_PHONE = "13800000001";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    TestRestTemplate rest;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void posCombinesBenefitsChargesCashAndRefunds() {
        String adminToken = loginAsAdmin();
        int stockBefore = storeMilkStock(adminToken);

        String cashier = "测试收银员" + UUID.randomUUID().toString().substring(0, 6);
        JsonNode shift = post("/store/v1/pos/shifts", Map.of(
                "storeId", STORE_ID,
                "cashierName", cashier,
                "openingFloat", 20000,
                "currency", "CNY"), null);
        String shiftId = shift.get("shift").get("id").asText();

        JsonNode sale = post("/store/v1/pos/sales", Map.of(
                "storeId", STORE_ID,
                "shiftId", shiftId,
                "cashierName", cashier,
                "items", List.of(Map.of("skuId", SKU_ID, "quantity", 2))), UUID.randomUUID().toString());
        String saleId = sale.get("sale").get("id").asText();
        assertEquals(3980, sale.get("sale").get("payable").get("amount").asInt());

        JsonNode member = get("/store/v1/pos/members?query=" + MEMBER_PHONE);
        String memberId = member.get("member").get("id").asText();
        post("/store/v1/pos/sales/" + saleId + "/member", Map.of("memberId", memberId), null);

        JsonNode coupon = post("/store/v1/pos/sales/" + saleId + "/coupon", Map.of("code", "MILK5"), null);
        assertEquals(500, coupon.get("sale").get("couponDiscount").get("amount").asInt());

        JsonNode points = post("/store/v1/pos/sales/" + saleId + "/points", Map.of("points", 100), null);
        assertEquals(3380, points.get("sale").get("payable").get("amount").asInt());

        JsonNode tendered = post("/store/v1/pos/sales/" + saleId + "/tenders", Map.of(
                "type", "CASH",
                "amount", 3380,
                "cashTendered", 3380,
                "idempotencyKey", UUID.randomUUID().toString()), null);
        assertEquals(0, tendered.get("remainingAmount").get("amount").asInt());

        JsonNode completed = post("/store/v1/pos/sales/" + saleId + "/complete", null, null);
        assertEquals("COMPLETED", completed.get("sale").get("status").asText());
        assertEquals(33, completed.get("sale").get("pointsEarned").asInt());
        assertEquals(stockBefore - 2, storeMilkStock(adminToken));

        String saleLineId = completed.get("sale").get("lines").get(0).get("id").asText();
        JsonNode partiallyRefunded = post("/store/v1/pos/sales/" + saleId + "/refund-lines", Map.of(
                "items", List.of(Map.of("saleLineId", saleLineId, "quantity", 1)),
                "reason", "集成测试部分退款",
                "operatorName", cashier), null);
        assertEquals("PARTIALLY_REFUNDED", partiallyRefunded.get("sale").get("status").asText());
        assertEquals(stockBefore - 1, storeMilkStock(adminToken));

        JsonNode refunded = post("/store/v1/pos/sales/" + saleId + "/refund-lines", Map.of(
                "items", List.of(Map.of("saleLineId", saleLineId, "quantity", 1)),
                "reason", "集成测试剩余退款",
                "operatorName", cashier), null);
        assertEquals("REFUNDED", refunded.get("sale").get("status").asText());
        assertEquals(stockBefore, storeMilkStock(adminToken));

        JsonNode memberAfter = get("/store/v1/pos/members?query=" + MEMBER_PHONE);
        assertEquals(500, memberAfter.get("member").get("points").asInt());

        JsonNode openShift = get("/store/v1/pos/shifts/" + shiftId);
        int expectedCash = openShift.get("expectedCash").get("amount").asInt();
        assertEquals(20000, expectedCash);
        JsonNode closed = post("/store/v1/pos/shifts/" + shiftId + "/close",
                Map.of("actualCash", expectedCash), null);
        assertEquals("CLOSED", closed.get("shift").get("status").asText());

        JsonNode report = get("/admin/v1/pos/reports/daily?storeId=" + STORE_ID, adminToken);
        assertTrue(report.get("salesCount").asInt() >= 1);
        assertTrue(report.get("refundCount").asInt() >= 1);
        assertEquals(0, report.get("cashVariance").get("amount").asInt());
        assertTrue(report.get("tenderMix").size() > 0);
    }

    private int storeMilkStock(String adminToken) {
        JsonNode batches = get("/admin/v1/inventory/batches?locationType=STORE&locationId="
                + STORE_ID + "&skuId=" + SKU_ID, adminToken);
        int total = 0;
        for (JsonNode batch : batches) {
            total += batch.get("quantity").asInt();
        }
        return total;
    }

    private String loginAsAdmin() {
        JsonNode captcha = get("/admin/v1/auth/captcha");
        String[] operands = captcha.get("question").asText().replace("= ?", "").split("\\+");
        int answer = Integer.parseInt(operands[0].trim()) + Integer.parseInt(operands[1].trim());
        JsonNode login = post("/admin/v1/auth/login", Map.of(
                "username", "admin",
                "password", "Admin@123456",
                "captchaId", captcha.get("captchaId").asText(),
                "captchaAnswer", String.valueOf(answer)), null);
        return login.get("token").asText();
    }

    private JsonNode get(String path) {
        return get(path, null);
    }

    private JsonNode get(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        ResponseEntity<JsonNode> response = rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertTrue(response.getStatusCode().is2xxSuccessful(), "GET " + path + " failed: " + response.getBody());
        return response.getBody();
    }

    private JsonNode post(String path, Object body, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            headers.set("Idempotency-Key", idempotencyKey);
        }
        ResponseEntity<JsonNode> response = rest.exchange(
                path, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
        assertTrue(response.getStatusCode().is2xxSuccessful(), "POST " + path + " failed: " + response.getBody());
        return response.getBody();
    }
}