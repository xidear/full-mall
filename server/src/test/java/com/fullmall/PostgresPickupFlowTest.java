package com.fullmall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.fullmall.test.TestHttp;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.fullmall.test.PostgresTestDatabase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "spring.modulith.events.jdbc.schema-initialization.enabled=true",
                "app.payment.provider=mock",
                "app.demo.read-only=false"
        })
@ActiveProfiles({"test", "dev"})
class PostgresPickupFlowTest {

    private static final String SKU_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final String STORE_ID = "22222222-2222-2222-2222-222222222222";

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestDatabase database = PostgresTestDatabase.get();
        registry.add("spring.datasource.url", database::jdbcUrl);
        registry.add("spring.datasource.username", database::username);
        registry.add("spring.datasource.password", database::password);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @org.springframework.beans.factory.annotation.Value("${local.server.port}")
    int port;

    TestHttp rest;

    @org.junit.jupiter.api.BeforeEach
    void initHttp() {
        rest = new TestHttp(port);
    }

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void pickupOrderCanBePaidPickedAndCollected() {
        // 下单要求"真人"：为测试手机号建立会员（线上访客下单会经 OTP 自动建会员）
        jdbc.update("INSERT INTO member (id, member_no, name, phone, tier, points_balance,"
                + " stored_value_balance, currency) VALUES (?, 'M-PICKUP-1', '自提顾客', '13800000000',"
                + " 'STANDARD', 0, 0, 'CNY') ON CONFLICT (phone) DO NOTHING", UUID.randomUUID());

        String adminToken = loginAsAdmin();

        JsonNode products = get("/api/v1/products?locale=zh-CN&size=20");
        assertFalse(products.get("items").isEmpty(), "Seed data must expose at least one product");

        JsonNode order = post("/api/v1/checkout", Map.of(
                "items", List.of(Map.of("skuId", SKU_ID, "quantity", 2)),
                "fulfillmentMode", "PICKUP",
                "storeId", STORE_ID,
                "contact", Map.of("name", "测试顾客", "phone", "13800000000"),
                "legalAcceptanceIds", List.of()));
        String orderId = order.get("id").asText();
        assertEquals("PENDING_PAYMENT", order.get("status").asText());

        JsonNode payment = post("/api/v1/payments/mock/" + orderId + "/pay", null);
        assertEquals("PAID", payment.get("status").asText());

        JsonNode tasks = get("/store/v1/pick-tasks?storeId=" + STORE_ID);
        assertTrue(tasks.isArray() && !tasks.isEmpty(), "Pickup order must create a pick task");
        JsonNode task = tasks.get(0);
        JsonNode line = task.get("lines").get(0);
        String taskId = task.get("id").asText();
        String batchId = line.get("suggestedBatchId").asText();
        int quantity = line.get("quantity").asInt();

        JsonNode picked = post("/store/v1/pick-tasks/" + taskId + "/confirm", Map.of(
                "batchId", batchId,
                "quantity", quantity,
                "scannedCode", batchId));
        assertEquals("PICKED", picked.get("status").asText());

        // 后台/客服接口：凭员工身份查看履约，无需顾客手机号
        JsonNode adminView = get("/admin/v1/orders/" + orderId + "/fulfillments", adminToken);
        assertEquals(1, adminView.size());
        assertNotNull(adminView.get(0).get("pickupCode").asText());

        JsonNode fulfillments = get("/admin/v1/orders/" + orderId + "/fulfillments", adminToken);
        JsonNode fulfillment = fulfillments.get(0);
        assertEquals("READY_FOR_PICKUP", fulfillment.get("status").asText());
        String pickupCode = fulfillment.get("pickupCode").asText();
        assertNotNull(pickupCode);

        // 备货完成通知必须带自提码，且三个渠道都投递
        var notices = jdbc.queryForList(
                // 会员订单：IN_APP 落 MEMBER、SMS/WECHAT 落 CUSTOMER，因此按"非员工"统计
                "SELECT content FROM notification WHERE event_type = 'PICKUP_READY'"
                        + " AND recipient_type <> 'STAFF' ORDER BY channel",
                String.class);
        assertEquals(3, notices.size());
        assertTrue(notices.stream().allMatch(content -> content.contains(pickupCode)),
                "自提通知必须包含提货码");

        JsonNode pickup = post("/store/v1/pickups/" + pickupCode + "/verify", Map.of(
                "verificationType", "PICKUP_CODE",
                "verificationValue", pickupCode));
        assertEquals("COMPLETED", pickup.get("status").asText());

        // 订单已绑定会员：顾客接口要求会员身份，这里直接查库断言最终状态
        assertEquals("FULFILLED", jdbc.queryForObject(
                "SELECT status FROM orders WHERE id = ?::uuid", String.class, orderId));

        JsonNode batches = get("/admin/v1/inventory/batches?locationType=STORE&locationId="
                + STORE_ID + "&skuId=" + SKU_ID, adminToken);
        JsonNode storeBatch = null;
        for (JsonNode batch : batches) {
            if ("MILK-SH-20261012".equals(batch.get("batchNo").asText())) {
                storeBatch = batch;
                break;
            }
        }
        assertNotNull(storeBatch, "Store milk batch must exist");
        // 买 2 件鲜奶自动触发"买二赠一"：付费行 2 件 + 赠品行 1 件，共从该批次出库 3 件
        assertEquals(2, storeBatch.get("quantity").asInt());
        assertEquals(0, storeBatch.get("reservedQuantity").asInt());
    }

    private String loginAsAdmin() {
        JsonNode captcha = get("/admin/v1/auth/captcha");
        String[] operands = captcha.get("question").asText().replace("= ?", "").split("\\+");
        int answer = Integer.parseInt(operands[0].trim()) + Integer.parseInt(operands[1].trim());
        JsonNode login = post("/admin/v1/auth/login", Map.of(
                "username", "admin",
                "password", "Admin@123456",
                "captchaId", captcha.get("captchaId").asText(),
                "captchaAnswer", String.valueOf(answer)));
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
        ResponseEntity<JsonNode> response = rest.exchange(
                path,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class);
        assertTrue(response.getStatusCode().is2xxSuccessful(), "GET " + path + " failed: " + response.getBody());
        return response.getBody();
    }

    private JsonNode post(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<JsonNode> response = rest.exchange(
                path,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                JsonNode.class);
        assertTrue(response.getStatusCode().is2xxSuccessful(), "POST " + path + " failed: " + response.getBody());
        return response.getBody();
    }
}