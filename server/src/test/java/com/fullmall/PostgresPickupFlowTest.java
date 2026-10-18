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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
class PostgresPickupFlowTest {

    private static final String SKU_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final String STORE_ID = "22222222-2222-2222-2222-222222222222";

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
    void pickupOrderCanBePaidPickedAndCollected() {
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

        JsonNode fulfillments = get("/api/v1/orders/" + orderId + "/fulfillments");
        JsonNode fulfillment = fulfillments.get(0);
        assertEquals("READY_FOR_PICKUP", fulfillment.get("status").asText());
        String pickupCode = fulfillment.get("pickupCode").asText();
        assertNotNull(pickupCode);

        JsonNode pickup = post("/store/v1/pickups/" + pickupCode + "/verify", Map.of(
                "verificationType", "PICKUP_CODE",
                "verificationValue", pickupCode));
        assertEquals("COMPLETED", pickup.get("status").asText());

        JsonNode finalOrder = get("/api/v1/orders/" + orderId);
        assertEquals("FULFILLED", finalOrder.get("status").asText());

        JsonNode batches = get("/admin/v1/inventory/batches?locationType=STORE&locationId="
                + STORE_ID + "&skuId=" + SKU_ID);
        JsonNode storeBatch = null;
        for (JsonNode batch : batches) {
            if ("MILK-SH-20261012".equals(batch.get("batchNo").asText())) {
                storeBatch = batch;
                break;
            }
        }
        assertNotNull(storeBatch, "Store milk batch must exist");
        assertEquals(3, storeBatch.get("quantity").asInt());
        assertEquals(0, storeBatch.get("reservedQuantity").asInt());
    }

    private JsonNode get(String path) {
        ResponseEntity<JsonNode> response = rest.getForEntity(path, JsonNode.class);
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