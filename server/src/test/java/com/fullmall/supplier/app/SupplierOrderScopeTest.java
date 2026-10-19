package com.fullmall.supplier.app;

import com.fullmall.fulfillment.app.FulfillmentService;
import com.fullmall.inventory.app.InventoryService;
import com.fullmall.platform.error.ApiException;
import com.fullmall.supplier.infra.SupplierRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * B1 负向测试：供应商 A 不能读取供应商 B 的订单（按 404 处理，不泄露存在性）。
 */
class SupplierOrderScopeTest {

    private static final UUID SUPPLIER_A = UUID.fromString("aaaaaaaa-0000-0000-0000-00000000000a");
    private static final UUID SUPPLIER_B = UUID.fromString("bbbbbbbb-0000-0000-0000-00000000000b");
    private static final UUID ORDER_OF_B = UUID.fromString("11111111-0000-0000-0000-00000000000b");

    private final SupplierRepository repository = mock(SupplierRepository.class);
    private final SupplierService service = new SupplierService(repository,
            mock(FulfillmentService.class), mock(InventoryService.class));

    @Test
    void supplierCannotReadAnotherSuppliersOrder() {
        // 仓储按 supplierId 过滤后，A 的列表里没有 B 的订单
        when(repository.findOrders(SUPPLIER_A, null)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getOrder(SUPPLIER_A, ORDER_OF_B))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("不属于当前供应商");
    }

    @Test
    void supplierCanReadOwnOrder() {
        when(repository.findOrders(SUPPLIER_A, null)).thenReturn(List.of(header()));
        when(repository.findOrderLines(ORDER_OF_B, SUPPLIER_A)).thenReturn(List.of());

        var order = service.getOrder(SUPPLIER_A, ORDER_OF_B);

        assertThat(order.fulfillmentId()).isEqualTo(ORDER_OF_B);
        assertThat(order.status()).isEqualTo("ALLOCATED");
    }

    private SupplierRepository.OrderHeader header() {
        return new SupplierRepository.OrderHeader(
                ORDER_OF_B, UUID.randomUUID(), "SO-1", "ALLOCATED",
                Instant.parse("2026-10-19T00:00:00Z"), null, null, null,
                "张三", "13800000001", "{}", 1000L, "CNY", "note");
    }
}